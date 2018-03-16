package bitwalking.bitwalking;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Date;

import bitwalking.bitwalking.remote_service.ServiceInitInfo;
import bitwalking.bitwalking.user_info.UserInfo;
import bitwalking.bitwalking.util.Globals;
import bitwalking.bitwalking.steps.steps_sources.MisfitSteps;
import bitwalking.bitwalking.util.Logger;

/**
 * Created by Marcus on 4/6/16.
 */
public class ServicePreferences {

    //region Members

    private static final String TAG = "ServicePreferences";

    Context _context;
    Gson _gson;

    final static String _USER_INFO = "USER-INFO";
    final static String _SERVICE_INIT_INFO = "SERVICE-INIT-INFO";
    final static String _STEPS_LOG_KEY = "ssklnwpe";
    final static String _BW_LAST_STEPS_UPDATE_TIME = "ewbl3kjhn44";
    final static String _USER_LAST_TODAY = "bqlktnbl2k4";
    final static String _USER_STEPS_SOURCE = "g2l4kmh3l9";
    final static String _USER_MISFIT_DEVICE_ADDRESS = "gl24kn3lknb4l";
    final static String _USER_MISFIT_DEVICE_SERIAL = "svdahl4g3";
    final static String _IS_RUNNING = "rekevknerw222";
    final static String _LAST_STEPS_TIME = "rgenlgwe";

    final static String _IS_BATTERY_LOW = "erlbnwrtkbl";


    final static String[] _KEYS_TO_CLEAN = new String[] {
            _SERVICE_INIT_INFO,
            _BW_LAST_STEPS_UPDATE_TIME,
            _USER_STEPS_SOURCE,
            _USER_MISFIT_DEVICE_ADDRESS,
            _USER_MISFIT_DEVICE_SERIAL
    };

    //endregion

    //region User steps

    public void storeStepsLastUpdateTime(long timeInMilli) {
        storeValueInServicePreferences(_BW_LAST_STEPS_UPDATE_TIME, String.valueOf(timeInMilli));

        Logger.instance().Log(Logger.INFO, TAG, "store: last update time = " + timeInMilli);
        if (timeInMilli != getStepsLastUpdateTime())
            Logger.instance().Log(Logger.ERROR, TAG, "last update time was not updated!!!");
    }

    public long getStepsLastUpdateTime() {
        long time = -1;
        String value = getValueFromServicePreferences(_BW_LAST_STEPS_UPDATE_TIME);

        if (null != value)
            time = Long.parseLong(value);

        Logger.instance().Log(Logger.INFO, TAG, "load: last update time = " + time);

        return time;
    }

    public void resetStepsLastUpdateTime() {
        Logger.instance().Log(Logger.INFO, TAG, "reset steps last update time");
        Globals.deletePreferencesKey(_servicePreferences, _BW_LAST_STEPS_UPDATE_TIME);
    }

    //endregion

    //region Last steps time

    public void storeLocationTriggerTime(long timeInMilli) {
        storeValueInServicePreferences(_LAST_STEPS_TIME, String.valueOf(timeInMilli));
    }

    public long getLocationTriggerTime() {
        long time = -1;
        String value = getValueFromServicePreferences(_LAST_STEPS_TIME);

        if (null != value)
            time = Long.parseLong(value);

        Logger.instance().Log(Logger.INFO, TAG, "load: last steps time = " + time);

        return time;
    }

    //endregion

    //region Methods

    private static SharedPreferences getServicePrefs(Context context) {
        return context.getSharedPreferences("ServicePreferences", Context.MODE_PRIVATE);
    }

    private synchronized void storeValueInServicePreferences(String key, String value) {
        if (null == value){
            Logger.instance().Log(Logger.ERROR, TAG, String.format("[%s] value string is null !!!", key));
            return;
        }

        Globals.setPreferencesKey(_servicePreferences, key, value);
    }

    private synchronized String getValueFromServicePreferences(String key) {
        String value = Globals.getPreferencesKey(_servicePreferences, key);
        return value;
    }

    //endregion

    //region steps log key

    public byte[] getStepsLogKey() {
        byte[] key = new byte[32];

        try {
            String keyString = getValueFromServicePreferences(_STEPS_LOG_KEY);
            if (null == keyString) {
                // generate new one
                SecureRandom sr = new SecureRandom();
                sr.nextBytes(key);
                storeValueInServicePreferences(_STEPS_LOG_KEY, new String(key, "UTF-8"));
            } else {
                key = keyString.getBytes("UTF-8");
            }
        }
        catch (Exception e) {
            BitwalkingApp.getInstance().trackException(e);
        }

        return key;
    }

    //endregion

    //region UserInfo

    private synchronized UserInfo getUserInfo() {
        UserInfo userInfo = null;

        try {
            String userInfoJsonString = getValueFromServicePreferences(_USER_INFO);
            userInfo = _gson.fromJson(userInfoJsonString, UserInfo.class);

            if (null == userInfo)
                Logger.instance().Log(Logger.ERROR, TAG, userInfoJsonString + " = " + _gson.toJson(userInfo));
        }
        catch (Exception e) {
            Logger.instance().Log(Logger.ERROR, TAG, "failed to load user info");
            e.printStackTrace();
        }

        return userInfo;
    }

    private ServiceInitInfo migrateServiceInitInfoFromUserInfo(UserInfo userInfo) {
        ServiceInitInfo info = new ServiceInitInfo();

        // User secret
        try {
            info.userSecret = userInfo.getAuthInfo().userSecret;
        } catch (Exception e) {
            BitwalkingApp.getInstance().trackException("migrateServiceInitInfoFromUserInfo: secret:", e);
        }

        // User msisdn
        try {
            info.userMsisdn = userInfo.getMeInfo().phone.msisdn;
        } catch (Exception e) {
            BitwalkingApp.getInstance().trackException("migrateServiceInitInfoFromUserInfo: msisdn:", e);
        }

        // User email
        try {
            info.userEmail = userInfo.getMeInfo().email;
        } catch (Exception e) {
            BitwalkingApp.getInstance().trackException("migrateServiceInitInfoFromUserInfo: email:", e);
        }

        return info;
    }

    public synchronized ServiceInitInfo getServiceInitInfo() {
        ServiceInitInfo serviceInitInfo = null;

        try {
            String serviceInitInfoJson = getValueFromServicePreferences(_USER_INFO);

            // Todo: remove this in the future
            // this is for older versions support
            if (null != serviceInitInfoJson) {
                Globals.deletePreferencesKey(_servicePreferences, _USER_INFO);
                new AppPreferences(_context).setForceLogout();
            }

            serviceInitInfoJson = getValueFromServicePreferences(_SERVICE_INIT_INFO);
            serviceInitInfo = _gson.fromJson(serviceInitInfoJson, ServiceInitInfo.class);

            if (null == serviceInitInfo)
                Logger.instance().Log(Logger.ERROR, TAG, serviceInitInfoJson + " = " + serviceInitInfoJson);
        }
        catch (Exception e) {
            Logger.instance().Log(Logger.ERROR, TAG, "failed to load service init info");
            e.printStackTrace();
        }

        return serviceInitInfo;
    }

    public synchronized void storeServiceInitInfo(ServiceInitInfo serviceInitInfo) {
        String serviceInfoJsonString = _gson.toJson(serviceInitInfo);
        Logger.instance().Log(Logger.DEBUG, TAG, "store ServiceInitInfo: " + serviceInfoJsonString);
        storeValueInServicePreferences(_SERVICE_INIT_INFO, serviceInfoJsonString);

        if (null != _serviceInitInfoUpdateListener)
            _serviceInitInfoUpdateListener.onServiceInitInfoUpdate(serviceInitInfo);
    }

    public void clearUserInfo() {
        Globals.deletePreferencesKey(_servicePreferences, _USER_INFO);
        Globals.deletePreferencesKey(_servicePreferences, _SERVICE_INIT_INFO);
        Globals.deletePreferencesKey(_servicePreferences, _STEPS_LOG_KEY);

        Globals.deletePreferencesKey(_servicePreferences, _USER_STEPS_SOURCE);
        Globals.deletePreferencesKey(_servicePreferences, _USER_MISFIT_DEVICE_ADDRESS);
        Globals.deletePreferencesKey(_servicePreferences, _USER_MISFIT_DEVICE_SERIAL);
    }

    public interface OnServiceInitInfoUpdate {
        void onServiceInitInfoUpdate(ServiceInitInfo serviceInitInfo);
    }

    private OnServiceInitInfoUpdate _serviceInitInfoUpdateListener = null;
    public void setUserInfoUpdateListener(OnServiceInitInfoUpdate listener) {
        _serviceInitInfoUpdateListener = listener;
    }

    //endregion

    //region vote

    private static final String USER_VOTE_KEY = "UserVote";
    public synchronized void setUserVote(String userVote) {
        storeValueInServicePreferences(USER_VOTE_KEY, userVote);
    }

    public synchronized String getUserVote() {
        return getValueFromServicePreferences(USER_VOTE_KEY);
    }

    public synchronized String getAndClearUserVote() {
        String userVote = getValueFromServicePreferences(USER_VOTE_KEY);
        Globals.deletePreferencesKey(_servicePreferences, USER_VOTE_KEY);
        return userVote;
    }

    //endregion

    //region Steps Source

    public void setMisfitDeviceInfo(MisfitSteps.SelectedDeviceInfo deviceInfo) {
        storeValueInServicePreferences(_USER_MISFIT_DEVICE_ADDRESS, deviceInfo.address);
        storeValueInServicePreferences(_USER_MISFIT_DEVICE_SERIAL, deviceInfo.serial);
    }

    public MisfitSteps.SelectedDeviceInfo getMisfitDeviceInfo() {
        MisfitSteps.SelectedDeviceInfo deviceInfo = new MisfitSteps.SelectedDeviceInfo();
        deviceInfo.address = getValueFromServicePreferences(_USER_MISFIT_DEVICE_ADDRESS);
        deviceInfo.serial = getValueFromServicePreferences(_USER_MISFIT_DEVICE_SERIAL);
        return deviceInfo;
    }

    public void setStepsSource(int source) {
        storeValueInServicePreferences(_USER_STEPS_SOURCE, String.valueOf(source));
    }

    public int getStepsSource() {
        String sourceString = getValueFromServicePreferences(_USER_STEPS_SOURCE);

        int source = Globals.STEPS_SOURCE_PHONE;
        if (null != sourceString) {
            try {
                source = Integer.parseInt(sourceString);
            }
            catch (Exception e) {
            }
        }

        return source;
    }

    //endregion

    //region Ctor

    SharedPreferences _servicePreferences;

    public ServicePreferences(Context context) {
        Logger.instance().Log(Logger.INFO, TAG, "Service preferences loaded by: " + context.getPackageName());

        _context = context;
        _gson = new Gson();
        _servicePreferences = _context.getSharedPreferences("ServicePreferences", Context.MODE_PRIVATE);
    }

    //endregion

    //region Log - in debug mode

    SharedPreferences _debugPreferences;
    static final String DEBUG_LOGS_KEY = "debug_logs";
    static final String STEPS_LOGS_KEY = "steps_logs";
    static final int LINES_COUNT_HISTORY = 1000; //fix  #31 OutOfMemory

    private void initDebug() {
        _debugPreferences = _context.getSharedPreferences("DebugPreferences", Context.MODE_PRIVATE);
    }

    public void addLog(String log) {
        if (BuildConfig.DEBUG) {       //not need in production mode
            String logs = getLogs() + "\n" + String.format("%s: %s", Globals.getFullDateFormat().format(new Date()), log);
            String[] lines = logs.split("\r\n|\r|\n");
            if (lines.length > LINES_COUNT_HISTORY) {
                StringBuilder strBuilder = new StringBuilder();
                for (int i = lines.length - LINES_COUNT_HISTORY; i < lines.length; i++) {
                    strBuilder.append(lines[i]);
                    strBuilder.append("\n");
                }

                logs = strBuilder.toString();
            }

            _debugPreferences.edit().putString(DEBUG_LOGS_KEY, logs).apply();
        }
    }

    public String getLogs() {
        if (null == _debugPreferences)
            initDebug();

        return _debugPreferences.getString(DEBUG_LOGS_KEY, "");
    }

    public void addSteps(String log) {
        if (BuildConfig.DEBUG) {       //not need in production mode
            String logs = getSteps() + "\n" + log;
            String[] lines = logs.split("\r\n|\r|\n");
            if (lines.length > LINES_COUNT_HISTORY) {
                StringBuilder strBuilder = new StringBuilder();
                for (int i = lines.length - LINES_COUNT_HISTORY; i < lines.length; i++) {
                    strBuilder.append(lines[i]);
                    strBuilder.append("\n");
                }

                logs = strBuilder.toString();
            }

            _debugPreferences.edit().putString(STEPS_LOGS_KEY, logs).apply();
        }
    }

    public String getSteps() {
        if (null == _debugPreferences)
            initDebug();

        return _debugPreferences.getString(STEPS_LOGS_KEY, "");
    }

    public void clearLogs() {
        _debugPreferences.edit()
                .putString(DEBUG_LOGS_KEY, "")
                .putString(STEPS_LOGS_KEY, "")
                .apply();
    }

    //endregion

    //region Low battery state

    public boolean isBatteryLow() {
        return _servicePreferences.getBoolean(_IS_BATTERY_LOW, false);
    }

    public void setBatteryLowState(boolean low) {
        _servicePreferences.edit().putBoolean(_IS_BATTERY_LOW, low).apply();
    }


    //endregion

    //region Is Running

    public boolean isRunning() {
        return _servicePreferences.getBoolean(_IS_RUNNING, false);
    }

    public void setRunningState(boolean running) {
        _servicePreferences.edit().putBoolean(_IS_RUNNING, running).apply();
    }


    //endregion

    //region User Last Today

    public static BigDecimal getUserLastToday(Context context) {
        BigDecimal today = new BigDecimal("0");

        try {
            String todayString = getServicePrefs(context).getString(_USER_LAST_TODAY, "0");
            today = new BigDecimal(todayString);
        } catch (Exception e) {
            BitwalkingApp.getInstance().trackException("Failed to get user last today: ", e);
        }

        return today;
    }

    public static void setUserLastToday(Context context, BigDecimal today) {
        getServicePrefs(context)
                .edit()
                .putString(_USER_LAST_TODAY, String.valueOf(today))
                .apply();
    }

    //endregion
}
