package bitwalking.bitwalking.steps;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Location;

import com.google.android.gms.common.api.GoogleApiClient;

import java.util.ArrayList;
import java.util.Date;

import bitwalking.bitwalking.BitwalkingApp;
import bitwalking.bitwalking.steps.location.SimpleLocation;
import bitwalking.bitwalking.steps.steps_info.StepsLocationExtra;
import bitwalking.bitwalking.steps.steps_sources.GoogleFitnessSteps;
import bitwalking.bitwalking.steps.steps_sources.SensorsSteps;
import bitwalking.bitwalking.util.Globals;
import bitwalking.bitwalking.ServicePreferences;
import bitwalking.bitwalking.steps.location.LocationSource;
import bitwalking.bitwalking.steps.steps_info.StepsBulk;
import bitwalking.bitwalking.steps.steps_sources.StepsSource;
import bitwalking.bitwalking.steps.steps_validation.StepsValidation;
import bitwalking.bitwalking.steps.steps_validation.StepsValidationParameters;
import bitwalking.bitwalking.steps.steps_validation.VerifiedStepsListener;
import bitwalking.bitwalking.steps.telephony_info.TelephonyInfoManager;
import bitwalking.bitwalking.util.Logger;

/**
 * Created by Marcus on 1/27/16.
 */
public class StepsManager implements VerifiedStepsListener, StepsSource.OnLiveStepsListener, LocationSource.StepsWithLocationListener,
                                     TelephonyInfoManager.OnStepsTelephoneReadyListener {

    final static String TAG = StepsManager.class.getSimpleName();
    Context _context;
    ServicePreferences _servicePrefs;

    // Steps Source
    StepsSource _stepsSource;

    StepsSource _defaultStepsSource;
//    StepsSource _misfitDeviceSource;

    // Steps Verification
    StepsValidation _stepsValidation;

    // Location Source
    ArrayList<StepsBulk> _bulksPendingForLocation;
    LocationSource _locationSource;

    // Telephony
    TelephonyInfoManager _telephony;

    // Listeners
    OnDeviceStepsListener _stepsListener;

    GoogleApiClient _client;

    boolean _isRunning = false;

    //region Ctor

    public StepsManager(Context context, GoogleApiClient client) {
        _client = client;
        _context = context;
        _bulksPendingForLocation = new ArrayList<>();

        // Validation
        _stepsValidation = new StepsValidation();
        _stepsValidation.addListener(this);

        // Count

        // Google steps count
        if (null != ((SensorManager)_context.getSystemService(Context.SENSOR_SERVICE)).getDefaultSensor(Sensor.TYPE_STEP_COUNTER)) {
            _defaultStepsSource = new SensorsSteps(_context);
        }
        else {
            _defaultStepsSource = new GoogleFitnessSteps(_context);
        }
        // Misfit steps device
//        _misfitDeviceSource = new MisfitSteps(_context);

        // Set default steps source
//        setStepsSource(_defaultStepsSource);

        _stepsSource = _defaultStepsSource;
        _stepsSource.setStepsListener(_stepsValidation);
        _stepsSource.setLiveStepsListener(this);
        _stepsSource.loadLastUpdateTime();

        // Location
//        _locationSource = new GoogleFitnessLocation(_context);
        _locationSource = new SimpleLocation(_context);

        // Telephony
        _telephony = new TelephonyInfoManager(_context);
    }

    //endregion

    //region Public Methods

    public void setStepsSource(int source) {

        setStepsSource(_defaultStepsSource);

//        switch (source) {
//            case Globals.STEPS_SOURCE_PHONE: {
//                setStepsSource(_defaultStepsSource);
//                break;
//            }
//            case Globals.STEPS_SOURCE_MISFIT: {
//                setStepsSource(_misfitDeviceSource);
//                break;
//            }
//            default: break;
//        }
    }

    private void setStepsSource(StepsSource source) {
        if (source != _stepsSource) {
            if (null != _stepsSource)
                _stepsSource.stopRecording();
            _stepsSource = source;
            _stepsSource.setStepsListener(_stepsValidation);
            _stepsSource.setLiveStepsListener(this);
            _stepsSource.startRecording();
            _stepsSource.loadLastUpdateTime();
        }
    }

    public boolean isRunning() { return _isRunning; }

    public void stopRecording() {
        _isRunning = false;

        _stepsSource.stopRecording();
        _locationSource.stop();
        _telephony.stop();
    }

    public void init() {
//        _misfitDeviceSource.init(new ServicePreferences(_context).getMisfitDeviceInfo());
        _defaultStepsSource.init(_client);
        _locationSource.init(_client);
    }

    public void startRecording() {
        _isRunning = _stepsSource.startRecording();
        _locationSource.start();
        _telephony.start();

//        _isRunning = true;
    }

    public void resetStepsStartTime() {
        if (null != _stepsSource)
            _stepsSource.resetLastUpdateTime();
    }

    public void setLiveStepsOn(boolean on) {
        _stepsSource.setLiveStepsOn(on);
    }

    public boolean isLiveStepsOn() { return _stepsSource.isLiveStepsOn(); }

    public void getStepsNow() {
        _stepsSource.getStepsNow();
    }

    public void setStepsListener(OnDeviceStepsListener listener) {
        _stepsListener = listener;
    }

    //endregion

    //region Steps Handle

    @Override
    public synchronized void onVerifiedSteps(StepsBulk bulk) {
        Logger.instance().Log(Logger.DEBUG, TAG, String.format("Got new verified steps, check limits: [%d]", bulk.getTotalSteps()));
        Logger.instance().Log(Logger.DEBUG, TAG, String.format("new steps - verified=[%d]", bulk.getTotalSteps()));

        // Update listener
        if (null != _locationSource) {
            if (bulk.getTotalSteps() < StepsValidationParameters.MIN_STEPS_FOR_WALK)
                addDebugLog("==== we got steps bulk with less than 10 steps ====");
            else if (bulk.getTotalSteps() >= StepsValidationParameters.MIN_STEPS_TO_TRIGGER_LOCATION)
                getServicePrefs().storeLocationTriggerTime(new Date().getTime());

            StepsBulk newBulk = new StepsBulk(bulk);
            newBulk.setSource(bulk.getSource());

            if (_bulksPendingForLocation.size() > 10) {
                StepsBulk tmpBulk = _bulksPendingForLocation.remove(0);
                addDebugLog("_bulksPendingForLocation full, remove 1: " + tmpBulk.getTotalSteps());
            }

            _bulksPendingForLocation.add(newBulk);

            StepsLocationExtra defaultLocation = getDefaultLocationExtra();

            if (null != defaultLocation) {
                for (StepsBulk s : _bulksPendingForLocation) {
                    s.setLocation(defaultLocation);
                    _locationSource.addLocationToSteps(newBulk, this);
                }

                _bulksPendingForLocation.clear();
            }
            else {
                addDebugLog("cannot get last known location");
            }
        }
        else {
            BitwalkingApp.getInstance().trackException(new Exception("location source is null, steps ignored :("));
        }
    }

    private StepsLocationExtra getDefaultLocationExtra() {
        Location location = _locationSource.getLastKnownLocation();
        if (null == location)
            return null;

        return new StepsLocationExtra(location);
    }

    @Override
    public void onLiveSteps(int steps) {
        Logger.instance().Log(Logger.DEBUG, TAG, "got live steps from google steps class");

        // Update listener
        if (null != _stepsListener)
            _stepsListener.onDeviceLiveSteps(steps);
    }

    //endregion

    //region Location

    @Override
    public void onStepsLocationReady(ArrayList<StepsBulk> steps) {
        if (steps.size() > 1) {
            StepsLocationExtra last = steps.get(0).getLocation();

            for (int nextLocationIndex = 1; nextLocationIndex < steps.size(); ++nextLocationIndex) {
                steps.get(nextLocationIndex).getLocation().calcSetDistance(last);
                last = steps.get(nextLocationIndex).getLocation();
            }
        }

        addDebugLog(String.format("Got location, add telephony [%d]", steps.size()));
        _telephony.addTelephonyInfoToSteps(steps, this);
    }

    //endregion

    //region Telephony

    @Override
    public void onTelephonyStepsReady(ArrayList<StepsBulk> steps) {
        addDebugLog(String.format("Got telephony [%d]", steps.size()));

        // Update listener
        if (null != _stepsListener)
            _stepsListener.onDeviceVerifiedSteps(steps);
        else {
            int sum = 0;
            for (StepsBulk s : steps)
                sum += s.getTotalSteps();
            addDebugLog("_stepsListener is null, steps are not logged !!! " + sum);
        }
    }

    //endregion

    private ServicePreferences getServicePrefs() {
        if (null == _servicePrefs) {
            _servicePrefs = new ServicePreferences(_context);
        }

        return _servicePrefs;
    }

    private void addDebugLog(String log) {
        if (Globals.LOG_TO_FILE) {
            getServicePrefs().addLog(log);
        }
    }

    public interface OnDeviceStepsListener {
        void onDeviceVerifiedSteps(ArrayList<StepsBulk> steps);
        void onDeviceLiveSteps(int steps);
    }
}
