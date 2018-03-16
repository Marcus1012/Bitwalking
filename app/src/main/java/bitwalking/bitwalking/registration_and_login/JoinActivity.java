package bitwalking.bitwalking.registration_and_login;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.telephony.PhoneNumberUtils;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;

import java.math.BigDecimal;

import bitwalking.bitwalking.AppPreferences;
import bitwalking.bitwalking.BitwalkingApp;
import bitwalking.bitwalking.activityes.MainActivity;
import bitwalking.bitwalking.R;
import bitwalking.bitwalking.invite_user.InvitationRegistrationActivity;
import bitwalking.bitwalking.mvi.login.LoginActivity;
import bitwalking.bitwalking.remote_service.BwService;
import bitwalking.bitwalking.server.api.ServerApi;
import bitwalking.bitwalking.server.requests.LoginRequest;
import bitwalking.bitwalking.server.requests.UserRegisterRequest;
import bitwalking.bitwalking.server.responses.EmailAvailableResponse;
import bitwalking.bitwalking.server.responses.RegistrationResponse;
import bitwalking.bitwalking.server.responses.UpdatePhoneResponse;
import bitwalking.bitwalking.server.responses.VerifyPhoneResponse;
import bitwalking.bitwalking.settings.CountryPickerActivity;
import bitwalking.bitwalking.user_info.TelephoneInfo;
import bitwalking.bitwalking.user_info.UserInfo;
import bitwalking.bitwalking.util.BWEditText;
import bitwalking.bitwalking.util.CustomViewPager;
import bitwalking.bitwalking.util.Globals;
import bitwalking.bitwalking.util.Logger;
import bitwalking.bitwalking.util.RobotoTextView;

public class JoinActivity extends Activity implements ViewPager.OnPageChangeListener {

    //region Members
    private static final String TAG = JoinActivity.class.getSimpleName();

    private CustomViewPager _pager;
    ProgressDialog _progress;
    Gson _gson = null;
    int _verificationCodeLength = 0;
    boolean _formLoaded = true;

    final int _COUNTRY_CODE_PICKER_REQ_ID = 3;

    private UserInfo _userInfo;

    // UI
    LinearLayout _circleIndexLayout;
    JoinInfoPagerAdapter _joinAdapter;
    ImageView _emailOkVImage;

    String _countryCode;
    RobotoTextView _textCountryPhoneCode, _editDateOfBirth;

    DatePickerDialog birthDatePickerDialog;

    EditText _editFirstName, _editLastName, _editEmail, _editPassword, _editPasswordAgain, _editPhone, _verifyCode;
    String _textFirstName, _textLastName, _textDateOfBirth, _textEmail, _textPassword, _textPasswordAgain, _textPhone, _textCountryCode;
    TextView termsView;

//    boolean _emailFree = true;
    String _affiliationCode = null, _invitationId = null;
    String _registrationSessionId = null;
    boolean _phoneVerificationOnly = false;

    //endregion

    //region Activity Events

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join);

       // findViewById(R.id.join_main_layout).setBackgroundResource(0);
       // getWindow().setBackgroundDrawableResource(R.drawable.login_background);

        _lastRegistrationForm = 0;

        _gson = new Gson();
        _pager = (CustomViewPager) findViewById(R.id.registration_pager);
        _joinAdapter = new JoinInfoPagerAdapter();
        _pager.setAdapter(_joinAdapter);
        _pager.addOnPageChangeListener(this);
        _pager.setPagingEnabled(false);

        // init UI
        _circleIndexLayout = (LinearLayout) findViewById(R.id.registration_form_idx_layout);
        initFormIndexCircles();

        if (savedInstanceState == null) {
            _pager.setCurrentItem(0);
        }

        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);

        //TelephonyManager manager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        //getNetworkCountryIso

        _affiliationCode = getIntent().getStringExtra(InvitationRegistrationActivity.INVITE_AFFILIATION_CODE);
        _invitationId = getIntent().getStringExtra(InvitationRegistrationActivity.INVITATION_ID);

        if (null == _affiliationCode)
            _affiliationCode = AppPreferences.getInviteAffiliationCode(this);
        else
            AppPreferences.setInviteAffiliationCode(this, _affiliationCode);

        handleRegistrationResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyProgress();
    }

    private void handleRegistrationResume() {
        if (getIntent().getBooleanExtra("resume", false)) {
            ((BitwalkingApp)getApplication()).trackEvent("register", "resume", "step." + JoinInfoPagerAdapter.EMAIL_ENTER_FORM_IDX);
            JoinActivity.RegistrationSessionInfo sessionInfo =
                    _gson.fromJson(new AppPreferences(this).getRegistrationSession(), JoinActivity.RegistrationSessionInfo.class);

            _textEmail = sessionInfo.email;
//            _countryCode = sessionInfo.phone.countryCode;
//            _textPhone = sessionInfo.phone.number;
            _verificationCodeLength = (null != sessionInfo.codeInfo) ? sessionInfo.codeInfo.length : 4;
            _registrationSessionId = sessionInfo.sessionId;

/*            if (!_countryCode.startsWith("+"))
                _countryCode = "+" + _countryCode;

            String countryName = IsoToPhone.getCountryName(IsoToPhone.getCountryByCode(_countryCode));
            _textCountryCode = String.format("%s (%s)", countryName, _countryCode);*/

            // Go to code enter form
            _pager.setCurrentItem(JoinInfoPagerAdapter.EMAIL_ENTER_FORM_IDX);
            _lastRegistrationForm = JoinInfoPagerAdapter.EMAIL_ENTER_FORM_IDX;
        }
        else if (getIntent().getBooleanExtra("verify.email", false)) {
            ((BitwalkingApp)getApplication()).trackEvent("register", "resume", "step." + JoinInfoPagerAdapter.VERIFY_CODE_FORM_IDX);
            JoinActivity.RegistrationSessionInfo sessionInfo =
                    _gson.fromJson(new AppPreferences(this).getRegistrationSession(), JoinActivity.RegistrationSessionInfo.class);
            _registrationSessionId = sessionInfo.sessionId;
            _textEmail = sessionInfo.email;
            _textPassword = sessionInfo.password;
            _verificationCodeLength = (null != sessionInfo.codeInfo) ? sessionInfo.codeInfo.length : 4;

            // Go to phone enter form
            _pager.setCurrentItem(JoinInfoPagerAdapter.VERIFY_CODE_FORM_IDX);
            _lastRegistrationForm = JoinInfoPagerAdapter.VERIFY_CODE_FORM_IDX;
            _phoneVerificationOnly = true;
        }




       /* else if (getIntent().getBooleanExtra("verify.phone", false)) {
            ((BitwalkingApp)getApplication()).trackEvent("register", "resume", "step." + JoinInfoPagerAdapter.PHONE_ENTER_FORM_IDX);
            JoinActivity.RegistrationSessionInfo sessionInfo =
                    _gson.fromJson(new AppPreferences(this).getRegistrationSession(), JoinActivity.RegistrationSessionInfo.class);
            _registrationSessionId = sessionInfo.sessionId;
            _textEmail = sessionInfo.email;
            _textPassword = sessionInfo.password;

            // Go to phone enter form
            _pager.setCurrentItem(JoinInfoPagerAdapter.PHONE_ENTER_FORM_IDX);
            _lastRegistrationForm = JoinInfoPagerAdapter.PHONE_ENTER_FORM_IDX;
            _phoneVerificationOnly = true;
        }*/
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
    public void onPageScrolled(final int position, float positionOffset, int positionOffsetPixels) {
        ((BitwalkingApp)getApplication()).trackScreenView("registration.step." + (position + 1));

        updateFormIndex(position);
    }

    @Override
    public void onPageSelected(int position) {
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    public void onCancelJoin(View v) {
        onBackPressed();
    }

    public void onSignInClick(View v) {
        Intent loginIntent = new Intent(JoinActivity.this, LoginActivity.class);
        startActivity(loginIntent);
        JoinActivity.this.finish();
    }

    private int _lastRegistrationForm = 0; // for google analytics
    public void onContinueJoin(View v) {
        if (!_formLoaded)
            return;
        _formLoaded = false;

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                _formLoaded = true;
            }
        }, 3000);

        int currentForm = _pager.getCurrentItem();

        if (validateForm(currentForm)) {
            switch (currentForm) {
               /* case JoinInfoPagerAdapter.PHONE_ENTER_FORM_IDX:
                    registerUser();
                    break;*/
                case JoinInfoPagerAdapter.VERIFY_CODE_FORM_IDX:
                    verifyCode();
                    break;
                case JoinInfoPagerAdapter.EMAIL_ENTER_FORM_IDX:
                    checkEmailFree(true);
                    registerUser();
                    break;
                default:
                    goToNextForm();
                    break;
            }
        }
    }

    private void goToNextForm() {
        int currentForm = _pager.getCurrentItem();

        // Go to next form
        ((BitwalkingApp)getApplication()).trackEvent("register", "next", "step." + currentForm);
        _pager.setCurrentItem(currentForm + 1);
        _lastRegistrationForm = currentForm + 1;
    }

    @Override
    public void onBackPressed() {
        int currentForm = _pager.getCurrentItem();
        ((BitwalkingApp)getApplication()).trackEvent("register", "back", "step." + currentForm);

       // if (currentForm == 0 || (_phoneVerificationOnly && currentForm == JoinInfoPagerAdapter.PHONE_ENTER_FORM_IDX))
        if (currentForm == 0)
        {
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
                        _countryCode = data.getStringExtra("Code");
                        String countryName = data.getStringExtra("Country");
                        _textCountryPhoneCode = (RobotoTextView)findViewById(R.id.join_country_phone_text);
                        _textCountryPhoneCode.setText(String.format("%s (%s)", null!=countryName?countryName:"null", _countryCode));
                        AppPreferences.setLastCountryCode(this, _countryCode);
                    }

                    String countryIso = data.getStringExtra("CountryIso");
                    if (null != countryIso && !countryIso.isEmpty()) {
                        AppPreferences.setLastCountryCode(this, IsoToPhone.getPhone(countryIso));
                    }
                    break;
                default:
                    break;
            }
        }
        else if (requestCode == RESULT_CANCELED) {
            switch (requestCode) {
                default: break;
            }
        }
    }

    private void loginUser(String userSecret, String emailMd5) {
        // Login user with all data
       /* Intent loginIntent = new Intent(JoinActivity.this, LoginActivity.class);
        loginIntent.putExtra(Globals.LOGIN_ACTIVITY_USER_SECRET_EXTRA, userSecret);
        startActivity(loginIntent);*/
        sendLoginRequest();




        JoinActivity.this.finish();
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
            v.setBackgroundResource(R.drawable.filled_grey_circle);
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
            ((Button) findViewById(R.id.continue_join)).setText("Next");
        }
        else {
            ((Button)findViewById(R.id.continue_join)).setText("Done");
        }
    }

    private void refreshFormPositionIndex(int form, int total) {
        for (int i = 0; i < total ;++i) {
            _circleIndexLayout.getChildAt(i).setBackgroundResource( (form == i) ?
                R.drawable.filled_white_circle : R.drawable.filled_grey_circle);
        }
    }

    //region Views Init UI

    private void initCodeVerify(View formView) {
        _verifyCode = (EditText) formView.findViewById(R.id.verification_code);
        _verifyCode.setFilters(new InputFilter[] { new InputFilter.LengthFilter(_verificationCodeLength) });
        ((TextView)findViewById(R.id.verification_code_text)).setText(
                String.format("Enter verification code sent to \n%1$s", _textEmail));

        TextView resendCodeView = (TextView)formView.findViewById(R.id.resendCodeView);
        resendCodeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });
    }

//    private void initDOB(View formView) {
//        _editDateOfBirth = (RobotoTextView) formView.findViewById(R.id.join_date_of_birth);
//        Calendar newCalendar = Calendar.getInstance();
//        birthDatePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
//
//            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
//                Calendar newDate = Calendar.getInstance();
//                newDate.set(year, monthOfYear, dayOfMonth);
//                _editDateOfBirth.setText(Globals.getDateOfBirthDisplayFormat().format(newDate.getTime()));
//            }
//
//        },  newCalendar.get(Calendar.YEAR) - Utilities.MIN_AGE,
//                newCalendar.get(Calendar.MONTH),
//                newCalendar.get(Calendar.DAY_OF_MONTH));
//
//        if (null != _textDateOfBirth && !_textDateOfBirth.isEmpty())
//            _editDateOfBirth.setText(_textDateOfBirth);
//    }

    private void initPhone(View formView) {
        _editPhone = (EditText) formView.findViewById(R.id.join_phone);
        _editPhone.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.done_form || id == EditorInfo.IME_ACTION_DONE) {
                    onContinueJoin(textView);
                    return true;
                }
                return false;
            }
        });

        // Set phone's code first time
        _textCountryPhoneCode = (RobotoTextView)formView.findViewById(R.id.join_country_phone_text);
        if (null != _textCountryCode && !_textCountryCode.isEmpty())
            _textCountryPhoneCode.setText(_textCountryCode);
        else {
            _countryCode = Globals.getUserCountryCode(this);
            String countryName = IsoToPhone.getCountryName(IsoToPhone.getCountryByCode(_countryCode));
            _textCountryCode = String.format("%s (%s)", countryName, _countryCode);
            _textCountryPhoneCode.setText(_textCountryCode);
        }

        if (null != _textPhone && !_textPhone.isEmpty())
            _editPhone.setText(_textPhone);
    }

    private void initFullName(View formView) {
        _editFirstName = (EditText)formView.findViewById(R.id.join_first_name);
        _editLastName = (EditText)formView.findViewById(R.id.join_last_name);
    }

    private void initEmail(View formView) {
        _emailOkVImage = (ImageView)formView.findViewById(R.id.join_email_ok_v);
        _editEmail = (EditText)formView.findViewById(R.id.join_email);
        _editEmail.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    _editEmail.setError(null);

                    String email = _editEmail.getText().toString().trim().toLowerCase();
                    // Check for a valid email address.
                    if (!TextUtils.isEmpty(email) && Utilities.isEmailValid(email)) {
                        checkEmailFree(false);
                    }
                }
            }
        });

        _editEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                _emailOkVImage.setVisibility(View.GONE);
            }
        });

        if (null != _textEmail && !_textEmail.isEmpty())
            _editEmail.setText(_textEmail);
    }

    private void initPasswords(View formView) {
        _editPassword = (EditText)formView.findViewById(R.id.join_password);
        _editPassword.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    String pass = _editPassword.getText().toString();
                    if (!pass.isEmpty()) {
                        if (!TextUtils.isEmpty(pass) && !Utilities.isPasswordValid(pass)) {
                            _editPassword.setError(getString(R.string.error_invalid_password));
                        }
                    }
                }
            }
        });
        _editPasswordAgain = (EditText)formView.findViewById(R.id.join_password_again);
        _editPasswordAgain.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    String pass = _editPassword.getText().toString();
                    String passAgain = _editPasswordAgain.getText().toString();
                    if (!passAgain.isEmpty() && !pass.isEmpty() && !pass.contentEquals(passAgain)) {
                        _editPasswordAgain.setError(getString(R.string.error_incorrect_password));
                    }
                }
            }
        });

        if (null != _textPassword && !_textPassword.isEmpty())
            _editPassword.setText(_textPassword);
        if (null != _textPasswordAgain && !_textPasswordAgain.isEmpty())
            _editPasswordAgain.setText(_textPasswordAgain);
    }

    private void initTermsLink(View formView){
        termsView = (TextView)formView.findViewById(R.id.termsView);
      /*  termsView.setMovementMethod(LinkMovementMethod.getInstance());
        String text = "<a href='http://www.bitwalking.com/terms'> By clicking next you accept the\nBitwalking terms</a>";
        termsView.setText(Html.fromHtml(text));*/
        termsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onUserAgreementClick(v);
            }
        });

    }

    //endregion

    private void initFormUI(int form) {
        View formView = _pager.findViewWithTag(String.format("JoinForm%d", (form + 1)));
        formView = formView.getRootView();

        switch (form) {
            // Phone verification code
            case 2: {
                initCodeVerify(formView);
                break;
            }
            // Bitrth Day + Phone
            case 1: {
//                initDOB(formView);
 //               initPhone(formView);
                initCodeVerify(formView);
                break;
            }
            // Country, First Name, Last Name
//            case 1: {
//                initFullName(formView);
//                initCountry(formView);
//                break;
//            }
            // Email + Password + Password again
            case 0: {
                initEmail(formView);
                initPasswords(formView);
                initTermsLink(formView);
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
        intent.putExtra("dark", true);
        startActivityForResult(intent, reqId);
    }

    //endregion

    //region Registration

    //region View Validation

    private boolean validateFullName(View focusView) {
        boolean valid = true;
        _textFirstName = _editFirstName.getText().toString().trim();
        _textLastName = _editLastName.getText().toString().trim();

        if (TextUtils.isEmpty(_textLastName)) {
            _editLastName.setError(getString(R.string.error_field_required));
            focusView = _editLastName;
            valid = false;
        }

        // Check if one one the inputs is empty
        if (TextUtils.isEmpty(_textFirstName)) {
            _editFirstName.setError(getString(R.string.error_field_required));
            focusView = _editFirstName;
            valid = false;
        }

        return valid;
    }

    private boolean validatePassword(View focusView) {
        boolean valid = true;

        _textPassword = _editPassword.getText().toString();
        _textPasswordAgain = _editPasswordAgain.getText().toString();

        // Check for a valid password repeat, if the user entered one.
        if (!_textPasswordAgain.contentEquals(_textPassword)) {
            _editPasswordAgain.setError(getString(R.string.error_incorrect_password));
            focusView = _editPasswordAgain;
            valid = false;
        }

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(_textPassword) && !Utilities.isPasswordValid(_textPassword)) {
            _editPassword.setError(getString(R.string.error_invalid_password));
            focusView = _editPassword;
            valid = false;
        }
        else if (TextUtils.isEmpty(_textPassword)) {
            _editPassword.setError(getString(R.string.error_field_required));
            focusView = _editPassword;
            valid = false;
        }

        return valid;
    }

    private boolean validateEmail(View focusView) {
        boolean valid = true;
        _textEmail = _editEmail.getText().toString().trim().toLowerCase();

        // Check for a valid email address.
        if (TextUtils.isEmpty(_textEmail)) {
            _editEmail.setError(getString(R.string.error_field_required));
            focusView = _editEmail;
            valid = false;
        } else if (!Utilities.isEmailValid(_textEmail)) {
            _editEmail.setError(getString(R.string.error_invalid_email));
            focusView = _editEmail;
            valid = false;
        }

        return valid;
    }

    @Deprecated
    private boolean validateDOB(View focusView) {
        boolean valid = true;
        _textDateOfBirth = _editDateOfBirth.getText().toString().trim();

        if (TextUtils.isEmpty(_textDateOfBirth) || !_textDateOfBirth.matches("[0-9]+-[0-9]+-[0-9]+")) {
            _editDateOfBirth.setError(getString(R.string.error_field_required));
            focusView = _editDateOfBirth;
            valid = false;
        }
        else if (!Utilities.isValidAge(_textDateOfBirth, Globals.getDateOfBirthDisplayFormat())) {
            _editDateOfBirth.setError(String.format("%s %d", getString(R.string.error_invalid_age), Utilities.MIN_AGE));
            focusView = _editDateOfBirth;
            valid = false;
        }
        else {
            _editDateOfBirth.setError(null);
        }

        return valid;
    }

    @Deprecated
    private boolean validatePhone(View focusView) {
        boolean valid = true;
        _textPhone = _editPhone.getText().toString().trim();
        _textCountryCode = _textCountryPhoneCode.getText().toString().trim();//_spinnerCountryPhone.getSelectedItem().toString().trim();

        if (_textPhone.startsWith(_countryCode)) {
            _textPhone = _textPhone.substring(_countryCode.length());
            _editPhone.setText(_textPhone);
        }

        // Check for a valid phone number, if the user entered one.
        if (!TextUtils.isEmpty(_textPhone) && !Utilities.isPhoneValid(_textPhone, _countryCode)) {
            _editPhone.setError(getString(R.string.error_invalid_phone));
            focusView = _editPhone;
            valid = false;
        }
        else if (TextUtils.isEmpty(_textPhone)) {
            _editPhone.setError(getString(R.string.error_field_required));
            focusView = _editPhone;
            valid = false;
        }
        else if (!PhoneNumberUtils.isWellFormedSmsAddress(TelephoneInfo.phoneToMSISDN(_countryCode, _textPhone))) {
            _editPhone.setError(getString(R.string.error_invalid_phone));
            focusView = _editPhone;
            valid = false;
        }

        return valid;
    }

    //endregion

    private boolean validateForm(int form){
        View focusView = null;
        boolean valid = true;

        switch (form) {
            case 1: { // validate phone number and DOB
//                valid = validateDOB(focusView);
               // valid = validatePhone(focusView);
                break;
            }
            // Country + First Name + Last Name
//            case 1: {
//                _textCountry = ((RobotoTextView)findViewById(R.id.join_country_text)).getText().toString().trim();
//                valid = validateFullName(focusView);
//                break;
//            }
            // Email + Password + Password again
            case 0: {
                valid = validatePassword(focusView);
                valid = validateEmail(focusView) && valid;
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

        if (code.length() != _verificationCodeLength)
            return;

        _progress = new ProgressDialog(JoinActivity.this);
        _progress.setMessage("Verifying code ...");
        _progress.setCancelable(false);
        _progress.show();

        try {
          //  final String msisdn = TelephoneInfo.phoneToMSISDN(_countryCode, _textPhone);

            ServerApi.PhoneVerifyInfo verifyInfo = new ServerApi.PhoneVerifyInfo();
          //  verifyInfo.phone = msisdn;
            verifyInfo.code = code;

            ServerApi.verifyCode(
                    verifyInfo,
                    new ServerApi.VerificationListener() {
                        @Override
                        public void onVerification(final VerifyPhoneResponse.SessionTokenInfo sessionInfo, final int code) {
                            JoinActivity.this.runOnUiThread(new Runnable() {
                                public void run() {
                                    switch (code) {
                                        case 200:
                                            ((BitwalkingApp)getApplication()).trackEvent("register", "verify", "success");
                                            loginUser(sessionInfo.userSecret, null); //todo check
                                            break;
                                        case 403:
                                            // Verification failed, show message
                                            String message = "Invalid verification code.";
                                            Globals.showSimpleAlertMessage(JoinActivity.this, "Verification Failed", message, "Dismiss");
                                        default:
                                            ((BitwalkingApp)getApplication()).trackEvent("register", "verify", "failed");
                                            break;
                                    }

                                    destroyProgress();
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

    ServerApi.RegistrationListener _registrationListener = new ServerApi.RegistrationListener() {
        @Override
        public void onRegistration(final RegistrationResponse.RegistrationPayload registrationPayload, final int code) {
            JoinActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    destroyProgress();

                    String message = "Could not complete registration.";
                    String failureLabel = "failure.unknown";
                    switch (code) {
                        case 202:
                            ((BitwalkingApp)getApplication()).trackEvent("register", "register", "success");
                            // Get response payload
                            _verificationCodeLength = registrationPayload.code.length;
                            _registrationSessionId = registrationPayload.registration.sessionIdentifier;

                            saveRegistrationSession(registrationPayload);

                            initFormUI(1); //fix 0 code verification

                          //  goToNextForm();
                            return;
                        case 400: // illegal parameter
                            failureLabel = "illegal.parameter.unknown";
                            message = "At least one of the provided parameters is empty or illegal.";
                            break;
                        case 409: // user exists
                            message = "User already registered.";
                            break;
                        default:
                            break;
                    }

                    ((BitwalkingApp)getApplication()).trackEvent("register", "register", failureLabel);
                    // Registration failed, show message
                    Globals.showSimpleAlertMessage(JoinActivity.this, "Registration Failed", message, "Dismiss");
                }
            });
        }
    };

    private void registerUser() {
        // Showing progress dialog
        _progress = new ProgressDialog(JoinActivity.this);
        _progress.setMessage("Please wait ...");
        _progress.setCancelable(false);
        _progress.show();

        try {
          //  TelephoneInfo phone = new TelephoneInfo(_countryCode, _textPhone);
            UserRegisterRequest registerPayload = new UserRegisterRequest(
                    null,
                    _textPassword,
                    null,
                    null,
                    _textEmail,
                    null,
                    _affiliationCode);

            // Check if user editing his profile
            if (null != _registrationSessionId) {
                ServerApi.resumeRegister(
                        _registrationSessionId,
                        registerPayload,
                        _registrationListener);
            }
            else {
                ServerApi.register(
                        registerPayload,
                        _registrationListener);
            }
        }
        catch (Exception e) {
            Logger.instance().Log(Logger.ERROR, TAG, "failed to send put");
            e.printStackTrace();

            destroyProgress();

            Globals.showSimpleAlertMessage(JoinActivity.this, "Error", "Could not complete registration.", "Dismiss");
            BitwalkingApp.getInstance().trackException(e);
        }
    }

    private void saveRegistrationSession(RegistrationResponse.RegistrationPayload payload) {
        RegistrationSessionInfo sessionInfo = new RegistrationSessionInfo();
        sessionInfo.email = _textEmail;
        sessionInfo.sessionId = _registrationSessionId;
//        sessionInfo.phone = new TelephoneInfo(_countryCode, _textPhone);
        sessionInfo.codeInfo = payload.code;

        new AppPreferences(this).setRegistrationSession(_gson.toJson(sessionInfo));
    }


    private void sendLoginRequest() {
        // Showing progress dialog
        _progress = new ProgressDialog(JoinActivity.this);
        _progress.setMessage("Signing in ...");
        _progress.setCancelable(false);
        _progress.show();

        try {
            LoginRequest loginPayload = new LoginRequest(
                    _textEmail,
                    _textPassword);

            ServerApi.login(
                    loginPayload,
                    new ServerApi.LoginListener() {
                        @Override
                        public void onLogin(final UserInfo userInfo) {
                            ((BitwalkingApp)getApplication()).trackEvent("session", "login", "success");
                            Globals.hideSoftKeyboard(JoinActivity.this);
                            _userInfo = userInfo;
                            if (null == _userInfo.getBalanceInfo().getBalance())
                                _userInfo.getBalanceInfo().setBalance(new BigDecimal("0"));
                            saveUserDataAndLogin();
                            new AppPreferences(JoinActivity.this).clearRegistrationSession();
                        }

                        @Override
                        public void onVerificationRequired(final RegistrationResponse.RegistrationPayload registrationPayload) {

                          /*  ServerApi.getRegistrationSession(registrationPayload.registration.sessionIdentifier, new ServerApi.SimpleServerResponseListener() {
                                @Override
                                public void onResponse(int code) {
                                    switch (code){
                                        case 200:compliteRegistration(registrationPayload);break;
                                        case 401:signUpComplite();break;
                                        default:onFailure(code);break;
                                    }
                                }
                            });*/


                        }

                        @Override
                        public void onFailure(int code) {
                            JoinActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    destroyProgress();

                                    ((BitwalkingApp)getApplication()).trackEvent("session", "login", "failure");
                                    Globals.showSimpleAlertMessage(JoinActivity.this, "Login Failed", "Invalid email or password.", "Dismiss");
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

    private void saveUserDataAndLogin() {
        AppPreferences.setUserId(JoinActivity.this, _userInfo.getMeInfo().email);
        AppPreferences.setUserSecret(JoinActivity.this, _userInfo.getAuthInfo().userSecret);
        AppPreferences.setUserInfo(JoinActivity.this, _userInfo);

        AppPreferences appPrefs = new AppPreferences(getBaseContext());
        appPrefs.setNeedToPushToken(true);
        appPrefs.clearForceLogout();

        // start service
        Intent intentService = new Intent(this, BwService.class);
        intentService.setAction(Globals.INIT_SERVICE_ACTION);
        intentService.putExtra(Globals.BITWALKING_SERVICE_INIT_INFO,
                new Gson().toJson(AppPreferences.getServiceInitInfo(JoinActivity.this)));
        startService(intentService);

        // login
        Intent intentLogin = new Intent(JoinActivity.this, MainActivity.class);
        intentLogin.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intentLogin.putExtra(Globals.BITWALKING_USER_INFO, true);
        startActivity(intentLogin);
        JoinActivity.this.finish();
    }



    private void checkEmailFree(final boolean goNext) {
        ServerApi.checkEmailFree(
                _editEmail.getText().toString().trim().toLowerCase(),
                new ServerApi.EmailFreeListener() {
                    @Override
                    public void onEmailFree(final EmailAvailableResponse.EmailAvailable emailFree) {
                        JoinActivity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                if (null == emailFree) {
                                    _editEmail.setError(getString(R.string.error_invalid_email));
                                    _emailOkVImage.setVisibility(View.GONE);
                                }
                                else {
                                    if (emailFree.available) {
                                        _editEmail.setError(null);
                                        _emailOkVImage.setVisibility(View.VISIBLE);

                                        if (goNext)
                                            goToNextForm();
                                    }
                                    else {
                                        _editEmail.setError(getString(R.string.error_email_not_free));
                                        _emailOkVImage.setVisibility(View.GONE);
                                    }
                                }
                            }
                        });
                    }
                });
    }

    public void onDOBClick(View v) {
        _editDateOfBirth.setError(null);
        birthDatePickerDialog.show();
    }

    //endregion

    //region Links

    final String _urlUserAgreement = "http://www.bitwalking.com/terms";
    final String _urlPrivacyPolicy = "http://www.bitwalking.com/privacy";

    public void onUserAgreementClick(View v) {
        ((BitwalkingApp)getApplication()).trackEvent("register", "external.link", "user_agreement");

        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(_urlUserAgreement));
        startActivity(i);
    }

    public void onPrivacyPolicyClick(View v) {
        ((BitwalkingApp)getApplication()).trackEvent("register", "external.link", "privacy_policy");

        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(_urlPrivacyPolicy));
        startActivity(i);
    }

    //endregion

    private class JoinInfoPagerAdapter extends PagerAdapter {
        private int[] _forms = new int[] {
                R.layout.join_info_1,
//                R.layout.join_info_2,
             //   R.layout.join_info_3,
//                R.layout.enter_phone_layout,
                R.layout.enter_verification_code_layout
                //R.layout.join_info_3
        };

        private static final int EMAIL_ENTER_FORM_IDX = 0;
     //   private static final int PHONE_ENTER_FORM_IDX = 1;
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
            v.setTag(String.format("JoinForm%d", (position + 1)));
            container.addView(v, 0);

            if (R.layout.join_info_1 == _forms[position]) {

                // Fix password hint text :\ stupid ....
                BWEditText password = (BWEditText) findViewById(R.id.join_password);
                password.setTypeface(Typeface.DEFAULT);
                password.setTransformationMethod(new PasswordTransformationMethod());
                password = (BWEditText) findViewById(R.id.join_password_again);
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

    public static class RegistrationSessionInfo {
        public String email;
        public String sessionId;
        public TelephoneInfo phone;
        public String password;
        public UpdatePhoneResponse.PhoneUpdateInfo.CodeInfo codeInfo;
    }
}
