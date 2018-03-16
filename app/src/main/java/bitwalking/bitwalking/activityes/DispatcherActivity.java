package bitwalking.bitwalking.activityes;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.appinvite.AppInvite;
import com.google.android.gms.appinvite.AppInviteInvitationResult;
import com.google.android.gms.appinvite.AppInviteReferral;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;

import java.util.List;
import java.util.Set;

import bitwalking.bitwalking.AppPreferences;
import bitwalking.bitwalking.BitwalkingApp;
import bitwalking.bitwalking.invite_user.InvitationRegistrationActivity;
import bitwalking.bitwalking.mvi.welcome.ActivityWelcome;
import bitwalking.bitwalking.registration_and_login.GoActivity;
import bitwalking.bitwalking.registration_and_login.JoinActivity;
import bitwalking.bitwalking.mvi.reset_password.ResetPasswordActivity;
import bitwalking.bitwalking.util.Globals;
import bitwalking.bitwalking.util.Logger;

/**
 * Created by Marcus on 11/19/15.
 *
 * This activity loaded first and decides which activity the app should start with.
 * The activity also receives bitwalking URI and launches the relevant activity.
 * URI example:
 *      - http://bitwalking.com/go-app/ResetPassword?id=ae2821a6-fcf4-4a2a-a54d-fc2d4318cf5b
 *        Will launch the reset password activity and pass it the id value
 * There is MapUriToActivity class to map uri to activity launch, it was added later thus it is handled last
 *      *** Consider Moving all uri handle inside MapUriToActivity ***
 *
 * The activity also handled firebase invitation once, but it is not used now - keep it for future use
 */
public class DispatcherActivity extends FragmentActivity implements GoogleApiClient.OnConnectionFailedListener {

    private final String TAG = DispatcherActivity.class.getSimpleName();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        handleInvitation();

        if (checkIfFirstTime()){
            startWelcome();
        }else {
            handleIntent(getIntent());
        }
    }

    private boolean checkIfFirstTime() {
        AppPreferences appPrefs = new AppPreferences(this);

        if (appPrefs.isFirstTime()) {
            ((BitwalkingApp)getApplication()).trackEvent("app", "launch", "target.first");
            appPrefs.setFirstTime();
            return true;
        }

        return false;
    }

    private void startWelcome(){
        Intent intent = new Intent(this,ActivityWelcome.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(getIntent());

        super.onNewIntent(intent);
    }

    private void handleIntent(Intent intent) {
        String resetCode = null;
        String emailValidationId = null, emailValidationUser = null, affiliationCode = null, invitationId = null;
        boolean welcome = false;
        boolean openStore = false;
        boolean openBalance = false;
        boolean newIntentHandled = false;
        String openUri = null;
        Uri intentData = intent.getData();

        if (intentData != null) {
            boolean justLaunch = true;
            Logger.instance().Log(Logger.DEBUG, TAG, "uri: " + openUri);

            List<String> segments = intentData.getPathSegments();
            Set<String> queries = intentData.getQueryParameterNames();

            for (String s : segments) {
                Logger.instance().Log(Logger.VERB, TAG, "segments: " + s);
            }
            for (String s : queries) {
                Logger.instance().Log(Logger.VERB, TAG, "query: " + s);
            }

            if (segments.size() > 1) {
                if (segments.get(0).contentEquals("go-app")) {
                    if (segments.get(1).contentEquals("ResetPassword")) {
                        justLaunch = false;
                        ((BitwalkingApp)getApplication()).trackEvent("app", "launch", "target.password.reset");
                        String resetId = intentData.getQueryParameter("id");
                        if (null != resetId && resetId.length() > 0)
                            resetCode = resetId;
                    } else if (segments.get(1).contentEquals("EmailValidation")) {
                        justLaunch = false;
                        ((BitwalkingApp)getApplication()).trackEvent("app", "launch", "target.email.validation");
                        String validationId = intentData.getQueryParameter("id");
                        if (null != validationId && validationId.length() > 0)
                            emailValidationId = validationId;

                        String user = intentData.getQueryParameter("user");
                        if (null != user && user.length() > 0)
                            emailValidationUser = user;
                    }
                    else if (segments.get(1).contentEquals("welcome-to-bitwalking")) {
                        justLaunch = false;
                        welcome = true;
                    }
                    else if (segments.get(1).contentEquals("UserInvite")) {
                        String code = intentData.getQueryParameter("code");
                        invitationId = intentData.getQueryParameter("invitation_id");
                        if (null != code && code.length() > 0) {
                            affiliationCode = code;
                            justLaunch = false;
                        }
                    }
                    else if (segments.get(1).contentEquals("store")) {
                        justLaunch = false;
                        openStore = true;
                        ((BitwalkingApp)getApplication()).trackEvent("app", "launch", "target.store");
                    }
                    else if (segments.get(1).contentEquals("balance")) {
                        justLaunch = false;
                        openBalance = true;
                        ((BitwalkingApp)getApplication()).trackEvent("app", "launch", "target.balance");
                    }
                    else {
                        // New uri handling
                        String activityUri = segments.get(1);
                        Class<? extends Activity> activity = MapUriToActivity.getActivityClass(activityUri);

                        if (null != activity) {
                            // Launch activity
                            Intent newIntent = new Intent(DispatcherActivity.this, activity);
                            newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            addQueryParamsToIntent(newIntent, intentData);
                            startActivity(newIntent);
                            newIntentHandled = true;
                        }
                    }
                }
            }

            if (justLaunch) {
                openUri = intentData.toString();
                if (null != openUri && !openUri.isEmpty())
                    ((BitwalkingApp) getApplication()).trackEvent("app", "launch", "target.uri");
                else
                    ((BitwalkingApp) getApplication()).trackEvent("app", "launch", "target.main");
            }
        }
        else {
            ((BitwalkingApp)getApplication()).trackEvent("app", "launch", "target.none");
        }

        AppPreferences appPrefs = new AppPreferences(getBaseContext());
        boolean userLoggedIn = appPrefs.isUserLoggedIn();

        if (null != resetCode) {
            Intent newIntent = new Intent(this, ResetPasswordActivity.class);
            newIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            newIntent.putExtra(ResetPasswordActivity.RESET_PASSWORD_CODE, resetCode);
            startActivity(newIntent);
        } else if (null != emailValidationId && null != emailValidationUser) {
            Intent newIntent = new Intent(this, GoActivity.class);
            newIntent.putExtra(GoActivity.VALIDATION_EMAIL, emailValidationUser);
            newIntent.putExtra(GoActivity.VALIDATION_TOKEN, emailValidationId);
            newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(newIntent);
        }
        else if (null != affiliationCode && !userLoggedIn) {
//            Intent newIntent = new Intent(this, InvitationRegistrationActivity.class);
//            newIntent.putExtra(InvitationRegistrationActivity.INVITE_AFFILIATION_CODE, affiliationCode);
//            newIntent.putExtra(InvitationRegistrationActivity.INVITATION_ID, invitationId);
//            startActivity(newIntent);
            ((BitwalkingApp)getApplication()).trackEvent("app", "launch", "target.invite");

            Intent newIntent = new Intent(this, JoinActivity.class);
            newIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            newIntent.putExtra(InvitationRegistrationActivity.INVITE_AFFILIATION_CODE, affiliationCode);
            newIntent.putExtra(InvitationRegistrationActivity.INVITATION_ID, invitationId);
            startActivity(newIntent);
        }
        else if (welcome) {
            startActivity(new Intent(this, WelcomeActivity.class));
        }
        else if (!newIntentHandled) {
            Intent goIntent = new Intent(this, GoActivity.class);
            goIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            if (openStore)
                goIntent.putExtra(Globals.BITWALKING_STORE, true);
            if (openBalance)
                goIntent.putExtra(Globals.BITWALKING_BALANCE, true);
            if (null != openUri && !openUri.isEmpty())
                goIntent.putExtra(GoActivity.OPEN_URI, openUri);

            startActivity(goIntent);
        }

        finish();
    }

    private void addQueryParamsToIntent(Intent intent, Uri uri) {
        for (String name : uri.getQueryParameterNames()) {
            String value = uri.getQueryParameter(name);
            if (Boolean.parseBoolean(value))
                intent.putExtra(name, true);
            else
                intent.putExtra(name, value);
        }
    }

    //region Google Firebase Invitation

    GoogleApiClient _googleApiClient;
    private void handleInvitation() {
        // Create an auto-managed GoogleApiClient with access to App Invites.
        _googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(AppInvite.API)
                .enableAutoManage(this, this)
                .build();

        // Check for App Invite invitations and launch deep-link activity if possible.
        // Requires that an Activity is registered in AndroidManifest.xml to handle
        // deep-link URLs.
        boolean autoLaunchDeepLink = true;
        AppInvite.AppInviteApi.getInvitation(_googleApiClient, this, autoLaunchDeepLink)
                .setResultCallback(
                        new ResultCallback<AppInviteInvitationResult>() {
                            @Override
                            public void onResult(AppInviteInvitationResult result) {
                                Logger.instance().Log(Logger.DEBUG, TAG, "getInvitation:onResult:" + result.getStatus());
                                if (result.getStatus().isSuccess()) {
                                    // Extract information from the intent
                                    Intent intent = result.getInvitationIntent();
                                    String deepLink = AppInviteReferral.getDeepLink(intent);
                                    String invitationId = AppInviteReferral.getInvitationId(intent);

                                    Logger.instance().Log(Logger.INFO, TAG, String.format("deep link = [%s] invitation id = [%s]", deepLink, invitationId));
                                }
                            }
                        });
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Logger.instance().Log(Logger.DEBUG, "Dispatcher", "onConnectionFailed");
    }

    //endregion
}