package bitwalking.bitwalking.registration_and_login;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.AccelerateInterpolator;
import android.widget.ProgressBar;
import android.widget.Toast;

import bitwalking.bitwalking.AppPreferences;
import bitwalking.bitwalking.R;
import bitwalking.bitwalking.activityes.MainActivity;
import bitwalking.bitwalking.activityes.WebActivity;
import bitwalking.bitwalking.mvi.login.LoginActivity;
import bitwalking.bitwalking.server.api.ServerApi;
import bitwalking.bitwalking.server.requests.ValidateEmailRequest;
import bitwalking.bitwalking.util.Globals;
import bitwalking.bitwalking.util.Logger;

import static android.view.View.VISIBLE;


/**
 * Created by Marcus on 11/22/15.
 *
 * This activity decides if to load login screen or there is a user logged in and we can move to main activity
 *
 * Before deciding which activity goes next, it will check if the device is compatible with our requirements
 * We would like that user will run bitwalking app only on smart phones - telephone
 * Required feature store inside _mustHaveFeatures array
 *
 * The activity also handles two extra options:
 *  - email validation: when user clicks on email verification url and launches it with bitwalking app
 *  - url load using in app browser: also when user clicks on url
 */
public class GoActivity extends Activity {

    private static final String TAG = GoActivity.class.getSimpleName();

    public static final String VALIDATION_EMAIL = "Email";
    public static final String VALIDATION_TOKEN = "Token";
    public static final String OPEN_URI         = "Uri";

    private boolean _cancelLogin = false;
    private ProgressBar _progress;
    AlertDialog _alert;

    String[] _mustHaveFeatures = new String[] {
            PackageManager.FEATURE_TELEPHONY,
            PackageManager.FEATURE_MICROPHONE,
            PackageManager.FEATURE_LOCATION_GPS,
//            PackageManager.FEATURE_SENSOR_ACCELEROMETER,
//            PackageManager.FEATURE_SENSOR_GYROSCOPE,
            //PackageManager.FEATURE_SENSOR_STEP_DETECTOR
//            PackageManager.FEATURE_SENSOR_STEP_COUNTER
    };


    private View rootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.go_activity);
       // overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        _progress = (ProgressBar)findViewById(R.id.go_activity_progress);
        rootView = findViewById(R.id.rootView);


    }

    @Override
    protected void onResume() {
        super.onResume();

        _cancelLogin = false;

        if (!checkDeviceCompatibility()) {
            AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(this);

            dlgAlert.setMessage("Sorry, your device is lack of one or more our required features.");
            dlgAlert.setTitle("Device not compatible");
            dlgAlert.setCancelable(false);
            dlgAlert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    GoActivity.this.finish();
                }
            });

            dlgAlert.create().show();
        }
        else {
            if (getIntent().hasExtra("logoutMessage")) {
                final String alertMessage = getIntent().getStringExtra("logoutMessage");
                AlertDialog.Builder builder = new AlertDialog.Builder(GoActivity.this);
                builder.setMessage(alertMessage)
                        .setCancelable(false)
                        .setPositiveButton("Dismiss", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                loadLoginScreen();
                            }
                        });

                if (null != _alert) {
                    _alert.dismiss();
                    _alert = null;
                }
                
                _alert = builder.create();
                _alert.show();
            }
            else if (handleIntent(getIntent())) {
                // nothing to do ...
            }
            else {


                new Handler().postDelayed(new Runnable(){
                    @Override
                    public void run() {
                        if (new AppPreferences(getBaseContext()).isUserLoggedIn()) {
                            loadMainScreen();

                        } else {
                            loadLoginScreen();
                        }

                    }
                }, 3000);


            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        _cancelLogin = true;

//        overridePendingTransition(R.anim.screen_fade_in, R.anim.screen_fade_out);
    }

    private void loadLoginScreen() {

        if (!_cancelLogin) {
            Intent intent = new Intent(GoActivity.this, LoginActivity.class);
            intent.putExtra("CoolAnimation", true);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }

        new Handler().postDelayed(new Runnable(){
            @Override
            public void run() {
                try {
                    GoActivity.this.finish();
                }catch (Exception e){

                }

            }
        }, 1500);

    }

    private void loadMainScreen() {
        if (!_cancelLogin) {




                    Intent mainIntent = new Intent(GoActivity.this, MainActivity.class);
                    Intent goIntent = getIntent();


                    mainIntent.putExtra(Globals.BITWALKING_BALANCE, goIntent.getBooleanExtra(Globals.BITWALKING_BALANCE, false));
                    mainIntent.putExtra(Globals.BITWALKING_STORE, goIntent.getBooleanExtra(Globals.BITWALKING_STORE, false));
                    mainIntent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

                     mainIntent.putExtra("CoolAnimation", true);
                   // overridePendingTransition(0, 0);

                     startActivity(mainIntent);


            new Handler().postDelayed(new Runnable(){
                @Override
                public void run() {
                    try {
                        GoActivity.this.finish();
                    }catch (Exception e){

                    }

                }
            }, 1500); //400 - animation delay in main screen

                }





    }

    @Deprecated
    protected void revealActivity(View rootLayout,AnimatorListenerAdapter listenerAdapter) {

        int cx = rootLayout.getWidth() / 2;
        int cy = rootLayout.getHeight() / 2;

        float finalRadius = Math.max(rootLayout.getWidth(), rootLayout.getHeight());


        Animator circularReveal = ViewAnimationUtils.createCircularReveal(rootLayout, cx, cy, 0, finalRadius);
        circularReveal.setDuration(400);
        circularReveal.setInterpolator(new AccelerateInterpolator());
        circularReveal.addListener(listenerAdapter);


        rootLayout.setVisibility(VISIBLE);
        circularReveal.start();

    }


    private void loadWebView(String uri) {
        Intent webIntent = new Intent(this, WebActivity.class);
        webIntent.putExtra("url", uri);
        startActivity(webIntent);

        GoActivity.this.finish();
    }

    private boolean checkDeviceCompatibility() {
        boolean good = true;

        for (String feature : _mustHaveFeatures) {
            if (false == getPackageManager().hasSystemFeature(feature)) {
                good = false;
                //break;
                Logger.instance().Log(Logger.DEBUG, TAG, "no: " + feature);
            }
        }

        return good;
    }

    private boolean handleIntent(Intent intent) {
        boolean handled = false;

        if (intent.hasExtra(VALIDATION_EMAIL) &&
            intent.hasExtra(VALIDATION_TOKEN)) {
            String email = intent.getStringExtra(VALIDATION_EMAIL);
            String token = intent.getStringExtra(VALIDATION_TOKEN);
            validateEmail(email, token);
            handled = true;
        }
        else if (intent.hasExtra(OPEN_URI)) {
            String uri = intent.getStringExtra(OPEN_URI);
            loadWebView(uri);
            handled = true;
        }

        return handled;
    }

    private void validateEmail(final String email, final String token) {
        // Showing progress dialog
        _progress = (ProgressBar)findViewById(R.id.go_activity_progress);
        _progress.setVisibility(VISIBLE);
        ValidateEmailRequest validateEmailReq = new ValidateEmailRequest(email, token);

        try {
            ServerApi.validateEmail(
                    AppPreferences.getUserId(getBaseContext()),
                    AppPreferences.getUserSecret(getBaseContext()),
                    validateEmailReq,
                    new ServerApi.SimpleServerResponseListener() {
                        @Override
                        public void onResponse(final int code) {
                            GoActivity.this.runOnUiThread(new Runnable() {
                                public void run() {
                                    String msg = "Email confirmation failed";
                                    switch (code) {
                                        case 200:
                                            msg = "Email confirmed";
                                            break;
                                        case 400: // illegal parameter
                                        case 403: // permission denied
                                            break;
                                        default:
                                            break;
                                    }

                                    if (null != _progress)
                                        _progress.setVisibility(View.GONE);
                                    Toast.makeText(GoActivity.this, msg, Toast.LENGTH_SHORT).show();
                                    GoActivity.this.onBackPressed();
                                }
                            });
                        }
                    });
        }
        catch (Exception e) {
            Logger.instance().Log(Logger.ERROR, TAG, "failed to send validate email put");
            e.printStackTrace();
            if (null != _progress)
                _progress.setVisibility(View.GONE);
        }
    }




}
