package bitwalking.bitwalking.activityes;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import bitwalking.bitwalking.BitwalkingApp;
import bitwalking.bitwalking.R;

/**
 * Created by Marcus on 9/22/16.
 */
public class OffGridActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.off_grid_activity);
    }

    public void onBackClick(View v) {
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        ((BitwalkingApp)getApplication()).trackScreenView("no.connection");
    }
}
