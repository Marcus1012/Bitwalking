package bitwalking.bitwalking.steps.location;

import android.content.Context;
import android.location.Location;

import java.util.ArrayList;

import bitwalking.bitwalking.steps.steps_info.StepsBulk;

/**
 * Created by Marcus on 2/1/16.
 */
public abstract class LocationSource {
    Context _context;

    protected LocationSource(Context context) {
        _context = context;
    }

    public abstract void init(Object extra);
    public abstract void start();
    public abstract void stop();
    public abstract void addLocationToSteps(StepsBulk bulk, StepsWithLocationListener listener);
    public abstract Location getLastKnownLocation();

    public interface StepsWithLocationListener {
        void onStepsLocationReady(ArrayList<StepsBulk> steps);
    }
}
