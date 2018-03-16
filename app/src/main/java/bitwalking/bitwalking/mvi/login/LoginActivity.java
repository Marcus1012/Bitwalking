package bitwalking.bitwalking.mvi.login;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.Gson;

import java.math.BigDecimal;

import bitwalking.bitwalking.AppPreferences;
import bitwalking.bitwalking.BitwalkingApp;
import bitwalking.bitwalking.BuildConfig;
import bitwalking.bitwalking.R;
import bitwalking.bitwalking.activityes.MainActivity;
import bitwalking.bitwalking.mvi.forgot_password.ForgotPasswordActivity;
import bitwalking.bitwalking.mvi.registration.ActivityFirstName;
import bitwalking.bitwalking.registration_and_login.GoActivity;
import bitwalking.bitwalking.registration_and_login.JoinActivity;
import bitwalking.bitwalking.registration_and_login.Utilities;
import bitwalking.bitwalking.remote_service.BwService;
import bitwalking.bitwalking.server.api.ServerApi;
import bitwalking.bitwalking.server.requests.LoginRequest;
import bitwalking.bitwalking.server.responses.RegistrationResponse;
import bitwalking.bitwalking.user_info.MeInfo;
import bitwalking.bitwalking.user_info.UserInfo;
import bitwalking.bitwalking.util.ActivityUtils;
import bitwalking.bitwalking.util.Globals;
import bitwalking.bitwalking.util.Logger;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

import static bitwalking.bitwalking.util.AnimUtils.onCreateActivityAnimation;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = LoginActivity.class.getSimpleName();

    EditText _editEmail, _editPassword;
    ProgressDialog _progress;
    boolean _showCoolReveal = false;
    View _rootLayout, _loginButton;

    Gson _gson = null;

    // Login info
    String _email;
    String _password;

    // User info after login
    UserInfo _userInfo;


    private View rootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.login_activity);


       rootView = findViewById(R.id.coordinator);
       // getWindow().setBackgroundDrawableResource(R.drawable.login_background);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && getIntent().getBooleanExtra("CoolAnimation",false)) {
            rootView.setVisibility(View.INVISIBLE);
            onCreateActivityAnimation(rootView, new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    ActivityUtils.ColorizeStatusBar(LoginActivity.this,android.R.color.white);
                }
            });
        } else {
            rootView.setVisibility(View.VISIBLE);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        }


        if (BuildConfig.DEBUG) {
           // findViewById(R.id.login_debug_text).setVisibility(View.VISIBLE);
        }





        _loginButton = findViewById(R.id.login_button);
        _editEmail = (EditText)findViewById(R.id.login_email);
        _editEmail.addTextChangedListener(_emailPasswordTextWatcher);
        _editPassword = (EditText)findViewById(R.id.login_password);
        _editPassword.addTextChangedListener(_emailPasswordTextWatcher);
        _editPassword.setTransformationMethod(new PasswordTransformationMethod());
        _editPassword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE) {
                    onLoginClick(textView);
                    return true;
                }
                return false;
            }
        });
        findViewById(R.id.login_sign_up_text).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSignUpClick(v);
            }
        });








        _gson = new Gson();

        if (handleIntent(getIntent())) {
            // Do nothing
        }
        else if (new AppPreferences(getBaseContext()).isUserLoggedIn()) {
            startActivity(new Intent(LoginActivity.this, GoActivity.class));
            LoginActivity.this.finish();
        }

        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        // Fix password hint text :\ stupid ....
        EditText password = (EditText) findViewById(R.id.login_password);
        password.setTypeface(Typeface.DEFAULT);
        password.setTransformationMethod(new PasswordTransformationMethod());

       // overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

        checkIfUserRegistering();

        if (_showCoolReveal) {
//            _rootLayout = findViewById(R.id.login_main_layout);
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                overridePendingTransition(R.anim.do_not_move, R.anim.fade_out);
//
//                if (savedInstanceState == null) {
//                    _rootLayout.setVisibility(View.INVISIBLE);
//
//                    ViewTreeObserver viewTreeObserver = _rootLayout.getViewTreeObserver();
//                    if (viewTreeObserver.isAlive()) {
//                        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
//                            @Override
//                            public void onGlobalLayout() {
//                                enterReveal();
//                                _rootLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
//                            }
//                        });
//                    }
//                }
//            }
        }
    }

    private TextWatcher _emailPasswordTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (_editEmail.getText().toString().isEmpty() || _editPassword.getText().toString().isEmpty())
                _loginButton.setEnabled(false);
            else
                _loginButton.setEnabled(true);
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };



    void destroyProgress() {
        if (null != _progress && _progress.isShowing())
            _progress.dismiss();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        destroyProgress();
    }

    private void validateSession(final JoinActivity.RegistrationSessionInfo sessionInfo) {
        ServerApi.getRegistrationSession(
                sessionInfo.sessionId,
                new ServerApi.SimpleServerResponseListener() {
                    @Override
                    public void onResponse(final int code) {
                        if (200 == code) {
                            LoginActivity.this.runOnUiThread(new Runnable() {
                                public void run() {
                                    // move to complete user's registration
                                    final String continueRegistrationMsg = String.format("Please finish registration");
                                    AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                                    builder.setMessage(continueRegistrationMsg)
                                            .setCancelable(true)
                                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id) {
                                                    Intent intent = new Intent(LoginActivity.this, JoinActivity.class);
                                                    intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                                    intent.putExtra("resume", true);
                                                    startActivity(intent);
                                                }
                                            })
                                            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                }
                                            });
                                    AlertDialog alert = builder.create();
                                    alert.show();
                                }
                            });
                        }
                        else {
                            new AppPreferences(LoginActivity.this).clearRegistrationSession();
                        }
                    }
                }
        );
    }

    private void checkIfUserRegistering() {
        JoinActivity.RegistrationSessionInfo sessionInfo =
                _gson.fromJson(new AppPreferences(this).getRegistrationSession(), JoinActivity.RegistrationSessionInfo.class);

        if (null != sessionInfo) {
            if (null != sessionInfo.email && !TextUtils.isEmpty(sessionInfo.email) &&
                null != sessionInfo.sessionId && !TextUtils.isEmpty(sessionInfo.sessionId) &&
                null != sessionInfo.phone &&
                    null != sessionInfo.phone.number && null != sessionInfo.phone.countryCode) {
                validateSession(sessionInfo);
            }
        }
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        try {
            super.startActivityForResult(intent, requestCode);
        } catch (Exception e) {
            BitwalkingApp.getInstance().trackException(new Exception("startActivityForResult failed, req=" + requestCode, e));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        ((BitwalkingApp)getApplication()).trackScreenView("login");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    boolean handleIntent(Intent intent) {
        boolean handled = true;

        if (intent.hasExtra(Globals.LOGIN_ACTIVITY_USERNAME_EXTRA)){
            _editEmail.setText(intent.getStringExtra(Globals.LOGIN_ACTIVITY_USERNAME_EXTRA));
        }

        if (intent.hasExtra(Globals.LOGIN_ACTIVITY_USERNAME_EXTRA) &&
                intent.hasExtra(Globals.LOGIN_ACTIVITY_PASSWORD_EXTRA)) {
            _editEmail.setText(intent.getStringExtra(Globals.LOGIN_ACTIVITY_USERNAME_EXTRA));
            _editPassword.setText(intent.getStringExtra(Globals.LOGIN_ACTIVITY_PASSWORD_EXTRA));
            onLoginClick(null);
        }
        else if (intent.hasExtra(Globals.LOGIN_ACTIVITY_USER_SECRET_EXTRA) &&
                    intent.hasExtra(Globals.LOGIN_ACTIVITY_USER_MSISDN_EXTRA)) {
            final String userSecret = intent.getStringExtra(Globals.LOGIN_ACTIVITY_USER_SECRET_EXTRA);
            final String userMsisdn = intent.getStringExtra(Globals.LOGIN_ACTIVITY_USER_MSISDN_EXTRA);

            // Showing progress dialog
            _progress = new ProgressDialog(LoginActivity.this);
            _progress.setMessage("Signing in ...");
            _progress.setCancelable(false);
            _progress.show();

            ServerApi.getMe(
                    userMsisdn,
                    userSecret,
                    new ServerApi.OnMeListener() {
                        @Override
                        public void onMe(MeInfo meInfo) {
                            if (null != meInfo) {
                                _userInfo = new UserInfo();
                                _userInfo.setMeInfo(meInfo);
                                _userInfo.initAuthInfo(userSecret);
                                _userInfo.initBalanceInfo(new BigDecimal("0"));
                                _userInfo.isNewUser = Boolean.TRUE;
                                saveUserDataAndLogin();
                                new AppPreferences(LoginActivity.this).clearRegistrationSession();

                                ((BitwalkingApp) getApplication()).trackEvent("session", "login", "success");
                            }
                            else {
                                destroyProgress();
                            }
                        }
                    });
        }
        else if (intent.hasExtra("CoolAnimation")) {
            _showCoolReveal = true;
        }
        else {
            handled = false;
        }

        return handled;
    }

    public void onSignUpClick(View v) {
        Intent intent = new Intent(LoginActivity.this, ActivityFirstName.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    public void onLoginClick(View v) {
        // Confirm username and password
        if (!checkUserNamePassword()) {
            // Show alert message
            AlertDialog.Builder dlgAlert = new AlertDialog.Builder(LoginActivity.this);

            dlgAlert.setMessage("Enter valid email and password.");
            dlgAlert.setPositiveButton("OK", null);
            dlgAlert.setCancelable(true);
            dlgAlert.create().show();

            return;
        }

//        Globals.hideSoftKeyboard(this);
        sendLoginRequest();
    }



    private void saveUserDataAndLogin() {
        AppPreferences.setUserId(LoginActivity.this, _userInfo.getMeInfo().email);
        AppPreferences.setUserSecret(LoginActivity.this, _userInfo.getAuthInfo().userSecret);
        AppPreferences.setUserInfo(LoginActivity.this, _userInfo);

        AppPreferences appPrefs = new AppPreferences(getBaseContext());
        appPrefs.setNeedToPushToken(true);
        appPrefs.clearForceLogout();

        // start service
        Intent intentService = new Intent(this, BwService.class);
        intentService.setAction(Globals.INIT_SERVICE_ACTION);
        intentService.putExtra(Globals.BITWALKING_SERVICE_INIT_INFO,
                new Gson().toJson(AppPreferences.getServiceInitInfo(LoginActivity.this)));
        startService(intentService);

        // login
        Intent intentLogin = new Intent(LoginActivity.this, MainActivity.class);
        intentLogin.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intentLogin.putExtra(Globals.BITWALKING_USER_INFO, true);
        startActivity(intentLogin);
        LoginActivity.this.finish();
    }

    private void sendLoginRequest() {
        // Showing progress dialog
        _progress = new ProgressDialog(LoginActivity.this);
        _progress.setMessage("Signing in ...");
        _progress.setCancelable(false);
        _progress.show();

        try {
            LoginRequest loginPayload = new LoginRequest(
                    _email,
                    _password);

            ServerApi.login(
                    loginPayload,
                    new ServerApi.LoginListener() {
                        @Override
                        public void onLogin(final UserInfo userInfo) {
                            ((BitwalkingApp)getApplication()).trackEvent("session", "login", "success");
                            Globals.hideSoftKeyboard(LoginActivity.this);
                            _userInfo = userInfo;
                            if (null == _userInfo.getBalanceInfo().getBalance())
                                _userInfo.getBalanceInfo().setBalance(new BigDecimal("0"));
                            saveUserDataAndLogin();
                            new AppPreferences(LoginActivity.this).clearRegistrationSession();
                        }

                        @Override
                        public void onVerificationRequired(final RegistrationResponse.RegistrationPayload registrationPayload) {

                        ServerApi.getRegistrationSession(registrationPayload.registration.sessionIdentifier, new ServerApi.SimpleServerResponseListener() {
                                @Override
                                public void onResponse(int code) {
                                    switch (code){
                                        case 200:compliteRegistration(registrationPayload);break;
                                        case 401:signUpComplite();break;
                                        default:onFailure(code);break;
                                    }
                                }
                            });


                        }

                        @Override
                        public void onFailure(int code) {
                            LoginActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    destroyProgress();

                                    ((BitwalkingApp)getApplication()).trackEvent("session", "login", "failure");
                                    Globals.showSimpleAlertMessage(LoginActivity.this, "Login Failed", "Invalid email or password.", "Dismiss");
                                }
                            });
                        }
                    }
            );
        }
        catch (Exception e) {
            Logger.instance().Log(Logger.ERROR, TAG, "failed to send put");
            e.printStackTrace();
        }
    }

    /**
     * Complite registration after check session valid (onVerificationRequired way)
     */
    private void compliteRegistration(final RegistrationResponse.RegistrationPayload registrationPayload){
        LoginActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                destroyProgress();

                try {
                    AppPreferences appPrefs = new AppPreferences(LoginActivity.this);
                    String sessionId = registrationPayload.registration.sessionIdentifier;
                    JoinActivity.RegistrationSessionInfo sessionInfo =
                            _gson.fromJson(appPrefs.getRegistrationSession(), JoinActivity.RegistrationSessionInfo.class);

                    if (null == sessionInfo || null == sessionInfo.sessionId || !sessionInfo.sessionId.contentEquals(sessionId)) {
                        sessionInfo = new JoinActivity.RegistrationSessionInfo();
                        sessionInfo.sessionId = sessionId;
                        sessionInfo.email = _email;
                        sessionInfo.password = _password;
                        appPrefs.setRegistrationSession(_gson.toJson(sessionInfo));
                    }

                    Intent intent = new Intent(LoginActivity.this, JoinActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    intent.putExtra("verify.email", true);
                    startActivity(intent);
                } catch (Exception e) {
                    BitwalkingApp.getInstance().trackException(e);
                    ((BitwalkingApp)getApplication()).trackEvent("session", "login", "failure");
                    Globals.showSimpleAlertMessage(LoginActivity.this, "Login Failed", "error while trying to login.", "Dismiss");
                }
            }
        });
    }

    private void signUpComplite(){
        LoginActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                destroyProgress();

                try {
                        AppPreferences appPrefs = new AppPreferences(LoginActivity.this);
                        JoinActivity.RegistrationSessionInfo  sessionInfo = new JoinActivity.RegistrationSessionInfo();
                        sessionInfo.sessionId = null;
                        sessionInfo.email = _email;
                        sessionInfo.password = _password;
                        appPrefs.setRegistrationSession(_gson.toJson(sessionInfo));

                    Intent intent = new Intent(LoginActivity.this, JoinActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    intent.putExtra("resume", true);
                    startActivity(intent);
                } catch (Exception e) {
                    BitwalkingApp.getInstance().trackException(e);
                    /*((BitwalkingApp)getApplication()).trackEvent("session", "login", "failure");
                    Globals.showSimpleAlertMessage(LoginActivity.this, "Login Failed", "error while trying to login.", "Dismiss");*/
                }
            }
        });
    }

    private boolean checkUserNamePassword() {
        boolean valid = true;
        _email = _editEmail.getText().toString().trim().toLowerCase();
        _password = _editPassword.getText().toString();

        if (!Utilities.isEmailValid(_email) || TextUtils.isEmpty(_password))
            valid = false;

        return valid;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (null != _editPassword)
            _editPassword.setText("");
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();

    }

    public void onForgotPasswordClick(View v) {
        Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("email", _editEmail.getText().toString());
        intent.putExtra("dark", true);
        startActivity(intent);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }
}
