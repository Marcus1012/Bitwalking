package bitwalking.bitwalking.steps.steps_sources;

import android.content.Context;
import android.location.Location;

import java.util.ArrayList;

import bitwalking.bitwalking.steps.steps_info.StepsBulk;

/**
 * Created by Marcus on 1/27/16.
 */
public abstract class StepsSource {
    protected OnNewStepsListener _stepsListener;
    protected OnLiveStepsListener _liveStepsListener;
    protected Context _context;
    protected boolean _liveStepsOn = false;

    public StepsSource(Context context) {
        _context = context;
    }

    public void setStepsListener(OnNewStepsListener listener) {
        _stepsListener = listener;
    }
    public void setLiveStepsListener(OnLiveStepsListener listener) {
        _liveStepsListener = listener;
    }

    protected void dispatchNewSteps(ArrayList<StepsBulk> newSteps) {
        if (null != _stepsListener) {
            _stepsListener.onNewSteps(newSteps);
        }
    }

    // Recording
    public abstract boolean startRecording();
    public abstract void init(Object extra);
    public abstract void stopRecording();

    // Location
    public abstract void setLiveStepsOn(boolean on);
    public boolean isLiveStepsOn() { return _liveStepsOn; }

    public abstract void getStepsNow();

    public abstract void loadLastUpdateTime();
    public abstract void resetLastUpdateTime();

    public interface OnNewStepsListener {
        void onNewSteps(ArrayList<StepsBulk> newStepBulks);
    }

    public interface OnLiveStepsListener {
        void onLiveSteps(int steps);
    }
}
