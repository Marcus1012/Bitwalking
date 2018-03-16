package bitwalking.bitwalking.remote_service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.support.v4.content.WakefulBroadcastReceiver;

import bitwalking.bitwalking.AppPreferences;
import bitwalking.bitwalking.remote_service.server_update.UpdateServerThread;
import bitwalking.bitwalking.util.Logger;

/**
 * Created by Marcus on 12/11/16.
 */

public class BwServiceReceiver extends BroadcastReceiver {
    private static final String TAG = BwServiceReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (null != intent.getAction() && !intent.getAction().isEmpty()) {
            String action = intent.getAction();
            if (action.equals("android.location.PROVIDERS_CHANGED") ||
                action.equals("android.intent.action.BATTERY_LOW") ||
                action.equals("android.intent.action.BATTERY_OKAY")) //||
//                action.equals(PERIODIC_TASK_ACTION))
            {
                tryStartService(context, action);
            }
        }
    }

    private void tryStartService(Context context, String action) {
        AppPreferences appPrefs = new AppPreferences(context);
        if (appPrefs.isUserLoggedIn()) {
            if (!appPrefs.isSwitchedOff()) {
                Logger.instance().Log(Logger.DEBUG, TAG, action + " received, send service");

                // Start Bitwalking service
                Intent serviceIntent = new Intent(context, BwService.class);
                serviceIntent.setAction(action);
                context.startService(serviceIntent);
            }
            else {
                Logger.instance().Log(Logger.DEBUG, TAG, action + ": app is switched off");
            }
        }
        else {
            Logger.instance().Log(Logger.DEBUG, TAG, action + ": no logged in user");
        }
    }
}
