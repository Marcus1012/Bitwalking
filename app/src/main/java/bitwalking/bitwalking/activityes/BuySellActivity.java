package bitwalking.bitwalking.activityes;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import bitwalking.bitwalking.BitwalkingApp;
import bitwalking.bitwalking.R;

/**
 * Created by Marcus on 7/13/16.
 */
public class BuySellActivity extends Activity {
    private boolean _toastShowed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.buy_sell_layout);

        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
    }

    public void onSubscribeClick(View v) {
        //todo:newserver
//        // update server with user subscription
//        AccessServer.INSTANCE.addRequest(new OnServerFunctionExecuted() {
//                                             @Override
//                                             public void executionResponse(String responseJson, int id) {
////                        SubscribeResponse res = new Gson().fromJson(responseJson, SubscribeResponse.class);
////                        String toastMsg = "Failed to subscribe";
////                        if (null != res && res.requestWasSuccessful()) {
////                            toastMsg = "Thank you for subscribing";
////                        }
//
//                                                 BuySellActivity.this.runOnUiThread(new Runnable() {
//                                                     @Override
//                                                     public void run() {
//                                                         if (!_toastShowed) {
//                                                             Toast.makeText(BuySellActivity.this, "Thank you for subscribing", Toast.LENGTH_SHORT).show();
//                                                             _toastShowed = true;
//                                                         }
//                                                     }
//                                                 });
//                                             }
//                                         },
//                0,
//                new Gson().toJson(new SubscribeRequest(AppPreferences.getUserInfo(this).name)),
//                AccessServer.GENERAL_SERVICE);
    }

    public void onBackClick(View v) {
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        ((BitwalkingApp)getApplication()).trackScreenView("buy/sell");
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
    }
}
