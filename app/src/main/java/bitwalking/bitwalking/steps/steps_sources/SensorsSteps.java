package bitwalking.bitwalking.steps.steps_sources;

import android.content.Context;
import android.hardware.SensorManager;
import android.location.Location;

import java.util.ArrayList;
import java.util.Date;

import bitwalking.bitwalking.ServicePreferences;
import bitwalking.bitwalking.steps.steps_info.StepsBulk;
import bitwalking.bitwalking.steps.step_detect.OnStepsDetectedListener;
import bitwalking.bitwalking.steps.step_detect.StepsDetect;
import bitwalking.bitwalking.steps.UserStepsData;
import bitwalking.bitwalking.steps.steps_validation.StepsValidationParameters;
import bitwalking.bitwalking.util.Logger;

/**
 * Created by Marcus on 1/27/16.
 */
public class SensorsSteps extends StepsSource implements OnStepsDetectedListener {

    final static String TAG = SensorsSteps.class.getSimpleName();

    // Steps Detect
    StepsDetect _stepsDetect;

    ServicePreferences _servicePrefs;

    public SensorsSteps(Context context) {
        super(context);

        _servicePrefs = new ServicePreferences(_context);
        // init acc and gyro filter
        Logger.instance().Log(Logger.DEBUG, TAG, "new step detect");

        _stepsDetect = new StepsDetect(_context, (SensorManager)_context.getSystemService(Context.SENSOR_SERVICE));
    }

    @Override
    public boolean startRecording() {
        boolean started = false;
        if (null != _stepsDetect) {
            Logger.instance().Log(Logger.DEBUG, TAG, "startRecording sampling sensors");
            _stepsDetect.addListener(this);
            _stepsDetect.startSampling();
            started = true;
        }

        return started;
    }

    @Override
    public void init(Object extra) {
        // init steps verification
//        _stepsValidation = new StepsValidation();
    }

    @Override
    public void stopRecording() {
        Logger.instance().Log(Logger.DEBUG, TAG, "stopRecording sampling sensors");
//        _stepsValidation.removeListener(this);
        _stepsDetect.stopSampling();
//        _stepsDetect.removeListener(_stepsValidation);
        _stepsDetect.removeListener(this);
    }

    @Override
    public void setLiveStepsOn(boolean on) {
        _liveStepsOn = on;
    }

    @Override
    public void onSystemSteps(StepsBulk steps) {
        // live steps
        if (_liveStepsOn && null != _liveStepsListener)
            _liveStepsListener.onLiveSteps(steps.getTotalSteps());

        if (null != _stepsListener) {
            ArrayList<StepsBulk> singleBulk = new ArrayList<>();
            singleBulk.add(steps);
            dispatchNewSteps(singleBulk);
        }
    }

    public void resetLastUpdateTime() {
        _stepsDetect.resetCounting();
    }

    @Override
    public void getStepsNow() {

    }

    @Override
    public void loadLastUpdateTime() {

    }
}
