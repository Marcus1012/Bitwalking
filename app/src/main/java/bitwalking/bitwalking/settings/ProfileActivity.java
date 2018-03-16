package bitwalking.bitwalking.settings;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.Switch;

import com.google.gson.Gson;

import java.util.ArrayList;

import bitwalking.bitwalking.AppPreferences;
import bitwalking.bitwalking.activityes.BwActivity;
import bitwalking.bitwalking.util.BWEditText;
import bitwalking.bitwalking.util.DolceVitaButton;
import bitwalking.bitwalking.util.RobotoTextView;
import bitwalking.bitwalking.util.RoundImageView;

/**
 * Created by Marcus Greenberg on 10/4/15.
 *.........................
 *.........................
 *...............###.......
 *..............#   #......
 *..............#   #......
 *.......###....#   #......
 *......#   #...#   #......
 *......#   #...#   #......
 *......#   #....###.......
 *......#   #..............
 *......#   #..............
 *.......###...............
 *.........................
 *.........................
 *.......BitWalking ©......
 *.........................
 */
public class ProfileActivity extends BwActivity implements View.OnClickListener {

    final static String TAG = "ProfileActivity";
    AppPreferences _appPrefs;

    private static final int TAKE_PROFILE_PHOTO                     = 1;
    private static final int CHOOSE_PROFILE_PHOTO                   = 2;
    public  static final int PHONE_EDIT_REQUEST_CODE                = 5;
    private static final int CHANGE_EMAIL_ID                        = 6;
    private static final int CAMERA_PERMISSIONS_REQUEST_ID          = 10;
    private static final int READ_EXTERNAL_PERMISSIONS_REQUEST_ID   = 11;
    private static final int COUNTRY_PICKUP_REQ_ID                  = 13;
    private static final int REQUEST_ENABLE_BT                      = 40;
    private static final int REQUEST_SELECT_DEVICE                  = 41;

    ProfileDetails _currentDetails;
    boolean _avatarUpdated = false;

    FrameLayout _rootLayout;
    boolean _backPressed;

    boolean _deleteAvatar = false;
    RoundImageView _profileImage;
    RobotoTextView _country, _email, _phone, _birthday, _changePass;
    BWEditText _fullname;
    DolceVitaButton _editDoneButton;
    ProgressBar _passwordVerifyProgress;
    ProgressDialog _progress;
    LinearLayout _popupLayout;
    Gson _gson;

    // Edit profile
    PopupWindow _popupWindow;
    DatePickerDialog birthDatePickerDialog;
    boolean _editEnabled = false;

    ArrayList<View> _editableViews = new ArrayList<>();

    // misfit
    Switch _wearableSwitch;

    @Override
    public void onClick(View v) {

    }

    @Override
    protected void onBwServiceConnected() {

    }

    @Override
    protected void onBwServiceDisconnected() {

    }

    /*

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_info_layout);

        _appPrefs = new AppPreferences(getBaseContext());

        // Set MyWallet screen as default
        if (savedInstanceState == null) {
            // nothing - profile info will be filled when service connected
        }
        else {
            _currentDetails = savedInstanceState.getParcelable("details");
        }

        addLogoutHandle();
        initUIElements();
        _rootLayout = (FrameLayout)findViewById(R.id.profile_info_layout);

        lockEdit();
        _gson = new Gson();

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            overridePendingTransition(R.anim.do_not_move, R.anim.do_not_move);
//
//            if (savedInstanceState == null) {
//                _rootLayout.setVisibility(View.INVISIBLE);
//
//                ViewTreeObserver viewTreeObserver = _rootLayout.getViewTreeObserver();
//                if (viewTreeObserver.isAlive()) {
//                    viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
//                        @Override
//                        public void onGlobalLayout() {
//                            enterReveal();
//                            _rootLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
//                        }
//                    });
//                }
//            }
//        }
//        else {
//            overridePendingTransition(R.anim.enter_from_bottom_left, R.anim.hold);
//        }

        _deleteAvatar = false;
        _backPressed = false;
        ((Switch)findViewById(R.id.set_wearable_on_off)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (null != _serviceApi) {
                    try {
                        if (isChecked && _serviceApi.getMisfitDeviceSerial() == null) {
                            buttonView.setChecked(false);
                            Toast.makeText(ProfileActivity.this, "No selected device", Toast.LENGTH_SHORT).show();
                        }
                        else {
                            _serviceApi.setStepsSource(isChecked ? Globals.STEPS_SOURCE_MISFIT : Globals.STEPS_SOURCE_PHONE);
                        }
                    } catch (Exception e) {
                        Logger.instance().Log(Logger.ERROR, TAG, "failed to set steps source");
                        buttonView.setChecked(!isChecked);
                    }
                }
            }
        });

        bindToBwService();

        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        try {
            super.startActivityForResult(intent, requestCode);
        } catch (Exception e) {
            BitwalkingApp.getInstance().trackException(new Exception("startActivityForResult failed, req=" + requestCode, e));
        }
    }

    private void initUIElements() {
        _profileImage = (RoundImageView)findViewById(R.id.settings_profile_image);

        _fullname = (BWEditText)findViewById(R.id.settings_user_fullname);
        _fullname.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                _currentDetails.fullName = _fullname.getText().toString().trim();
            }
        });

        _email = (RobotoTextView)findViewById(R.id.settings_user_email);
        _phone = (RobotoTextView)findViewById(R.id.change_user_phone);
        _changePass = (RobotoTextView)findViewById(R.id.change_password_text);
        // Birthday
        _birthday = (RobotoTextView)findViewById(R.id.settings_user_birth_date);

        _birthday.setOnClickListener(this);
        findViewById(R.id.settings_user_birth_date_layout).setOnClickListener(this);

        // Country
        _country = (RobotoTextView)findViewById(R.id.profile_user_country);

        // Change email
        findViewById(R.id.settings_user_email).setOnClickListener(this);
        findViewById(R.id.settings_user_email_layout).setOnClickListener(this);
        // Change password
        findViewById(R.id.change_password_text).setOnClickListener(this);
        findViewById(R.id.change_password_text_layout).setOnClickListener(this);
        // Change phone
        findViewById(R.id.change_user_phone).setOnClickListener(this);
        findViewById(R.id.change_user_phone_layout).setOnClickListener(this);

        // Listen to 'done' click
        _editDoneButton = (DolceVitaButton)findViewById(R.id.profile_edit_button);
        _editDoneButton.setOnClickListener(this);
        _editDoneButton.setText("E D I T");

        // Add relevant view to list of enabled\disabled for edit
        _editableViews.add(_fullname);
        _editableViews.add(_birthday);
        _editableViews.add(_country);
//        _editableViews.add(_email);
//        _editableViews.add(_changePass);
//        _editableViews.add(_phone);

        _wearableSwitch = (Switch)findViewById(R.id.set_wearable_on_off);

        try {
            ((TextView) findViewById(R.id.profile_app_version)).setText("Version " + getPackageManager()
                    .getPackageInfo(getPackageName(), 0).versionName);
        }
        catch (Exception e) {
            BitwalkingApp.getInstance().trackException("Failed to load version name", e);
        }
    }

    private void unlockEdit() {
        for (View v : _editableViews) {
            if (null != v) {
                v.setEnabled(true);
            }
        }

        _editEnabled = true;
    }

    private void lockEdit() {
        for (View v : _editableViews) {
            v.setEnabled(false);
        }

        _editEnabled = false;
    }

    private void showBirthdayPicker() {
        // Birthday
        if (null == birthDatePickerDialog) {
            String[] date = _birthday.getText().toString().split("-");

            Context context = this;
            if (Globals.isBrokenSamsungDevice()) { // Fu**ing android - http://stackoverflow.com/a/31855744
                context = new ContextThemeWrapper(this, android.R.style.Theme_Holo_Light_Dialog);
            }

            birthDatePickerDialog = new DatePickerDialog(context, new DatePickerDialog.OnDateSetListener() {

                public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                    Calendar newDate = Calendar.getInstance();
                    newDate.setTimeZone(TimeZone.getTimeZone("UTC"));
                    newDate.set(year, monthOfYear, dayOfMonth, 0, 0, 0);
                    newDate.set(Calendar.MILLISECOND, 0);
                    if (newDate.getTime().after(Utilities.getMinimalDateOfBirth())) {
                        _birthday.setError(String.format("%s %d", getString(R.string.error_invalid_age), Utilities.MIN_AGE));
                        return;
                    }

                    _currentDetails.birthday = newDate.getTime();
                    _birthday.setText(Globals.getDateOfBirthDisplayFormat().format(_currentDetails.birthday));
                }

            }, Integer.parseInt(date[2]),
                    Integer.parseInt(date[1]) - 1,
                    Integer.parseInt(date[0]));
        }

//        birthDatePickerDialog.getDatePicker().setSpinnersShown(true);
//        birthDatePickerDialog.getDatePicker().setCalendarViewShown(false);
        birthDatePickerDialog.show();
        _birthday.setError(null);
    }

    @Override
    public void onResume() {
        super.onResume();

        _fullname.clearFocus();
        updateUserSettings();

        if (null != _currentDetails && _currentDetails.canEdit) {
            unlockEdit();
            _editDoneButton.setText("S A V E");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (null != birthDatePickerDialog && birthDatePickerDialog.isShowing())
            birthDatePickerDialog.hide();
        _fullname.clearFocus();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        if (null != _currentDetails)
            savedInstanceState.putParcelable("details", _currentDetails);

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    private void fillProfileInfo() {
        Bitmap image = _appPrefs.getProfileImage();

        UserInfo userInfo = null;
        try {
            userInfo = AppPreferences.getUserInfo(ProfileActivity.this);
        }
        catch (Exception e) {
            BitwalkingApp.getInstance().trackException(e);
        }

        if (null == userInfo) {
            ProfileActivity.this.onBackPressed();
            return;
        }

        String fullName = userInfo.getMeInfo().fullName;
        Date birthday = null;
        try {
            String tmp = Globals.getUTCDateFormat().format(new Date());
            birthday = Globals.getUTCDateFormat().parse(userInfo.getMeInfo().dateOfBirth);
        }
        catch (ParseException e) {
            BitwalkingApp.getInstance().trackException(e);
        }

        String country = userInfo.getMeInfo().country;
        TelephoneInfo phone = userInfo.getMeInfo().phone;
        String email = userInfo.getMeInfo().email;

        _currentDetails = new ProfileDetails(image, fullName, birthday, country, phone, email, false);
    }

    private void addLogoutHandle() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Globals.BITWALKING_LOGOUT_BROADCAST);
        intentFilter.addAction(Globals.BITWALKING_SWITCH_OFF_BROADCAST);
        registerReceiver(logoutReceive, intentFilter);
    }

    BroadcastReceiver logoutReceive = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Logger.instance().Log(Logger.DEBUG, TAG, "Logout in progress");
            //At this point you should startRecording the login activity and finish this one
            ProfileActivity.this.finish();
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(logoutReceive);
        unbindBwService();
        destroyProgress();
    }

    public void onExitClick(View v) {
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
        if (null != _currentDetails && _currentDetails.canEdit) {
            confirmDiscardChanges();
        }
        else {
            ((BitwalkingApp) getApplication()).trackEvent("profile", "close", "");
            exitProfile();
        }
    }

    private void confirmDiscardChanges() {
        ProfileActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                final String failedMessage = "Discard changes?";
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(ProfileActivity.this);
                builder.setMessage(failedMessage)
                        .setCancelable(true)
//                        .setNeutralButton("Save", new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                                saveProfileDetails();
//                            }
//                        })
                        .setPositiveButton("Discard", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                ((BitwalkingApp) getApplication()).trackEvent("profile", "edit", "cancel");
                                exitProfile();
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                android.app.AlertDialog alert = builder.create();
                alert.show();
            }
        });
    }

    private void exitProfile() {
        super.onBackPressed();

        ((BitwalkingApp) getApplication()).trackScreenView("profile");
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
    }

    public void onSignOutClick(View v) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(Globals.BITWALKING_PRE_LOGOUT_BROADCAST);
        sendBroadcast(broadcastIntent);
        exitProfile();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.settings_user_birth_date:
            case R.id.settings_user_birth_date_layout:
                if (_editEnabled)
                    showBirthdayPicker();
                break;
            case R.id.profile_edit_button:
                if (_editEnabled) {
                    // save
                    saveProfileDetails();
                }
                else {
                    // Prompt password window
                    promptPassword();
                }
                break;
            // Password verification popup
            case R.id.verify_password_cancel:
                if (_popupWindow != null) {
                    _popupWindow.dismiss();
                    Globals.hideSoftKeyboard(this);
                }

                break;
            case R.id.verify_password_submit:
                if (_popupWindow != null) {
                    String password = ((BWEditText)_popupWindow.getContentView().findViewById(R.id.verify_password)).getText().toString();
                    verifyPassword(password);
                }

                break;
            case R.id.verify_password_forgot:
                onForgotPasswordClick();
                break;
            // email
            case R.id.settings_user_email:
            case R.id.settings_user_email_layout:
//                if (_editEnabled)
                    onChangeEmailClick();
                break;
            // password
            case R.id.change_password_text:
            case R.id.change_password_text_layout:
//                if (_editEnabled)
                onChangePasswordClick();
                break;
            // phone
            case R.id.change_user_phone:
            case R.id.change_user_phone_layout:
//                if (_editEnabled)
                onChangePhoneClick();
                break;
            default:
                break;
        }

        _fullname.clearFocus();
    }

    public void onChangePasswordClick() {
        Intent intent = new Intent(this, ChangePasswordActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    public void onChangeEmailClick() {
        Intent intent = new Intent(this, ChangeEmailActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivityForResult(intent, CHANGE_EMAIL_ID);
    }

    public void onForgotPasswordClick() {
        Intent intent = new Intent(this, ForgotPasswordActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    private void verifyPassword(String password) {
        if (null == password || password.isEmpty()) {
            return;
        }

        _passwordVerifyProgress.setVisibility(View.VISIBLE);
        _popupLayout.setVisibility(View.INVISIBLE);

        VerifyPasswordRequest verifyPayload = new VerifyPasswordRequest(_currentDetails.email, password);

        ServerApi.verifyPassword(
                AppPreferences.getUserId(getBaseContext()),
                AppPreferences.getUserSecret(getBaseContext()),
                verifyPayload,
                new ServerApi.SimpleServerResponseListener() {
                    @Override
                    public void onResponse(final int code) {
                        ProfileActivity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                _passwordVerifyProgress.setVisibility(View.INVISIBLE);
                                _popupLayout.setVisibility(View.VISIBLE);

                                switch (code) {
                                    case 200:
                                        if (null != _currentDetails) {
                                            unlockEdit();
                                            _currentDetails.canEdit = true;
                                            _editDoneButton.setText("S A V E");

                                            Toast.makeText(ProfileActivity.this, "Password verified", Toast.LENGTH_SHORT).show();
                                        }

                                        if (null != _popupWindow)
                                            _popupWindow.dismiss();
                                        Globals.hideSoftKeyboard(ProfileActivity.this);
                                        break;
                                    case 400: // illegal parameter
                                    default:
                                        showPopupError("Invalid password");
                                        break;
                                }
                            }
                        });
                    }
                });
    }

    private void showPopupError(String error) {
        RobotoTextView errorTextView = (RobotoTextView)_popupWindow.getContentView().findViewById(R.id.verify_password_invalid_msg);
        errorTextView.setText(error);
        errorTextView.setVisibility(View.VISIBLE);
    }

    private void promptPassword() {

        if (null != _popupWindow)
            _popupWindow.dismiss();

        LayoutInflater layoutInflater
                = (LayoutInflater)ProfileActivity.this.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        View popupView = layoutInflater.inflate(R.layout.enter_password_layout, null);
        _popupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);

        popupView.findViewById(R.id.verify_password_cancel).setOnClickListener(this);
        popupView.findViewById(R.id.verify_password_submit).setOnClickListener(this);
        popupView.findViewById(R.id.verify_password_forgot).setOnClickListener(this);

        _passwordVerifyProgress = (ProgressBar)popupView.findViewById(R.id.profile_edit_password_verify_progress);
        _passwordVerifyProgress.setVisibility(View.INVISIBLE);
        _popupLayout = (LinearLayout)popupView.findViewById(R.id.password_verify_popup_layout);

        // Fix password hint text :\ stupid ....
        BWEditText password = (BWEditText)popupView.findViewById(R.id.verify_password);
        password.setTypeface(Typeface.DEFAULT);
        password.setTransformationMethod(new PasswordTransformationMethod());

        _popupLayout.setVisibility(View.VISIBLE);

        _popupWindow.setOutsideTouchable(true);
        _popupWindow.setTouchable(true);
        _popupWindow.setFocusable(true);
        _popupWindow.setAnimationStyle(R.style.PopupWindowAnimation);
        _popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, -150);

        InputMethodManager inputMgr = (InputMethodManager)ProfileActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMgr.toggleSoftInput(0, InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

    private boolean validateInput() {
        View focusView = null;
        boolean valid = true;

        // Validate birthday
        String birthday = Globals.getDateOfBirthDisplayFormat().format(_currentDetails.birthday);

        if (TextUtils.isEmpty(birthday) || !birthday.matches("[0-9]+-[0-9]+-[0-9]+")) {
            _birthday.setError(getString(R.string.error_field_required));
            focusView = _birthday;
            valid = false;
        }
        else if (!Utilities.isValidAge(birthday, Globals.getDateOfBirthDisplayFormat())) {
            _birthday.setError(String.format("%s %d", getString(R.string.error_invalid_age), Utilities.MIN_AGE));
            focusView = _birthday;
            valid = false;
        }
        else {
            _birthday.setError(null);
        }

        // Validate fullname
        String fullName = _currentDetails.fullName.trim();

        // Check for a valid phone number, if the user entered one.
//        if (!TextUtils.isEmpty(fullName) && !Utilities.isValidFullName(fullName)) {
//            _fullname.setError(getString(R.string.error_invalid_fullname));
//            focusView = _fullname;
//            valid = false;
//        }
//        else
        if (TextUtils.isEmpty(fullName)) {
            _fullname.setError(getString(R.string.error_field_required));
            focusView = _fullname;
            valid = false;
        }

        if (!valid && focusView != null)
            focusView.requestFocus();

        return valid;
    }

    public void updateProfileImage() {
        updateUserSettings();
    }

    AppPreferences.OnProfileImageChange profileImageChangeListener = new AppPreferences.OnProfileImageChange() {
        @Override
        public void profileImageChanged() {
            saveAndUpdateDetails();
        }
    };

    void destroyProgress() {
        if (null != _progress && _progress.isShowing())
            _progress.dismiss();
    }

    private void saveDone() {
        destroyProgress();
        _currentDetails.canEdit = false;
        Toast.makeText(ProfileActivity.this, "Saved", Toast.LENGTH_SHORT).show();
        ProfileActivity.this.onBackPressed();
        BitwalkingApp.getInstance().trackEvent("profile", "edit", "success");
    }

    private void saveAndUpdateDetails() {
        // send update to server + update service
        try {
            String newBirthDate = Globals.getUTCDateFormat().format(_currentDetails.birthday);
            if (null != _currentDetails.fullName || null != newBirthDate || null != _currentDetails.country) {
                updateMeServer(_currentDetails.fullName, newBirthDate, _currentDetails.email, _currentDetails.country);
            }
            else {
                saveDone();
            }
        }
        catch (Exception e) {
            BitwalkingApp.getInstance().trackException(e);
        }
    }

    public void saveProfileDetails() {
        if (null != _currentDetails) {

            _currentDetails.fullName = _fullname.getText().toString().trim();
//            _currentDetails.country = _country.getText().toString().trim();
            try {
                _currentDetails.birthday = Globals.getDateOfBirthDisplayFormat().parse(_birthday.toString());
            }
            catch (Exception e) {
                BitwalkingApp.getInstance().trackException(e);
            }

            if (validateInput()) {

                // Showing progress dialog
                _progress = new ProgressDialog(ProfileActivity.this);
                _progress.setMessage("Updating profile ...");
                _progress.setCancelable(false);
                _progress.show();

                if (_avatarUpdated) {
                    AppPreferences appPrefs = new AppPreferences(this);
                    appPrefs.setProfileImageChangeListener(profileImageChangeListener);
                    appPrefs.updateAvatar(_currentDetails.image);
                }
                else {
                    if (_deleteAvatar)
                        new AppPreferences(this).deleteAvatar();

                    saveAndUpdateDetails();
                }
            }
        }
    }

    private void updateMeServer(final String newFullName, final String newBirthDate, final String email, final String newCountry) {
        try {
            UpdateMeRequest updateMeRequest = new UpdateMeRequest(newCountry, newFullName, email, newBirthDate);

            ServerApi.putMe(
                    AppPreferences.getUserId(getBaseContext()),
                    AppPreferences.getUserSecret(getBaseContext()),
                    updateMeRequest,
                    new ServerApi.SimpleServerResponseListener() {
                        @Override
                        public void onResponse(final int code) {
                            ProfileActivity.this.runOnUiThread(new Runnable() {
                                public void run() {
                                    String message = "Could not update profile.";
                                    switch (code) {
                                        case 200:
                                            try {
                                                UserInfo userInfo = AppPreferences.getUserInfo(ProfileActivity.this);
                                                userInfo.getMeInfo().fullName = newFullName;
                                                userInfo.getMeInfo().dateOfBirth = newBirthDate;
                                                userInfo.getMeInfo().email = email;
                                                userInfo.getMeInfo().country = newCountry;
                                                AppPreferences.setUserInfo(ProfileActivity.this, userInfo);
                                            }
                                            catch (Exception e) {
                                                BitwalkingApp.getInstance().trackException(e);
                                            }

                                            saveDone();
                                            return;
                                        case 400: // illegal parameter
                                        case 403: // permission denied
                                            break;
                                        default:
                                            break;
                                    }

                                    destroyProgress();
                                    BitwalkingApp.getInstance().trackEvent("profile", "edit", "failure");
                                    // Registration failed, show message
                                    Globals.showSimpleAlertMessage(ProfileActivity.this, "Update Failed", message, "Dismiss");
                                }
                            });
                        }
                    });
        }
        catch (Exception e) {
            Logger.instance().Log(Logger.ERROR, TAG, "failed to send put");
            e.printStackTrace();
        }
    }

    //region Profile country pick

    public void onEditCountry(View v) {
        if (null != _currentDetails && _currentDetails.canEdit) {
            Intent intent = new Intent(this, CountryPickerActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivityForResult(intent, COUNTRY_PICKUP_REQ_ID);
        }
    }

    //endregion

    //region Profile phone edit

    public void onChangePhoneClick() {
        // edit phone number
        Intent intent = new Intent(this, PhoneVerificationActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("oldPhone", _gson.toJson(_currentDetails.phone));
        startActivityForResult(intent, PHONE_EDIT_REQUEST_CODE);
    }

    //endregion

    //region Profile image pick

    final static int _PROFILE_IMAGE_SIZE_DP = 120;

    public void onEditProfileImageClick(View v) {
        if (null != _currentDetails && !_currentDetails.canEdit)
            return;

        final CharSequence[] items = { "DELETE PHOTO", "TAKE PHOTO", "CHOOSE PHOTO" };
        AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this);
        builder.setTitle("Profile Photo");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (items[item].equals("TAKE PHOTO")) {
                    if (Globals.havePermission(ProfileActivity.this, Manifest.permission.CAMERA, CAMERA_PERMISSIONS_REQUEST_ID))
                        takePhotoIntent();
                } else if (items[item].equals("CHOOSE PHOTO")) {
                    if (Globals.havePermission(ProfileActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE, READ_EXTERNAL_PERMISSIONS_REQUEST_ID))
                        choosePhotoIntent();
                } else if (items[item].equals("DELETE PHOTO")) {
                    dialog.dismiss();
                    setDefaultProfileImage();
                }
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                ((BitwalkingApp)getApplication()).trackEvent("profile", "change.avatar", "cancel");
            }
        });
        builder.show();
    }

    private void setDefaultProfileImage() {
        _currentDetails.image = BitmapFactory.decodeResource(getBaseContext().getResources(), R.drawable.profile_default_circle);
        _deleteAvatar = true;
        updateUserSettings();
    }

    private void updateUserSettings() {
        if (null != _currentDetails) {
            // Top
            _profileImage.setImageBitmap(_currentDetails.image);

            // Settings
            _fullname.setText(_currentDetails.fullName);
            _email.setText(_currentDetails.email);

            if (_currentDetails.phone.hasPhone()) {
                String number = _currentDetails.phone.number.replaceFirst("0*", "");
                _phone.setText(String.format("+%s (0) %s", _currentDetails.phone.countryCode, number));
            }

            // Birth Day
            _birthday.setText(Globals.getDateOfBirthDisplayFormat().format(_currentDetails.birthday));
            // Country
            _country.setText(IsoToPhone.getCountryName(_currentDetails.country));
        }
    }

    private void takePhotoIntent() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, TAKE_PROFILE_PHOTO);
    }

    private void choosePhotoIntent() {
        Intent intent = new Intent(
                Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select profile photo"), CHOOSE_PROFILE_PHOTO);
    }

    private Bitmap cropProfileImage(Bitmap image) {
        int min = Math.min(image.getWidth(), image.getHeight());
        int max = Math.max(image.getWidth(), image.getHeight());
        int n = min / 2;
        Bitmap croppedImage = Bitmap.createBitmap(image,
                (int) image.getWidth() / 2 - n,
                (int) image.getHeight() / 2 - n,
                (int) min,
                (int) min);

        return croppedImage;
    }

    private Bitmap cropProfileImage2(Bitmap srcBmp) {
        Bitmap dstBmp = null;

        if (srcBmp.getWidth() >= srcBmp.getHeight()){

            dstBmp = Bitmap.createBitmap(
                    srcBmp,
                    srcBmp.getWidth()/2 - srcBmp.getHeight()/2,
                    0,
                    srcBmp.getHeight(),
                    srcBmp.getHeight()
            );

        }else{

            dstBmp = Bitmap.createBitmap(
                    srcBmp,
                    0,
                    srcBmp.getHeight()/2 - srcBmp.getWidth()/2,
                    srcBmp.getWidth(),
                    srcBmp.getWidth()
            );
        }

        return dstBmp;
    }


    private Bitmap getThumbnailBitmap(final Uri uri, final int thumbnailSize) throws IOException{
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(getContentResolver().openInputStream(uri), null, bounds);
        int originalSize = (bounds.outHeight > bounds.outWidth) ? bounds.outHeight : bounds.outWidth;
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inJustDecodeBounds = false;
        opt.inMutable = true;
        opt.inSampleSize = originalSize / thumbnailSize;
        return BitmapFactory.decodeStream(getContentResolver().openInputStream(uri), null, opt);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode) {
            case TAKE_PROFILE_PHOTO: {
                String resultLabel = "cancel";
                if (resultCode == RESULT_OK) {
                    if (null != data && null != data.getAction()) {
                        Bundle extras = data.getExtras();
                        Bitmap selectedImage = (Bitmap) extras.get("data");
                        setProfileImage(cropProfileImage2(selectedImage));
                        resultLabel = "confirm";
                    }
                }

                ((BitwalkingApp)getApplication()).trackEvent("profile", "change.avatar", resultLabel);

                break;
            }
            case CHOOSE_PROFILE_PHOTO: {
                String resultLabel = "cancel";
                if (resultCode == RESULT_OK) {
                    if (null != data && null != data.getData()) {

                        Bitmap selectedImage = null;
                        try {
                            Uri imgUri = data.getData();
                            final float scale = getBaseContext().getResources().getDisplayMetrics().density;
                            int pixels = (int) (_PROFILE_IMAGE_SIZE_DP * scale + 0.5f);

                            Bitmap bitmap = getThumbnailBitmap(imgUri, pixels);
                            if (null != bitmap)
                                selectedImage = fixRotation(data.getData(), bitmap);
                        } catch (Exception e) {
                            e.printStackTrace();
                            return;
                        }

                        setProfileImage(cropProfileImage2(selectedImage));
                        resultLabel = "confirm";
                    }
                }

                ((BitwalkingApp)getApplication()).trackEvent("profile", "change.avatar", resultLabel);

                break;
            }
            case PHONE_EDIT_REQUEST_CODE: {
                String resultLabel = "cancel";
                if (resultCode == RESULT_OK) {
                    if (null != data) {
                        String newTelephonyJson = data.getStringExtra(PhoneVerificationActivity.NEW_USER_TELEPHONE);
                        String newUserSecret = data.getStringExtra(PhoneVerificationActivity.NEW_USER_SECRET_KEY);

                        _currentDetails.phone = _gson.fromJson(newTelephonyJson, TelephoneInfo.class);
                        try {
                            UserInfo userInfo = AppPreferences.getUserInfo(ProfileActivity.this);
                            userInfo.getMeInfo().phone = _currentDetails.phone;
                            if (null != newUserSecret)
                                userInfo.getAuthInfo().userSecret = newUserSecret;
                            AppPreferences.setUserInfo(ProfileActivity.this, userInfo);

                            try {
                                // Update service with new info
                                _serviceApi.updateServiceInfo(_gson.toJson(AppPreferences.getServiceInitInfo(ProfileActivity.this)));
                            } catch (Exception e) {
                                BitwalkingApp.getInstance().trackException("_serviceApi.updateServiceInfo failed", e);
                            }
                        }
                        catch (Exception e) {
                            BitwalkingApp.getInstance().trackException("failed to PHONE_EDIT_REQUEST_CODE", e);
                        }
                    }
                }

                ((BitwalkingApp)getApplication()).trackEvent("profile", "change.phone", resultLabel);

                break;
            }
            case COUNTRY_PICKUP_REQ_ID: {
                String resultLabel = "cancel";
                if (resultCode == RESULT_OK) {
                    if (null != data && data.hasExtra("CountryIso")) {
                        String newCountryIso = data.getStringExtra("CountryIso");
                        if (!newCountryIso.toLowerCase().contentEquals(_currentDetails.country.toLowerCase()))
                            resultLabel = "confirm";
                        _currentDetails.country = newCountryIso;
                    }
                }

                ((BitwalkingApp)getApplication()).trackEvent("profile", "change.country", resultLabel);

                break;
            }
            case REQUEST_ENABLE_BT: {
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "Bluetooth is on", Toast.LENGTH_SHORT).show();
                    Intent newIntent = new Intent(ProfileActivity.this, MisfitDeviceListActivity.class);
                    newIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Logger.instance().Log(Logger.DEBUG, TAG, "BT not enabled");
                    Toast.makeText(this, "Failed to turn Bluetooth on", Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case REQUEST_SELECT_DEVICE: {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    ShineDevice device = data.getParcelableExtra(MisfitService.EXTRA_DEVICE);
                    Logger.instance().Log(Logger.DEBUG, TAG, "new Misift device selected: " + device.getName() + "[" + device.getSerialNumber() + "]");

                    if (_boundToService && null != _serviceApi) {
                        try {
                            _serviceApi.setMisfitDevice(device.getAddress(), device.getSerialNumber());
                        }
                        catch (Exception e) {
                            Logger.instance().Log(Logger.ERROR, "TAG", "failed to set misfit devicein bw-service");
                        }
                    }
                    else {
                        Logger.instance().Log(Logger.ERROR, TAG, "cannot set device, bw-service not bound.");
                    }
                }

                break;
            }
            case CHANGE_EMAIL_ID: {
                if (resultCode == RESULT_OK && data != null) {
                    String newEmail = data.getStringExtra("NewEmail");
                    if (null != newEmail && !newEmail.isEmpty()) {
                        // update user's new email
                        _currentDetails.email = newEmail;

                        try {
                            UserInfo userInfo = AppPreferences.getUserInfo(ProfileActivity.this);
                            userInfo.getMeInfo().email = newEmail;
                            AppPreferences.setUserInfo(ProfileActivity.this, userInfo);
                        }
                        catch (Exception e) {
                            BitwalkingApp.getInstance().trackException(e);
                        }

                        updateUserSettings();
                    }
                }
                break;
            }
            default: break;
        }
    }

    private Bitmap fixRotation(Uri imageUri, Bitmap bitmap) {
        String[] orientationColumn = {MediaStore.Images.Media.ORIENTATION};
        Cursor cur = getContentResolver().query(imageUri, orientationColumn, null, null, null);
        int orientation = -1;
        if (cur != null && cur.moveToFirst()) {
            orientation = cur.getInt(cur.getColumnIndex(orientationColumn[0]));
            Logger.instance().Log(Logger.DEBUG, TAG, "orientation = " + orientation);
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(orientation);

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            switch (requestCode) {
                case READ_EXTERNAL_PERMISSIONS_REQUEST_ID:
                    choosePhotoIntent();
                    break;
                case CAMERA_PERMISSIONS_REQUEST_ID:
                    takePhotoIntent();
                    break;
                default: break;
            }
        }
    }

    public void setProfileImage(Bitmap profileImage) {
        final float scale = getBaseContext().getResources().getDisplayMetrics().density;
        int pixels = (int) (_PROFILE_IMAGE_SIZE_DP * scale * 2 + 0.5f);
        profileImage = scaleDown(profileImage, pixels, false);
        _currentDetails.image = profileImage;
        _avatarUpdated = true;
    }

    public static Bitmap scaleDown(Bitmap realImage, float maxImageSize, boolean filter) {
        float ratio = Math.min(
                (float) maxImageSize / realImage.getWidth(),
                (float) maxImageSize / realImage.getHeight());
        int width = Math.round((float) ratio * realImage.getWidth());
        int height = Math.round((float) ratio * realImage.getHeight());

        Bitmap newBitmap = Bitmap.createScaledBitmap(realImage, width, height, filter);
        return newBitmap;
    }

    //endregion

    //region Misfit

    public void onMisfitSetupClick(View v) {
        if (_wearableSwitch.isChecked())
            return; // Ignore click

        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            Logger.instance().Log(Logger.INFO, TAG, "misfit config - bluetooth is off");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            Intent newIntent = new Intent(ProfileActivity.this, MisfitDeviceListActivity.class);
            newIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
        }
    }

    //endregion

    public void onHelpClick(View v) {
        ((BitwalkingApp)getApplication()).trackEvent("profile", "open.support.faq", "");
        ((BitwalkingApp)getApplication()).trackScreenView("support");

        Intent intent = new Intent(this, WebActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("url", "bitwalking.com/help");
        startActivity(intent);
    }

    public void onLegalClick(View v) {
        ((BitwalkingApp)getApplication()).trackEvent("profile", "open.legal", "");
        startActivity(new Intent(this, LegalActivity.class));
    }

    @Override
    protected void onBwServiceConnected() {
        if (null != _serviceApi && _boundToService) {
            try {
                _wearableSwitch.setChecked(_serviceApi.getStepsSource() == Globals.STEPS_SOURCE_MISFIT);
                fillProfileInfo();
                updateUserSettings();
            }
            catch (Exception e) {
                BitwalkingApp.getInstance().trackException(e);
            }
        }
    }

    @Override
    protected void onBwServiceDisconnected() {

    }
    */
}
