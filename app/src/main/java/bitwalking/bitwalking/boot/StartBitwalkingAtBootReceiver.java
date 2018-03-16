package bitwalking.bitwalking.boot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import bitwalking.bitwalking.AppPreferences;
import bitwalking.bitwalking.remote_service.BwService;
import bitwalking.bitwalking.util.Globals;
import bitwalking.bitwalking.util.Logger;

/**
 * Created by Marcus on 1/30/16.
 */
public class StartBitwalkingAtBootReceiver extends BroadcastReceiver {
    final static String TAG = StartBitwalkingAtBootReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED) ||
            intent.getAction().equals(Intent.ACTION_MY_PACKAGE_REPLACED)) {
            tryStartService(context, intent.getAction());
        }
    }

    private void tryStartService(Context context, String action) {
        AppPreferences appPrefs = new AppPreferences(context);
        if (appPrefs.isUserLoggedIn()) {
            if (!appPrefs.isSwitchedOff()) {
                Logger.instance().Log(Logger.DEBUG, TAG, "Boot complete, load bitwalking service");

                // Start Bitwalking service
                Intent serviceIntent = new Intent(context, BwService.class);
                serviceIntent.setAction(Globals.START_SERVICE_ACTION);
                context.startService(serviceIntent);
            }
            else {
                Logger.instance().Log(Logger.DEBUG, TAG, "Boot complete, app is switched off");
            }
        }
        else {
            Logger.instance().Log(Logger.DEBUG, TAG, "Boot complete, no logged in user");
        }
    }
}