package bitwalking.bitwalking.transactions.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Toast;

import com.google.gson.Gson;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.single.BasePermissionListener;
import com.karumi.dexter.listener.single.PermissionListener;

import java.math.BigDecimal;

import bitwalking.bitwalking.BitwalkingApp;
import bitwalking.bitwalking.R;
import bitwalking.bitwalking.transactions.PaymentContactDeviceInfo;
import bitwalking.bitwalking.util.Globals;
import bitwalking.bitwalking.util.Logger;

/**
 * Created by Marcus on 7/13/16.
 */
public class SendRequestActivity extends FragmentActivity
        implements ContactPickerFragment.OnContactPickListener, PaymentAmountFragment.OnSendRequestAmountListener {
    private static final int READ_CONTACTS_PERMISSIONS_REQUEST_ID = 123;
    private static final String TAG = SendRequestActivity.class.getSimpleName();

    public enum PaymentType {
        request,
        send
    };

    private BigDecimal _currentAmount = null;
    private PaymentType _currentPaymentType;
    private PaymentContactDeviceInfo _currentContact;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.send_request_activity);

        getSupportFragmentManager().
                beginTransaction().
                add(R.id.send_request_fragment, new PaymentAmountFragment(), "amount").
                commit();

        overridePendingTransition(R.anim.enter_from_right, R.anim.hold);
    }

    public void onBackClick(View v) {
        onBackPressed();
    }

    //region Contacts

    private static final int CONTACT_PICK_REQ_ID = 1;

    @Override
    public void onContactPick(PaymentContactDeviceInfo contact) {
//        Intent intent = new Intent(Intent.ACTION_VIEW);
//        Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, String.valueOf(contact.contactId));
//        intent.setData(uri);
//        SendRequestActivity.this.startActivity(intent);

        _currentContact = contact;
        confirmPayment();
    }

    @Override
    public void onSend(BigDecimal amount) {
        _currentAmount = amount;
        if (_currentAmount.doubleValue()>0) {
            _currentPaymentType = PaymentType.send;
            showContactPicker();
        }
    }

    @Override
    public void onRequest(BigDecimal amount) {
        _currentAmount = amount;
        if (_currentAmount.doubleValue()>0) {
            _currentPaymentType = PaymentType.request;
            showContactPicker();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case CONTACT_PICK_REQ_ID: {
                break;
            }
            default:
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case READ_CONTACTS_PERMISSIONS_REQUEST_ID:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showContactPicker();
                }
                else {
                    // ignore
                    Toast.makeText(this, "No permission to access Contacts", Toast.LENGTH_SHORT).show();
                }

                break;
            default: break;
        }
    }


    private void showContactPicker() {

        Dexter.withActivity(this)
                .withPermission(Manifest.permission.READ_CONTACTS)
                .withListener(new BasePermissionListener(){
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        super.onPermissionGranted(response);

                        ContactPickerFragment contactsFragment = new ContactPickerFragment();
                        Bundle bundle = new Bundle();
                        bundle.putString("Purpose", String.valueOf(_currentPaymentType));
                        contactsFragment.setArguments(bundle);
                        addFragmentToStack(contactsFragment, "contacts");
                    }
                })
                .check();

      /*  if (Globals.havePermission(this, android.Manifest.permission.READ_CONTACTS, READ_CONTACTS_PERMISSIONS_REQUEST_ID)) {

        }*/
    }

    void addFragmentToStack(Fragment fragment, String tag) {
        getSupportFragmentManager().
                beginTransaction().
                setCustomAnimations(R.anim.enter_from_right, R.anim.hold, R.anim.hold, R.anim.exit_to_right).
                add(R.id.send_request_fragment, fragment, tag).
                addToBackStack(tag).
                commitAllowingStateLoss();
    }

    //endregion

    @Override
    public void onBackPressed() {
        Logger.instance().Log(Logger.DEBUG, TAG, "stack count = " + getFragmentManager().getBackStackEntryCount());
        if (getFragmentManager().getBackStackEntryCount() == 0) {
            super.onBackPressed();

            ((BitwalkingApp)getApplication()).trackScreenView("send/request");
            overridePendingTransition(R.anim.hold, R.anim.exit_to_right);
        } else {
            getFragmentManager().popBackStack();
        }
    }

    private void confirmPayment() {
        Intent confirmIntent = new Intent(SendRequestActivity.this, ConfirmPaymentActivity.class);
        confirmIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        confirmIntent.putExtra(ConfirmPaymentActivity.CONTACT_INFO_JSON_EXTRA, new Gson().toJson(_currentContact));
        confirmIntent.putExtra(ConfirmPaymentActivity.PAYMENT_AMOUNT_EXTRA, String.valueOf(_currentAmount));
        confirmIntent.putExtra(ConfirmPaymentActivity.PAYMENT_TYPE_EXTRA, String.valueOf(_currentPaymentType));
        startActivity(confirmIntent);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

    }
}