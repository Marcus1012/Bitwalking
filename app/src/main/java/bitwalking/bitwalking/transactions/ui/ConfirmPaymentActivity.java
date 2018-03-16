package bitwalking.bitwalking.transactions.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.Gson;

import java.math.BigDecimal;

import bitwalking.bitwalking.AppPreferences;
import bitwalking.bitwalking.BitwalkingApp;
import bitwalking.bitwalking.R;
import bitwalking.bitwalking.server.api.ServerApi;
import bitwalking.bitwalking.server.requests.PaymentSendRequest;
import bitwalking.bitwalking.transactions.ContactsInfoLoad;
import bitwalking.bitwalking.transactions.PaymentContactDeviceInfo;
import bitwalking.bitwalking.util.Globals;
import bitwalking.bitwalking.util.Logger;
import bitwalking.bitwalking.util.RoundImageView;

/**
 * Created by Marcus on 11/15/16.
 */

public class ConfirmPaymentActivity extends Activity {
    private static final String TAG = ConfirmPaymentActivity.class.getSimpleName();

    public static final String CONTACT_INFO_JSON_EXTRA = "ConfirmPayment.Extra.ContactInfo";
    public static final String PAYMENT_AMOUNT_EXTRA = "ConfirmPayment.Extra.Amount";
    public static final String PAYMENT_TYPE_EXTRA = "ConfirmPayment.Extra.Type";
    public static final String PAYMENT_NOTE_EXTRA = "ConfirmPayment.Extra.Note";

    // Payment info
    private SendRequestActivity.PaymentType _type = SendRequestActivity.PaymentType.send;
    private PaymentContactDeviceInfo _contact;
    private Bitmap _contactImage;
    private BigDecimal _amount;
    private String _note;

    TextView _titleText;
    TextView _buttonText;
    TextView _amountText;
    RoundImageView _profileImage;
    TextView _initialsText;
    TextView _nameText;
    TextView _emailText;
    EditText _noteEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.send_confirm_layout);

        handleIntent(getIntent());

        _titleText = (TextView) findViewById(R.id.payment_review_title);
        _buttonText = (TextView) findViewById(R.id.payment_review_button_text);
        _amountText = (TextView) findViewById(R.id.payment_review_amount);
        _profileImage = (RoundImageView) findViewById(R.id.payment_review_image);
        _initialsText = (TextView) findViewById(R.id.payment_review_initials);
        _nameText = (TextView) findViewById(R.id.payment_review_name);
        _emailText = (TextView) findViewById(R.id.payment_review_email);
        _noteEdit = (EditText) findViewById(R.id.payment_review_note);

        refreshUI();
        Globals.hideSoftKeyboard(this);

        overridePendingTransition(R.anim.enter_from_right, R.anim.hold);
    }

    private boolean handleIntent(Intent intent) {
        boolean handled = false;

        // Contact id and info
        if (intent.hasExtra(CONTACT_INFO_JSON_EXTRA)) {
            try {
                String contactInfoJson = intent.getStringExtra(CONTACT_INFO_JSON_EXTRA);
                _contact = new Gson().fromJson(contactInfoJson, PaymentContactDeviceInfo.class);
                _contactImage = ContactsInfoLoad.instance.getContactDevicePhoto(ConfirmPaymentActivity.this, _contact.contactId);
                handled = true;
            } catch (Exception e) {
                Logger.instance().Log(Logger.ERROR, TAG, "failed to init contact info " + e.getMessage());
            }

            if (null == _contact)
                Globals.backWithMsg(ConfirmPaymentActivity.this, "Error loading contact");
        }

        // Type
        if (intent.hasExtra(PAYMENT_TYPE_EXTRA)) {
            _type = SendRequestActivity.PaymentType.valueOf(intent.getStringExtra(PAYMENT_TYPE_EXTRA));
            handled = true;
        }

        // Amount
        if (intent.hasExtra(PAYMENT_AMOUNT_EXTRA)) {
            _amount = new BigDecimal(intent.getStringExtra(PAYMENT_AMOUNT_EXTRA));
            handled = true;
        }

        // Note
        if (intent.hasExtra(PAYMENT_NOTE_EXTRA)) {
            _note = intent.getStringExtra(PAYMENT_NOTE_EXTRA);
            handled = true;
        }

        return handled;
    }

    private void refreshUI() {
        try {
            switch (_type) {
                case send:
                    _titleText.setText("review and send");
                    _buttonText.setText(" send ");
                    break;
                case request:
                    _titleText.setText("review and request");
                    _buttonText.setText(" request ");
                    break;
                default:
            }

            _amountText.setText(" " + Globals.bigDecimalToNiceString(_amount));
            _nameText.setText(_contact.name);
            _emailText.setText(_contact.email);

            if (_contactImage != null) {
                _profileImage.setImageBitmap(_contactImage);
                _initialsText.setVisibility(View.GONE);
            } else {
                // Show default profile image with initials
                _profileImage.setImageResource(R.drawable.transaction_default_profile_image);
                _initialsText.setText(Globals.getNameInitials(_contact.name));
                _initialsText.setVisibility(View.VISIBLE);
            }

            _noteEdit.setEnabled(true);
            if (null != _note && !_note.isEmpty()) {
                _noteEdit.setText(_note);
            }

            findViewById(R.id.payment_info_status_layout).setVisibility(View.GONE);
            findViewById(R.id.payment_info_layout_background).setBackgroundColor(Color.TRANSPARENT);
        } catch (Exception e) {
            Logger.instance().Log(Logger.ERROR, TAG, "failed to load payment info");
            Globals.backWithMsg(ConfirmPaymentActivity.this, "Error loading payment info");
        }
    }

    public void onBackClick(View v) {
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        ((BitwalkingApp)getApplication()).trackScreenView("confirm.payment");
        overridePendingTransition(R.anim.hold, R.anim.exit_to_right);
    }

    public void onSendPayment(View v) {
        Log.d(TAG,"Send payment");
        switch (_type){
            case request:requestPayment();break;
            case send:sendPayment();break;
        }
    }

    private void sendPayment(){
        PaymentSendRequest body = new PaymentSendRequest();
        body.note = _noteEdit.getText().toString();

        PaymentSendRequest.Payee payee = new PaymentSendRequest.Payee();
        payee.identity = _emailText.getText().toString();
        body.payee = payee;

        PaymentSendRequest.PaymentTransferInfo transferInfo = new PaymentSendRequest.PaymentTransferInfo();
        transferInfo.amount = Double.valueOf(_amountText.getText().toString()) ;
        body.transfer.add(transferInfo);

        PaymentSendRequest.TransactionInformation transactionInformation = new PaymentSendRequest.TransactionInformation();
        transactionInformation.sum = Double.valueOf(_amountText.getText().toString()) ;
        body.transactionInformation = transactionInformation;


        ServerApi.sendPayment(AppPreferences.getUserId(ConfirmPaymentActivity.this),
                AppPreferences.getUserSecret(ConfirmPaymentActivity.this), body, new ServerApi.PaymentActionListener() {
                    @Override
                    public void onOk() {
                        Log.d(TAG,"OK");
                    }

                    @Override
                    public void onNotFound() {
                        Log.d(TAG,"NotFound");
                    }

                    @Override
                    public void onConflict() {
                        Log.d(TAG,"Conflict");
                    }
                });
    }
    private void requestPayment(){
      /*  ServerApi.getNotifications(AppPreferences.getUserId(ConfirmPaymentActivity.this),
                AppPreferences.getUserSecret(ConfirmPaymentActivity.this), new ServerApi.NotificationsListener() {
                    @Override
                    public void onError() {
                        Log.d(TAG,"Error");
                    }

                    @Override
                    public void onNotifications() {
                        Log.d(TAG,"Notifications");
                    }
                });*/
    }
}
