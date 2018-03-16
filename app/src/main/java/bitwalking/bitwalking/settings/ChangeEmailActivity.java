package bitwalking.bitwalking.settings;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import bitwalking.bitwalking.AppPreferences;
import bitwalking.bitwalking.server.api.ServerApi;
import bitwalking.bitwalking.server.requests.UpdateUserEmail;
import bitwalking.bitwalking.server.responses.EmailAvailableResponse;
import bitwalking.bitwalking.util.Globals;
import bitwalking.bitwalking.BitwalkingApp;
import bitwalking.bitwalking.R;
import bitwalking.bitwalking.registration_and_login.Utilities;
import bitwalking.bitwalking.util.BWEditText;
import bitwalking.bitwalking.util.Logger;

/**
 * Created by Marcus on 12/9/15.
 */
public class ChangeEmailActivity extends Activity {

    BWEditText _editNewEmail, _editNewEmailAgain, _editPassword;
    ImageView _emailOkVImage;
    ProgressDialog _progress;

    Gson _gson = null;

    final int _CHECK_EMAIL_REQ_ID = 1;
    final int _CHANGE_EMAIL_REQ_ID = 2;

    String _newEmail;
    String _newEmailAgain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.change_email_activity);

        _emailOkVImage = (ImageView)findViewById(R.id.change_email_ok_v);
        _editNewEmail = (BWEditText)findViewById(R.id.change_new_email);
        _editNewEmail.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    _editNewEmail.setError(null);
                    _editNewEmailAgain.setError(null);
                    _emailOkVImage.setVisibility(View.GONE);

                    String email = _editNewEmail.getText().toString().trim().toLowerCase();
                    // Check for a valid email address.
                    if (!TextUtils.isEmpty(email)) {
                        if (Utilities.isEmailValid(email)) {
                            checkEmailFree();

                            if (!_editNewEmailAgain.getText().toString().isEmpty() &&
                                !email.contentEquals(_editNewEmailAgain.getText().toString().trim().toLowerCase())) {
                                _editNewEmailAgain.setError(getString(R.string.error_invalid_email));
                            }
                        }
                        else {
                            _editNewEmail.setError(getString(R.string.error_invalid_email));
                        }
                    }
                    else {
                        _editNewEmail.setError(getString(R.string.error_field_required));
                    }
                }
            }
        });

        _editNewEmailAgain = (BWEditText)findViewById(R.id.change_new_email_again);
        _editNewEmailAgain.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    _editNewEmailAgain.setError(null);

                    String email = _editNewEmailAgain.getText().toString().trim().toLowerCase();
                    // Check for a valid email address.
                    if (!email.contentEquals(_editNewEmail.getText().toString().trim().toLowerCase())) {
                        _editNewEmailAgain.setError(getString(R.string.error_invalid_email));
                    }
                }
            }
        });
        _editPassword = (BWEditText)findViewById(R.id.change_email_password);
        _editPassword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE) {
                    onContinueChange(textView);
                    return true;
                }
                return false;
            }
        });

        _gson = new Gson();

        addLogoutHandle();

        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyProgress();
    }

    private void checkEmailFree() {
        ServerApi.checkEmailFree(
                _editNewEmail.getText().toString().trim().toLowerCase(),
                new ServerApi.EmailFreeListener() {
                    @Override
                    public void onEmailFree(final EmailAvailableResponse.EmailAvailable emailFree) {
                        ChangeEmailActivity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                if (null != emailFree && emailFree.available) {
                                        _editNewEmail.setError(null);
                                    _emailOkVImage.setVisibility(View.VISIBLE);
                                }
                                else {

                                    _editNewEmail.setError(getString(R.string.error_email_not_free));
                                    _emailOkVImage.setVisibility(View.GONE);
                                }
                            }
                        });
                    }
                }
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        ((BitwalkingApp)getApplication()).trackScreenView("change.email");
    }

    @Override
    protected void onStop() {
        super.onStop();

        try {
            unregisterReceiver(logoutReceiver);
        }
        catch (Exception e) {
        }
    }

    private void addLogoutHandle() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Globals.BITWALKING_LOGOUT_BROADCAST);
        registerReceiver(logoutReceiver, intentFilter);
    }

    BroadcastReceiver logoutReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            Logger.instance().Log(Logger.DEBUG, "ChangeEmailActivity", "Logout in progress");
            //At this point you should startRecording the login activity and finish this one
            finish();
        }
    };

    void destroyProgress() {
        if (null != _progress && _progress.isShowing())
            _progress.dismiss();
    }

    private void sendChangeEmailRequest() {

        _progress = new ProgressDialog(ChangeEmailActivity.this);
        _progress.setMessage("Changing email ...");
        _progress.setCancelable(false);
        _progress.show();

        UpdateUserEmail changeEmailReq = new UpdateUserEmail(
                _editPassword.getText().toString(),
                _newEmail);

        ServerApi.changeEmail(
                AppPreferences.getUserId(getBaseContext()),
                AppPreferences.getUserSecret(getBaseContext()),
                changeEmailReq,
                new ServerApi.SimpleServerResponseListener() {
                    @Override
                    public void onResponse(final int code) {
                        ChangeEmailActivity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                destroyProgress();

                                if (200 == code) {
                                    ((BitwalkingApp) getApplication()).trackEvent("profile", "change.email", "success");
                                    Toast.makeText(ChangeEmailActivity.this, "Email Changed", Toast.LENGTH_SHORT).show();

                                    Intent resultIntent = new Intent();
                                    resultIntent.putExtra("NewEmail", _newEmail);
                                    setResult(Activity.RESULT_OK, resultIntent);

                                    ChangeEmailActivity.this.finish();
                                }
                                else {
                                    String alertMessage = "Illegal new email.";
                                    if (403 == code)
                                        alertMessage = "Invalid password.";

                                    ((BitwalkingApp)getApplication()).trackEvent("profile", "change.email", "failure");
                                    // Show alert message
                                    Globals.showSimpleAlertMessage(ChangeEmailActivity.this, "Error", alertMessage, "Dismiss");
                                }
                            }
                        });
                    }
                });
    }

    public void onContinueChange(View v) {
        boolean valid = true;
        View focusView = null;

        String password = _editPassword.getText().toString();
        _newEmail = _editNewEmail.getText().toString().trim().toLowerCase();
        _newEmailAgain = _editNewEmailAgain.getText().toString().trim().toLowerCase();

        // Check if the user entered password.
        if (TextUtils.isEmpty(password)) {
            _editPassword.setError(getString(R.string.error_field_required));
            focusView = _editPassword;
            valid = false;
        }

        // Check if the email repeated correctly
        if (!_newEmail.contentEquals(_newEmailAgain)) {
            _editNewEmailAgain.setError(getString(R.string.error_invalid_email));
            focusView = _editNewEmailAgain;
            valid = false;
        }

        // Check for a valid new email, if the user entered one.
        if (TextUtils.isEmpty(_newEmail)) {
            _editNewEmail.setError(getString(R.string.error_field_required));
            focusView = _editNewEmail;
            valid = false;
        }
        else if (!Utilities.isEmailValid(_newEmail)) {
            _editNewEmail.setError(getString(R.string.error_invalid_email));
            focusView = _editNewEmail;
            valid = false;
        }

        if (valid) {
            sendChangeEmailRequest();
        }
        else {
            if (focusView != null)
                focusView.requestFocus();
        }
    }

    public void onCancelChange(View v) {
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        ((BitwalkingApp)getApplication()).trackEvent("profile", "change.email", "cancel");
        super.onBackPressed();

        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
    }
}
