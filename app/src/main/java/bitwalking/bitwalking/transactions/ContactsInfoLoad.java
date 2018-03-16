package bitwalking.bitwalking.transactions;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.provider.ContactsContract;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

/**
 * Created by Marcus on 11/21/16.
 */

public enum ContactsInfoLoad {
    instance;

    private static final String DISPLAY_NAME_CONSTANT = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ?
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY :
            ContactsContract.Contacts.DISPLAY_NAME;

    Context _currentContext;

    ContactsInfoLoad() {

    }

    public ArrayList<PaymentContactDeviceInfo> getDeviceContactsInfo() {
        if (null == _currentContext)
            return null;

        ArrayList<PaymentContactDeviceInfo> contacts = new ArrayList<>();
        HashSet<String> contactsHash = new HashSet<>();
        ContentResolver cr = _currentContext.getContentResolver();
        String[] PROJECTION = new String[] {
                ContactsContract.Contacts._ID,
                DISPLAY_NAME_CONSTANT,
                ContactsContract.CommonDataKinds.Email.DATA,
                ContactsContract.CommonDataKinds.Email.CONTACT_ID
        };
        String order = "CASE WHEN "
                + DISPLAY_NAME_CONSTANT
                + " NOT LIKE '%@%' THEN 1 ELSE 2 END, "
                + DISPLAY_NAME_CONSTANT
                + ", "
                + ContactsContract.CommonDataKinds.Email.DATA
                + " COLLATE NOCASE";
        String filter = ContactsContract.CommonDataKinds.Email.DATA + " NOT LIKE ''";
        Cursor cur = cr.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, PROJECTION, filter, null, order);

        if (cur.moveToFirst()) {
            do {
                String contactId = cur.getString(0);
                // keep unique only
                if (contactsHash.add(contactId)) {
                    PaymentContactDeviceInfo newContact = new PaymentContactDeviceInfo();
                    newContact.name = cur.getString(1);
                    newContact.email = cur.getString(2);
                    newContact.contactId = cur.getString(3);
                    contacts.add(newContact);
                }
            } while (cur.moveToNext());
        }

        cur.close();

        Collections.sort(contacts);

        return contacts;
    }

    public void getContactDeviceInfo(String email, ContactInfoListener callback) {
        if (null != callback) {
            PaymentContactDeviceInfo contact = getDeviceContactByEmail(email);
            contact.profileImage = getContactDevicePhoto(contact.contactId);
            callback.onDeviceInfo(contact);
        }
    }

    private PaymentContactDeviceInfo getDeviceContactByEmail(String email) {
        if (null == _currentContext)
            return null;

        PaymentContactDeviceInfo contact = null;
        ContentResolver cr = _currentContext.getContentResolver();
        String[] PROJECTION = new String[] {
                ContactsContract.Contacts._ID,
                DISPLAY_NAME_CONSTANT,
                ContactsContract.CommonDataKinds.Email.DATA,
                ContactsContract.CommonDataKinds.Email.CONTACT_ID
        };
        String order = "CASE WHEN "
                + DISPLAY_NAME_CONSTANT
                + " NOT LIKE '%@%' THEN 1 ELSE 2 END, "
                + DISPLAY_NAME_CONSTANT
                + ", "
                + ContactsContract.CommonDataKinds.Email.DATA
                + " COLLATE NOCASE";
        String filter = ContactsContract.CommonDataKinds.Email.DATA + String.format(" LIKE '%s'", email);
        Cursor cur = cr.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, PROJECTION, filter, null, order);
        if (cur.moveToFirst()) {
            contact = new PaymentContactDeviceInfo();
            contact.name = cur.getString(1);
            contact.email = cur.getString(2);
            contact.contactId = cur.getString(3);
        }

        cur.close();
        return contact;
    }

    public void getContactServerInfo(String contactId, final ContactInfoListener callback) {
        // todo: Query server for info

        // fake info
        PaymentContactServerInfo info = new PaymentContactServerInfo();
        info.email = "fake@email.com";
        info.fullName = "Fake Name";
        info.profileImage = null;

        if (null != callback)
            callback.onServerInfo(info);
    }

    public void setContext(Context context) {
        _currentContext = context;
    }

    public Bitmap getContactDevicePhoto(Context context, String contactId) {
        setContext(context);
        return getContactDevicePhoto(contactId);
    }

    public Bitmap getContactDevicePhoto(String contactId) {
        Bitmap photo = null;
        try {
            InputStream inputStream = ContactsContract.Contacts.openContactPhotoInputStream(
                    _currentContext.getContentResolver(),
                    ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI , new Long(contactId).longValue()));
            if (inputStream != null)
                photo= BitmapFactory.decodeStream(inputStream);
        } catch (Exception e) {
        }

        return photo;
    }

    public interface ContactInfoListener {
        void onDeviceInfo(PaymentContactDeviceInfo info);
        void onServerInfo(PaymentContactServerInfo info);
    }
}
