package bitwalking.bitwalking.mvi.settings;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import bitwalking.bitwalking.AppPreferences;
import bitwalking.bitwalking.R;
import bitwalking.bitwalking.user_info.UserInfo;
import butterknife.BindView;
import butterknife.ButterKnife;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

/**
 * Created by alexey on 22.08.17.
 */

public class ActivitySettings extends AppCompatActivity {


    @BindView(R.id.logoutView)
    Button logoutView;
    private ImageView profileImage;
    private TextView profileName;

    private AppPreferences _appPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_settings);
        ButterKnife.bind(this);
        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);

        profileImage = (ImageView)findViewById(R.id.profileView);
        profileName = (TextView)findViewById(R.id.nameView);

        _appPrefs = new AppPreferences(getBaseContext());

        showData();
    }


    private void showData(){
        profileImage.setImageBitmap(_appPrefs.getProfileImage());
        profileName.setText(getCurrentUserInfo().getMeInfo().fullName);
    }

    private UserInfo getCurrentUserInfo() {
        return AppPreferences.getUserInfo(ActivitySettings.this);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
    }
}
