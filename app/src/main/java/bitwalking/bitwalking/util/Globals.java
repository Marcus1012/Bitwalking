package bitwalking.bitwalking.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextPaint;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import bitwalking.bitwalking.AppPreferences;
import bitwalking.bitwalking.BitwalkingApp;
import bitwalking.bitwalking.BuildConfig;
import bitwalking.bitwalking.activityes.MainActivity;
import bitwalking.bitwalking.R;
import bitwalking.bitwalking.registration_and_login.IsoToPhone;
import bitwalking.bitwalking.registration_and_login.Utilities;

/**
 * Created by Marcus Greenberg on 10/2/15.
 *.........................
 *.........................
 *...............###.......
 *..............#   #......
 *..............#   #......
 *.......###....#   #......
 *......#   #...#   #......
 *......#   #...#   #......
 *......#   #....###.......
 *......#   #..............
 *......#   #..............
 *.......###...............
 *.........................
 *.........................
 *.......BitWalking ©......
 *.........................
 */
public enum Globals {
    INSTANCE;

    private static final String TAG = Globals.class.getSimpleName();

    public static final String BITWALKING_USER_INFO             = "Bitwalking.UserInfo";
    public static final String BITWALKING_SERVICE_INIT_INFO     = "Bitwalking.Service.InitInfo";

    public static final String BITWALKING_JUST_BIND             = "bitwalking.bitwalking.Bind.Service";

    public static final String BITWALKING_PRE_LOGOUT_BROADCAST  = "Bitwalking.PreLogout";
    public static final String BITWALKING_LOGOUT_BROADCAST      = "Bitwalking.Logout";
    public static final String BITWALKING_SWITCH_OFF_BROADCAST  = "Bitwalking.SwitchOff";

    public static final String BITWALKING_OPEN_URI              = "Bitwalking.Open.Uri";
    public static final String BITWALKING_STORE                 = "Bitwalking.Store";
    public static final String BITWALKING_PLAY_STORE            = "PlayStore";
    public static final String BITWALKING_BALANCE               = "Balance";
    public static final String BITWALKING_USER_INVITE           = "Bitwalking.UserInvite";
    public static final String BITWALKING_EVENT                 = "Bitwalking.Events";
    public static final String BITWALKING_SEND_LOGS             = "SendLogs";
    public static final String BITWALKING_FORCE_SEND_LOGS       = "SendLogs!";
    public static final String BITWALKING_FORCE_UPDATE          = "Update!";
    public static final String BITWALKING_EVENT_ID              = "Event.Id";

    public static final String LOGIN_ACTIVITY_USERNAME_EXTRA    = "Login.UserName";
    public static final String LOGIN_ACTIVITY_PASSWORD_EXTRA    = "Login.Password";
    public static final String LOGIN_ACTIVITY_USER_SECRET_EXTRA = "Login.UserSecret";
    public static final String LOGIN_ACTIVITY_USER_MSISDN_EXTRA = "Login.UserMsisdn";
    public static final String LOGIN_ACTIVITY_USER_EMAILMD5_EXTRA = "Login.UserEmailmd5";

    public static String START_SERVICE_ACTION = "bitwalking.bitwalking.bw_service.action.start";
    public static String INIT_SERVICE_ACTION = "bitwalking.bitwalking.bw_service.action.init";
    public static String STOP_SERVICE_ACTION = "bitwalking.bitwalking.bw_service.action.stop";
    public static String LOGOUT_SERVICE_ACTION = "bitwalking.bitwalking.bw_service.action.logout";
    public static String SWITCH_OFF_SERVICE_ACTION = "bitwalking.bitwalking.bw_service.action.switchoff";

    public static final int STEPS_SOURCE_PHONE = 1;
    public static final int STEPS_SOURCE_MISFIT = 2;

    // Log from remote service to file
    public static final boolean LOG_TO_FILE = true;
    // Events on/off
    public static final boolean EVENTS_ON = BuildConfig.DEBUG || true;
    // Transactions on/off
    public static final boolean TRANSACTIONS_ON = BuildConfig.DEBUG || false;
    // Mining history on/off
    public static final boolean MINING_HISTORY_ON = BuildConfig.DEBUG || true;

    public static String bigDecimalToNiceString(BigDecimal number) {
        return String.valueOf(number.setScale(2, BigDecimal.ROUND_DOWN));
    }

    // Hide keyboard
    public static void hideSoftKeyboard(Activity activity) {
        if(activity.getCurrentFocus() != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
        }
    }

    // Show keyboard
    public static void showSoftKeyboard(Activity activity) {
        if(activity.getCurrentFocus() != null) {
            InputMethodManager inputMgr = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMgr.toggleSoftInput(0, InputMethodManager.SHOW_FORCED);
        }
    }

    public static Bitmap getScreenShot(Activity activity) {
        View rootView = activity.getWindow().getDecorView().findViewById(android.R.id.content);
        View screenView = rootView.getRootView();
        screenView.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(screenView.getDrawingCache());
        screenView.setDrawingCacheEnabled(false);

        return bitmap;
    }

    public static void showSimpleAlertMessage(Context context, String title, String message, String button) {
        try {
            // Show alert message
            AlertDialog.Builder dlgAlert = new AlertDialog.Builder(context);

            dlgAlert.setTitle(title);
            dlgAlert.setMessage(message);
            dlgAlert.setPositiveButton(button, null);
            dlgAlert.setCancelable(true);
            dlgAlert.create().show();
        }
        catch (Exception e) {
            BitwalkingApp.getInstance().trackException(new Exception("failed to show alert dialog", e));
        }
    }

    public static Date getZeroTimeDate(Date dateToZeroTime) {
        Date res;
        Calendar calendar = Calendar.getInstance();

        calendar.setTime( dateToZeroTime );
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        res = calendar.getTime();

        return res;
    }

    public static DateFormat getUTCDateFormat() {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH);
        df.setTimeZone(tz);
        return df;
    }

    public static DateFormat getFullDateFormat() {
        TimeZone tz = TimeZone.getDefault();//getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH);
        df.setTimeZone(tz);
        return df;
    }

    public static DateFormat getDateOfBirthDisplayFormat() {
        DateFormat df = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        return df;
    }

    //region Permissions

    public static boolean havePermission(Activity activity, String permission, final int requestId) {
        boolean granted = true;
        int res = ContextCompat.checkSelfPermission(activity, permission);
        if (PackageManager.PERMISSION_GRANTED != res) {
            Logger.instance().Log(Logger.DEBUG, TAG, "request permissions: " + permission);
            ActivityCompat.requestPermissions(activity, new String[]{ permission }, requestId);
            granted = false;
        }

        return granted;
    }

    //endregion

    //region os info

    public static boolean isBrokenSamsungDevice() { // Check stackoverflow if you need more info
        return (Build.MANUFACTURER.equalsIgnoreCase("Samsung")
                && isBetweenAndroidVersions(
                Build.VERSION_CODES.LOLLIPOP,
                Build.VERSION_CODES.LOLLIPOP_MR1));
    }

    public static boolean isBetweenAndroidVersions(int min, int max) {
        return Build.VERSION.SDK_INT >= min && Build.VERSION.SDK_INT <= max;
    }

    public static String getOsVersion() {
        StringBuilder builder = new StringBuilder();
        builder.append("[").append(Build.VERSION.RELEASE);

        Field[] fields = Build.VERSION_CODES.class.getFields();
        for (Field field : fields) {
            String fieldName = field.getName();
            int fieldValue = -1;

            try {
                fieldValue = field.getInt(new Object());
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                e.printStackTrace();
            }

            if (fieldValue == Build.VERSION.SDK_INT) {
                builder.append("][").append(fieldName).append("][");
                builder.append("sdk=").append(fieldValue).append("]");
            }
        }

        return builder.toString();
    }

    //endregion

    //region Preferences

    public static synchronized void setPreferencesKey(SharedPreferences sharedPreferences, String key, String value) {
        sharedPreferences.edit()
        .putString(key, value)
        .apply();
    }

    public static synchronized String getPreferencesKey(SharedPreferences sharedPreferences, String key) {
        return sharedPreferences.getString(key, null);
    }

    public static synchronized void deletePreferencesKey(SharedPreferences sharedPreferences, String key) {
        sharedPreferences.edit()
        .remove(key)
        .apply();
    }

    //endregion

    //region Helpers

    public void fitText(TextView textView, int width, String text) {
        TextPaint textPaint = textView.getPaint();
        float maxWidth = (float)(width) * 0.8f;
        float currentWidthSum = 0;
        String[] words = text.split("\\s");
        String finalText = "";

        for (String word : words) {
            float wordWidth = textPaint.measureText(word);

            if (currentWidthSum + wordWidth > maxWidth) { // new line
                finalText += "\n";
                currentWidthSum = 0;
            }

            currentWidthSum += wordWidth + 1;
            finalText += word + " ";
        }

        textView.setText(finalText);
    }

    public static byte[] bitmapToBytes(final Bitmap img) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        img.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return baos.toByteArray();
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static String getUserCountryCode(Context context) {
        String lastCountryCode = AppPreferences.getLastCountryCode(context);
        if (null != lastCountryCode && Utilities.isPhoneCodeValid(lastCountryCode)) {

        }
        else {
            String phoneCode = IsoToPhone.getPhone(context.getResources().getConfiguration().locale.getCountry());
            if (null != phoneCode && phoneCode.length() > 0)
                lastCountryCode = phoneCode;
        }

        return lastCountryCode;
    }

    public static String getTimeDifferenceInNiceString(Date start, Date end) {
        if (end.before(start))
            return "passed";

        long diff = end.getTime() - start.getTime();
        long days = TimeUnit.MILLISECONDS.toDays(diff);
        long hours = TimeUnit.MILLISECONDS.toHours(diff);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);

        String timeDiff = "";

        if (days > 0)
            timeDiff = String.format("%d days", days);
        else if (hours > 0)
            timeDiff = String.format("%d hours", hours);
        else
            timeDiff = String.format("%d minutes", (minutes > 0 ? minutes : 1));

        return timeDiff;
    }

    //endregion

    public static Uri getDownloadLink() {
        return new Uri.Builder()
                .scheme("http")
                .authority("dl.bitwalking.com")
                .build();
    }

    public static void openAppPlayStore(Context context) {
        final String appPackageName = context.getPackageName(); // getPackageName() from Context or Activity object
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
        } catch (android.content.ActivityNotFoundException anfe) {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
        }
    }

    public static void showServerConnectionErrorMsg(Context context) {
        Toast.makeText(context, "Cannot connect bitwalking network", Toast.LENGTH_SHORT).show();
    }

    public static String getNameInitials(String name) {
        String initials = "";
        String words[] = name.split(" ");
        for (String w : words)
            initials += w.substring(0, 1);

        return initials;
    }

    public static void backWithMsg(Activity activity, String msg) {
        activity.onBackPressed();
        Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
    }

    public static Date getMigrationDate() {
        Date migrationDate = null;
        try {
            migrationDate = getUTCDateFormat().parse("2016-09-30T00:00:00.000Z");
        }
        catch (Exception e) {
        }

        return migrationDate;
    }

    public static int getNotificationIcon() {
        boolean whiteIcon = (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP);
        return whiteIcon ? R.drawable.notification_icon : R.drawable.notification_icon;
    }

    private static final int FORCE_UPDATE_NOTIFICATION_ID = 3;
    public static void notifyForceUpdate(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(BITWALKING_FORCE_UPDATE, true);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(getNotificationIcon())
                .setContentTitle("Update available now!")
                .setContentText("Go to Play Store")
                .setAutoCancel(true)
                .setOngoing(true)
                .setContentIntent(pendingIntent);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            notificationBuilder.setColor(0xFF000000);
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(FORCE_UPDATE_NOTIFICATION_ID, notificationBuilder.build());
    }

    public static void hideForceUpdateNotification(Context context) {
        NotificationManager nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(FORCE_UPDATE_NOTIFICATION_ID);
    }
}
