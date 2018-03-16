package bitwalking.bitwalking.activityes;

import android.app.Activity;

import java.util.HashMap;
import java.util.Map;

import bitwalking.bitwalking.invite_user.UserInviteActivity;
import bitwalking.bitwalking.mvi.events.EventsActivity;
import bitwalking.bitwalking.registration_and_login.GoActivity;
import bitwalking.bitwalking.registration_and_login.JoinActivity;
import bitwalking.bitwalking.mvi.login.LoginActivity;
import bitwalking.bitwalking.mvi.forgot_password.ForgotPasswordActivity;
import bitwalking.bitwalking.mvi.reset_password.ResetPasswordActivity;
import bitwalking.bitwalking.settings.ChangeEmailActivity;
import bitwalking.bitwalking.settings.ChangePasswordActivity;
import bitwalking.bitwalking.settings.InviteBusinessActivity;
import bitwalking.bitwalking.settings.ProfileActivity;
import bitwalking.bitwalking.transactions.ui.SendRequestActivity;
import bitwalking.bitwalking.vote_product.VoteProductActivity;

/**
 * Created by Marcus on 11/3/16.
 */

public class MapUriToActivity {

    private static Map<String, Class<? extends Activity>> uri2activity = new HashMap<>();
    static {
        uri2activity.put("main", MainActivity.class);
        uri2activity.put("events", EventsActivity.class);
        uri2activity.put("complete-profile", CompleteProfileActivity.class);
        uri2activity.put("wallet", WalletActivity.class);
        uri2activity.put("off-grid", OffGridActivity.class);
        uri2activity.put("what-to-do", WhatToDoActivity.class);
        uri2activity.put("profile", ProfileActivity.class);
        uri2activity.put("change-password", ChangePasswordActivity.class);
        uri2activity.put("change-email", ChangeEmailActivity.class);
        uri2activity.put("forgot-password", ForgotPasswordActivity.class);
        uri2activity.put("reset-password", ResetPasswordActivity.class);
        uri2activity.put("buy-sell", BuySellActivity.class);
        uri2activity.put("send-request", SendRequestActivity.class);
        uri2activity.put("invite-business", InviteBusinessActivity.class);
        uri2activity.put("user-invite", UserInviteActivity.class);
        uri2activity.put("register", JoinActivity.class);
        uri2activity.put("store-vote", VoteProductActivity.class);
        uri2activity.put("login", LoginActivity.class);
        uri2activity.put("email-validation", GoActivity.class);
        uri2activity.put("open-uri", GoActivity.class);
    }

    public static Class<? extends Activity> getActivityClass(String uri) {
        if (null == uri)
            return null;

        return uri2activity.get(uri);
    }
}
