package bitwalking.bitwalking.invite_user;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import bitwalking.bitwalking.AppPreferences;
import bitwalking.bitwalking.R;
import bitwalking.bitwalking.registration_and_login.JoinActivity;

/**
 * Created by Marcus on 5/28/16.
 */
public class InvitationRegistrationActivity extends Activity {
    public static final String INVITE_AFFILIATION_CODE = "InviteAffiliationCode";
    public static final String INVITATION_ID = "InvitationId";
    String _affiliationCode = null, _invitationId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.invite_registration_activity);

        _affiliationCode = getIntent().getStringExtra(INVITE_AFFILIATION_CODE);
        _invitationId = getIntent().getStringExtra(INVITATION_ID);
        if (null != _affiliationCode)
           AppPreferences.setInviteAffiliationCode(this, _affiliationCode);

        // TODO: need to finish the screen that shows details about user invitation and registration finish
        // temporary move to the next screen
        onFinishRegistration(null);

        ((TextView)findViewById(R.id.invitation_code_text)).setText(String.valueOf(_affiliationCode));
    }

    public void onFinishRegistration(View v) {
        Intent intent = new Intent(InvitationRegistrationActivity.this, JoinActivity.class);
        intent.putExtra(INVITE_AFFILIATION_CODE, _affiliationCode);
        intent.putExtra(INVITATION_ID, _invitationId);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        startActivity(intent);
    }
}
