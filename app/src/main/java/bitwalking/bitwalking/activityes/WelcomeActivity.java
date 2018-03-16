package bitwalking.bitwalking.activityes;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import bitwalking.bitwalking.R;
import bitwalking.bitwalking.registration_and_login.GoActivity;

/**
 * Created by Marcus on 12/19/15.
 */
public class WelcomeActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_to_bitwalking);
    }

    public void onStartClick(View v) {
        Intent intent = new Intent(WelcomeActivity.this, GoActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        WelcomeActivity.this.finish();
    }
}
