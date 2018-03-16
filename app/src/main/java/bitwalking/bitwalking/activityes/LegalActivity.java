package bitwalking.bitwalking.activityes;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import bitwalking.bitwalking.BitwalkingApp;
import bitwalking.bitwalking.R;

/**
 * Created by Marcus on 1/9/17.
 */

public class LegalActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.legal_layout);
        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
    }


    public void onTermsOfUseClick(View v) {
        ((BitwalkingApp)getApplication()).trackEvent("legal", "open.term.of.use", "");

        Intent intent = new Intent(this, WebActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("url", "bitwalking.com/terms");
        startActivity(intent);
    }

    public void onPrivacyPolicyClick(View v) {
        ((BitwalkingApp)getApplication()).trackEvent("legal", "open.privacy.policy", "");

        Intent intent = new Intent(this, WebActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("url", "bitwalking.com/privacy");
        startActivity(intent);
    }

    public void onBackClick(View v) {
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        ((BitwalkingApp)getApplication()).trackScreenView("legal");
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
    }
}
