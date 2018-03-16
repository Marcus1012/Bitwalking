package bitwalking.bitwalking.mvi.forgot_password;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import com.google.gson.Gson;

import bitwalking.bitwalking.BitwalkingApp;
import bitwalking.bitwalking.R;
import bitwalking.bitwalking.mvi.forgot_password.complete.ActivityComplite;
import bitwalking.bitwalking.registration_and_login.Utilities;
import bitwalking.bitwalking.server.api.ServerApi;
import bitwalking.bitwalking.server.requests.ResetPasswordRequest;
import bitwalking.bitwalking.util.ActivityUtils;
import bitwalking.bitwalking.util.Globals;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

/**
 * Created by Marcus on 12/5/15.
 */
public class ForgotPasswordActivity extends Activity {
    ProgressDialog _progress;
    Gson _gson = null;

    EditText _editEmail;
    String _email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

       /* if (getIntent().getBooleanExtra("dark", false))
            setContentView(R.layout.forgot_password_login_layout);
        else
            setContentView(R.layout.forgot_password_activity);*/
        setContentView(R.layout.forgot_password_login_layout);
        ActivityUtils.AttachTransperentTitleBar(this);
        ActivityUtils.ColorizeStatusBar(this,android.R.color.white);
        _gson = new Gson();

        initViews();

        String email = getIntent().getStringExtra("email");
        if (null != email)
            _editEmail.setText(email);

        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyProgress();
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        try {
            super.startActivityForResult(intent, requestCode);
        } catch (Exception e) {
            BitwalkingApp.getInstance().trackException(new Exception("startActivityForResult failed, req=" + requestCode, e));
        }
    }


    public void onBackClick(View v){
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
        ((BitwalkingApp)getApplication()).trackEvent("session", "password.request", "cancel");
        super.onBackPressed();

        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
    }

    private void initViews() {
        _editEmail = (EditText)findViewById(R.id.forgot_email);
    }

    public void onRecoverPasswordClick(View v) {
        if (isInputValid()) {
            sendForgotPasswordRequest();
        }
    }

    public void onCancelForgot(View v) {
        onBackPressed();
    }

    void destroyProgress() {
        if (null != _progress && _progress.isShowing())
            _progress.dismiss();
    }
    private void sendForgotPasswordRequest() {
        Globals.hideSoftKeyboard(ForgotPasswordActivity.this);

        _progress = new ProgressDialog(ForgotPasswordActivity.this);
        _progress.setMessage("Please wait ...");
        _progress.setCancelable(false);
        _progress.show();

        ResetPasswordRequest resetPayload = new ResetPasswordRequest(_email);

        ServerApi.forgotPassword(
                resetPayload,
                new ServerApi.SimpleServerResponseListener() {
                    @Override
                    public void onResponse(final int code) {
                        ForgotPasswordActivity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                destroyProgress();

                                if (200 == code) {
                                   /* AlertDialog.Builder builder = new AlertDialog.Builder(ForgotPasswordActivity.this);
                                    builder.setMessage("Recovery link was sent to your email")
                                            .setCancelable(true)
                                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id) {


                                                }
                                            });
                                    AlertDialog alert = builder.create();
                                    alert.show();*/


                                    startDoneActivity();
                                    finish();

                                    ((BitwalkingApp)getApplication()).trackEvent("session", "password.request", "success");
                                }
                                else {
                                    // Registration failed, show message
                                    ((BitwalkingApp)getApplication()).trackEvent("session", "password.request", "failure");
                                    Globals.showSimpleAlertMessage(ForgotPasswordActivity.this, "Reset Request Failed", "Invalid email", "Dismiss");
                                }
                            }
                        });
                    }
                }
        );
    }

    private void startDoneActivity(){
        Intent intent = new Intent(ForgotPasswordActivity.this,ActivityComplite.class);
        startActivity(intent);
    }

    private boolean isInputValid() {
        boolean valid = true;
        View focusView = null;

        _email = _editEmail.getText().toString().toLowerCase().trim();

        // Check for a valid email address.
        if (TextUtils.isEmpty(_email)) {
            _editEmail.setError(getString(R.string.error_field_required));
            focusView = _editEmail;
            valid = false;
        } else if (!Utilities.isEmailValid(_email)) {
            _editEmail.setError(getString(R.string.error_invalid_email));
            focusView = _editEmail;
            valid = false;
        }

        if (!valid && focusView != null)
            focusView.requestFocus();

        return valid;
    }

    @Override
    protected void onResume() {
        super.onResume();
        ((BitwalkingApp)getApplication()).trackScreenView("password.request");
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }
}

