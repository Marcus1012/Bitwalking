package bitwalking.bitwalking.steps.steps_sources;

import android.content.Context;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessStatusCodes;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Subscription;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.request.OnDataPointListener;
import com.google.android.gms.fitness.request.SensorRequest;
import com.google.android.gms.fitness.result.DailyTotalResult;
import com.google.android.gms.fitness.result.DataReadResult;
import com.google.android.gms.fitness.result.ListSubscriptionsResult;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.support.annotation.NonNull;

import bitwalking.bitwalking.BitwalkingApp;
import bitwalking.bitwalking.BuildConfig;
import bitwalking.bitwalking.util.Globals;
import bitwalking.bitwalking.ServicePreferences;
import bitwalking.bitwalking.steps.steps_info.StepsBulk;
import bitwalking.bitwalking.util.Logger;

/**
 * Created by Marcus on 1/27/16.
 */
public class GoogleFitnessSteps extends StepsSource implements OnDataPointListener {
    final static String TAG = GoogleFitnessSteps.class.getSimpleName();

    final static int LIVE_STEPS_INTERVAL_SECONDS = 5;
    final static int STEPS_READ_COUNT_TRIGGER = 100;
    final static int STEPS_READ_INTERVAL = (BuildConfig.DEBUG) ? 1 * 60 * 1000 : 10 * 60 * 1000; // 10 minutes
    final static long STEPS_READ_TIME_LIMIT = Math.max(STEPS_READ_INTERVAL * 2, 20 * 60 * 1000);
    GoogleApiClient _client;

    ServicePreferences _servicePrefs = null;
    boolean _running = false;
    long _lastUpdateTime;
//    Handler _updateStepsHandler;
    int _currentDailyTotal = 0;
    long _lastStepsReadTime = 0;

    public GoogleFitnessSteps(Context context) {
        super(context);
        _periodicExecutor = new ScheduledThreadPoolExecutor(2);
    }

    private static long STEPS_HISTORY_READ_INTERVAL = 1 * 60 * 60 * 1000; // 1 hour

    private volatile ScheduledFuture<?> _stepsFuture;
    ScheduledThreadPoolExecutor _periodicExecutor;
    Runnable _getStepsUpdate = new Runnable() {
        @Override
        public void run() {
            if (_running) {
                try {
                    synchronized (_stepsBeingHandled) {
                        if (0 < _lastStepsReadTime && new Date().getTime() > (_lastStepsReadTime + STEPS_READ_TIME_LIMIT)) {
                            // init read steps
                            _stepsBeingHandled = false;
                            addDebugLog("steps read took too much time, init");
                        }

                        if (!_stepsBeingHandled) {
                            _stepsBeingHandled = true;
                            _lastStepsReadTime = new Date().getTime();
                            getNextSteps();
                        }
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                    BitwalkingApp.getInstance().trackException(new Exception("failed to read steps from google", e));

                    _stepsBeingHandled = false;
                    addDebugLog("_getStepsUpdate exception: " + e.getMessage());
                }
                finally {
//                    _updateStepsHandler.postDelayed(_getStepsUpdate, STEPS_READ_INTERVAL);
                }
            }
        }
    };

    private class FitnessRequestCallback implements ResultCallback<Status> {
        String _requestInfo;

        public FitnessRequestCallback(String requestInfo) {
            _requestInfo = requestInfo;
        }

        @Override
        public void onResult(Status status) {
            if (status.isSuccess()) {
                if (status.getStatusCode() == FitnessStatusCodes.SUCCESS_ALREADY_SUBSCRIBED) {
                    addDebugLog(String.format("[%s]: SUCCESS_ALREADY_SUBSCRIBED.", _requestInfo));
                } else {
                    addDebugLog(String.format("[%s]: Successfully subscribed!", _requestInfo));
                }
            } else {
                addDebugLog(String.format("[%s]: Failed subscribing. [%d][%s]", _requestInfo, status.getStatusCode(), status.getStatusMessage()));
            }
        }
    }

    @Override
    public boolean startRecording() {
        if (null != _client && _client.isConnected()) {
            addDebugLog("start recording steps");

            Fitness.RecordingApi.subscribe(_client, DataType.TYPE_STEP_COUNT_DELTA)
                    .setResultCallback(new FitnessRequestCallback("Record steps delta"));
            _running = true;
            loadLastUpdateTime();

            _stepsFuture = _periodicExecutor.scheduleAtFixedRate(
                    _getStepsUpdate, 60 * 1000, STEPS_READ_INTERVAL, TimeUnit.MILLISECONDS);
        }
        else {
            if (null != _client) {
                addDebugLog("failed to startRecording, reconnecting");
                _client.reconnect();
            }
            else {
                addDebugLog("failed to startRecording, _client is null");
            }

            return false;
        }

        return true;
    }

    public void loadLastUpdateTime() {
        _lastUpdateTime = getServicePrefs().getStepsLastUpdateTime();
        if (_lastUpdateTime < 0) {
            addDebugLog(String.format("_lastUpdateTime < 0: %s", Globals.getFullDateFormat().format(_lastUpdateTime)));
            resetLastUpdateTime();
        }

        long minimalDateForUpdate = new Date().getTime() - (12 * 60 * 60 * 1000); // 12 hours
        if (_lastUpdateTime < minimalDateForUpdate) {
            String old = Globals.getUTCDateFormat().format(new Date(_lastUpdateTime));
            String now = Globals.getUTCDateFormat().format(new Date());
            addDebugLog(String.format("update time too early [old=%s] [now=%s]", old, now));
            _lastUpdateTime = minimalDateForUpdate;
        }

        addDebugLog(String.format("load last update time = %s", Globals.getFullDateFormat().format(_lastUpdateTime)));
    }

    public void resetLastUpdateTime() {
        _lastUpdateTime = new Date().getTime();
        getServicePrefs().storeStepsLastUpdateTime(_lastUpdateTime);

        addDebugLog(String.format("reset last update time = %s", Globals.getFullDateFormat().format(_lastUpdateTime)));
    }

    private ServicePreferences getServicePrefs() {
        if (null == _servicePrefs)
            _servicePrefs = new ServicePreferences(_context);

        return _servicePrefs;
    }

    private void listStepsDeltaSubscribers() {
        if (null == _client || !_client.isConnected())
            return;

        Fitness.RecordingApi.listSubscriptions(_client, DataType.TYPE_STEP_COUNT_DELTA)
                .setResultCallback(new ResultCallback<ListSubscriptionsResult>() {
                    @Override
                    public void onResult(ListSubscriptionsResult listSubscriptionsResult) {
                        Logger.instance().Log(Logger.DEBUG, TAG, "list of subscriptions:");
                        for (Subscription s : listSubscriptionsResult.getSubscriptions()) {
                            Logger.instance().Log(Logger.DEBUG, TAG, s.toDebugString());
//                            com.google.android.gms.fitness.data.DataSource ds = s.getDataSource();
//                            if (null != ds) {
//                                Logger.instance().Log(Logger.DEBUG, TAG, ds.getStreamName());
//                            }
                        }
                    }
                });
    }

    @Override
    public void init(Object extra) {
        if (null != extra)
            _client = (GoogleApiClient)extra;
    }

    @Override
    public void stopRecording() {
        _running = false;

        if (null != _client && _client.isConnected()) {
            Fitness.RecordingApi.unsubscribe(_client, DataType.TYPE_STEP_COUNT_DELTA);
        }

//        if (null != _updateStepsHandler)
//            _updateStepsHandler.removeCallbacks(_getStepsUpdate);
        if (null != _stepsFuture)
            _stepsFuture.cancel(true);

        addDebugLog("stop recording steps");
    }

    @Override
    public void setLiveStepsOn(boolean on) {
        Logger.instance().Log(Logger.DEBUG, TAG, "try set live steps - " + (on ? "on" : "off"));

        if (null != _client && _client.isConnected()) {
            Fitness.SensorsApi.remove(_client, this);

            if (on) {
                Fitness.SensorsApi.add(
                        _client,
                        new SensorRequest.Builder()
                                .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
//                                .setAccuracyMode(SensorRequest.ACCURACY_MODE_HIGH)
                                .setSamplingRate(LIVE_STEPS_INTERVAL_SECONDS, TimeUnit.SECONDS)
                                .build(),
                        this)
                        .setResultCallback(new FitnessRequestCallback("Live steps"));
            }

            _liveStepsOn = on;

            Logger.instance().Log(Logger.DEBUG, TAG, "set live steps - " + (on ? "on" : "off"));
        }
        else {
            Logger.instance().Log(Logger.INFO, TAG, "failed to set live steps , _client is null");
        }
    }

    @Override
    public void onDataPoint(DataPoint dataPoint) {
//        Logger.instance().Log(Logger.DEBUG, TAG, "got live steps from google api");

        // Handle live steps
        if (null != _liveStepsListener)
            _liveStepsListener.onLiveSteps(dataPoint.getValue(Field.FIELD_STEPS).asInt());
    }

    @Override
    public void getStepsNow() {
        //todo: maybe implement in the future...
    }

    private class MyStepsReadResultCallback implements ResultCallback<DataReadResult> {
        private class StepsReadInfo {
            long totalStartTime;
            long totalEndTime;
            long currentStartTime;
            long currentEndTime;
            ArrayList<StepsBulk> steps = new ArrayList<>();
        }

        private int _newDailyTotal;
        private StepsReadInfo _lastStepsReadInfo;

        public MyStepsReadResultCallback(long totalStartTime, long totalEndTime, int newDailyTotal) {
            _lastStepsReadInfo = new StepsReadInfo();

            _lastStepsReadInfo.totalEndTime = totalEndTime;
            _lastStepsReadInfo.totalStartTime = totalStartTime;
            _lastStepsReadInfo.currentStartTime = _lastStepsReadInfo.totalEndTime;
            _lastStepsReadInfo.currentEndTime = _lastStepsReadInfo.currentStartTime;

            _newDailyTotal = newDailyTotal;
        }

        @Override
        public void onResult(@NonNull DataReadResult dataReadResult) {
            if (null != dataReadResult) {
                if (dataReadResult.getStatus().isSuccess()) {
                    ArrayList<StepsBulk> steps = new ArrayList<>();

                    if (dataReadResult.getBuckets().size() > 0) {
                        for (Bucket bucket : dataReadResult.getBuckets()) {
                            List<DataSet> dataSets = bucket.getDataSets();
                            for (DataSet dataSet : dataSets) {
                                addStepsBulks(dataSet, steps);
                            }
                        }
                    } else if (dataReadResult.getDataSets().size() > 0) {
                        for (DataSet dataSet : dataReadResult.getDataSets()) {
                            addStepsBulks(dataSet, steps);
                        }
                    }

                    if (steps.size() > 0) {
                        _lastStepsReadInfo.steps.addAll(steps);
                    }

                    addDebugLog(String.format("added %d bulks", steps.size()));
                } else {
                    // log
                    addDebugLog(String.format("steps read failed: %d - %s", dataReadResult.getStatus().getStatusCode(), dataReadResult.getStatus().getStatusMessage()));
                    if (dataReadResult.getStatus().hasResolution()) {
                        addDebugLog("it has resolution :\\");
                    }
                }
            }
            else {
                addDebugLog("dataReadResult = null");
            }

            handleNext();
        }

        public void handleNext() {
            if (_lastStepsReadInfo.currentStartTime > _lastStepsReadInfo.totalStartTime) {
                _lastStepsReadInfo.currentEndTime = _lastStepsReadInfo.currentStartTime;
                _lastStepsReadInfo.currentStartTime = _lastStepsReadInfo.currentEndTime - STEPS_HISTORY_READ_INTERVAL;

                if (_lastStepsReadInfo.currentStartTime < _lastStepsReadInfo.totalStartTime)
                    _lastStepsReadInfo.currentStartTime = _lastStepsReadInfo.totalStartTime;

                if (Globals.LOG_TO_FILE) {
                    addDebugLog(String.format("handleNext: %s - %s",
                            Globals.getUTCDateFormat().format(new Date(_lastStepsReadInfo.currentStartTime)),
                            Globals.getUTCDateFormat().format(new Date(_lastStepsReadInfo.currentEndTime))));
                }

                try {
                    final PendingResult<DataReadResult> results = Fitness.HistoryApi.readData(
                            _client,
                            new DataReadRequest.Builder()
                                    .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                                    .bucketByTime(2, TimeUnit.MINUTES)
                                    .setTimeRange(_lastStepsReadInfo.currentStartTime, _lastStepsReadInfo.currentEndTime, TimeUnit.MILLISECONDS)
                                    .build());

                    results.setResultCallback(_stepsReadCallback, 5, TimeUnit.MINUTES);
                }
                catch (Exception e) {
                    addDebugLog(String.format("Failed to get steps history: " + e.getMessage()));
                }
            }
            else {
                // done
                finishRead();
            }
        }

        private void finishRead() {
            if (Globals.LOG_TO_FILE && null != _lastStepsReadInfo.steps) {
                for (StepsBulk bulk : _lastStepsReadInfo.steps) {
                    addDebugLog(bulk.toJsonString());
                }
            }

            if (_lastStepsReadInfo.steps.size() > 0) {
                dispatchNewSteps(_lastStepsReadInfo.steps);
                long latest = getLatestEndTime(_lastStepsReadInfo.steps);

                if (latest > 0) {
                    _lastUpdateTime = latest + 1;
                    getServicePrefs().storeStepsLastUpdateTime(_lastUpdateTime);
                }

                _currentDailyTotal = _newDailyTotal;
            }

            synchronized (_stepsBeingHandled) {
                _stepsBeingHandled = false;
            }

            if (_client.isConnected()) {
                _client.disconnect();
                addDebugLog("finishRead: disconnect");
            }
        }

        private long getLatestEndTime(ArrayList<StepsBulk> bulks) {
            long latest = 0;
            for (StepsBulk s : bulks) {
                if (s.getEndTime() > latest)
                    latest = s.getEndTime();
            }

            return latest;
        }

        private void addStepsBulks(DataSet dataSet, ArrayList<StepsBulk> steps) {
            for (DataPoint dp : dataSet.getDataPoints()) {
                try {
                    if ( dp.getValue(com.google.android.gms.fitness.data.Field.FIELD_STEPS).asInt() > 0) {
                        StepsBulk bulk = new StepsBulk(
                                dp.getStartTime(TimeUnit.MILLISECONDS),
                                dp.getEndTime(TimeUnit.MILLISECONDS),
                                dp.getValue(com.google.android.gms.fitness.data.Field.FIELD_STEPS).asInt());
                        bulk.setSource(dp.getOriginalDataSource().getStreamName());
                        steps.add(bulk);
                    }
                }
                catch (Exception e) {
                    BitwalkingApp.getInstance().trackException(e);
                }
            }
        }
    }

    MyStepsReadResultCallback _stepsReadCallback;
    Boolean _stepsBeingHandled = false;

    private void getNextSteps() {
        addDebugLog("getNextSteps: start");
        final long nowTime = new Date().getTime();
        if (_lastUpdateTime > nowTime) {
            Logger.instance().Log(Logger.INFO, TAG, "fix last update time - maybe the user was playing with the date...");
            _lastUpdateTime = nowTime - 1;
        }

        _client.reconnect();

        // Read daily total
        PendingResult<DailyTotalResult> result =
                Fitness.HistoryApi.readDailyTotal(_client, DataType.TYPE_STEP_COUNT_DELTA);
        result.setResultCallback(new ResultCallback<DailyTotalResult>() {
            @Override
            public void onResult(@NonNull DailyTotalResult dailyTotalResult) {
                Integer dailyTotal = -1;
                try {
                    if (dailyTotalResult.getStatus().isSuccess()) {
                        DataSet totalSet = dailyTotalResult.getTotal();
                        dailyTotal = totalSet.isEmpty()
                                ? 0
                                : totalSet.getDataPoints().get(0).getValue(Field.FIELD_STEPS).asInt();

                        addDebugLog("daily total = " + dailyTotal);
                        if (dailyTotal > 0 && (dailyTotal >= (_currentDailyTotal + STEPS_READ_COUNT_TRIGGER) || dailyTotal < _currentDailyTotal)) {

                            if (Globals.LOG_TO_FILE) {
                                DateFormat df = Globals.getFullDateFormat();
                                addDebugLog(String.format("handle steps between [%s]-[%s]",
                                        df.format(new Date(_lastUpdateTime)), df.format(new Date(nowTime))));
                            }

                            _stepsReadCallback = new MyStepsReadResultCallback(_lastUpdateTime, nowTime, dailyTotal);
                            _stepsReadCallback.handleNext();
                        } else {
                            // done
                            synchronized (_stepsBeingHandled) {
                                _stepsBeingHandled = false;
                                addDebugLog("getNextSteps: done - no new steps");
                            }
                        }
                    } else {
                        addDebugLog(String.format("getNextSteps: There was a problem getting the step count [%s][%d]",
                                dailyTotalResult.getStatus().getStatusMessage(), dailyTotalResult.getStatus().getStatusCode()));

                        // done
                        synchronized (_stepsBeingHandled) {
                            _stepsBeingHandled = false;
                            addDebugLog("getNextSteps: failed to get daily steps");
                        }
                    }
                } catch (Exception e) {
                    // done
                    synchronized (_stepsBeingHandled) {
                        _stepsBeingHandled = false;
                        addDebugLog("getNextSteps: Exception - " + e.getMessage());
                    }
                }
            }
        }, 2, TimeUnit.MINUTES);
    }

    private void addDebugLog(String log) {
        log = "Steps: " + log;
        if (Globals.LOG_TO_FILE) {
            getServicePrefs().addLog(log);
        }

        Logger.instance().Log(Logger.DEBUG, TAG, log);
    }
}
