package bitwalking.bitwalking.activityes;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import android.text.TextUtils;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Calendar;
import java.util.TimeZone;

import bitwalking.bitwalking.AppPreferences;
import bitwalking.bitwalking.BitwalkingApp;
import bitwalking.bitwalking.R;
import bitwalking.bitwalking.registration_and_login.IsoToPhone;
import bitwalking.bitwalking.registration_and_login.Utilities;
import bitwalking.bitwalking.server.api.ServerApi;
import bitwalking.bitwalking.server.requests.UpdateMeRequest;
import bitwalking.bitwalking.settings.CountryPickerActivity;
import bitwalking.bitwalking.user_info.MeInfo;
import bitwalking.bitwalking.user_info.UserInfo;
import bitwalking.bitwalking.util.Globals;
import bitwalking.bitwalking.util.Logger;
import bitwalking.bitwalking.util.RobotoTextView;
import bitwalking.bitwalking.util.RoundImageView;

/**
 * Created by Marcus on 9/14/16.
 */
public class CompleteProfileActivity extends Activity {
    ProgressDialog _progress;

    EditText _editFirstName, _editLastName;
    RobotoTextView _textDOB, _textCountry;
    DatePickerDialog birthDatePickerDialog;
    RoundImageView _profileImage;
    Bitmap _profileBitmap;

    String _firstName, _lastName, _dob, _country, _email = null;
    boolean _updateComplete = false;

    Gson _gson;

    final static String TAG = CompleteProfileActivity.class.getSimpleName();
    final static int _COUNTRY_PICKER_REQ_ID = 1;

    //region Init

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.complete_profile_activity);

        _gson = new Gson();

        _editFirstName = (EditText)findViewById(R.id.profile_first_name);
        _editLastName = (EditText)findViewById(R.id.profile_last_name);
        _textCountry = (RobotoTextView)findViewById(R.id.profile_country);
        _profileImage = (RoundImageView)findViewById(R.id.complete_profile_image);

        initDOB();
        initUi();

        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyProgress();
    }

    private void initDOB() {
        _textDOB = (RobotoTextView)findViewById(R.id.profile_dob);
        Calendar newCalendar = Calendar.getInstance();

        Context context = this;
        if (Globals.isBrokenSamsungDevice()) { // Fu**ing android - http://stackoverflow.com/a/31855744
            context = new ContextThemeWrapper(this, android.R.style.Theme_Holo_Light_Dialog);
        }

        birthDatePickerDialog = new DatePickerDialog(context, new DatePickerDialog.OnDateSetListener() {

            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                _editFirstName.clearFocus();
                _editLastName.clearFocus();

                Calendar newDate = Calendar.getInstance();
                newDate.setTimeZone(TimeZone.getTimeZone("UTC"));
                newDate.set(year, monthOfYear, dayOfMonth, 0, 0, 0);
                newDate.set(Calendar.MILLISECOND, 0);
                _textDOB.setText(Globals.getDateOfBirthDisplayFormat().format(newDate.getTime()));
                _dob = Globals.getUTCDateFormat().format(newDate.getTime());
            }

        },  newCalendar.get(Calendar.YEAR) - Utilities.MIN_AGE,
                newCalendar.get(Calendar.MONTH),
                newCalendar.get(Calendar.DAY_OF_MONTH));
    }

    private void initUi() {
        try {
            UserInfo userInfo = AppPreferences.getUserInfo(CompleteProfileActivity.this);
            if (null != userInfo) {
                _email = userInfo.getMeInfo().email;

                if (_editFirstName.getText().toString().isEmpty() &&
                        _editLastName.getText().toString().isEmpty() &&
                        null != userInfo.getMeInfo().fullName) {
                    int x = userInfo.getMeInfo().fullName.indexOf(' ');
                    if (x >= 0) {
                        _editFirstName.setText(userInfo.getMeInfo().fullName.substring(0, x));

                        if (x < (userInfo.getMeInfo().fullName.length() - 1))
                            _editLastName.setText(userInfo.getMeInfo().fullName.substring(x + 1));
                    }
                }

                if (null == _country && null != userInfo.getMeInfo().country) {
                    _country = userInfo.getMeInfo().country;
                    _textCountry.setText(IsoToPhone.getCountryName(userInfo.getMeInfo().country));
                }

                if (null == _dob && null != userInfo.getMeInfo().dateOfBirth) {
                    _dob = userInfo.getMeInfo().dateOfBirth;
                    try {
                        _textDOB.setText(Globals.getDateOfBirthDisplayFormat().format(Globals.getUTCDateFormat().parse(_dob)));
                    } catch (Exception e) {
                        _textDOB.setText("");
                    }
                }

                Bitmap profileImage = new AppPreferences(CompleteProfileActivity.this).getProfileImage();
                if (null != profileImage)
                    _profileImage.setImageBitmap(profileImage);
            }
        }
        catch (Exception e) {
            BitwalkingApp.getInstance().trackException(e);
        }
    }

    //endregion

    //region Input Validation

    private boolean validateFullName(View focusView) {
        boolean valid = true;
        _firstName = _editFirstName.getText().toString().trim();
        _lastName = _editLastName.getText().toString().trim();

        if (TextUtils.isEmpty(_lastName)) {
            _editLastName.setError(getString(R.string.error_field_required));
            focusView = _editLastName;
            valid = false;
        }

        // Check if one one the inputs is empty
        if (TextUtils.isEmpty(_firstName)) {
            _editFirstName.setError(getString(R.string.error_field_required));
            focusView = _editFirstName;
            valid = false;
        }

        return valid;
    }

    private boolean validateDOB(View focusView) {
        boolean valid = true;
        String dobText = _textDOB.getText().toString();

        if (TextUtils.isEmpty(dobText) || !dobText.matches("[0-9]+-[0-9]+-[0-9]+")) {
            _textDOB.setError(getString(R.string.error_field_required));
            focusView = _textDOB;
            valid = false;
        }
        else if (!Utilities.isValidAge(dobText, Globals.getDateOfBirthDisplayFormat())) {
            _textDOB.setError(String.format("%s %d", getString(R.string.error_invalid_age), Utilities.MIN_AGE));
            focusView = _textDOB;
            valid = false;
        }
        else {
            _textDOB.setError(null);
        }

        return valid;
    }

    private boolean validateCountry(View focusView) {
        boolean valid = true;
        if (TextUtils.isEmpty(_country)) {
            _textCountry.setError(getString(R.string.error_field_required));
            focusView = _textCountry;
            valid = false;
        }

        return valid;
    }

    //endregion

    //region Clicks

    public void onDOBClick(View v) {
        _textDOB.setError(null);
        birthDatePickerDialog.show();
    }

    public void onCountryClick(View v) {
        _editFirstName.clearFocus();
        _editLastName.clearFocus();

        Intent intent = new Intent(this, CountryPickerActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("showCountryCode", false);
        startActivityForResult(intent, _COUNTRY_PICKER_REQ_ID);
    }

    public void onSaveProfile(View v) {
        View focusView = null;

        if (validateFullName(focusView)) {
            if (validateDOB(focusView)) {
                if (validateCountry(focusView)) {
                    // All valid, update user's profile
                    updateProfile();
                }
            }
        }

        if (focusView != null)
            focusView.requestFocus();
    }

    public void onExitClick(View v) {
        onBackPressed();
    }

    //endregion

    //region Profile image pick

    private static final int TAKE_PROFILE_PHOTO                     = 1;
    private static final int CHOOSE_PROFILE_PHOTO                   = 2;
    private static final int _PROFILE_IMAGE_SIZE_DP                 = 120;
    private static final int CAMERA_PERMISSIONS_REQUEST_ID          = 10;
    private static final int READ_EXTERNAL_PERMISSIONS_REQUEST_ID   = 11;

    public void onEditProfileImageClick(View v) {
//        if (true)
//            return;

        final CharSequence[] items = { "DELETE PHOTO", "TAKE PHOTO", "CHOOSE PHOTO" };
        AlertDialog.Builder builder = new AlertDialog.Builder(CompleteProfileActivity.this);
        builder.setTitle("Profile Photo");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (items[item].equals("TAKE PHOTO")) {
                    if (Globals.havePermission(CompleteProfileActivity.this, android.Manifest.permission.CAMERA, CAMERA_PERMISSIONS_REQUEST_ID))
                        takePhotoIntent();
                } else if (items[item].equals("CHOOSE PHOTO")) {
                    if (Globals.havePermission(CompleteProfileActivity.this, android.Manifest.permission.READ_EXTERNAL_STORAGE, READ_EXTERNAL_PERMISSIONS_REQUEST_ID))
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
        _profileBitmap = null;
        _profileImage.setImageBitmap(BitmapFactory.decodeResource(getBaseContext().getResources(), R.drawable.profile_default_circle));
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

    /**
     * returns the thumbnail image bitmap from the given url
     *
     * @param path
     * @param thumbnailSize
     * @return
     */
    private Bitmap getThumbnailBitmap(final String path, final int thumbnailSize) throws FileNotFoundException {
        Bitmap bitmap;
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(new FileInputStream(new File(path)), null, bounds);
        if ((bounds.outWidth == -1) || (bounds.outHeight == -1)) {
            bitmap = null;
        }
        int originalSize = (bounds.outHeight > bounds.outWidth) ? bounds.outHeight
                : bounds.outWidth;
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = originalSize / thumbnailSize;
        bitmap = BitmapFactory.decodeStream(new FileInputStream(new File(path)), null, opts);
        return bitmap;
    }

    protected void onProfileImageActivityResult(int requestCode, int resultCode, Intent data)
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
                            String[] filePathColumn = {MediaStore.Images.Media.DATA};
                            Cursor cursor = getContentResolver().query(imgUri, filePathColumn, null, null, null);
                            cursor.moveToFirst();
                            String filePath = cursor.getString(cursor.getColumnIndex(filePathColumn[0]));
                            final float scale = getBaseContext().getResources().getDisplayMetrics().density;
                            int pixels = (int) (_PROFILE_IMAGE_SIZE_DP * scale + 0.5f);

                            Bitmap bitmap = getThumbnailBitmap(filePath, pixels);
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
        _profileBitmap = profileImage;
        _profileImage.setImageBitmap(_profileBitmap);
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

    //region Save

    private void destroyProgress() {
        if (null != _progress && _progress.isShowing())
            _progress.dismiss();
    }

    private void updateProfile() {
        // Showing progress dialog
        _progress = new ProgressDialog(CompleteProfileActivity.this);
        _progress.setMessage("Updating profile ...");
        _progress.setCancelable(false);
        _progress.show();

        try {
            final String fullName = String.format("%s %s", _firstName, _lastName);
            UpdateMeRequest updateMeRequest = new UpdateMeRequest(_country, fullName, _email, _dob);
            ServerApi.putMe(
                    AppPreferences.getUserId(getBaseContext()),
                    AppPreferences.getUserSecret(getBaseContext()),
                    updateMeRequest,
                    new ServerApi.SimpleServerResponseListener() {
                        @Override
                        public void onResponse(final int code) {
                            CompleteProfileActivity.this.runOnUiThread(new Runnable() {
                                public void run() {
                                    String message = "Could not update profile.";
                                    switch (code) {
                                        case 200:
                                            try {
                                                UserInfo userInfo = AppPreferences.getUserInfo(CompleteProfileActivity.this);
                                                if (null == userInfo.getMeInfo())
                                                    userInfo.setMeInfo(new MeInfo());
                                                userInfo.getMeInfo().fullName = fullName;
                                                userInfo.getMeInfo().dateOfBirth = _dob;
                                                userInfo.getMeInfo().country = _country;
                                                AppPreferences.setUserInfo(CompleteProfileActivity.this, userInfo);
                                            }
                                            catch (Exception e) {
                                                BitwalkingApp.getInstance().trackException("failed to update service userInfo", e);
                                            }

                                            saveProfileImage();
                                            return;
                                        case 400: // illegal parameter
                                        case 403: // permission denied
                                            break;
                                        default:
                                            break;
                                    }

                                    destroyProgress();

                                    BitwalkingApp.getInstance().trackEvent("profile", "completion", "failure");
                                    // Registration failed, show message
                                    Globals.showSimpleAlertMessage(CompleteProfileActivity.this, "Update Failed", message, "Dismiss");
                                }
                            });
                        }
                    });
        }
        catch (Exception e) {
            Logger.instance().Log(Logger.ERROR, TAG, "failed to send put");
            e.printStackTrace();
            if (null != _progress && _progress.isShowing())
                _progress.dismiss();
        }
    }

    AppPreferences.OnProfileImageChange profileImageChangeListener = new AppPreferences.OnProfileImageChange() {
        @Override
        public void profileImageChanged() {
            CompleteProfileActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    saveDone();
                }
            });
        }
    };

    private void saveProfileImage() {
        if (null != _profileBitmap) {
            try {
                AppPreferences appPrefs = new AppPreferences(this);
                appPrefs.setProfileImageChangeListener(profileImageChangeListener);
                appPrefs.updateAvatar(_profileBitmap);
            }
            catch (Exception e) {
                BitwalkingApp.getInstance().trackException(e);
                saveDone();
            }
        }
        else {
            saveDone();
        }
    }

    private void saveDone() {
        destroyProgress();

        _updateComplete = true;

        Toast.makeText(CompleteProfileActivity.this, "Updated", Toast.LENGTH_SHORT).show();
        CompleteProfileActivity.this.onBackPressed();
        BitwalkingApp.getInstance().trackEvent("profile", "completion", "success");
    }

    @Override
    public void onBackPressed() {
        if (_updateComplete) {
            setResult(Activity.RESULT_OK, null);
        }
        else {
            setResult(Activity.RESULT_CANCELED, null);
        }

        super.onBackPressed();
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
    }

    //endregion

    //region Callbacks

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        onProfileImageActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case _COUNTRY_PICKER_REQ_ID:
                    if (data.hasExtra("Country"))
                        _textCountry.setText(data.getStringExtra("Country"));
                        _country = data.getStringExtra("CountryIso");
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Globals.hideSoftKeyboard(CompleteProfileActivity.this);
    }

    //endregion
}
