package bitwalking.bitwalking;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.Date;

import bitwalking.bitwalking.registration_and_login.Utilities;
import bitwalking.bitwalking.remote_service.ServiceInitInfo;
import bitwalking.bitwalking.server.BitwalkingServer;
import bitwalking.bitwalking.server.FinishAction;
import bitwalking.bitwalking.server.api.ServerApi;
import bitwalking.bitwalking.server.requests.UpdateUserAvatar;
import bitwalking.bitwalking.server.responses.GetUserAvatarResponse;
import bitwalking.bitwalking.user_info.UserInfo;
import bitwalking.bitwalking.util.Globals;
import bitwalking.bitwalking.util.Logger;

/**
 * Created by Marcus on 4/6/16.
 */
public class AppPreferences {

    private static final String TAG = AppPreferences.class.getSimpleName();

    private static final String IS_FIRST_TIME = "AppFirstTime";
    private static final String NEED_PUSH_TOKEN = "NeedPushToken";
    private static final String COMING_FROM_DEBUG = "ComingFromDebug";
    private static final String USER_ID_KEY = "asldkn54p";
    private static final String COUNTRY_CODE = "rengekj";
    private static final String USER_SECRET_KEY = "wongq3i";
    private static final String AVATAR_INFO_KEY = "AvatarInfo";
    private static final String USER_INVITE_CODE = "AffiliationInviteCode";
    private static final String REGISTRATION_SESSION = "Registration.Session";
    private static final String FORCE_LOGOUT = "sdefw.fewklob";
    private static final String FORCE_UPDATE = "sdefw.sadgass";
    private static final String USER_INFO = "brAB3Ra6brt0443";
    private static final String SWITCHED_OFF = "54tgwrdg";

    Context _context;
    static SharedPreferences _defaultPreferences;

    private SharedPreferences getDefaultPrefs() {
        if (null == _defaultPreferences)
            _defaultPreferences = PreferenceManager.getDefaultSharedPreferences(_context);

        return _defaultPreferences;
    }

    //region Force Logout
    public boolean getForceLogout() {
        return (null != Globals.getPreferencesKey(getDefaultPrefs(), FORCE_LOGOUT));
    }

    public void setForceLogout() {
        Globals.setPreferencesKey(getDefaultPrefs(), FORCE_LOGOUT, "bob");
    }

    public void clearForceLogout() {
        Globals.deletePreferencesKey(getDefaultPrefs(), FORCE_LOGOUT);
    }

    //endregion

    //region Force update
    public boolean getForceUpdate() {
        return (null != Globals.getPreferencesKey(getDefaultPrefs(), FORCE_UPDATE));
    }

    public void setForceUpdate() {
        Globals.setPreferencesKey(getDefaultPrefs(), FORCE_UPDATE, "bob");
    }

    public void clearForceUpdate() {
        Globals.deletePreferencesKey(getDefaultPrefs(), FORCE_UPDATE);
    }

    //endregion

    //region Switched Off
    public boolean isSwitchedOff() {
        return (null != Globals.getPreferencesKey(getDefaultPrefs(), SWITCHED_OFF));
    }

    public void setSwitchedOff(boolean switchedOff) {
        if (switchedOff)
            Globals.setPreferencesKey(getDefaultPrefs(), SWITCHED_OFF, "true");
        else
            Globals.deletePreferencesKey(getDefaultPrefs(), SWITCHED_OFF);
    }

    //endregion

    //region First Time
    public boolean isFirstTime() {
        return (null == Globals.getPreferencesKey(getDefaultPrefs(), IS_FIRST_TIME));
    }

    public void setFirstTime() {
        Globals.setPreferencesKey(getDefaultPrefs(), IS_FIRST_TIME, "false");
    }

    //endregion

    //region User Logged In

    public boolean isUserLoggedIn() {
        boolean loggedIn = false;
        try {
            if (null != getUserId(_context) && null != getUserSecret(_context))
                loggedIn = true;
        } catch (Exception e) {
            BitwalkingApp.getInstance().trackException("isUserLoggedIn: failed", e);
            e.printStackTrace();
        }

        return loggedIn;
    }

    //endregion

    //region Registration Session

    public String getRegistrationSession() {
        return Globals.getPreferencesKey(getDefaultPrefs(), REGISTRATION_SESSION);
    }

    public void setRegistrationSession(String sessionInfo) {
        Globals.setPreferencesKey(getDefaultPrefs(), REGISTRATION_SESSION, sessionInfo);
    }

    public void clearRegistrationSession() {
        Globals.deletePreferencesKey(getDefaultPrefs(), REGISTRATION_SESSION);
    }

    //endregion

    //region Profile Image

    static Bitmap _currentProfileImage = null;

    public void deleteProfileImage() {
        File profileFile = getProfileImagePath();

        if (profileFile.exists())
            profileFile.delete();
    }

    public void resetProfileImage() {
        _currentProfileImage = BitmapFactory.decodeResource(_context.getResources(), R.drawable.profile_default_circle);
        saveProfileImage(_currentProfileImage);
    }

    private File getProfileImagePath() {

        ContextWrapper cw = new ContextWrapper(_context);
        // path to /data/data/yourapp/app_data/imageDir
        File directory = cw.getDir(Utilities.getInternalFolderName(), Context.MODE_PRIVATE);
        // Create imageDir
        File profileImagePath = new File(directory, Utilities.getProfileImageFileName());

        return profileImagePath;
    }

    public interface OnProfileImageChange {
        void profileImageChanged();
    }

    static OnProfileImageChange profileImageListener = null;

    public void setProfileImageChangeListener(OnProfileImageChange listener) {
        profileImageListener = listener;
    }

    private void saveProfileImage(Bitmap finalBitmap) {
        File profileFile = getProfileImagePath();

        if (profileFile.exists())
            profileFile.delete();

        try {
            FileOutputStream out = new FileOutputStream(profileFile);
            finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();

            getProfileImage();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (null != profileImageListener)
            profileImageListener.profileImageChanged();
    }

    public void deleteAvatar() {
        ServerApi.deleteAvatar(
                getUserId(_context),
                getUserSecret(_context),
                new ServerApi.SimpleServerResponseListener() {
                    @Override
                    public void onResponse(final int code) {
                        if (200 == code) {
                            Globals.deletePreferencesKey(getDefaultPrefs(), AVATAR_INFO_KEY);
                            resetProfileImage();
                        }
                    }
                });
    }

    public void updateAvatar(final Bitmap avatar) {
        ServerApi.putAvatar(
                getUserId(_context),
                getUserSecret(_context),
                new UpdateUserAvatar(avatar),
                new ServerApi.AvatarListener() {
                    @Override
                    public void onAvatar(GetUserAvatarResponse.AvatarInfo avatarInfo, final int code) {
                        switch (code) {
                            case 200:
                            case 201:
                                storeAvatarInfo(avatarInfo, false);
                                saveProfileImage(avatar);
                                break;
                            default:
                                break;
                        }
                    }
                });
    }

    public Bitmap getProfileImage() {
        // check if server has updated pic
        try {
            File file = getProfileImagePath();

            if(!file.exists()) {
                resetProfileImage();
            }

            _currentProfileImage = BitmapFactory.decodeStream(new FileInputStream(getProfileImagePath()));
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return _currentProfileImage;
    }

    public void loadAvatarFromServer() {
        loadAvatarFromServer(null);
    }

    public void loadAvatarFromServer(final FinishAction finish) {
        try {
            ServerApi.getAvatar(
                    getUserId(_context),
                    getUserSecret(_context),
                    new ServerApi.AvatarListener() {
                        @Override
                        public void onAvatar(GetUserAvatarResponse.AvatarInfo avatarInfo, final int code) {
                            switch (code) {
                                case 200:
                                    storeAvatarInfo(avatarInfo, true);
                                case 400:
                                    if (null != finish)
                                        finish.done();
                                    break;
                                default:
                                    if (null != finish)
                                        finish.failed();
                                    break;
                            }
                        }
                    });
        }
        catch (Exception e) {
            Logger.instance().Log(Logger.ERROR, TAG, "failed to load avatar from server");
            e.printStackTrace();
        }
    }

    private GetUserAvatarResponse.AvatarInfo loadAvatarInfo() {
        String avatarInfoJson = Globals.getPreferencesKey(getDefaultPrefs(), AVATAR_INFO_KEY);
        GetUserAvatarResponse.AvatarInfo avatarInfo = null;

        try {
            avatarInfo = new Gson().fromJson(avatarInfoJson, GetUserAvatarResponse.AvatarInfo.class);
        }
        catch (Exception e) {
            BitwalkingApp.getInstance().trackException(e);
        }

        return avatarInfo;
    }

    private void storeAvatarInfo(GetUserAvatarResponse.AvatarInfo newInfo, boolean downloadImage) {

        try {
            GetUserAvatarResponse.AvatarInfo oldInfo = loadAvatarInfo();

            if (needUpdateAvatar(oldInfo, newInfo)) {
                Globals.setPreferencesKey(getDefaultPrefs(), AVATAR_INFO_KEY, new Gson().toJson(newInfo));

                if (downloadImage) {
                    // download user avatar
                    Bitmap image = null;
                    try {
                        String avatarUrl = BitwalkingServer.SERVER_URL + "/" + newInfo.imageUri;
                        Logger.instance().Log(Logger.VERB, TAG, "Download avatar: " + avatarUrl);
                        image = BitmapFactory.decodeStream(new URL(avatarUrl).openConnection().getInputStream());
                    }
                    catch (Exception e) {
                        Logger.instance().Log(Logger.VERB, "ImagesAdapter", "Failed to load image url, set default");
                        e.printStackTrace();
                    }

                    if (null != image)
                        saveProfileImage(image);
                }
            }
        }
        catch (Exception e) {
            BitwalkingApp.getInstance().trackException(e);
        }
    }

    private boolean needUpdateAvatar(GetUserAvatarResponse.AvatarInfo oldAvatar, GetUserAvatarResponse.AvatarInfo newAvatar) {
        boolean needUpdate = false;

        if (null == oldAvatar) {
            needUpdate = true;
        }
        else {
            try {
                Date oldUpdateDate = Globals.getFullDateFormat().parse(oldAvatar.updateTimestamp);
                Date newUpdateDate = Globals.getFullDateFormat().parse(newAvatar.updateTimestamp);

                needUpdate = newUpdateDate.after(oldUpdateDate);
            } catch (Exception e) {
                BitwalkingApp.getInstance().trackException(e);
            }
        }

        return needUpdate;
    }

    //endregion

    //region First Time

    public boolean needToPushToken() {
        return getDefaultPrefs().getBoolean(NEED_PUSH_TOKEN, true);
    }

    public void setNeedToPushToken(boolean need) {
        getDefaultPrefs().edit().putBoolean(NEED_PUSH_TOKEN, need).apply();
    }

    //endregion


    //region User Info

    public static UserInfo getUserInfo(Context context) {
        UserInfo userInfo = null;

        try {
            String userInfoJson = Globals.getPreferencesKey(PreferenceManager.getDefaultSharedPreferences(context), USER_INFO);
            userInfo = new Gson().fromJson(userInfoJson, UserInfo.class);
        } catch (Exception e) {
            BitwalkingApp.getInstance().trackException("getUserInfo: failed", e);
        }

        return userInfo;
    }

    public static synchronized void setUserInfo(Context context, UserInfo userInfo) {
        String userInfoJson = new Gson().toJson(userInfo);
        Globals.setPreferencesKey(PreferenceManager.getDefaultSharedPreferences(context), USER_INFO, userInfoJson);
    }

    //endregion

    //region Service Info

    public static ServiceInitInfo getServiceInitInfo(Context context) {
        ServiceInitInfo info = new ServiceInitInfo();

        // User secret
        try {
            info.userSecret = getUserSecret(context);
        } catch (Exception e) {
            BitwalkingApp.getInstance().trackException("getServiceInitInfo: secret:", e);
        }

        // User msisdn
        try {
            info.userMsisdn = getUserId(context);
        } catch (Exception e) {
            BitwalkingApp.getInstance().trackException("getServiceInitInfo: msisdn:", e);
        }

        // User email
        try {
            info.userEmail = getUserInfo(context).getMeInfo().email;
        } catch (Exception e) {
            BitwalkingApp.getInstance().trackException("getServiceInitInfo: email:", e);
        }

        return info;
    }

    //endregion

    //region user id

    public static String getUserId(Context context) {
        return Globals.getPreferencesKey(PreferenceManager.getDefaultSharedPreferences(context), USER_ID_KEY);
    }

    public static void setUserId(Context context, String userId) {
        Globals.setPreferencesKey(PreferenceManager.getDefaultSharedPreferences(context), USER_ID_KEY, userId);
    }

    //endregion

    //region country code

    public static String getLastCountryCode(Context context) {
        return Globals.getPreferencesKey(PreferenceManager.getDefaultSharedPreferences(context), COUNTRY_CODE);
    }

    public static void setLastCountryCode(Context context, String countryCode) {
        Globals.setPreferencesKey(PreferenceManager.getDefaultSharedPreferences(context), COUNTRY_CODE, countryCode);
    }

    //endregion

    //region user secret

    public static String getUserSecret(Context context) {
        return Globals.getPreferencesKey(PreferenceManager.getDefaultSharedPreferences(context), USER_SECRET_KEY);
    }

    public static void setUserSecret(Context context, String userSecret) {
        Globals.setPreferencesKey(PreferenceManager.getDefaultSharedPreferences(context), USER_SECRET_KEY, userSecret);
    }

    //endregion

    //region invite code

    public static String getInviteAffiliationCode(Context context) {
        return Globals.getPreferencesKey(PreferenceManager.getDefaultSharedPreferences(context), USER_INVITE_CODE);
    }

    public static void setInviteAffiliationCode(Context context, String inviteCode) {
        Globals.setPreferencesKey(PreferenceManager.getDefaultSharedPreferences(context), USER_INVITE_CODE, inviteCode);
    }

    //endregion

    public void clearAll() {
        Logger.instance().Log(Logger.INFO, TAG, "clear all");

        getDefaultPrefs().edit()
                .remove(AVATAR_INFO_KEY)
                .remove(USER_ID_KEY)
                .remove(USER_SECRET_KEY)
                .remove(USER_INFO).apply();

        _currentProfileImage = null;
    }

    public AppPreferences(Context context) {
        Logger.instance().Log(Logger.INFO, TAG, "App preferences loaded by: " + context.getPackageName());

        _context = context;
    }
}
