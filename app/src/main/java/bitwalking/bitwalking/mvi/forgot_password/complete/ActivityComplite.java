package bitwalking.bitwalking.mvi.forgot_password.complete;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;

import bitwalking.bitwalking.R;
import bitwalking.bitwalking.mvi.login.LoginActivity;
import bitwalking.bitwalking.util.ActivityUtils;
import butterknife.ButterKnife;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

/**
 * Created by alexey on 23.08.17.
 */

public class ActivityComplite extends AppCompatActivity {

    private Button doneView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_forgot_pas_done);
        ButterKnife.bind(this);
        ActivityUtils.AttachTransperentTitleBar(this);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        doneView=(Button)findViewById(R.id.doneView);

        doneView.setOnClickListener(v->
                {
                    Intent intent = new Intent(this,LoginActivity.class);
                  //  String email = getIntent().getStringExtra(ActivityEmail.EMAIL);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                   // intent.putExtra(Globals.LOGIN_ACTIVITY_USERNAME_EXTRA,email);
                    startActivity(intent);
                }
               );


    }





    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }


    }
