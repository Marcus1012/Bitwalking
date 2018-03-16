package bitwalking.misfit;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.widget.Toast;

import com.google.gson.Gson;
import com.misfit.ble.setting.SDKSetting;
import com.misfit.ble.shine.ActionID;
import com.misfit.ble.shine.ShineAdapter;
import com.misfit.ble.shine.ShineAdapter.ShineScanCallback;
import com.misfit.ble.shine.ShineConfiguration;
import com.misfit.ble.shine.ShineConnectionParameters;
import com.misfit.ble.shine.ShineDevice;
import com.misfit.ble.shine.ShineProfile;
import com.misfit.ble.shine.ShineProperty;
import com.misfit.ble.shine.controller.ConfigurationSession;
import com.misfit.ble.shine.result.Activity;
import com.misfit.ble.shine.result.SyncResult;
import com.misfit.ble.util.MutableBoolean;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import bitwalking.bitwalking.util.Logger;

/**
 * Created by Marcus on 5/3/16.
 */
public class MisfitService extends Service {
    private static final String TAG = MisfitService.class.getSimpleName();

    public static final int SHINE_SERVICE_INITIALIZED = 0;
    public static final int SHINE_SERVICE_DISCOVERED = 1;
    public static final int SHINE_SERVICE_CONNECTED = 2;
    public static final int SHINE_SERVICE_CLOSED = 3;
    public static final int SHINE_SERVICE_OPERATION_END = 4;
    public static final int SHINE_SERVICE_OTA_RESET = 5;
    public static final int SHINE_SERVICE_RSSI_READ = 6;
    public static final int SHINE_SERVICE_OTA_PROGRESS_CHANGED = 7;
    public static final int SHINE_SERVICE_STREAMING_USER_INPUT_EVENTS_RECEIVED_EVENT = 8;
    public static final int SHINE_SERVICE_BUTTON_EVENTS = 9;
    public static final int SHINE_SERVICE_MESSAGE = 10;
    public static final int SHINE_SERVICE_SCANNED_FAILED = 11;
    public static final int SHINE_SERVICE_SYNC_DATA = 12;
    public static final int SHINE_SERVICE_ACTIVATION_STATE = 13;
    public static final int SHINE_SERVICE_DEVICE_CONFIGURED = 14;

    // Bundle Key
    public static final String EXTRA_DEVICE = "MisfitService.extra.device";
    public static final String EXTRA_RSSI = "MisfitService.extra.rssi";
    public static final String EXTRA_MESSAGE = "MisfitService.extra.message";
    public static final String EXTRA_SERIAL_STRING = "MisfitService.extra.serialstring";
    public static final String EXTRA_DEVICE_FAMILY = "MisfitService.extra.devicefamily";
    public static final String EXTRA_FW_VERSION = "MisfitService.extra.fwversion";
    public static final String EXTRA_MODEL_NUMBER = "MisfitService.extra.modelnumber";
    /**
     * Connecting TimeOut Timer
     */
    public static final int CONNECTING_TIMEOUT = 30000;

    /**
     * Service's Binder
     */
    private final IBinder _binder = new LocalBinder();
    protected Handler _handler;
    protected Handler _deviceDiscoveringHandler;
    private ShineProfile _shineProfile;
    private ShineAdapter _shineAdapter;
    private com.misfit.ble.shine.result.SyncResult _summaryResult = null;

    private Timer _connectingTimeOutTimer = new Timer();
    private ConnectingTimedOutTimerTask _currentConnectingTimeOutTimerTask = null;

    private String _userName = "bitwalking.example.user";

    @Override
    public IBinder onBind(Intent intent) {
        return _binder;
    }

    public class LocalBinder extends Binder {
        public MisfitService getService() {
            return MisfitService.this;
        }
    }

    /**
     * Set Up
     */
    @Override
    public void onCreate() {
        super.onCreate();
        try {
            SDKSetting.setUp(this.getApplicationContext(), _userName);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            Toast.makeText(this, "ShineSDK setup failed: " + ex.toString(), Toast.LENGTH_LONG).show();
        }

        _shineAdapter = ShineAdapter.getDefaultAdapter(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (null != intent && intent.hasExtra("email")) {
            _userName = intent.getStringExtra("email");
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        if (_shineProfile != null) {
            _shineProfile.close();
        }

        super.onDestroy();
    }

    public void setHandler(final Handler handler) {
        _handler = handler;
    }

    public void setDeviceDiscoveringHandler(final Handler handler) {
        _deviceDiscoveringHandler = handler;
    }

    /**
     * Callback
     */
    private ShineScanCallback _shineScanCallback = new ShineScanCallback() {
        @Override
        public void onScanResult(ShineDevice device, int rssi) {
            onDeviceFound(device, rssi);
        }

        @Override
        public void onScanFailed(final ShineAdapter.ScanFailedErrorCode errorCode) {
            Logger.instance().Log(Logger.DEBUG, TAG, "scan failed");
            Message msg = Message.obtain(_deviceDiscoveringHandler, SHINE_SERVICE_SCANNED_FAILED);
            msg.sendToTarget();

            if (null != _handler) {
                _handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        onOperationCompleted(SHINE_SERVICE_SCANNED_FAILED, "ScanFailed: " + errorCode);
                    }
                }, 500L);
            }
        }
    };

    /**
     * Public Interface - Scanning
     */
    public boolean startScanning() {
        if (_shineAdapter == null)
            return false;

        boolean result = true;
        try {
            _shineAdapter.startScanning(_shineScanCallback);
        } catch (IllegalStateException ex) {
            ex.printStackTrace();
            result = false;
            Toast.makeText(this, "Start scanning failed: " + ex.toString(), Toast.LENGTH_LONG).show();
        }

        return result;
    }

    public void stopScanning() {
        if (_shineAdapter == null)
            return;

        _shineAdapter.stopScanning(_shineScanCallback);
    }

    public boolean getConnectedShines() {
        if (_shineAdapter == null)
            return false;

        boolean result = true;
        try {
            _shineAdapter.getConnectedShines(new ShineAdapter.ShineRetrieveCallback() {
                @Override
                public void onConnectedShinesRetrieved(List<ShineDevice> connectedShines) {
                    for (ShineDevice device : connectedShines) {
                        onDeviceFound(device, 0);
                    }
                }
            });
        } catch (IllegalStateException ex) {
            ex.printStackTrace();
            result = false;
            Toast.makeText(this, "Get connected devices failed: " + ex.toString(), Toast.LENGTH_LONG).show();
        }

        return result;
    }

    private void onDeviceFound(ShineDevice device, int rssi) {
        Bundle mBundle = new Bundle();
        mBundle.putParcelable(MisfitService.EXTRA_DEVICE, device);
        String deviceSerial = device.getSerialNumber();
        mBundle.putString(MisfitService.EXTRA_SERIAL_STRING, deviceSerial);
        mBundle.putInt(MisfitService.EXTRA_RSSI, rssi);

        Logger.instance().Log(Logger.DEBUG, TAG, "device found: " + deviceSerial);

        Message msg = Message.obtain(_deviceDiscoveringHandler, SHINE_SERVICE_DISCOVERED);
        msg.setData(mBundle);
        msg.sendToTarget();
    }

    /**
     * Public Interface - Operate
     */
    public boolean connect(ShineDevice device) {
        try {
            if (_shineProfile != null) {
                _shineProfile.close();
            }

            if (device.isInvalid()) {
                Toast.makeText(this, "ShineDevice instance has become INVALID. Please scan for it again!", Toast.LENGTH_SHORT).show();
                return false;
            }

            _shineProfile = device.connectProfile(this, false, new ShineProfile.ConnectionCallback() {
                @Override
                public void onConnectionStateChanged(ShineProfile shineProfile, ShineProfile.State newState) {
                    boolean isConnected = (ShineProfile.State.CONNECTED == newState);
                    stopConnectionTimeOutTimer();

                    if (isConnected) {
                        String firmwareVersion = _shineProfile.getFirmwareVersion();
                        String modelNumber = _shineProfile.getModelNumber();
                        String deviceFamilyName = getDeviceFamilyName(_shineProfile.getDeviceFamily());

                        Bundle mBundle = new Bundle();
                        mBundle.putParcelable(MisfitService.EXTRA_DEVICE, _shineProfile.getDevice());
                        mBundle.putString(MisfitService.EXTRA_DEVICE_FAMILY, deviceFamilyName);
                        mBundle.putString(MisfitService.EXTRA_FW_VERSION, firmwareVersion);
                        mBundle.putString(MisfitService.EXTRA_MODEL_NUMBER, modelNumber);

                        if (null != _handler) {
                            Message msg = Message.obtain(_handler, SHINE_SERVICE_CONNECTED);
                            msg.setData(mBundle);
                            msg.sendToTarget();
                        }
                    } else {
                        _shineProfile = null;

                        if (null != _handler) {
                            Message msg = Message.obtain(_handler, SHINE_SERVICE_CLOSED);
                            msg.sendToTarget();
                        }
                    }
                }
            });
        } catch (IllegalStateException ex) {
            ex.printStackTrace();
            Toast.makeText(this, "Attempt to connect failed: " + ex.toString(), Toast.LENGTH_LONG).show();
        }

        if (_shineProfile == null)
            return false;

        startConnectionTimeOutTimer();
        return true;
    }

    public int getDeviceFamily() {
        return _shineProfile.getDeviceFamily();
    }

    public boolean isConnected() {
        return _shineProfile != null &&
                (_shineProfile.getState() == ShineProfile.State.OTA || _shineProfile.getState() == ShineProfile.State.CONNECTED);
    }

    public boolean isBusy() {
        return isConnected() && _shineProfile.getCurrentAction() != null;
    }

    public boolean isReady() {
        return isConnected() && _shineProfile.getCurrentAction() == null;
    }

    public void startGettingDeviceConfiguration() {
        _shineProfile.getDeviceConfiguration(new ShineProfile.ConfigurationCallback() {
            @Override
            public void onConfigCompleted(ActionID actionID, ShineProfile.ActionResult resultCode, Hashtable<ShineProperty, Object> data) {
                ConfigurationSession session = (ConfigurationSession) data.get(ShineProperty.SHINE_CONFIGURATION_SESSION);

                if (resultCode == ShineProfile.ActionResult.SUCCEEDED) {
                    onOperationCompleted("onGettingDeviceConfigurationSucceeded:" + buildShineConfigurationString(session));
                } else {
                    onOperationCompleted("onGettingDeviceConfigurationFailed:" + buildShineConfigurationString(session));
                }
            }
        });
    }

    private class SetConfigurationSession extends ConfigurationSession {
        public SetConfigurationSession(ShineConfiguration shineConfiguration) {
            super();
            mShineConfiguration = shineConfiguration;
        }

        private void prepareSetTimeParams() {
            long timestamp = System.currentTimeMillis();
            mTimestamp = timestamp / 1000;
            mPartialSecond = (short)(timestamp - mTimestamp * 1000);
            mTimeZoneOffset = (short)(TimeZone.getDefault().getOffset(timestamp) / 1000 / 60);
        }
    }

    public void startSettingDeviceConfiguration(long activityPoint, long goalPoint, byte clockState) {
        ShineConfiguration shineConfiguration = new ShineConfiguration();

        shineConfiguration.mActivityPoint = activityPoint;
        shineConfiguration.mGoalValue = goalPoint;
        shineConfiguration.mClockState = clockState;

        SetConfigurationSession configurationSession = new SetConfigurationSession(shineConfiguration);
        configurationSession.prepareSetTimeParams();

        _shineProfile.setDeviceConfiguration(configurationSession, new ShineProfile.ConfigurationCallback() {
            @Override
            public void onConfigCompleted(ActionID actionID, ShineProfile.ActionResult resultCode, Hashtable<ShineProperty, Object> data) {

                if (resultCode == ShineProfile.ActionResult.SUCCEEDED) {
                    Bundle mBundle = new Bundle();
                    mBundle.putParcelable(MisfitService.EXTRA_DEVICE, _shineProfile.getDevice());

                    if (null != _handler) {
                        Message msg = Message.obtain(_handler, SHINE_SERVICE_DEVICE_CONFIGURED);
                        msg.setData(mBundle);
                        msg.sendToTarget();
                    }
                } else {
                    Logger.instance().Log(Logger.INFO, TAG, "device configuration failed");
                }
            }
        });
    }

    public void startSync() {
        _summaryResult = new SyncResult();
        _shineProfile.sync(new ShineProfile.SyncCallback() {
            @Override
            public void onSyncCompleted(ShineProfile.ActionResult resultCode) {
                if (resultCode == ShineProfile.ActionResult.SUCCEEDED) {
                    Bundle mBundle = new Bundle();
                    mBundle.putParcelable(MisfitService.EXTRA_DEVICE, _shineProfile.getDevice());
                    mBundle.putString(MisfitService.EXTRA_MESSAGE, buildStepsSyncResultString(_summaryResult));

                    if (null != _handler) {
                        Message msg = Message.obtain(_handler, SHINE_SERVICE_SYNC_DATA);
                        msg.setData(mBundle);
                        msg.sendToTarget();
                    }
                } else {
                    Logger.instance().Log(Logger.INFO, TAG, "sync steps data failed");
//                    onOperationCompleted("onSyncFailed:" + buildSyncResultString(_summaryResult));
                }
            }

            @Override
            public void onSyncDataRead(SyncResult syncResult, Bundle extraInfo, MutableBoolean shouldStop) {
                if (syncResult == null)
                    return;

                _summaryResult.mActivities.addAll(0, syncResult.mActivities);
            }
        });
    }

    public void startOTAing(byte[] firmwareData) {
        _shineProfile.ota(firmwareData, new ShineProfile.OTACallback() {
            @Override
            public void onOTACompleted(ShineProfile.ActionResult resultCode) {
                if (ShineProfile.ActionResult.SUCCEEDED == resultCode) {
                    onOperationCompleted("OTA COMPLETED - SHINE RESET");
                } else {
                    onOperationCompleted("OTA FAILED");
                }
            }

            @Override
            public void onOTAProgressChanged(float progress) {
                String message = "OTA PROGRESS: " + String.format("%.1f", progress * 100) + "%";
                onOperationCompleted(SHINE_SERVICE_OTA_PROGRESS_CHANGED, message);
            }
        });
    }

    public void readRssi() {
        if (_shineProfile != null) {
            _shineProfile.readRssi(new ShineProfile.ConfigurationCallback() {
                @Override
                public void onConfigCompleted(ActionID actionID, ShineProfile.ActionResult resultCode, Hashtable<ShineProperty, Object> data) {
                    int rssi = (int) data.get(ShineProperty.RSSI);

                    Bundle mBundle = new Bundle();
                    mBundle.putInt(MisfitService.EXTRA_RSSI, rssi);

                    if (null != _handler) {
                        Message msg = Message.obtain(_handler, SHINE_SERVICE_RSSI_READ);
                        msg.setData(mBundle);
                        msg.sendToTarget();
                    }
                }
            });
        }
    }

    public void playAnimation() {
        _shineProfile.playAnimation(new ShineProfile.ConfigurationCallback() {
            @Override
            public void onConfigCompleted(ActionID actionID, ShineProfile.ActionResult resultCode, Hashtable<ShineProperty, Object> data) {
                if (resultCode == ShineProfile.ActionResult.SUCCEEDED) {
                    onOperationCompleted("PLAY ANIMATION SUCCEEDED");
                } else {
                    onOperationCompleted("PLAY ANIMATION FAILED");
                }
            }
        });
    }

    public void stopPlayingAnimation() {
        _shineProfile.stopPlayingAnimation(new ShineProfile.ConfigurationCallback() {
            @Override
            public void onConfigCompleted(ActionID actionID, ShineProfile.ActionResult resultCode, Hashtable<ShineProperty, Object> data) {
                if (resultCode == ShineProfile.ActionResult.SUCCEEDED) {
                    onOperationCompleted("STOP PLAYING ANIMATION SUCCEEDED");
                } else {
                    onOperationCompleted("STOP PLAYING ANIMATION FAILED");
                }
            }
        });
    }


    public void getActivationState() {
        _shineProfile.getActivationState(new ShineProfile.ConfigurationCallback() {
            @Override
            public void onConfigCompleted(ActionID actionID, ShineProfile.ActionResult actionResult, Hashtable<ShineProperty, Object> hashtable) {

                Bundle mBundle = new Bundle();
                mBundle.putParcelable(MisfitService.EXTRA_DEVICE, _shineProfile.getDevice());

                if (actionResult == ShineProfile.ActionResult.SUCCEEDED) {
                    boolean isActivated = (Boolean)hashtable.get(ShineProperty.ACTIVATION_STATE);
                    mBundle.putBoolean(MisfitService.EXTRA_MESSAGE, isActivated);

                } else {
                    Logger.instance().Log(Logger.INFO, TAG, "failed to get device activation state");
                }

                if (null != _handler) {
                    Message msg = Message.obtain(_handler, SHINE_SERVICE_ACTIVATION_STATE);
                    msg.setData(mBundle);
                    msg.sendToTarget();
                }
            }
        });
    }

    public void activate() {
        _shineProfile.activate(new ShineProfile.ConfigurationCallback() {
            @Override
            public void onConfigCompleted(ActionID actionID, ShineProfile.ActionResult resultCode, Hashtable<ShineProperty, Object> data) {
                if (resultCode == ShineProfile.ActionResult.SUCCEEDED) {
                    onOperationCompleted("ACTIVATE SUCCEEDED");
                } else {
                    onOperationCompleted("ACTIVATE FAILED");
                }
            }
        });
    }

    public void close() {
        if (_shineProfile != null) {
            _shineProfile.close();
        }
    }

    public void interrupt() {
        _shineProfile.interrupt();
    }

    /**
     * Connection Timer
     */
    private void onConnectingTimedOut(ConnectingTimedOutTimerTask timerTask) {
        if (timerTask == _currentConnectingTimeOutTimerTask) {
            _currentConnectingTimeOutTimerTask = null;
            close();
        }
    }

    private void startConnectionTimeOutTimer() {
        stopConnectionTimeOutTimer();

        _currentConnectingTimeOutTimerTask = new ConnectingTimedOutTimerTask();
        _connectingTimeOutTimer.schedule(_currentConnectingTimeOutTimerTask, CONNECTING_TIMEOUT);
    }

    public void stopConnectionTimeOutTimer() {
        if (_currentConnectingTimeOutTimerTask != null) {
            _currentConnectingTimeOutTimerTask.mIsCancelled = true;
            _currentConnectingTimeOutTimerTask.cancel();
        }
    }

    private class ConnectingTimedOutTimerTask extends TimerTask {
        public boolean mIsCancelled = false;

        public ConnectingTimedOutTimerTask() {
            mIsCancelled = false;
        }

        @Override
        public void run() {
            if (!mIsCancelled) {
                MisfitService.this.onConnectingTimedOut(this);
            }
        }
    }

    /**
     * Steps Sync Data
     */
    private String buildStepsSyncResultString(SyncResult syncResult) {
        Gson gson = new Gson();
        String result = null;
        if (syncResult != null) {
//            int totalPoint = 0;
            StepsSyncData steps = new StepsSyncData();
            int totalSteps = 0;
            if (syncResult.mActivities != null) {
                for (Activity activity : syncResult.mActivities) {
//                    totalPoint += activity.mPoints;
                    totalSteps += activity.mBipedalCount;
                    steps.stepsData.add(new StepsSyncData.MisfitSteps(
                            activity.mStartTimestamp, activity.mEndTimestamp, activity.mBipedalCount));
                }
            }

            Logger.instance().Log(Logger.INFO, TAG, String.format("Misfit synced with %d steps", totalSteps));
            result = gson.toJson(steps);
        }
        return result;
    }

    private String buildShineConfigurationString(ConfigurationSession session) {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("\nTimeStamp: " + session.mTimestamp);
        stringBuilder.append("\nPartialSecond: " + session.mPartialSecond);
        stringBuilder.append("\nTimeZoneOffset: " + session.mTimeZoneOffset);

        stringBuilder.append("\nActivityPoint: " + session.mShineConfiguration.mActivityPoint);
        stringBuilder.append("\nGoalValue: " + session.mShineConfiguration.mGoalValue);
        stringBuilder.append("\nClockState: " + session.mShineConfiguration.mClockState);
        stringBuilder.append("\nBatteryLevel: " + session.mShineConfiguration.mBatteryLevel);

        return stringBuilder.toString();
    }

    private String buildConnectionParametersString(ShineConnectionParameters connectionParameters) {
        if (connectionParameters == null) {
            return "";
        }

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("\nConnection Interval: " + connectionParameters.getConnectionInterval());
        stringBuilder.append("\nConnection Latency: " + connectionParameters.getConnectionLatency());
        stringBuilder.append("\nSupervision Timeout: " + connectionParameters.getSupervisionTimeout());

        return stringBuilder.toString();
    }

    private String getDeviceFamilyName(int deviceFamily) {
        String deviceFamilyName = "Unknown";

        switch (deviceFamily) {
            case ShineProfile.DEVICE_FAMILY_SHINE:
                deviceFamilyName = "Shine";
                break;
            case ShineProfile.DEVICE_FAMILY_FLASH:
                deviceFamilyName = "Flash";
                break;
            case ShineProfile.DEVICE_FAMILY_BUTTON:
                deviceFamilyName = "Button";
                break;
            case ShineProfile.DEVICE_FAMILY_SHINE_MKII:
                deviceFamilyName = "Shine MKII";
                break;
            default:
                break;
        }
        return deviceFamilyName;
    }

    private void onOperationCompleted(int eventId, String message) {
        Bundle mBundle = new Bundle();
        mBundle.putString(EXTRA_MESSAGE, message);

        if (null != _handler) {
            Message msg = Message.obtain(_handler, eventId);
            msg.setData(mBundle);
            msg.sendToTarget();
        }
    }

    private void onOperationCompleted(String message) {
        onOperationCompleted(SHINE_SERVICE_OPERATION_END, message);
    }

    public static class StepsSyncData {
        public ArrayList<MisfitSteps> stepsData;

        StepsSyncData() {
            stepsData = new ArrayList<>();
        }

        public static class MisfitSteps {
            public long startTime;
            public long endTime;
            public int steps;

            public MisfitSteps(long stime, long etime, int steps) {
                this.startTime = stime;
                this.endTime = etime;
                this.steps = steps;
            }
        }
    }
}