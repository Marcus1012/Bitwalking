package bitwalking.bitwalking.registration_and_login;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;

import bitwalking.bitwalking.AppPreferences;
import bitwalking.bitwalking.BitwalkingApp;
import bitwalking.bitwalking.R;
import bitwalking.bitwalking.server.api.ServerApi;
import bitwalking.bitwalking.server.requests.UpdateUserPhone;
import bitwalking.bitwalking.server.responses.UpdatePhoneResponse;
import bitwalking.bitwalking.server.responses.VerifyPhoneResponse;
import bitwalking.bitwalking.settings.CountryPickerActivity;
import bitwalking.bitwalking.user_info.TelephoneInfo;
import bitwalking.bitwalking.util.BWEditText;
import bitwalking.bitwalking.util.CustomViewPager;
import bitwalking.bitwalking.util.Globals;
import bitwalking.bitwalking.util.Logger;
import bitwalking.bitwalking.util.RobotoTextView;

/**
 * Created by Marcus on 11/17/15.
 */
public class PhoneVerificationActivity extends Activity implements ViewPager.OnPageChangeListener {

    //region Members

    private static final String TAG = PhoneVerificationActivity.class.getSimpleName();

    public static final String NEW_USER_SECRET_KEY = "NewUserSecret";
    public static final String NEW_USER_TELEPHONE = "NewUserTelephone";

    private CustomViewPager _pager;
    ProgressDialog _progress;
    Gson _gson = null;
    int _verificationCodeLength = 0;

    final int _COUNTRY_CODE_PICKER_REQ_ID = 3;

    // UI
    LinearLayout _circleIndexLayout;
    ChangePhonePagerAdapter _changePhoneAdapter;

    String _phoneCountry;
    RobotoTextView _textCountryPhoneCode;
    EditText _editPassword, _editPhone, _verifyCode;
    String _textCountryIso, _textPassword, _textPhone;

    TelephoneInfo _newTelephony;

    //endregion

    //region Activity Events

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.change_phone_layout);

        _gson = new Gson();
        _pager = (CustomViewPager) findViewById(R.id.change_phone_pager);
        _changePhoneAdapter = new ChangePhonePagerAdapter();
        _pager.setAdapter(_changePhoneAdapter);
        _pager.addOnPageChangeListener(this);
        _pager.setPagingEnabled(false);

        _phoneCountry = getBaseContext().getResources().getConfiguration().locale.getDisplayCountry();

        // init UI
        _circleIndexLayout = (LinearLayout) findViewById(R.id.change_phone_form_idx_layout);
        initFormIndexCircles();

        if (savedInstanceState == null) {
            _pager.setCurrentItem(0);
        }

        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
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
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPageScrolled(final int position, float positionOffset, int positionOffsetPixels) {
        ((BitwalkingApp)getApplication()).trackScreenView("change.phone." + (position + 1));

        updateFormIndex(position);
    }

    @Override
    public void onPageSelected(int position) {
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    public void onCancelEdit(View v) {
        onBackPressed();
    }

    public void onContinueChange(View v) {
        int currentForm = _pager.getCurrentItem();

        if (validateForm(currentForm)) {
            switch (currentForm) {
                case ChangePhonePagerAdapter.PHONE_ENTER_FORM_IDX:
                    changePhone();
                    break;
                case ChangePhonePagerAdapter.VERIFY_CODE_FORM_IDX:
                    verifyCode();
                    break;
                default:
                    break;
            }
        }
    }

    private void goToNextForm() {
        int currentForm = _pager.getCurrentItem();

        // Go to next form
//        ((BitwalkingApp)getApplication()).trackEvent("register", "next", "step." + currentForm);
        _pager.setCurrentItem(currentForm + 1);
    }

    @Override
    public void onBackPressed() {
        int currentForm = _pager.getCurrentItem();
//        ((BitwalkingApp)getApplication()).trackEvent("register", "back", "step." + currentForm);

        if (currentForm == 0) {
            // Exit - go back to login activity
            super.onBackPressed();

            overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
        }
        else {
            _pager.setCurrentItem(currentForm - 1);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case _COUNTRY_CODE_PICKER_REQ_ID:
                    if (data.hasExtra("Code")) {
                        String newCode = data.getStringExtra("Code");
                        _textCountryPhoneCode.setText(newCode);
                        AppPreferences.setLastCountryCode(this, newCode);
                    }

                    break;
                default:
                    break;
            }
        }
    }

    //endregion

    //region UI Handle

    private void initFormIndexCircles() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                (int) getResources().getDimension(R.dimen.circle_diameter),
                (int) getResources().getDimension(R.dimen.circle_diameter));
        lp.setMargins(0, 0, (int) getResources().getDimension(R.dimen.space_between_circles), 0);

        for (int i = 0; i < _pager.getAdapter().getCount(); ++i) {
            View v = new View(getBaseContext());
            v.setLayoutParams(lp);
            v.setBackgroundResource(R.drawable.empty_black_circle);
            _circleIndexLayout.addView(v);
        }
    }

    public void updateFormIndex(int index) {
        int currentForm = index;
        int total = _pager.getAdapter().getCount();

        refreshFormPositionIndex(currentForm, total);
        refreshNextButton(currentForm, total);
        initFormUI(index);
    }

    private void refreshNextButton(int index, int total) {
        if (index < total - 1) {
            ((Button) findViewById(R.id.continue_change)).setText("N E X T");
        }
        else {
            ((Button)findViewById(R.id.continue_change)).setText("V E R I F Y");
        }
    }

    private void refreshFormPositionIndex(int form, int total) {
        for (int i = 0; i < total ;++i) {
            _circleIndexLayout.getChildAt(i).setBackgroundResource( (form == i) ?
                    R.drawable.filled_black_circle : R.drawable.empty_black_circle);
        }
    }

    private void initFormUI(int form) {
        View formView = _pager.findViewWithTag(String.format("ChangePhoneForm%d", (form + 1)));
        formView = formView.getRootView();

        switch (form) {
            case 1: {
                _verifyCode = (EditText) formView.findViewById(R.id.verification_code);
                _verifyCode.setFilters(new InputFilter[] { new InputFilter.LengthFilter(_verificationCodeLength) });
                break;
            }
            case 0: {
                _editPhone = (EditText) formView.findViewById(R.id.phone_number);
                _editPhone.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                        if (id == R.id.done_form || id == EditorInfo.IME_ACTION_DONE) {
                            onContinueChange(textView);
                            return true;
                        }
                        return false;
                    }
                });

                // Set phone's code first time
                _textCountryPhoneCode = (RobotoTextView)formView.findViewById(R.id.country_code);
                _textCountryPhoneCode.setText(Globals.getUserCountryCode(this));
                _editPassword = (EditText) findViewById(R.id.change_password);

                break;
            }
            default: break;
        }
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

    //endregion

    //region Registration

    private boolean validateForm(int form){
        View focusView = null;
        boolean valid = true;

        switch (form) {
            case 0: { // validate phone number
                _textPassword = _editPassword.getText().toString();
                if (TextUtils.isEmpty(_textPassword)) {
                    _editPassword.setError(getString(R.string.error_field_required));
                    focusView = _editPhone;
                    valid = false;
                }

                _textPhone = _editPhone.getText().toString().trim();
                _textCountryIso = _textCountryPhoneCode.getText().toString().trim();//_spinnerCountryPhone.getSelectedItem().toString().trim();

                // Check for a valid phone number, if the user entered one.
                if (!TextUtils.isEmpty(_textPhone) && !Utilities.isPhoneValid(_textPhone, _textCountryIso)) {
                    _editPhone.setError(getString(R.string.error_invalid_phone));
                    focusView = _editPhone;
                    valid = false;
                }
                else if (TextUtils.isEmpty(_textPhone)) {
                    _editPhone.setError(getString(R.string.error_field_required));
                    focusView = _editPhone;
                    valid = false;
                }

//                _textPhone = String.format("%s%s", _textCountryIso, _editPhone.getText().toString().trim());
                break;
            }
            default: break;
        }

        if (!valid && focusView != null)
            focusView.requestFocus();

        return valid;
    }

    void destroyProgress() {
        if (null != _progress && _progress.isShowing())
            _progress.dismiss();
    }

    private void verifyCode() {
        String code = _verifyCode.getText().toString();

        _progress = new ProgressDialog(PhoneVerificationActivity.this);
        _progress.setMessage("Verifying code ...");
        _progress.setCancelable(false);
        _progress.show();

        try {
            ServerApi.PhoneVerifyInfo phoneVerifyInfo = new ServerApi.PhoneVerifyInfo();
            phoneVerifyInfo.code = code;
            _newTelephony.msisdn = TelephoneInfo.phoneToMSISDN(_newTelephony.countryCode, _newTelephony.number);
            phoneVerifyInfo.phone = _newTelephony.msisdn;

            ServerApi.verifyCode(
                    phoneVerifyInfo,
                    new ServerApi.VerificationListener() {
                        @Override
                        public void onVerification(final VerifyPhoneResponse.SessionTokenInfo sessionInfo, int code) {
                            PhoneVerificationActivity.this.runOnUiThread(new Runnable() {
                                public void run() {
                                    destroyProgress();

                                    if (null != sessionInfo) {
                                        done(_newTelephony, sessionInfo.userSecret);
                                    }
                                    else {
                                        Globals.showSimpleAlertMessage(PhoneVerificationActivity.this, "Verification Failed", "Invalid verification code.", "Dismiss");
                                    }
                                }
                            });
                        }
                    }
            );
        }
        catch (Exception e) {
            Logger.instance().Log(Logger.ERROR, TAG, "failed to verifyCode");
            e.printStackTrace();

            BitwalkingApp.getInstance().trackException("verifyCode: failed", e);

            destroyProgress();
        }
    }

    private void changePhone() {
        // Showing progress dialog
        _progress = new ProgressDialog(PhoneVerificationActivity.this);
        _progress.setMessage("Please wait ...");
        _progress.setCancelable(false);
        _progress.show();

        try {

            _newTelephony = new TelephoneInfo(_textCountryIso, _textPhone);
            UpdateUserPhone updatePhonePayload = new UpdateUserPhone(
                    _textPassword,
                    _newTelephony);

            ServerApi.changePhone(
                    AppPreferences.getUserId(getBaseContext()),
                    AppPreferences.getUserSecret(getBaseContext()),
                    updatePhonePayload,
                    new ServerApi.PhoneChangeListener() {
                        @Override
                        public void onPhoneChange(final UpdatePhoneResponse.PhoneUpdateInfo phoneUpdateInfo, final int code) {
                            PhoneVerificationActivity.this.runOnUiThread(new Runnable() {
                                public void run() {
                                    destroyProgress();

                                    if (null != phoneUpdateInfo) {
                                        _newTelephony.msisdn = phoneUpdateInfo.phone.msisdn;
                                        _verificationCodeLength = phoneUpdateInfo.code.length;
                                        goToNextForm();
                                    }
                                    else {
                                        String message = "Invalid phone number.";
                                        if (409 == code)
                                            message = "Phone already registered in the system.";
                                        Globals.showSimpleAlertMessage(PhoneVerificationActivity.this, "Update Failed", message, "Dismiss");
                                    }
                                }
                            });
                        }
                    });
        }
        catch (Exception e) {
            Logger.instance().Log(Logger.ERROR, TAG, "failed to change phone");
            e.printStackTrace();

            BitwalkingApp.getInstance().trackException("changePhone: failed", e);

            destroyProgress();
        }
    }

    private void done(TelephoneInfo newTelephony, String newUserSecret) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(NEW_USER_TELEPHONE, _gson.toJson(newTelephony));
        AppPreferences.setUserId(getBaseContext(), newTelephony.msisdn);

        if (null != newUserSecret) {
            AppPreferences.setUserSecret(getBaseContext(), newUserSecret);
            resultIntent.putExtra(NEW_USER_SECRET_KEY, newUserSecret);
        }

        setResult(Activity.RESULT_OK, resultIntent);
        PhoneVerificationActivity.this.finish();
    }

    //endregion

    private class ChangePhonePagerAdapter extends PagerAdapter {
        private int[] _forms = new int[] {
                R.layout.enter_phone_password,
                R.layout.enter_verification_code_layout
                //R.layout.join_info_3
        };

        private static final int PHONE_ENTER_FORM_IDX = 0;
        private static final int VERIFY_CODE_FORM_IDX = 1;

        @Override
        public int getCount() {
            return _forms.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == ((View) object);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            LayoutInflater inflater = (LayoutInflater) container.getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View v = inflater.inflate(_forms[position], null);
            v.setTag(String.format("ChangePhoneForm%d", (position + 1)));
            container.addView(v, 0);

            if (R.layout.enter_phone_password == _forms[position]) {

                // Fix password hint text :\ stupid ....
                BWEditText password = (BWEditText) findViewById(R.id.change_password);
                password.setTypeface(Typeface.DEFAULT);
                password.setTransformationMethod(new PasswordTransformationMethod());
            }

            return v;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }
    }
}
