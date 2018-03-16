package bitwalking.bitwalking.notifications;

import android.content.Context;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.google.gson.Gson;

import java.util.HashMap;

import bitwalking.bitwalking.AppPreferences;
import bitwalking.bitwalking.server.BitwalkingServer;
import bitwalking.bitwalking.server.OnServerResponse;
import bitwalking.bitwalking.server.api.ServerApi;
import bitwalking.bitwalking.server.requests.EmptyRequest;
import bitwalking.bitwalking.server.responses.BasicServerResponse;
import bitwalking.bitwalking.server.responses.GetSurveyResponse;

/**
 * Created by Marcus on 6/13/16.
 */
public class BitwalkingFirebaseInstanceIDService extends FirebaseInstanceIdService {

    private static final String TAG = BitwalkingFirebaseInstanceIDService.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Refreshed token: " + refreshedToken);

        updateServer(refreshedToken, getBaseContext());
    }

    public static void updateServer(String token, Context context) {
        ServerApi.pushToken(
                AppPreferences.getUserId(context),
                AppPreferences.getUserSecret(context),
                token,
                new ServerApi.SimpleServerResponseListener() {
                    @Override
                    public void onResponse(int code) {
                        // TODO: maybe log if code is invalid
                    }
                });
    }
}