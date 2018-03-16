package bitwalking.bitwalking.mvi.profile;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.ParseException;
import java.util.Date;

import bitwalking.bitwalking.AppPreferences;
import bitwalking.bitwalking.BitwalkingApp;
import bitwalking.bitwalking.R;
import bitwalking.bitwalking.mvi.MviBtwActivity;
import bitwalking.bitwalking.settings.ProfileDetails;
import bitwalking.bitwalking.user_info.TelephoneInfo;
import bitwalking.bitwalking.user_info.UserInfo;
import bitwalking.bitwalking.util.Globals;
import bitwalking.bitwalking.util.RoundImageView;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

/**
 * Created by alexey on 20.08.17.
 */

public class ProfileActivity extends MviBtwActivity<ViewProfile,PresenterProfile> implements ViewProfile {

    AppPreferences _appPrefs;

    private TextView nameView,
            lastView,
            emailView,
            phoneView;
    private ImageView profileView;

    @NonNull
    @Override
    public PresenterProfile createPresenter() {
        return new PresenterProfile();
    }

    @Override
    protected void onBwServiceConnected() {

    }

    @Override
    protected void onBwServiceDisconnected() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_info_layout);

        nameView = (TextView)findViewById(R.id.nameView);
        lastView = (TextView)findViewById(R.id.lastView);
        emailView = (TextView)findViewById(R.id.emailView);
        phoneView = (TextView)findViewById(R.id.phoneView);
        profileView = (RoundImageView) findViewById(R.id.profileView);

        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);

        _appPrefs = new AppPreferences(getBaseContext());
        fillProfileInfo();
    }

    @Override
    public void render(StateProfile state) {

    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
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

        ProfileDetails   _currentDetails = new ProfileDetails(image, fullName, birthday, country, phone, email, false);

        nameView.setText(userInfo.getMeInfo().fullName);
        lastView.setText(userInfo.getMeInfo().fullName);
        emailView.setText(userInfo.getMeInfo().email);
        phoneView.setText(userInfo.getMeInfo().phone.number);

        profileView.setImageBitmap(image);
    }


    public void backClick(View v){
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
        ((BitwalkingApp)getApplication()).trackScreenView("profile");
    }

}
