package bitwalking.bitwalking.remote_service.server_update;

import android.content.Context;

import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import bitwalking.bitwalking.BitwalkingApp;
import bitwalking.bitwalking.BuildConfig;
import bitwalking.bitwalking.ServicePreferences;
import bitwalking.bitwalking.remote_service.ServiceInitInfo;
import bitwalking.bitwalking.server.api.ServerApi;
import bitwalking.bitwalking.server.requests.UpdateUserSteps;
import bitwalking.bitwalking.server.responses.UserTodayResponse;
import bitwalking.bitwalking.steps.steps_info.StepsBulk;
import bitwalking.bitwalking.remote_service.UserTodayListener;
import bitwalking.bitwalking.steps.LogSteps;
import bitwalking.bitwalking.util.Globals;
import bitwalking.bitwalking.util.Logger;

/**
 * Created by Marcus on 12/14/15.
 */
public class UpdateServerThread implements Runnable {
    private final String TAG = UpdateServerThread.class.getSimpleName();

    Context _context;
    ServicePreferences _servicePrefs;
    boolean _isRunning = false;

    final int _STEPS_UPDATE_INTERVAL =  (BuildConfig.DEBUG) ? 2 * 60 * 1000 : 10 * 60 * 1000; // 20 minutes in milliseconds
    final int _TODAY_UPDATE_INTERVAL =  (BuildConfig.DEBUG) ? 1 * 60 * 1000 : 12 * 60 * 1000; // 30 minutes in milliseconds

    // User steps update
    private volatile ScheduledFuture<?> _todayFuture;
    ScheduledThreadPoolExecutor _todayPeriodicExecutor;
    private volatile ScheduledFuture<?> _stepsFuture;
    ScheduledThreadPoolExecutor _stepsPeriodicExecutor;

    UserTodayListener _todayListener;

    public UpdateServerThread(Context context, UserTodayListener todayListener) {
        _context = context;
        _todayListener = todayListener;
        _isRunning = false;
        _todayPeriodicExecutor = new ScheduledThreadPoolExecutor(1);
        _stepsPeriodicExecutor = new ScheduledThreadPoolExecutor(1);
    }

    private ServicePreferences getServicePrefs() {
        if (null == _servicePrefs)
            _servicePrefs = new ServicePreferences(_context);

        return _servicePrefs;
    }

    private ServiceInitInfo getServiceInfo() {
        return getServicePrefs().getServiceInitInfo();
    }

    Runnable _updateStepsTask = new Runnable() {
        public void run() {
            try {
                // steps update
                if (Globals.LOG_TO_FILE) {
                    getServicePrefs().addLog("update server with new steps");
                }

                // steps update
                sendStepsUpdate();
            }
            catch (Exception e) {
                BitwalkingApp.getInstance().trackException("_updateStepsTask", e);
            }
            finally {
            }
        }
    };

    Runnable _updateTodayTask = new Runnable() {
        public void run() {
            try {
                Logger.instance().Log(Logger.DEBUG, TAG, "updating today runnable");
                // today update
                refreshToday();
            }
            catch (Exception e) {
                BitwalkingApp.getInstance().trackException("_updateTodayTask", e);
            }
            finally {
            }
        }
    };

    @Override
    public void run() {
        if (!_isRunning) {
            _stepsFuture = _stepsPeriodicExecutor.scheduleAtFixedRate(
                    _updateStepsTask, _STEPS_UPDATE_INTERVAL, _STEPS_UPDATE_INTERVAL, TimeUnit.MILLISECONDS);
            _todayFuture = _todayPeriodicExecutor.scheduleAtFixedRate(
                    _updateTodayTask, _TODAY_UPDATE_INTERVAL, _TODAY_UPDATE_INTERVAL, TimeUnit.MILLISECONDS);

            _isRunning = true;
        }
    }

    public void stopUpdate() {
        if (_isRunning) {
            if (null != _stepsFuture)
                _stepsFuture.cancel(false);
            if (null != _todayFuture)
                _todayFuture.cancel(false);

            _isRunning = false;
        }
    }

    public void refreshToday() {
        sendTodayUpdateRequest();
    }

    //endregion

    //region today update request

    private void sendTodayUpdateRequest() {
        ServerApi.getToday(
                getServiceInfo().userMsisdn,
                getServiceInfo().userSecret,
                new ServerApi.MiningListener() {
                    @Override
                    public void onMining(Date start, UserTodayResponse.MiningInfo miningInfo) {
                        if (null != miningInfo)
                            _todayListener.onUserTodayUpdate(miningInfo.mining);
                    }
                });
    }

    //endregion

    //region Steps update

    private boolean updateStepBulk(ArrayList<StepsBulk> bulks) {
        boolean updated = false;

        if (Globals.LOG_TO_FILE) {
            final GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.registerTypeAdapter(StepsBulk.class, new StepsBulk.StepsBulkSerializer());
            String log = gsonBuilder.setPrettyPrinting().create().toJson(bulks);
            getServicePrefs().addSteps(log);
        }

        int resCode = -1;
        try {
            resCode = ServerApi.sendStepsNow(
                    getServiceInfo().userMsisdn,
                    getServiceInfo().userSecret,
                    new UpdateUserSteps(bulks));

            if (200 == resCode)
                updated = true;
        } catch (Exception e) {
            BitwalkingApp.getInstance().trackException(e);
        }

        if (Globals.LOG_TO_FILE) {
            if (updated)
                getServicePrefs().addSteps("---------- true ----------");
            else {
                getServicePrefs().addSteps(
                        String.format("{ code: %d, msisdn: %s, secret: %s }",
                                resCode,
                                null!=getServiceInfo().userMsisdn?getServiceInfo().userMsisdn:"null",
                                null!=getServiceInfo().userSecret?getServiceInfo().userSecret:"null")); //fix crash #11
                getServicePrefs().addSteps("---------- false ----------");
            }
        }

        return updated;
    }

    public void sendStepsUpdate() {
        final ArrayList<StepsBulk> stepsToUpdate = LogSteps.INSTANCE.getNextStepsLog(_context);
        if (stepsToUpdate.size() > 0) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (!updateStepBulk(stepsToUpdate)) {
                        Logger.instance().Log(Logger.INFO, TAG, "failed to update bulks");
                        if (Globals.LOG_TO_FILE)
                            getServicePrefs().addSteps("failed to update bulks");
                    } else {
                        Logger.instance().Log(Logger.INFO, TAG, "bulks successfully updated");
                        LogSteps.INSTANCE.clearPendingSteps(_context);

                        if (Globals.LOG_TO_FILE)
                            getServicePrefs().addSteps("bulks successfully updated, clear pending");
                    }
                }
            }).start();
        }
        else {
            if (Globals.LOG_TO_FILE)
                getServicePrefs().addSteps(Globals.getUTCDateFormat().format(new Date()) + ": no steps to update");
            LogSteps.INSTANCE.clearPendingSteps(_context);
        }
    }

    public boolean sendStepsUpdateNow() {
        ArrayList<StepsBulk> stepsToUpdate = LogSteps.INSTANCE.getNextStepsLog(_context);

        if (stepsToUpdate.size() > 0) {
            do {
                if (!updateStepBulk(stepsToUpdate)) {
                    Logger.instance().Log(Logger.INFO, TAG, "failed to update bulks");
                    if (Globals.LOG_TO_FILE)
                        getServicePrefs().addSteps("failed to update bulks");

                    return false;
                } else {
                    Logger.instance().Log(Logger.INFO, TAG, "bulks successfully updated");
                    LogSteps.INSTANCE.clearPendingSteps(_context);

                    if (Globals.LOG_TO_FILE)
                        getServicePrefs().addSteps("bulks successfully updated, clear pending");
                    stepsToUpdate = LogSteps.INSTANCE.getNextStepsLog(_context);
                }
            } while (stepsToUpdate.size() > 0);
        }
        else {
            if (Globals.LOG_TO_FILE)
                getServicePrefs().addSteps(Globals.getUTCDateFormat().format(new Date()) + ": no steps to update (now)");
            LogSteps.INSTANCE.clearPendingSteps(_context);
        }

        return true;
    }

    //endregion
}
