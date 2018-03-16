package bitwalking.bitwalking.mvi.reset_password;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import com.google.gson.Gson;

import bitwalking.bitwalking.AppPreferences;
import bitwalking.bitwalking.BitwalkingApp;
import bitwalking.bitwalking.server.api.ServerApi;
import bitwalking.bitwalking.server.requests.ConfirmPasswordResetRequest;
import bitwalking.bitwalking.settings.CountryPickerActivity;
import bitwalking.bitwalking.util.Globals;
import bitwalking.bitwalking.activityes.MainActivity;
import bitwalking.bitwalking.R;
import bitwalking.bitwalking.mvi.login.LoginActivity;
import bitwalking.bitwalking.registration_and_login.Utilities;
import bitwalking.bitwalking.util.BWEditText;
import bitwalking.bitwalking.util.Logger;

/**
 * Created by Marcus on 12/5/15.
 */
public class ResetPasswordActivity extends Activity {

    private static final String TAG = ResetPasswordActivity.class.getSimpleName();
    public static final String RESET_PASSWORD_CODE = "ResetPasswordCode";
    private static final int LOGOUT_REQUEST_ID = 1;
    private static final int _COUNTRY_CODE_PICKER_REQ_ID = 2;

    ProgressDialog _progress;
    Gson _gson = null;

    String _resetCode;
    BWEditText _editCountryCode, _editPhone, _editEmail, _editNewPassword, _editNewPasswordAgain;
    String _textCountryCode, _textPhone, _textEmail,  _textNewPassword, _textNewPasswordAgain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reset_password_layout);

        _resetCode = getIntent().getStringExtra(RESET_PASSWORD_CODE);
        _gson = new Gson();

        initViews();
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

    @Override
    protected void onResume() {
        super.onResume();
        ((BitwalkingApp)getApplication()).trackScreenView("password.reset");
    }

    @Override
    public void onBackPressed() {
        ((BitwalkingApp)getApplication()).trackEvent("session", "password.reset", "cancel");
        super.onBackPressed();
    }

    private void initViews() {
        _editCountryCode = (BWEditText)findViewById(R.id.reset_password_country_code);
        _editPhone = (BWEditText)findViewById(R.id.reset_password_phone);
        _editEmail = (BWEditText)findViewById(R.id.reset_email);
        _editNewPassword = (BWEditText)findViewById(R.id.reset_password_new_password);
        _editNewPasswordAgain = (BWEditText)findViewById(R.id.reset_password_new_password_again);

        _editCountryCode.setText(Globals.getUserCountryCode(this));
    }

    public void onContinueReset(View v) {
        if (isInputValid()) {
            sendResetPasswordRequest();
        }
    }

    public void onCancelReset(View v) {
        onBackPressed();
    }

    void destroyProgress() {
        if (null != _progress && _progress.isShowing())
            _progress.dismiss();
    }

    private void sendResetPasswordRequest() {

        _progress = new ProgressDialog(ResetPasswordActivity.this);
        _progress.setMessage("Please wait ...");
        _progress.setCancelable(false);
        _progress.show();

        ConfirmPasswordResetRequest resetPayload =
                new ConfirmPasswordResetRequest(_textEmail, _resetCode, _textNewPassword);

        ServerApi.resetPassword(
                resetPayload,
                new ServerApi.SimpleServerResponseListener() {
                    @Override
                    public void onResponse(final int code) {
                        ResetPasswordActivity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                destroyProgress();

                                if (200 == code) {
                                    ((BitwalkingApp) getApplication()).trackEvent("session", "password.reset", "success");

                                    // Logout current user
                                    if (new AppPreferences(getBaseContext()).isUserLoggedIn()) {
                                        Intent logoutIntent = new Intent(ResetPasswordActivity.this, MainActivity.class);
                                        logoutIntent.putExtra(Globals.BITWALKING_LOGOUT_BROADCAST, true);
                                        startActivityForResult(logoutIntent, LOGOUT_REQUEST_ID);
                                    } else {
                                        // Login with new pass
                                        Intent intent = new Intent(ResetPasswordActivity.this, LoginActivity.class);
                                        intent.putExtra(Globals.LOGIN_ACTIVITY_USERNAME_EXTRA, _textEmail);
                                        intent.putExtra(Globals.LOGIN_ACTIVITY_PASSWORD_EXTRA, _textNewPassword);
                                        startActivity(intent);
                                        ResetPasswordActivity.this.finish();
                                    }
                                }
                                else {
                                    ((BitwalkingApp)getApplication()).trackEvent("session", "password.reset", "failure");
                                    // Show alert message
                                    AlertDialog.Builder dlgAlert = new AlertDialog.Builder(ResetPasswordActivity.this);

                                    dlgAlert.setMessage(getResources().getString(R.string.reset_failed_message));
                                    dlgAlert.setTitle("Reset Failed");
                                    dlgAlert.setPositiveButton("OK", null);
                                    dlgAlert.setCancelable(true);
                                    dlgAlert.create().show();
                                }
                            }
                        });
                    }
                }
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case LOGOUT_REQUEST_ID: {
                    // Move to phone verification
                    Intent logintIntent = new Intent(ResetPasswordActivity.this, LoginActivity.class);
                    logintIntent.putExtra(Globals.LOGIN_ACTIVITY_USERNAME_EXTRA, _textEmail);
                    logintIntent.putExtra(Globals.LOGIN_ACTIVITY_PASSWORD_EXTRA, _textNewPassword);
                    startActivity(logintIntent);
                    ResetPasswordActivity.this.finish();
                    break;
                }
                case _COUNTRY_CODE_PICKER_REQ_ID:
                    if (data.hasExtra("Code")) {
                        String newCode = data.getStringExtra("Code");
                        _editCountryCode.setText(newCode);
                        AppPreferences.setLastCountryCode(this, newCode);
                    }
                    break;
                default:
                    break;
            }
        }
        else {
            Logger.instance().Log(Logger.INFO, TAG, "Something went wrong");
        }
    }

    private boolean isInputValid() {
        boolean valid = true;
        View focusView = null;

//        _textCountryCode = _editCountryCode.getText().toString();
//        _textPhone = _editPhone.getText().toString().trim();
        _textEmail = _editEmail.getText().toString().toLowerCase().trim();
        _textNewPassword = _editNewPassword.getText().toString();
        _textNewPasswordAgain = _editNewPasswordAgain.getText().toString();

        // Check for a valid new password repeat, if the user entered one.
        if (!_textNewPasswordAgain.contentEquals(_textNewPassword)) {
            _editNewPasswordAgain.setError(getString(R.string.error_incorrect_password));
            focusView = _editNewPasswordAgain;
            valid = false;
        }

        // Check for a valid new password, if the user entered one.
        if (!TextUtils.isEmpty(_textNewPassword) && !Utilities.isPasswordValid(_textNewPassword)) {
            _editNewPassword.setError(getString(R.string.error_invalid_password));
            focusView = _editNewPassword;
            valid = false;
        }
        else if (TextUtils.isEmpty(_textNewPassword)) {
            _editNewPassword.setError(getString(R.string.error_field_required));
            focusView = _editNewPassword;
            valid = false;
        }

        if (TextUtils.isEmpty(_textEmail)) {
            _editEmail.setError(getString(R.string.error_field_required));
            focusView = _editEmail;
            valid = false;
        }
        else if (!Utilities.isEmailValid(_textEmail)) {
            _editEmail.setError(getString(R.string.error_invalid_email));
            focusView = _editEmail;
            valid = false;
        }

//        // Check for a valid email address.
//        if (TextUtils.isEmpty(_textPhone)) {
//            _editPhone.setError(getString(R.string.error_field_required));
//            focusView = _editPhone;
//            valid = false;
//        } else if (!Utilities.isPhoneValid(_textPhone)) {
//            _editPhone.setError(getString(R.string.error_invalid_phone));
//            focusView = _editPhone;
//            valid = false;
//        }

        if (!valid && focusView != null)
            focusView.requestFocus();

        return valid;
    }

    public void onEditCountryCode(View v) {
        showCountryPicker(_COUNTRY_CODE_PICKER_REQ_ID, true);
    }

    private void showCountryPicker(int reqId, boolean showCode) {
        Intent intent = new Intent(this, CountryPickerActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("showCountryCode", showCode);
        startActivityForResult(intent, reqId);
    }

}
