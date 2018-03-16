package bitwalking.bitwalking.remote_service;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Icon;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;

import java.math.BigDecimal;
import java.util.ArrayList;

import bitwalking.bitwalking.BitwalkingApp;
import bitwalking.bitwalking.boot.StartBitwalkingAtBootReceiver;
import bitwalking.bitwalking.steps.steps_sources.MisfitSteps;
import bitwalking.bitwalking.remote_service.server_update.UpdateServerThread;
import bitwalking.bitwalking.transactions.PaymentRequestNotification;
import bitwalking.bitwalking.util.Globals;
import bitwalking.bitwalking.ServicePreferences;
import bitwalking.bitwalking.activityes.DispatcherActivity;
import bitwalking.bitwalking.activityes.MainActivity;
import bitwalking.bitwalking.R;
import bitwalking.bitwalking.steps.LogSteps;
import bitwalking.bitwalking.steps.StepsManager;
import bitwalking.bitwalking.steps.steps_info.StepsBulk;
import bitwalking.bitwalking.util.Logger;

/**
 * Created by Marcus on 10/29/15.
 */
public class BwService extends Service
        implements
            GoogleApiClient.OnConnectionFailedListener,
            GoogleApiClient.ConnectionCallbacks,
            UserTodayListener,
            StepsManager.OnDeviceStepsListener {

    //region Members
    public static final String TAG = BwService.class.getSimpleName();
    public static final String PERIODIC_TASK_ACTION = "bitwalking.bitwalking.PERIODIC_TASK";

    boolean _isRunning = false;

    // data log
    ServicePreferences _servicePrefs;
    UpdateServerThread _updateServerThread;

    // Steps Manager
    StepsManager _stepsManager;
 
    // Steps source
    int _stepsSource;

    // Google api
    GoogleApiClient _googleApiClient;

    //endregion

    //region Remote Binder

    private BWServiceListener _listener = null;

    private BWServiceApi.Stub apiEndpoint = new BWServiceApi.Stub() {

        @Override
        public boolean updateStepsAndDetails() throws RemoteException {
            Logger.instance().Log(Logger.DEBUG, TAG, "inside updateStepsAndDetails");

            if (getUpdateThread().sendStepsUpdateNow()) {
                Logger.instance().Log(Logger.DEBUG, TAG, "update steps now - done");
                return true;
            }

            return false;
        }

        @Override
        public void refreshToday() throws RemoteException {
            Logger.instance().Log(Logger.DEBUG, TAG, "inside updateStepsAndDetails");

            getUpdateThread().refreshToday();
        }

        @Override
        public void dispatchLastStepsUpdate() throws RemoteException {
            dispatchStepsInfo();
        }

        @Override
        public void addListener(BWServiceListener listener) throws RemoteException {
            if (null == _listener)
                _listener = listener;
            else {
                synchronized (_listener) {
                    _listener = listener;
                }
            }
        }

        @Override
        public void removeListener(BWServiceListener listener) throws RemoteException {
            if (null == _listener)
                return;

            synchronized (_listener) {
                _listener = null;
            }
        }

        @Override
        public void updateServiceInfo(String serviceInfoJson) {
            ServiceInitInfo serviceNewInfo = new Gson().fromJson(serviceInfoJson, ServiceInitInfo.class);
            Logger.instance().Log(Logger.DEBUG, TAG, "set service info, aidl api");

            if (null == serviceNewInfo ||
                null == serviceNewInfo.userEmail ||
                null == serviceNewInfo.userSecret ||
                null == serviceNewInfo.userMsisdn) {
                _servicePrefs.addLog("invalid service info json: " + serviceInfoJson);
            }
            _servicePrefs.storeServiceInitInfo(serviceNewInfo);
        }

        @Override
        public String getServiceInfo() {
            return new Gson().toJson(_servicePrefs.getServiceInitInfo());
        }

        @Override
        public void startSteps() {
            addDebugLog("BwService: Start steps");
            if (!getStepsManager().isRunning())
                getStepsManager().startRecording();
        }

        @Override
        public void stopSteps() {
            addDebugLog("BwService: Stop steps");
            getStepsManager().stopRecording();
        }

        @Override
        public String getMisfitDeviceSerial() {
            return _servicePrefs.getMisfitDeviceInfo().serial;
        }

        @Override
        public void setMisfitDevice(String deviceAddr, String deviceSerial) {
            MisfitSteps.SelectedDeviceInfo misfitDeviceInfo = new MisfitSteps.SelectedDeviceInfo();
            misfitDeviceInfo.address = deviceAddr;
            misfitDeviceInfo.serial = deviceSerial;
            _servicePrefs.setMisfitDeviceInfo(misfitDeviceInfo);

            // TODO: log this in server
            Logger.instance().Log(Logger.INFO, TAG, String.format("set misfit device serial=[%s] address=[%s]", deviceSerial, deviceAddr));
        }

        @Override
        public int getStepsSource() {
            return _stepsSource;
        }

        @Override
        public boolean setStepsSource(final int source) {
            Logger.instance().Log(Logger.DEBUG, TAG, String.format("set steps source old = %d, new = %d", _stepsSource, source));

            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    boolean result = setStepsSourceHandler(source);

                    if (result)
                        _servicePrefs.setStepsSource(_stepsSource);
                }
            });


            return true;
        }

        @Override
        public void clearLogs() {
            if (null != _servicePrefs)
                _servicePrefs.clearLogs();
        }

        @Override
        public String getLogs() {
            String logs = "";

            if (null != _servicePrefs)
                logs = _servicePrefs.getLogs();

            return logs;
        }

        @Override
        public String getSteps() {
            String logs = "";

            if (null != _servicePrefs)
                logs = _servicePrefs.getSteps();

            return logs;
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if (BwService.class.getName().equals(intent.getAction())) {
            Logger.instance().Log(Logger.DEBUG, TAG, "Bound by intent " + intent);
            return apiEndpoint;
        } else {
            return null;
        }
    }

    //endregion

    //region Boot complete receiver

    private void enableBootCompleteReceiver() {
        ComponentName receiver = new ComponentName(BwService.this, StartBitwalkingAtBootReceiver.class);
        getPackageManager().setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }

    private void disableBootCompleteReceiver() {
        ComponentName receiver = new ComponentName(BwService.this, StartBitwalkingAtBootReceiver.class);

        getPackageManager().setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    //endregion

    //region Service Methods

    private ServicePreferences getServicePrefs() {
        if (null == _servicePrefs)
            _servicePrefs = new ServicePreferences(BwService.this);

        return _servicePrefs;
    }

    protected GoogleApiClient getGoogleApiClient() {
        if (null == _googleApiClient) {
            // Create a Google Fit Client instance with default user account.
            _googleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Fitness.HISTORY_API)
                    .addApi(Fitness.RECORDING_API)
                    .addApi(LocationServices.API)
                    .addScope(new Scope(Scopes.FITNESS_LOCATION_READ))
                    .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ))
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }

        return _googleApiClient;
    }

    private StepsManager getStepsManager() {
        if (null == _stepsManager) {
            addDebugLog("creating new StepsManager");
            _stepsManager = new StepsManager(BwService.this, getGoogleApiClient());
            _stepsManager.init();
            _stepsManager.setStepsListener(BwService.this);
            if (!_stepsManager.isRunning())
                _stepsManager.startRecording();
        }

        return _stepsManager;
    }

    private UpdateServerThread getUpdateThread() {
        if (null == _updateServerThread) {
            addDebugLog("creating new UpdateThread");
            _updateServerThread = new UpdateServerThread(BwService.this, this);
        }

        return _updateServerThread;
    }

    private boolean setStepsSourceHandler(int source) {
        boolean result = false;

        if (source == _stepsSource)
            return true; // nothing changed

        switch (source) {
            case Globals.STEPS_SOURCE_MISFIT: {
                MisfitSteps.SelectedDeviceInfo misfitDeviceInfo = getServicePrefs().getMisfitDeviceInfo();

                if (null != misfitDeviceInfo.address) {
                    result = true;
                    _stepsSource = Globals.STEPS_SOURCE_MISFIT;

                    getStepsManager().setStepsSource(_stepsSource);
                    Logger.instance().Log(Logger.INFO, TAG, "set steps source to misfit");

                    new Handler().postDelayed(checkBtIsBackOn, 1000);
                }

                break;
            }
            case Globals.STEPS_SOURCE_PHONE: {
                result = true;
                _stepsSource = Globals.STEPS_SOURCE_PHONE;
                // Try to get steps from previous source
                getStepsManager().getStepsNow();

                getStepsManager().setStepsSource(_stepsSource);
                Logger.instance().Log(Logger.INFO, TAG, "set steps source to phone");
                break;
            }
            default:
                // invalid source value
        }

        return result;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        addDebugLog("Creating service");
        init();
    }

    private void init() {
//        initBatteryState();
//        initServiceReceivers();
//        restartPeriodicTask();
        stopPeriodicTask(this);
        registerBwReceiver();
        enableBootCompleteReceiver();
    }

    private void initBatteryState() {
        IntentFilter batteryStatusIntentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatusIntent = registerReceiver(null, batteryStatusIntentFilter);

        if (batteryStatusIntent != null) {
            int level = batteryStatusIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatusIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            float batteryPercentage = level / (float) scale;
            float lowBatteryPercentageLevel = 0.14f;

            try {
                int lowBatteryLevel = Resources.getSystem().getInteger(Resources.getSystem().getIdentifier("config_lowBatteryWarningLevel", "integer", "android"));
                lowBatteryPercentageLevel = lowBatteryLevel / (float) scale;
            } catch (Resources.NotFoundException e) {
                Logger.instance().Log(Logger.ERROR, TAG, "Missing low battery threshold resource");
            }

            getServicePrefs().setBatteryLowState(batteryPercentage < lowBatteryPercentageLevel);
        } else {
            getServicePrefs().setBatteryLowState(false);
        }
    }

    private void restartPeriodicTask() {
        boolean isBatteryOk = !getServicePrefs().isBatteryLow();
        Intent alarmIntent = new Intent(PERIODIC_TASK_ACTION);
        boolean isAlarmUp = PendingIntent.getBroadcast(BwService.this, 0, alarmIntent, PendingIntent.FLAG_NO_CREATE) != null;

        if (isBatteryOk && !isAlarmUp) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            alarmIntent.setAction(PERIODIC_TASK_ACTION);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(BwService.this, 0, alarmIntent, 0);
            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), AlarmManager.INTERVAL_FIFTEEN_MINUTES, pendingIntent);
        }
    }

    public void stopPeriodicTask(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent alarmIntent = new Intent(PERIODIC_TASK_ACTION);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, 0);
        alarmManager.cancel(pendingIntent);
    }

    private void registerBwReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.location.PROVIDERS_CHANGED");
        filter.addAction("android.intent.action.BATTERY_LOW");
        filter.addAction("android.intent.action.BATTERY_OKAY");
        filter.addAction(PERIODIC_TASK_ACTION);

        registerReceiver(bwServiceReceiver, filter);
    }

    BroadcastReceiver bwServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (null != intent) {
                handleReceiverActions(intent);
            }
        }
    };

    private boolean handleReceiverActions(Intent intent) {
        boolean handled = false;

        if (null != intent.getAction() && !intent.getAction().isEmpty()) {
            if (intent.getAction().equals("android.location.PROVIDERS_CHANGED")) {
                handleGpsTrigger();
                handled = true;
            }
            else if (intent.getAction().equals("android.intent.action.BATTERY_LOW")) {
                addDebugLog("PeriodicTask: BATTERY_LOW - stop");
                getServicePrefs().setBatteryLowState(true);
//                    stopPeriodicTaskHeartBeat(context);
                handled = true;
            } else if (intent.getAction().equals("android.intent.action.BATTERY_OKAY")) {
                addDebugLog("PeriodicTask: BATTERY_OKAY - restart");
                getServicePrefs().setBatteryLowState(false);
//                    restartPeriodicTaskHeartBeat(context);
                handled = true;
            } else if (intent.getAction().equals(PERIODIC_TASK_ACTION)) {
                doPeriodicTask();
                handled = true;
            }
        }

//        if (handled) {
//            WakefulBroadcastReceiver.completeWakefulIntent(intent);
//        }

        return handled;
    }

    private void doPeriodicTask() {
        try {
            getUpdateThread().sendStepsUpdate();
            getUpdateThread().run();
            addDebugLog("PeriodicTask: sendStepsUpdateNow - true");
        } catch (Exception e) {
            addDebugLog("PeriodicTask: sendStepsUpdateNow - false: " + e.getMessage());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (null == intent) { return START_STICKY; }

        String action = intent.getAction();
        if (null != action) {
            // Stop
            if (action.equals(Globals.STOP_SERVICE_ACTION)) {
                addDebugLog("BwService: Stop");
                stopForeground(true);
                stopSelf();
            }
            // Start
            else if (action.equals(Globals.START_SERVICE_ACTION)) {
                if (!_isRunning) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            addDebugLog("BwService: Start");
                            runServiceInit();

                            if (!getServicePrefs().isRunning()) {
                                getServicePrefs().setRunningState(true);
                                getStepsManager().resetStepsStartTime();
                            }
                        }
                    }).start();
                    _isRunning = true;
                }
            }
            // Init
            else if (action.equals(Globals.INIT_SERVICE_ACTION)) {
                addDebugLog("BwService: Init");
                if (intent.hasExtra(Globals.BITWALKING_SERVICE_INIT_INFO)) {
                    String serviceInitInfoJson = intent.getStringExtra(Globals.BITWALKING_SERVICE_INIT_INFO);
                    Logger.instance().Log(Logger.DEBUG, TAG, "got service init info: " + serviceInitInfoJson);
                    ServiceInitInfo serviceInitInfo = new Gson().fromJson(serviceInitInfoJson, ServiceInitInfo.class);
                    getServicePrefs().storeServiceInitInfo(serviceInitInfo);
                }

                if (!checkIfInfoValid()) {
                    ServiceInitInfo serviceInitInfo = getServicePrefs().getServiceInitInfo();
                    addDebugLog("invalid service info json: " + new Gson().toJson(serviceInitInfo));
                    Logger.instance().Log(Logger.DEBUG, TAG, "serviceInitInfo is null, stop service");
                    stopSelf();
                }
            }
            // Logout
            else if (action.equals(Globals.LOGOUT_SERVICE_ACTION)) {
                addDebugLog("BwService: logout");
                handleLogout();
                stopForeground(true);
                stopSelf();
            }
            // Switch off
            else if (action.equals(Globals.SWITCH_OFF_SERVICE_ACTION)) {
                addDebugLog("BwService: switch off");
                // Handle switch off
                getServicePrefs().resetStepsLastUpdateTime();
                stopForeground(true);
                stopSelf();
            }
            // Location on/off
            else if (action.equals("android.location.PROVIDERS_CHANGED")) {
                addDebugLog("BwService: provider changed");
                handleGpsTrigger();
            }
            // Periodic Task
            else if (action.equals(PERIODIC_TASK_ACTION)) {
                addDebugLog("BwService: periodic task");
                doPeriodicTask();
            }
            // Pay
            else if (action.equals(PaymentRequestNotification.PAY_ACTION)) {
                Toast.makeText(BwService.this, "pay", Toast.LENGTH_SHORT).show();
            }
            // Reject
            else if (action.equals(PaymentRequestNotification.REJECT_ACTION)) {
                Toast.makeText(BwService.this, "reject", Toast.LENGTH_SHORT).show();
            }
        }

        return START_STICKY;
    }

    boolean checkIfInfoValid() {
        boolean valid = true;
        ServiceInitInfo serviceInitInfo = getServicePrefs().getServiceInitInfo();

        if (null == serviceInitInfo ||
            null == serviceInitInfo.userEmail ||
            null == serviceInitInfo.userSecret ||
            null == serviceInitInfo.userMsisdn) {
            valid = false;
        }
        return valid;
    }

    private void handleLogout() {
        getServicePrefs().clearUserInfo();
        getServicePrefs().resetStepsLastUpdateTime();
        LogSteps.INSTANCE.clearAllLogs(BwService.this);
    }

    private void runServiceInit() {
//        int tmp = getServicePrefs().getStepsSource();
//        Logger.instance().Log(Logger.DEBUG, TAG, "set initial steps source to " + tmp);
//        if (setStepsSourceHandler(tmp))
//            _stepsSource = tmp;
//        else
//            Logger.instance().Log(Logger.WARN, TAG, "failed to set steps source at init");

        getStepsManager();

        // Init server update thread
        getUpdateThread().run();

        // Show notification
        showNotification();

        // Gps notification
        handleGpsTrigger();

//        initBatteryState();
    }

    //region Google api

    private void startGooglePlayService() {
        Logger.instance().Log(Logger.DEBUG, TAG, "inside startGooglePlayService");
        if (!getGoogleApiClient().isConnected()) {
            Logger.instance().Log(Logger.DEBUG, TAG, "connect to google service");
            getGoogleApiClient().connect();
        }
    }

    private void stopGooglePlayService() {
        if (getGoogleApiClient().isConnected() || getGoogleApiClient().isConnecting()) {
            getGoogleApiClient().disconnect();
        }
    }


    @Override
    public void onConnected(Bundle bundle) {
        if (null != _stepsManager) {
            if (!_stepsManager.isRunning())
                _stepsManager.startRecording();
        }

        Logger.instance().Log(Logger.DEBUG, TAG, "TYPE_STEP_COUNT_DELTA is on ");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Logger.instance().Log(Logger.DEBUG, TAG, "google api Suspended");

        stopGooglePlayService();
        startGooglePlayService();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        String log = String.format("google api Failed - error [%d]: %s", result.getErrorCode(), result.getErrorMessage());
        Logger.instance().Log(Logger.DEBUG, TAG, log);
        if (null != _servicePrefs) {
            _servicePrefs.addLog(log);
        }

        boolean notificationRequired = true;

        if (null != _listener) {
            synchronized (_listener) {
                try {
                    _listener.onGoogleApiClientError();
                    notificationRequired = false;
                }
                catch (RemoteException e) {
                    log = "Failed to notify google api error: listener = " + _listener;
                    Logger.instance().Log(Logger.INFO, TAG, log);
                    if (null != _servicePrefs) {
                        _servicePrefs.addLog(log);
                    }
                }
            }
        }

        if (notificationRequired) {
            // show notification, user's action required
        }
    }

    //endregion

    @Override
    public void onDestroy() {
        super.onDestroy();

        addDebugLog("BwService: onDestroy");

        getUpdateThread().stopUpdate();
        getStepsManager().stopRecording();
        getServicePrefs().setRunningState(false);

        hideNotification(R.string.bw_service_gps_alert);
        hideNotification(R.string.bw_service_bt_alert);
//        hideNotification(ONGOING_NOTIFICATION_ID);

        stopPeriodicTask(BwService.this);
        disableBootCompleteReceiver();
        unregisterReceiver(bwServiceReceiver);
        _isRunning = false;
    }

    private String getTodayString() {
        BigDecimal today = ServicePreferences.getUserLastToday(BwService.this);
        return String.format("%s W$", Globals.bigDecimalToNiceString(today));
    }

    static final int ONGOING_NOTIFICATION_ID = 9441;
    private void showNotification() {
        CharSequence text = getTodayString();
        addDebugLog("showNotification: " + text);

        final Intent notificationIntent = new Intent(BwService.this, DispatcherActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        PendingIntent contentIntent = PendingIntent.getActivity(BwService.this, 0, notificationIntent, 0);

        // Switch off intent
        Intent switchOffIntent = new Intent(BwService.this, MainActivity.class);
//        switchOffIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        switchOffIntent.putExtra("SwitchOff", true);
        PendingIntent pIntent = PendingIntent.getActivities(BwService.this, (int) System.currentTimeMillis(), new Intent[]{switchOffIntent}, 0);

        // Set the info for the views that show in the notification panel.
        Notification.Builder notificationBuilder = new Notification.Builder(BwService.this)
                .setSmallIcon(Globals.getNotificationIcon())  // the status icon
                .setTicker(text)  // the status text
                .setWhen(System.currentTimeMillis())  // the time stamp
                .setContentTitle(getText(R.string.bw_service_label))  // the label of the entry
                .setContentText(text)  // the contents of the entry
                .setOnlyAlertOnce(true)
                .setContentIntent(contentIntent);  // The intent to send when the entry is clicked

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            notificationBuilder.setColor(0xFF000000);
        }

        if (android.os.Build.VERSION.SDK_INT >= 23) {
            Icon myIcon = Icon.createWithResource(BwService.this, getSwitchOffIcon());
            Notification.Action switchOffAction =
                    new Notification.Action.Builder(myIcon, "SWITCH OFF", pIntent).build();
            notificationBuilder.addAction(switchOffAction);

        }
        else if (android.os.Build.VERSION.SDK_INT >= 16) {
            notificationBuilder.addAction(getSwitchOffIcon(), "SWITCH OFF", pIntent);
        }

        if (android.os.Build.VERSION.SDK_INT >= 16) {
            notificationBuilder.setPriority(Notification.PRIORITY_MIN);
        }

        startForeground(ONGOING_NOTIFICATION_ID, buildNotification(notificationBuilder));
    }

    private int getSwitchOffIcon() {
        boolean whiteIcon = (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP);
        return whiteIcon ? R.drawable.switch_off : R.drawable.switch_off_white;
    }

    private Notification buildNotification(Notification.Builder builder) {
        return (android.os.Build.VERSION.SDK_INT < 16) ?
            builder.getNotification() :
            builder.build();
    }

    private void showGpsOffAlertNotification() {
        NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        // In this sample, we'll use the same text for the ticker and the expanded notification
        String alertString = "Location services off, steps will not be counted!";

        // The PendingIntent to launch our activity if the user selects this notification
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        PendingIntent contentIntent = PendingIntent.getActivity(BwService.this, 0, intent, 0);

        // Set the info for the views that show in the notification panel.
        Notification.Builder alertBuilder = new Notification.Builder(BwService.this)
                .setSmallIcon(Globals.getNotificationIcon())  // the status icon
                .setTicker(alertString)  // the status text
                .setWhen(System.currentTimeMillis())  // the time stamp
                .setContentTitle(getText(R.string.bw_service_label))  // the label of the entry
                .setContentText(alertString)  // the contents of the entry
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentIntent(contentIntent);  // The intent to send when the entry is clicked

        if (android.os.Build.VERSION.SDK_INT >= 16)
            alertBuilder.setStyle(new Notification.BigTextStyle().bigText(alertString));

        // Send the notification.
        Notification alert = buildNotification(alertBuilder);
        alert.flags = Notification.FLAG_AUTO_CANCEL;
        alert.flags = Notification.FLAG_ONGOING_EVENT;
        // We use a string id because it is a unique number.  We use it later to cancel.
        nm.notify(R.string.bw_service_gps_alert, alert);
    }

    private void showBluetoothOffAlertNotification() {
        NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        // In this sample, we'll use the same text for the ticker and the expanded notification
        String alertString = "Bluetooth is off, steps will not be sync!";

        // The PendingIntent to launch our activity if the user selects this notification
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        PendingIntent contentIntent = PendingIntent.getActivity(BwService.this, 0, intent, 0);

        // Set the info for the views that show in the notification panel.
        Notification.Builder alertBuilder = new Notification.Builder(BwService.this)
                .setSmallIcon(Globals.getNotificationIcon())  // the status icon
                .setTicker(alertString)  // the status text
                .setWhen(System.currentTimeMillis())  // the time stamp
                .setContentTitle(getText(R.string.bw_service_label))  // the label of the entry
                .setContentText(alertString)  // the contents of the entry
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentIntent(contentIntent);  // The intent to send when the entry is clicked

        if (android.os.Build.VERSION.SDK_INT >= 16)
            alertBuilder.setStyle(new Notification.BigTextStyle().bigText(alertString));

        // Send the notification.
        Notification alert = buildNotification(alertBuilder);
        alert.flags = Notification.FLAG_AUTO_CANCEL;
        alert.flags = Notification.FLAG_ONGOING_EVENT;
        // We use a string id because it is a unique number.  We use it later to cancel.
        nm.notify(R.string.bw_service_bt_alert, alert);
    }

    Runnable checkGpsIsOn = new Runnable() {
        @Override
        public void run() {
            try {
                // Check if gps is off, no counting steps if so
                if (isLocationOn()) {
                    hideNotification(R.string.bw_service_gps_alert);
                } else {
                    showGpsOffAlertNotification();
                    new Handler().postDelayed(checkGpsIsOn, 1000);
                }
            } catch (Exception e) {
                BitwalkingApp.getInstance().trackException("checkGpsIsOn failed", e);
            } finally {
            }
        }
    };

    Runnable checkBtIsBackOn = new Runnable() {
        @Override
        public void run() {
            if (_stepsSource != Globals.STEPS_SOURCE_MISFIT) {
                hideNotification(R.string.bw_service_bt_alert);
            }
            else {
                // Check if gps is off, no counting steps if so
                if (BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                    hideNotification(R.string.bw_service_bt_alert);
                }
                else {
                    showBluetoothOffAlertNotification();
                }

                new Handler().postDelayed(checkBtIsBackOn, 1000);
            }
        }
    };

    private void hideNotification(int notificationId) {
        NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        nm.cancel(notificationId);
    }

    private void showNoLocationPermissionAlertNotification() {
        NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        // In this sample, we'll use the same text for the ticker and the expanded notification
        String alertString = "\'Bitwalking\' must have permission to access location for verifying steps";

        // The PendingIntent to launch our activity if the user selects this notification
        Intent intent = new Intent(BwService.this, DispatcherActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent contentIntent = PendingIntent.getActivity(BwService.this, 0, intent, 0);

        // Set the info for the views that show in the notification panel.
        Notification.Builder alertBuilder = new Notification.Builder(BwService.this)
                .setSmallIcon(Globals.getNotificationIcon())  // the status icon
                .setTicker(alertString)  // the status text
                .setWhen(System.currentTimeMillis())  // the time stamp
                .setContentTitle(getText(R.string.bw_service_label))  // the label of the entry
                .setContentText(alertString)  // the contents of the entry
                .setContentIntent(contentIntent);  // The intent to send when the entry is clicked

        // Send the notification.
        Notification alert = buildNotification(alertBuilder);
        alert.flags = Notification.FLAG_AUTO_CANCEL;
        // We use a string id because it is a unique number.  We use it later to cancel.
        nm.notify(R.string.bw_service_gps_alert, alert);
    }

    //endregion

    //region Steps

    @Override
    public synchronized void onUserTodayUpdate(BigDecimal today) {
        if (null != today) {
            addDebugLog("new today: " + today);
            Logger.instance().Log(Logger.DEBUG, TAG, "new today: " + today);
            ServicePreferences.setUserLastToday(BwService.this, today);
            dispatchStepsInfo();
        }
    }

    @Override
    public synchronized void onDeviceLiveSteps(int steps) {

    }

    @Override
    public synchronized void onDeviceVerifiedSteps(ArrayList<StepsBulk> newSteps) {
        if (newSteps.size() > 0)
            handleGpsTrigger();

        Logger.instance().Log(Logger.DEBUG, TAG, "new steps:");
        for (StepsBulk s : newSteps) {
            Logger.instance().Log(Logger.DEBUG, TAG, s.toString());
        }

        if (Globals.LOG_TO_FILE) {
            addDebugLog(String.format("log new steps: %d", newSteps.size()));
        }

        LogSteps.INSTANCE.addLog(BwService.this, newSteps);
    }

    private boolean isLocationOn() {
        boolean gps_enabled = false;
        boolean network_enabled = false;
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        try {
            gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception ex) {}

        try {
            network_enabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch(Exception ex) {}

        return gps_enabled || network_enabled;
    }

    private void handleGpsTrigger() {
        if (!isLocationOn()) {
            showGpsOffAlertNotification();
            return;
        }
        else {
            hideNotification(R.string.bw_service_gps_alert);
        }

        if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(BwService.this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            showNoLocationPermissionAlertNotification();
            return;
        }
    }

    private void dispatchStepsInfo() {
        if (null != _listener) {
            synchronized (_listener) {
                try {
                    BigDecimal today = ServicePreferences.getUserLastToday(BwService.this);
                    _listener.onTodayUpdate(String.valueOf(today));
                }
                catch (RemoteException e) {
                    Logger.instance().Log(Logger.INFO, "BWSerive", "Failed to notify listener " + _listener);
                }
            }
        }
        else {
            Logger.instance().Log(Logger.DEBUG, TAG, "_listener is null, steps did not dispatch");
        }

        showNotification();
    }

    //endregion

    private void addDebugLog(String log) {
        if (Globals.LOG_TO_FILE) {
            getServicePrefs().addLog(log);
        }

        Logger.instance().Log(Logger.DEBUG, TAG, log);
    }
}
