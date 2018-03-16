package bitwalking.bitwalking.settings;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import bitwalking.bitwalking.AppPreferences;
import bitwalking.bitwalking.BitwalkingApp;
import bitwalking.bitwalking.server.api.ServerApi;
import bitwalking.bitwalking.server.requests.UpdateUserPassword;
import bitwalking.bitwalking.util.Globals;
import bitwalking.bitwalking.R;
import bitwalking.bitwalking.registration_and_login.Utilities;
import bitwalking.bitwalking.util.BWEditText;
import bitwalking.bitwalking.util.Logger;

/**
 * Created by Marcus on 12/1/15.
 */
public class ChangePasswordActivity extends Activity {

    private static final String TAG = ChangePasswordActivity.class.getSimpleName();

    BWEditText _editNewPassword, _editNewPasswordAgain, _editPassword;
    ProgressDialog _progress;

    Gson _gson = null;

    String _oldPassword;
    String _newPassword;
    String _newPasswordAgain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.change_password_activity);

        _editPassword = (BWEditText)findViewById(R.id.change_old_password);
        _editNewPassword = (BWEditText)findViewById(R.id.change_new_password);
        _editNewPasswordAgain = (BWEditText)findViewById(R.id.change_new_password_again);
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

        // this is shit
        _editPassword.setTypeface(Typeface.DEFAULT);
        _editPassword.setTransformationMethod(new PasswordTransformationMethod());
        _editNewPassword.setTypeface(Typeface.DEFAULT);
        _editNewPassword.setTransformationMethod(new PasswordTransformationMethod());
        _editNewPasswordAgain.setTypeface(Typeface.DEFAULT);
        _editNewPasswordAgain.setTransformationMethod(new PasswordTransformationMethod());

        _gson = new Gson();

        addLogoutHandle();

        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyProgress();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ((BitwalkingApp)getApplication()).trackScreenView("change.password");
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
            Logger.instance().Log(Logger.DEBUG, TAG, "Logout in progress");
            //At this point you should startRecording the login activity and finish this one
            finish();
        }
    };

    void destroyProgress() {
        if (null != _progress && _progress.isShowing())
            _progress.dismiss();
    }

    private void sendChangePasswordRequest() {
        // Showing progress dialog
        _progress = new ProgressDialog(ChangePasswordActivity.this);
        _progress.setMessage("Changing password ...");
        _progress.setCancelable(false);
        _progress.show();

        try {
            UpdateUserPassword updatePassword = new UpdateUserPassword(
                    _oldPassword,
                    _newPassword);

            ServerApi.changePassword(
                    AppPreferences.getUserId(getBaseContext()),
                    AppPreferences.getUserSecret(getBaseContext()),
                    updatePassword,
                    new ServerApi.SimpleServerResponseListener() {
                        @Override
                        public void onResponse(final int code) {
                            ChangePasswordActivity.this.runOnUiThread(new Runnable() {
                                public void run() {
                                    destroyProgress();

                                    if (200 == code) {
                                        ((BitwalkingApp) getApplication()).trackEvent("profile", "change.password", "success");
                                        Toast.makeText(ChangePasswordActivity.this, "Password Changed", Toast.LENGTH_SHORT).show();
                                        ChangePasswordActivity.this.finish();
                                    }
                                    else {
                                        ((BitwalkingApp)getApplication()).trackEvent("profile", "change.password", "failure");
                                        Globals.showSimpleAlertMessage(ChangePasswordActivity.this, "Failed to change password", "Password is incorrect.", "Dismiss");
                                    }
                                }
                            });
                        }
                    });
        }
        catch (Exception e) {
            Logger.instance().Log(Logger.ERROR, TAG, "failed to change password");
            e.printStackTrace();

            BitwalkingApp.getInstance().trackException("sendChangePasswordRequest: failed", e);
            destroyProgress();
        }
    }

    public void onContinueChange(View v) {
        boolean valid = true;
        View focusView = null;

        _oldPassword = _editPassword.getText().toString();
        _newPassword = _editNewPassword.getText().toString();
        _newPasswordAgain = _editNewPasswordAgain.getText().toString();

        // Check for a valid new password (again), if the user entered one.
        if (!_newPasswordAgain.contentEquals(_newPassword)) {
            _editNewPasswordAgain.setError(getString(R.string.error_incorrect_password));
            focusView = _editNewPasswordAgain;
            valid = false;
        }
        else if (TextUtils.isEmpty(_newPasswordAgain)) {
            _editNewPasswordAgain.setError(getString(R.string.error_field_required));
            focusView = _editNewPasswordAgain;
            valid = false;
        }

        // Check for a valid new password, if the user entered one.
        if (!TextUtils.isEmpty(_newPassword) && !Utilities.isPasswordValid(_newPassword)) {
            _editNewPassword.setError(getString(R.string.error_invalid_password));
            focusView = _editNewPassword;
            valid = false;
        }
        else if (_newPassword.contentEquals(_oldPassword)) {
            _editNewPassword.setError(getString(R.string.error_same_password_new_old));
            focusView = _editNewPassword;
            valid = false;
        }
        else if (TextUtils.isEmpty(_newPasswordAgain)) {
            _editNewPasswordAgain.setError(getString(R.string.error_field_required));
            focusView = _editNewPasswordAgain;
            valid = false;
        }

        // Check for a valid old password, if the user entered one.
        if (TextUtils.isEmpty(_oldPassword)) {
            _editPassword.setError(getString(R.string.error_field_required));
            focusView = _editPassword;
            valid = false;
        }

        if (valid) {
            sendChangePasswordRequest();
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
        ((BitwalkingApp)getApplication()).trackEvent("profile", "change.password", "cancel");
        super.onBackPressed();

        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
    }
}
