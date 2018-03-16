package bitwalking.bitwalking.steps.steps_sources;

import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import com.google.gson.Gson;
import com.misfit.ble.shine.ShineConfiguration;
import com.misfit.ble.shine.ShineDevice;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

import bitwalking.bitwalking.ServicePreferences;
import bitwalking.bitwalking.steps.steps_info.StepsBulk;
import bitwalking.bitwalking.util.Globals;
import bitwalking.bitwalking.util.Logger;
import bitwalking.misfit.MisfitService;
import bitwalking.misfit.MisfitServiceRemote;

/**
 * Created by Marcus on 5/8/16.
 */
public class MisfitSteps extends StepsSource {

    final static String TAG = MisfitSteps.class.getSimpleName();
    final static int STEPS_READ_INTERVAL = 1 * 60 * 1000; // 10 minute
    final static long DAY_TIME_IN_MILLISECOND = 24 * 60 * 60 * 1000;
    final static long WEEK_TIME_IN_MILLISECOND = 7 * DAY_TIME_IN_MILLISECOND;

    private MisfitServiceRemote _service;

    ServicePreferences _servicePrefs = null;
    boolean _running = false;
    long _lastUpdateTime;
    Handler _updateStepsHandler, _refreshDevices;

    SelectedDeviceInfo _userDeviceInfo = null;

    private static int _state;
    private static final int BTLE_STATE_IDLE = 0;
    private static final int BTLE_STATE_SCANNING = 1;
    private static final int BTLE_STATE_CLOSED = 2;
    private static final int BTLE_STATE_CONNECTING = 3;
    private static final int BTLE_STATE_CONNECTED = 4;

    private static final String _btStatesStrings[] = new String[] {
            "IDLE",
            "SCANNING",
            "CLOSED",
            "CONNECTING",
            "CONNECTED"
    };

    public MisfitSteps(Context context) {
        super(context);
    }

    Runnable _getStepsUpdate = new Runnable() {
        @Override
        public void run() {
            if (_running) {
                try {
                    if (BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                        Logger.instance().Log(Logger.DEBUG, TAG, "start scanning devices for sync process");

                        if (_service.isConnected()) {
                            setState(BTLE_STATE_CONNECTED);
                            _service.getActivationState();
                        } else {
                            _service.stopScanning();
                            if (!_service.startScanning())
                                Logger.instance().Log(Logger.INFO, TAG, "failed to start scanning for misfit device :\\");
                            else
                                setState(BTLE_STATE_SCANNING);
                        }
                    }
                    else {
                        Logger.instance().Log(Logger.DEBUG, TAG, "bluetooth is off, skip scanning");
                    }
                }
                catch (Exception e) {

                }
                finally {
                    _updateStepsHandler.postDelayed(_getStepsUpdate, STEPS_READ_INTERVAL);
                }
            }
        }
    };

    private long getLatestEndTime(ArrayList<StepsBulk> bulks) {
        long latest = 0;
        for (StepsBulk s : bulks) {
            if (s.getEndTime() > latest)
                latest = s.getEndTime();
        }

        return latest;
    }

    private ArrayList<StepsBulk> getStepsHistory(MisfitService.StepsSyncData stepsData) {
        ArrayList<StepsBulk> steps = null;
        try {
            steps = new ArrayList<>();
            Date lastUpdateTime = new Date(_lastUpdateTime);
            Date nowDate = new Date(new Date().getTime() + 1 * 60 * 1000); // 1 minute spare

            for (MisfitService.StepsSyncData.MisfitSteps misfitSteps : stepsData.stepsData) {
                Date startDate = new Date(misfitSteps.startTime * 1000);
                Date endDate = new Date(misfitSteps.endTime * 1000);
                boolean added = false;

                if (misfitSteps.steps > 0) {
                    if ((lastUpdateTime.before(startDate) || lastUpdateTime.equals(startDate)) && nowDate.after(endDate)) {
                        StepsBulk newBulk = new StepsBulk(startDate.getTime(), endDate.getTime(), misfitSteps.steps);
                        newBulk.setSource(_userDeviceInfo.toString());
                        steps.add(newBulk);
                        added = true;
                    }
                }

                Logger.instance().Log(Logger.INFO, TAG, String.format("%s steps bulk - [%s]-[%s] [%d]",
                        (added) ? "added" : "ignore",
                        Globals.getFullDateFormat().format(startDate),
                        Globals.getFullDateFormat().format(endDate),
                        misfitSteps.steps));
            }
        } catch (Exception e) {
            Logger.instance().Log(Logger.ERROR, TAG, "failed to convert StepsSyncData into StepsBulk");
        }

        return steps;
    }

    private ServicePreferences getServicePrefs() {
        if (null == _servicePrefs)
            _servicePrefs = new ServicePreferences(_context);

        return _servicePrefs;
    }


    private ServiceConnection _serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            _service = ((MisfitServiceRemote.LocalBinder) rawBinder).getService();
            _service.setDeviceDiscoveringHandler(_handler);
            _service.setHandler(_handler);

            _updateStepsHandler = new Handler();
            _updateStepsHandler.postDelayed(_getStepsUpdate, STEPS_READ_INTERVAL);
        }

        public void onServiceDisconnected(ComponentName classname) {
            _service = null;
        }
    };

    boolean isOurDevice(Bundle data) {
        if (null != data) {
            final ShineDevice device = data.getParcelable(MisfitService.EXTRA_DEVICE);

            if (null == _userDeviceInfo || null == device || null == device.getName() || null == device.getSerialNumber())
                return false;

            if (_userDeviceInfo.address.contentEquals(device.getAddress()) &&
                _userDeviceInfo.serial.contentEquals(device.getSerialNumber())) {

                return true;
            }
        }

        return false;
    }

    public Handler _handler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case MisfitService.SHINE_SERVICE_DISCOVERED: {
                    Bundle data = message.getData();
                    final ShineDevice device = data.getParcelable(MisfitService.EXTRA_DEVICE);
                    final String serialString = data.getString(MisfitService.EXTRA_SERIAL_STRING);
                    final int rssi = data.getInt(MisfitService.EXTRA_RSSI);

                    Logger.instance().Log(Logger.DEBUG, TAG, "add misfit device: " + device.getAddress() + " serial: " + serialString);
                    addDevice(device, serialString, rssi);

                    break;
                }
                case MisfitService.SHINE_SERVICE_INITIALIZED:
                    break;
                case MisfitService.SHINE_SERVICE_ACTIVATION_STATE: {
                    setState(BTLE_STATE_CONNECTED);

                    Bundle data = message.getData();
                    if (isOurDevice(data)) {
                        if (data.getBoolean(MisfitService.EXTRA_MESSAGE, true)) {
                            Logger.instance().Log(Logger.DEBUG, TAG, "config device");
                            _service.startSettingDeviceConfiguration(0, 2500, ShineConfiguration.CLOCK_STATE_SHOW_CLOCK_FIRST);
                        } else {
                            Logger.instance().Log(Logger.DEBUG, TAG, "activate device");
                            _service.activate();
                        }
                    }

                    break;
                }
                case MisfitService.SHINE_SERVICE_CONNECTED: {
                    setState(BTLE_STATE_CONNECTED);

                    Bundle data = message.getData();
                    if (isOurDevice(data)) {

                        // Connect to device - sync steps :)
                        Logger.instance().Log(Logger.DEBUG, TAG, "connected to device, get activation state");
                        _service.getActivationState();
                    }
                    break;
                }
                case MisfitService.SHINE_SERVICE_CLOSED:
                    setState(BTLE_STATE_IDLE);
                    break;
                case MisfitService.SHINE_SERVICE_OPERATION_END: {
                    setState(BTLE_STATE_CONNECTED);
                    break;
                }
                case MisfitService.SHINE_SERVICE_DEVICE_CONFIGURED: {
                    Bundle data = message.getData();
                    if (isOurDevice(data)) {
                        // Connect to device - sync steps :)
                        Logger.instance().Log(Logger.DEBUG, TAG, "sync device steps");
                        _service.startSync();
                        _service.playAnimation();
                    }
                    break;
                }
                case MisfitService.SHINE_SERVICE_SYNC_DATA: {
                    setState(BTLE_STATE_CONNECTED);

                    // Sync done - get the data result
                    Bundle data = message.getData();
                    MisfitService.StepsSyncData stepsData = new Gson().fromJson(
                            data.getString(MisfitService.EXTRA_MESSAGE), MisfitService.StepsSyncData.class);

                    Logger.instance().Log(Logger.DEBUG, TAG, "handle device sync steps");
                    handleSyncedSteps(stepsData);

                    _service.stopPlayingAnimation();
                    _service.close();
                    setState(BTLE_STATE_IDLE);

                    break;
                }
                case MisfitService.SHINE_SERVICE_SCANNED_FAILED:
                    setState(BTLE_STATE_IDLE);
                    String failedMsg = message.getData().getString(MisfitService.EXTRA_MESSAGE);
                    Logger.instance().Log(Logger.INFO, TAG, "failed because of: " + failedMsg);
                    break;
                default:
                    break;//super.handleMessage(message);
            }
        }
    };

    private void handleSyncedSteps(MisfitService.StepsSyncData stepsData) {

                try {
                    long nowTime = new Date().getTime();
                    if (_lastUpdateTime > nowTime) {
                        Logger.instance().Log(Logger.INFO, TAG, "fix last update time - maybe the user was playing with the date...");
                        _lastUpdateTime = nowTime;
                    }

                    ArrayList<StepsBulk> steps = getStepsHistory(stepsData);

                    if (null != steps && steps.size() > 0) {
                        dispatchNewSteps(steps);

                        long latest = getLatestEndTime(steps);

                        if (latest > 0) {
                            _lastUpdateTime = latest + 1;
                            getServicePrefs().storeStepsLastUpdateTime(_lastUpdateTime);

                            Logger.instance().Log(Logger.DEBUG, TAG, "update last time = " + Globals.getFullDateFormat().format(new Date()));
                        }
                    }
                }
                catch (Exception e) {
                    Logger.instance().Log(Logger.INFO, TAG, "failed to handle synced steps");
                }
    }

    private void addDevice(ShineDevice device, String serialString, int rssi) {
        if (null == _userDeviceInfo || null == device || null == device.getName() || null == device.getSerialNumber() || null == device.getAddress())
            return;

        if (_userDeviceInfo.address.contentEquals(device.getAddress()) &&
            _userDeviceInfo.serial.contentEquals(device.getSerialNumber())) {

            if (_state != BTLE_STATE_CONNECTED && _state != BTLE_STATE_CONNECTING) {
                // This is user's device - connect
                _service.stopScanning();
                _service.connect(device);
                setState(BTLE_STATE_CONNECTING);
            }
        }
    }

    private void bindShineService() {
        Intent bindIntent = new Intent(_context, MisfitServiceRemote.class);
        bindIntent.putExtra("email", getServicePrefs().getServiceInitInfo().userEmail);
        _context.startService(bindIntent);
        _context.bindService(bindIntent, _serviceConnection, Context.BIND_AUTO_CREATE);
    }

    public void loadLastUpdateTime() {
        _lastUpdateTime = getServicePrefs().getStepsLastUpdateTime();
        if (_lastUpdateTime < 0) {
            resetLastUpdateTime();
        }

        Logger.instance().Log(Logger.DEBUG, TAG, "load last time = " + Globals.getFullDateFormat().format(new Date()));
    }

    public void resetLastUpdateTime() {
        _lastUpdateTime = new Date().getTime();
        getServicePrefs().storeStepsLastUpdateTime(_lastUpdateTime);

        Logger.instance().Log(Logger.DEBUG, TAG, "reset last time = " + Globals.getFullDateFormat().format(new Date()));
    }

    private void setState(int state) {
        Logger.instance().Log(Logger.DEBUG, TAG, "BT state = " + _btStatesStrings[_state]);
        _state = state;
    }

    @Override
    public boolean startRecording() {
        _running = true;
        bindShineService();
        loadLastUpdateTime();

        return true;
    }

    @Override
    public void init(Object extra) {
        _userDeviceInfo = (SelectedDeviceInfo)extra;
    }

    @Override
    public void stopRecording() {

        _running = false;
        _context.unbindService(_serviceConnection);
    }

    @Override
    public void setLiveStepsOn(boolean on) {

    }

    @Override
    public void getStepsNow() {
        if (_running) {
            try {
                if (!_service.startScanning())
                    Logger.instance().Log(Logger.INFO, TAG, "failed to start scanning for misfit device :\\");
                else
                    setState(BTLE_STATE_SCANNING);
            } catch (Exception e) {

            }
        }
    }

    private class ReadStepsTask extends AsyncTask<Long, Void, ArrayList<StepsBulk>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        protected ArrayList<StepsBulk> doInBackground(Long... args) {
            long startTime = args[0];
            long endTime = args[1];

            DateFormat df = Globals.getFullDateFormat();
            ArrayList<StepsBulk> steps = null;

            if (null != _service) {
                try {
                    // Get steps from misfit device

                }
                catch (Exception e) {
                    Logger.instance().Log(Logger.ERROR, TAG, "Failed to get steps from device");
                }
            }

            return steps;
        }

        protected void onPostExecute(Integer steps) {

        }
    }

    public static class SelectedDeviceInfo {
        public String address;
        public String serial;

        @Override
        public String toString() {
            return String.format("Misfit:[s=%s][a=%s]", serial, address);
        }
    }
}
