package bitwalking.bitwalking.steps.telephony_info;

import android.content.Context;
import android.os.AsyncTask;
import android.telephony.CellLocation;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import bitwalking.bitwalking.BitwalkingApp;
import bitwalking.bitwalking.ServicePreferences;
import bitwalking.bitwalking.registration_and_login.Utilities;
import bitwalking.bitwalking.steps.steps_info.StepsBulk;
import bitwalking.bitwalking.steps.steps_info.StepsTelephonyExtra;
import bitwalking.bitwalking.util.Globals;
import bitwalking.bitwalking.util.Logger;

/**
 * Created by Marcus on 5/25/16.
 */
public class TelephonyInfoManager {

    private static String TAG = TelephonyInfoManager.class.getSimpleName();
    private static int MAX_TELEPHONY_HISTORY_ITEMS = 200;
    private static int TELEPHONY_DATA_REQ_INTERVAL_MS = 2 * 60 * 1000;

    private Context _context;
    private TelephonyManager _telephonyManager;
    private ArrayList<TelephonyData> _telephonyHistory;
    private TelephonyData _lastTelephonyData = null;
    private boolean _running = false;
    private ServicePreferences _servicePrefs;
    private volatile ScheduledFuture<?> _telephonyFuture;
    private ScheduledThreadPoolExecutor _periodicExecutor;

    public TelephonyInfoManager(Context context) {
        _context = context;
        _telephonyHistory = new ArrayList<>();
        _periodicExecutor = new ScheduledThreadPoolExecutor(1);
    }

    private TelephonyManager getTelephonyManager() {
        if (null == _telephonyManager)
            _telephonyManager = (TelephonyManager) _context.getSystemService(Context.TELEPHONY_SERVICE);

        return _telephonyManager;
    }

    public TelephonyData getTelephonyInfo() {
        int mcc = 0, mnc = 0, cid = 0, lac = 0;

        CellLocation cellLoc = getTelephonyManager().getCellLocation();
        if(cellLoc instanceof GsmCellLocation) {
            GsmCellLocation cellLocation = (GsmCellLocation) cellLoc;
            cid = cellLocation.getCid();
            lac = cellLocation.getLac();
        }

        String networkOperator = getTelephonyManager().getNetworkOperator();

        String timestamp = Utilities.getTimestamp();

        if (TextUtils.isEmpty(networkOperator) == false) {
            try {
                mcc = Integer.parseInt(networkOperator.substring(0, 3));
            }
            catch (Exception e) {
                mcc = -1;
            }
            try {
                mnc = Integer.parseInt(networkOperator.substring(3));
            }
            catch (Exception e) {
                mnc = -1;
            }
        }

        return new TelephonyData(timestamp, mcc, mnc, cid, lac);
    }

    private void addNextTelephonyData() {
        TelephonyData currentData = getTelephonyInfo();

        // Check if the data is new
        if (null == _lastTelephonyData || !_lastTelephonyData.equals(currentData)) {
            synchronized (_telephonyHistory) {
                // Check if we have full capacity
                if (_telephonyHistory.size() >= MAX_TELEPHONY_HISTORY_ITEMS)
                    // Remove oldest one
                    _telephonyHistory.remove(0);

                // Add new and update latest
                _telephonyHistory.add(currentData);
                _lastTelephonyData = currentData;
            }
        }
    }

    private Runnable _getTelephonyDataRunnable = new Runnable() {
        @Override
        public void run() {
            if (_running) {
                try {
                    addNextTelephonyData();
                } catch (Exception e) {
                    BitwalkingApp.getInstance().trackException("failed adding telephony data", e);
                } finally {
                }
            }
        }
    };

    public void start() {
        _telephonyFuture = _periodicExecutor.scheduleAtFixedRate(
                _getTelephonyDataRunnable, 1000, TELEPHONY_DATA_REQ_INTERVAL_MS, TimeUnit.MILLISECONDS);
        _running = true;
    }

    public void stop() {
        _running = false;
        if (null != _telephonyFuture)
            _telephonyFuture.cancel(false);
    }

    public void addTelephonyInfoToSteps(ArrayList<StepsBulk> bulks, OnStepsTelephoneReadyListener listener) {
        new AddTelephonyDataToStepsTask(listener).execute(bulks);
    }

    private class AddTelephonyDataToStepsTask extends AsyncTask<ArrayList<StepsBulk>, Void, ArrayList<StepsBulk>> {

        OnStepsTelephoneReadyListener _listener;
        public AddTelephonyDataToStepsTask(OnStepsTelephoneReadyListener listener) {
            _listener = listener;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        protected ArrayList<StepsBulk> doInBackground(ArrayList<StepsBulk>... args) {
            ArrayList<StepsBulk> bulks = args[0];
            ArrayList<StepsBulk> finalBulks = new ArrayList<>();

            for (StepsBulk bulk : bulks) {
                ArrayList<TelephonyData> data = getTelephonyData(bulk.getStartTime(), bulk.getEndTime());
                finalBulks.addAll(addDataToSteps(bulk, data));
            }

            return finalBulks;
        }

        protected void onPostExecute(ArrayList<StepsBulk> stepsWithTelephony) {
            addDebugLog(String.format("done adding telephony, %d bulks", stepsWithTelephony.size()));
            if (null != _listener)
                _listener.onTelephonyStepsReady(stepsWithTelephony);
        }

        private ArrayList<StepsBulk> addDataToSteps(StepsBulk bulk, ArrayList<TelephonyData> data) {
            ArrayList<StepsBulk> finalSteps = new ArrayList<>();
            long stepsBefore = bulk.getTotalSteps(), stepsAfter = 0;

            // handle no locations
            if (null == data || 0 == data.size()) {
                finalSteps.add(bulk);
            }
            // Handle one location
            else if (1 == data.size()) {
                bulk.setTelephony(new StepsTelephonyExtra(data.get(0)));
                finalSteps.add(bulk);
            }
            // More than one location for the bulk - should be divided
            else {
                addDebugLog(String.format("Steps bulk has %d telephony data", data.size()));
                Logger.instance().Log(Logger.DEBUG, TAG, "before: " + bulk);

                for (int i = 0; i < data.size() - 1; ++i) {
                    StepsBulk subBulk = bulk.splitBulk(data.get(i).getTimestampLong(), 1);
                    if (null != subBulk) {
                        subBulk.setTelephony(new StepsTelephonyExtra(data.get(i)));
                        finalSteps.add(subBulk);
                    }
                }

                // Add last one

                bulk.setTelephony(new StepsTelephonyExtra(data.get(data.size() - 1)));
                finalSteps.add(bulk);

                Logger.instance().Log(Logger.DEBUG, TAG, "after: ");
                for (StepsBulk s : finalSteps) {
                    Logger.instance().Log(Logger.DEBUG, TAG, s.toString());
                }
            }

            for (StepsBulk b : finalSteps) {
                stepsAfter += b.getTotalSteps();
            }

            if (stepsAfter != stepsBefore)
                addDebugLog(String.format("steps lost during telephony add!!! [before=%d][after=%d]", stepsBefore, stepsAfter));

            return finalSteps;
        }

        private ArrayList<TelephonyData> getTelephonyData(long startTime, long endTime) {
            ArrayList<TelephonyData> data = new ArrayList<>();

            synchronized (_telephonyHistory) {
                for (TelephonyData t : _telephonyHistory) {
                    if (startTime <= t.getTimestampLong() && t.getTimestampLong() <= endTime)
                        data.add(t);
                }
            }

            if (null != _lastTelephonyData && data.size() == 0)
                data.add(_lastTelephonyData);

            return data;
        }
    }

    private ServicePreferences getServicePrefs() {
        if (null == _servicePrefs)
            _servicePrefs = new ServicePreferences(_context);

        return _servicePrefs;
    }

    private void addDebugLog(String log) {
        log = "Telephony: " + log;
        if (Globals.LOG_TO_FILE) {
            getServicePrefs().addLog(log);
        }

        Logger.instance().Log(Logger.DEBUG, TAG, log);
    }

    public interface OnStepsTelephoneReadyListener {
        void onTelephonyStepsReady(ArrayList<StepsBulk> steps);
    }
}
