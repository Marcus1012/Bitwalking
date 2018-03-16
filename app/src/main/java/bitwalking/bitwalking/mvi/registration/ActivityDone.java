package bitwalking.bitwalking.mvi.registration;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import bitwalking.bitwalking.R;
import bitwalking.bitwalking.mvi.login.LoginActivity;
import bitwalking.bitwalking.util.ActivityUtils;
import bitwalking.bitwalking.util.Globals;
import butterknife.BindView;
import butterknife.ButterKnife;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

/**
 * Created by alexey on 29.08.17.
 */

public class ActivityDone extends AppCompatActivity{

    @BindView(R.id.nameView)
    TextView nameView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_rgistration_done);
        ButterKnife.bind(this);
        ActivityUtils.AttachTransperentTitleBar(this);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

        String name = getIntent().getStringExtra(ActivityFirstName.FIRSTNAME);

        nameView.setText(name + ",\n" +
                "It’s great to have\n" +
                "you with us!");
    }


    public void onDoneClick(View v){
        Intent intent = new Intent(this,LoginActivity.class);
        String email = getIntent().getStringExtra(ActivityEmail.EMAIL);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(Globals.LOGIN_ACTIVITY_USERNAME_EXTRA,email);
        startActivity(intent);
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }
}
