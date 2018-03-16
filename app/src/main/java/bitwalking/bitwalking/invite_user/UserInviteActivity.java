package bitwalking.bitwalking.invite_user;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.appinvite.AppInviteInvitation;
import com.google.gson.Gson;

import bitwalking.bitwalking.AppPreferences;
import bitwalking.bitwalking.BitwalkingApp;
import bitwalking.bitwalking.activityes.BwActivity;
import bitwalking.bitwalking.R;
import bitwalking.bitwalking.user_info.UserInfo;
import bitwalking.bitwalking.util.Globals;
import bitwalking.bitwalking.util.Logger;

/**
 * Created by Marcus on 5/26/16.
 */
public class UserInviteActivity extends BwActivity {

    private static final String TAG = UserInviteActivity.class.getSimpleName();
    private static final int GET_USER_INVITE_AFF_CODE_REQ_ID = 1;

    boolean _invitePressed = false;
    ProgressBar _inviteLoading;
    TextView _mainText;
    Gson _gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.user_invite_layout);

        _inviteLoading = (ProgressBar)findViewById(R.id.user_invite_loading);
        _mainText = (TextView)findViewById(R.id.user_invite_text);

        setInviteMainText();
        bindToBwService();

        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        try {
            super.startActivityForResult(intent, requestCode);
        } catch (Exception e) {
            BitwalkingApp.getInstance().trackException(new Exception("startActivityForResult failed, req=" + requestCode, e));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindBwService();
    }

    private void setInviteMainText() {
        _mainText.setText(Html.fromHtml("Invite your friends to<br>Bitwalking and get access<br>to early features and<br>exclusive promotions"), TextView.BufferType.SPANNABLE);
//        _mainText.setText(Html.fromHtml("You'll receive 2W$ for<br>each friend who joins<br>Bitwalking, after their<br>first earned W$."), TextView.BufferType.SPANNABLE);
//        _mainText.append(" ");
//        String details = "Details";
//        final SpannableString spannableString = new SpannableString(details);
//
//        ClickableSpan clickableSpan = new ClickableSpan() {
//            @Override
//            public void onClick(View textView) {
//                showDetailsPopup();
//                textView.invalidate();
//            }
//            @Override
//            public void updateDrawState(TextPaint ds) {
//                super.updateDrawState(ds);
//                ds.setUnderlineText(false);
//            }
//        };
//        spannableString.setSpan(clickableSpan, 0, details.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
//
//        // Span to set text color
//        int color = ContextCompat.getColor(_mainText.getContext(), R.color.cool_green);
//        // Set the text color for first 'Details' characters
//        spannableString.setSpan(new ForegroundColorSpan(color), 0, details.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
////        spannableString.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, details.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
//
//        _mainText.append(spannableString);
//        _mainText.setMovementMethod(LinkMovementMethod.getInstance());
    }

    PopupWindow _popupWindow;
    public void showDetailsPopup() {

        if (null != _popupWindow)
            _popupWindow.dismiss();

        LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        View popupView = layoutInflater.inflate(R.layout.my_simple_show_text_popup, null);
        _popupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);

        // Set text
        String detailsText = "Every time a new Bitwalking user signs up with your invitation, you will receive 2W$ once they have walked and earned their first W$. This apply automatically and expires 10 days from the issue date.";
        final TextView textView = (TextView)popupView.findViewById(R.id.simple_popup_text);
        final float scale = getResources().getDisplayMetrics().density;
        int pixels = (int) (320 * scale + 0.5f);
        Globals.INSTANCE.fitText(textView, pixels, detailsText);

        popupView.findViewById(R.id.simple_popup_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _popupWindow.dismiss();
            }
        });

        _popupWindow.setOutsideTouchable(true);
        _popupWindow.setTouchable(true);
        _popupWindow.setFocusable(true);
        _popupWindow.setAnimationStyle(R.style.PopupWindowAnimation);
        _popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, -150);
        _popupWindow.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
    }

    public void onCancelInvite(View v) {
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
        ((BitwalkingApp)getApplication()).trackScreenView("invite.friend");
    }

    private final int REQUEST_INVITE = 3;
    private final int SEND_INVITE_REQUEST = 4;
    public void onInviteFriendsClick(View v) {

        if (_invitePressed || null == _serviceApi)
            return;

        _invitePressed = true;
        _inviteLoading.setVisibility(View.VISIBLE);

        UserInfo userInfo = AppPreferences.getUserInfo(UserInviteActivity.this);

        if (null == userInfo)
            return;

//        String code = userInfo.getMeInfo().friendInviteToken;
//        openUserInvite(code);
        sendInvitation(null);
    }

    @Deprecated
    private void openUserInvite(String affiliationCode) {
//        String inviteString =
//                "Hi, I've been Bitwalking and thought you might like it.\n" +
//                        "Sign up at www.bitwalking.com";
        String inviteString =
                "Hi, I'm generating money by walking! Join #bitwalking, the new global currency: dl.bitwalking.com";

//        Uri myDeepLink = buildDeepLink(affiliationCode);
        Uri myDeepLink = Globals.getDownloadLink();

        // Google app invitations
        String title = "Invite people to Bitwalking";
        Intent intent = new AppInviteInvitation.IntentBuilder(title)
                .setMessage(inviteString)
                .setDeepLink(myDeepLink)
                .build();
        startActivityForResult(intent, REQUEST_INVITE);

        ((BitwalkingApp)getApplication()).trackEvent("main", "open.invite", "");
    }

    private void sendInvitation(String affiliationCode) {
//        Uri dynamicLink = buildInviteLink(buildDeepLink(affiliationCode));
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
    }

//    private Uri buildDeepLink(String affiliationCode) {
//        // Build the link with all required parameters
//        Uri.Builder builder = new Uri.Builder()
//                .scheme("http")
//                .authority("bitwalking.com")
//                .path("/go-app/UserInvite")
//                .appendQueryParameter("code", affiliationCode);
//
//        // Return the completed deep link.
//        return builder.build();
//    }

//    private Uri buildInviteLink(Uri deepLink) {
//        // Get the unique appcode for this app.
//        String appCode = getString(R.string.app_code);
//
//        // Get this app's package name.
//        String packageName = getApplicationContext().getPackageName();
//
//        // Build the link with all required parameters
//        Uri.Builder builder = new Uri.Builder()
//                .scheme("https")
//                .authority(appCode + ".app.goo.gl")
//                .path("/")
//                .appendQueryParameter("link", deepLink.toString())
//                .appendQueryParameter("apn", packageName);
//
//        // Return the completed deep link.
//        return builder.build();
//    }

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
