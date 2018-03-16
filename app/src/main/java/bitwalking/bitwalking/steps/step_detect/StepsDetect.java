package bitwalking.bitwalking.steps.step_detect;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.SystemClock;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import bitwalking.bitwalking.BitwalkingApp;
import bitwalking.bitwalking.ServicePreferences;
import bitwalking.bitwalking.steps.steps_info.StepsBulk;
import bitwalking.bitwalking.steps.steps_validation.StepsValidationParameters;
import bitwalking.bitwalking.util.Globals;
import bitwalking.bitwalking.util.Logger;

/**
 * Created by Marcus on 10/31/15.
 */
public class StepsDetect implements SensorEventListener {

    //region Members

    final static String TAG = StepsDetect.class.getSimpleName();
    final static int MAX_TIME_BETWEEN_SENSORS_EVENT = 30 * 60 * 1000; // Half hour

    // Sensors variables
    private SensorManager _sensorManager;
    private String _stepsSensorInfo;
    private Sensor _stepsSensor;
    boolean _firstStepsSinceLogin = false;
    Context _context;
    ServicePreferences _servicePrefs;
    StepsSample _aggregatedSteps;
    boolean _countingSteps = false;

    // Listener
    ArrayList<OnStepsDetectedListener> _listeners;

    Gson _gson;
    private class StepsSample {
        StepsSample() {
            this.stepsCount = 0;
            this.timestamp = 0;
        }

        StepsSample(int count, long timestamp) {
            this.stepsCount = count;
            this.timestamp = timestamp;
        }

        int stepsCount;
        long timestamp; // ms
    }

    //endregion

    //region Methods

    public StepsDetect(Context context, SensorManager sensorManager) {
        _sensorManager = sensorManager;
        _stepsSensor = _sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        _stepsSensorInfo = String.format("[%s][%s][%d]", _stepsSensor.getName(), _stepsSensor.getVendor(), _stepsSensor.getVersion());
        _listeners = new ArrayList<>();

        _context = context;
        _gson = new Gson();
        _servicePrefs = new ServicePreferences(_context);
    }

    public void startSampling() {
        if (!_countingSteps) {
            Logger.instance().Log(Logger.DEBUG, TAG, "start sampling");
            _sensorManager.registerListener(this, _stepsSensor, StepsValidationParameters.SAMPLING_INTERVAL_US);
        }

        _countingSteps = true;
    }

    public void stopSampling() {
        if (_countingSteps) {
            Logger.instance().Log(Logger.DEBUG, TAG, "stop sampling");
            _sensorManager.unregisterListener(this, _stepsSensor);
        }

        _countingSteps = false;
    }

    //endregion

    //region Sensors Handle Methods

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            long localTimestampMs;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                localTimestampMs = System.currentTimeMillis() + ((event.timestamp - SystemClock.elapsedRealtimeNanos()) / 1000000L);
            }
            else {
                localTimestampMs = System.currentTimeMillis() + (((event.timestamp / 1000000L) - SystemClock.elapsedRealtime()));
            }

            StepsSample newSteps = new StepsSample((int) event.values[0], localTimestampMs);

            if (_firstStepsSinceLogin) {
                _firstStepsSinceLogin = false;

                initStepsCounter(newSteps);
                addDebugLog(String.format("First steps after reset: %d", newSteps.stepsCount));
            }
            else {
                StepsSample lastSteps = getLastStepsCount();
                if (lastSteps == null) { // no last steps, act like reset
                    initStepsCounter(newSteps);
                    addDebugLog("No last steps, init");
                }
                else if (lastSteps.stepsCount > newSteps.stepsCount) { // Some how last steps is higher than new
                    initStepsCounter(newSteps);
                    addDebugLog(String.format("Last [%d] > new [%d]", lastSteps.stepsCount, newSteps.stepsCount));
                }
                else if (lastSteps.stepsCount < newSteps.stepsCount) { // All should be ok, count delta
                    int deltaSteps = newSteps.stepsCount - lastSteps.stepsCount;

                    if (deltaSteps <= 0) {
                        initStepsCounter(newSteps);
                        addDebugLog(String.format("new step is negative :\\ last = %d new %d",
                                lastSteps.stepsCount, newSteps.stepsCount));
                    }
                    else {
                        if (null != _aggregatedSteps) {
                            _aggregatedSteps.stepsCount += deltaSteps;
                        }
                        else {
                            _aggregatedSteps = new StepsSample(deltaSteps, lastSteps.timestamp);
                        }

                        long timeDelta = newSteps.timestamp - _aggregatedSteps.timestamp;
                        if (timeDelta > StepsValidationParameters.MIN_STEPS_TIME_FOR_WALK) { // Enough time for a walk
                            if (timeDelta > MAX_TIME_BETWEEN_SENSORS_EVENT) {
                                addDebugLog(String.format("ignore steps, delta time is too big [time=%d s] [ignored=%d]",
                                        TimeUnit.MILLISECONDS.toSeconds(timeDelta), _aggregatedSteps.stepsCount));
                            }
                            else if (_aggregatedSteps.stepsCount >= StepsValidationParameters.MIN_STEPS_FOR_WALK) {
                                addDebugLog(String.format("new system steps [new=%d] [total=%d]", _aggregatedSteps.stepsCount, newSteps.stepsCount));
                                dispatchSystemStep(
                                        _aggregatedSteps.stepsCount,
                                        _aggregatedSteps.timestamp,
                                        newSteps.timestamp);
                            }
                            else {
                                addDebugLog(String.format("not enough steps, ignore [ignored=%d] [total=%d]", _aggregatedSteps.stepsCount, newSteps.stepsCount));
                            }

                            initStepsCounter(newSteps);
                        } else {
                            // Nothing, wait for enough steps
                            updateLastStepsCount(newSteps);
                        }
                    }
                }
            }
        }
    }

    private void initStepsCounter(StepsSample initSample) {
        updateLastStepsCount(initSample);
        _aggregatedSteps = new StepsSample(0, initSample.timestamp);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private static final String LAST_STEPS_COUNT_PREFS = "last_steps_count";
    private StepsSample getLastStepsCount() {
        SharedPreferences sharedPrefs = _context.getSharedPreferences(LAST_STEPS_COUNT_PREFS, Context.MODE_PRIVATE);

        StepsSample last = null;
        try {
            String stepsJson = sharedPrefs.getString("steps", "");
            last = _gson.fromJson(stepsJson, StepsSample.class);
        } catch (Exception e) {
            BitwalkingApp.getInstance().trackException("getLastStepsCount failed", e);
            addDebugLog("getLastStepsCount failed, init count: " + e. getMessage());
        }

        return last;
    }

    private void updateLastStepsCount(StepsSample steps) {
        SharedPreferences sharedPrefs = _context.getSharedPreferences(LAST_STEPS_COUNT_PREFS, Context.MODE_PRIVATE);
        String stepsJson = _gson.toJson(steps);
        sharedPrefs.edit().putString("steps", stepsJson).apply();
    }

    public void resetCounting() {
        _firstStepsSinceLogin = true;
        addDebugLog("resetCounting: _firstStepsSinceLogin = true");
    }

    private void addDebugLog(String log) {
        if (Globals.LOG_TO_FILE) {
            _servicePrefs.addLog("StepsSensor: " + log);
        }

        Logger.instance().Log(Logger.DEBUG, TAG, log);
    }

    //endregion

    //region Samples Dispatch

    public void addListener(OnStepsDetectedListener listener) {
        if (listener != null && !_listeners.contains(listener)) {
            _listeners.add(listener);
        }
    }

    public void removeListener(OnStepsDetectedListener listener) {
        if (listener != null && _listeners.contains(listener)) {
            _listeners.remove(listener);
        }
    }

    private void dispatchSystemStep(int steps, long startTimestamp, long endTimestamp) {
        StepsBulk newStepsBulk = new StepsBulk(startTimestamp, endTimestamp, steps);
        newStepsBulk.setSource(_stepsSensorInfo);
        for (OnStepsDetectedListener l : _listeners) {
            if (l != null)
                l.onSystemSteps(newStepsBulk);
        }
    }

    //endregion
}