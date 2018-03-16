package bitwalking.bitwalking.activityes;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import com.google.android.gms.appinvite.AppInviteInvitation;
import com.google.gson.Gson;

import bitwalking.bitwalking.AppPreferences;
import bitwalking.bitwalking.BitwalkingApp;
import bitwalking.bitwalking.R;
import bitwalking.bitwalking.user_info.UserInfo;
import bitwalking.bitwalking.util.Globals;
import bitwalking.bitwalking.util.Logger;

/**
 * Created by Marcus on 9/11/16.
 */
public class WhatToDoActivity extends BwActivity {
    private static final String TAG = WhatToDoActivity.class.getSimpleName();

    String _screenName;
    boolean _invitePressed = false;
    ProgressBar _inviteLoading;
    Gson _gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.what_to_do_activity);
        _inviteLoading = (ProgressBar)findViewById(R.id.user_invite_loading);

        _screenName = getIntent().getStringExtra("Screen.Name");
        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
        bindToBwService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindBwService();
    }

    public void onInviteFriends(View v) {
        if (_invitePressed || null == _serviceApi)
            return;

        _invitePressed = true;
        _inviteLoading.setVisibility(View.VISIBLE);

        UserInfo userInfo = null;
        try {
            userInfo = AppPreferences.getUserInfo(WhatToDoActivity.this);
        }
        catch (Exception e) {
            BitwalkingApp.getInstance().trackException(e);
        }

        if (null == userInfo)
            return;

        String code = userInfo.getMeInfo().friendInviteToken;
//        openUserInvite(code);
        sendInvitation(code);
    }

    public void onBackClick(View v) {
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
        if (null != _screenName)
            ((BitwalkingApp)getApplication()).trackScreenView(_screenName);
    }

    private final int REQUEST_INVITE = 3;
    private final int SEND_INVITE_REQUEST = 4;

    private void sendInvitation(String affiliationCode) {
        Uri dynamicLink = Globals.getDownloadLink();

//        String title =
//                "Hi, I've been Bitwalking and thought you might like it.\n" +
//                "Sign up here:";
//        String title =
//                "Hi, I'm generating money by walking!";
//        String text = "Join #bitwalking, the new global currency: " + dynamicLink.toString();
        String text = "Hi, I'm generating money by walking! Join #bitwalking, the new global currency: " + dynamicLink.toString();
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
//        intent.putExtra(Intent.EXTRA_SUBJECT, title);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        startActivityForResult(Intent.createChooser(intent, "Invite friends"), SEND_INVITE_REQUEST);

        ((BitwalkingApp)getApplication()).trackEvent("main", "open.invite", "");
//        Uri dynamicLink = buildInviteLink(buildDeepLink(affiliationCode));
//
//        String title =
//                "Hi, I've been Bitwalking and thought you might like it.\n" +
//                        "Sign up here:";
//        Intent intent = new Intent(Intent.ACTION_SEND);
//        intent.setType("text/plain");
//        intent.putExtra(Intent.EXTRA_SUBJECT, title);
//        intent.putExtra(Intent.EXTRA_TEXT, dynamicLink.toString());
//        startActivityForResult(Intent.createChooser(intent, title), SEND_INVITE_REQUEST);
//
//        ((BitwalkingApp)getApplication()).trackEvent("main", "open.invite", "");
    }

    private Uri buildDeepLink(String affiliationCode) {
        // Build the link with all required parameters
        Uri.Builder builder = new Uri.Builder()
                .scheme("http")
                .authority("bitwalking.com")
                .path("/go-app/UserInvite")
                .appendQueryParameter("code", affiliationCode);

        // Return the completed deep link.
        return builder.build();
    }

    private Uri buildInviteLink(Uri deepLink) {
        // Get the unique appcode for this app.
        String appCode = getString(R.string.app_code);

        // Get this app's package name.
        String packageName = getApplicationContext().getPackageName();

        // Build the link with all required parameters
        Uri.Builder builder = new Uri.Builder()
                .scheme("https")
                .authority(appCode + ".app.goo.gl")
                .path("/")
                .appendQueryParameter("link", deepLink.toString())
                .appendQueryParameter("apn", packageName);

        // Return the completed deep link.
        return builder.build();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_INVITE) {
            if (resultCode == RESULT_OK) {
                // Check how many invitations were sent and log a message
                // The ids array contains the unique invitation ids for each invitation sent
                // (one for each contact select by the user). You can use these for analytics
                // as the ID will be consistent on the sending and receiving devices.
                String[] ids = AppInviteInvitation.getInvitationIds(resultCode, data);
                ((BitwalkingApp)getApplication()).trackEvent("main", "open.invite", "invited." + ids.length);

                for (String s : ids) {
                    Logger.instance().Log(Logger.DEBUG, TAG, s);
                }

            } else {
                // Sending failed or it was canceled, show failure message to the user
                ((BitwalkingApp)getApplication()).trackEvent("main", "open.invite", "canceled");
            }

            _invitePressed = false;
            _inviteLoading.setVisibility(View.GONE);
        }

        if (requestCode == SEND_INVITE_REQUEST) {
            if (resultCode == RESULT_OK) {
                // Check how many invitations were sent and log a message
                // The ids array contains the unique invitation ids for each invitation sent
                // (one for each contact select by the user). You can use these for analytics
                // as the ID will be consistent on the sending and receiving devices.
                ((BitwalkingApp)getApplication()).trackEvent("main", "open.invite", "invited." + 1);
            } else {
                // Sending failed or it was canceled, show failure message to the user
                ((BitwalkingApp)getApplication()).trackEvent("main", "open.invite", "canceled");
            }

            _invitePressed = false;
            _inviteLoading.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onBwServiceConnected() {
    }

    @Override
    protected void onBwServiceDisconnected() {
    }
}
