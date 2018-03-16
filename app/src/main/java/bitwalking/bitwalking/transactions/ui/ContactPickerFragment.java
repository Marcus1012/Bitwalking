package bitwalking.bitwalking.transactions.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import bitwalking.bitwalking.R;
import bitwalking.bitwalking.transactions.ContactsInfoLoad;
import bitwalking.bitwalking.transactions.PaymentContactDeviceInfo;
import bitwalking.bitwalking.util.Globals;
import bitwalking.bitwalking.util.RoundImageView;

/**
 * Created by Marcus on 11/8/16.
 */

public class ContactPickerFragment extends Fragment implements AdapterView.OnItemClickListener {


    private static final String TAG = ContactPickerFragment.class.getSimpleName();

    // Defines a variable for the search string
    private String _searchString = "";

    private OnContactPickListener _callback;

    private EditText _searchText;
    private TextView _titleText;
    private View _allMyContactsText;
    private ListView _contactsList;
    private MyContactsListAdapter _contactsAdapter;
    private ArrayList<PaymentContactDeviceInfo> _contacts;
    private ArrayList<PaymentContactDeviceInfo> _filteredContacts;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.contact_picker_layout, container, false);

        _searchText = (EditText) v.findViewById(R.id.contact_search_text);
        _titleText = (TextView) v.findViewById(R.id.contacts_title);
        _allMyContactsText = v.findViewById(R.id.all_my_contacts_text_view);
        _contactsList = (ListView) v.findViewById(R.id.contacts_list);

        handleArgs(getArguments());
        initSearchText();

        Globals.hideSoftKeyboard(getActivity());

        return v;
    }

    private void handleArgs(Bundle args) {
        String purposeString = args.getString("Purpose", null);
        if (null != purposeString) {
            String title = "contacts";
            SendRequestActivity.PaymentType purpose = SendRequestActivity.PaymentType.valueOf(purposeString);

            switch (purpose) {
                case send: title = "send to"; break;
                case request: title = "request from"; break;
                default:
            }

            _titleText.setText(title);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            _callback = (OnContactPickListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnContactPickListener");
        }
    }

    private void initContactsAdapter() {
        _contacts = ContactsInfoLoad.instance.getDeviceContactsInfo();
        _filteredContacts = new ArrayList<>(_contacts);
        _contactsAdapter = new MyContactsListAdapter(getActivity());
        _contactsList.setAdapter(_contactsAdapter);
        _contactsList.setOnItemClickListener(this);
    }

    private void initSearchText() {
        _searchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                _searchString = s.toString().toLowerCase();
                filterContacts();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    void filterContacts() {
        _filteredContacts.clear();
        for (PaymentContactDeviceInfo c : _contacts) {
            if ((null != c.name && c.name.toLowerCase().contains(_searchString)) ||
                (null != c.email && c.email.toLowerCase().contains(_searchString))) {
                _filteredContacts.add(c);
            }
        }

        _contactsAdapter.notifyDataSetChanged();
        _allMyContactsText.setVisibility( (_searchString.isEmpty()) ? View.VISIBLE : View.GONE );
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_CONTACTS)) {
            getActivity().onBackPressed();
        }
        else {
            ContactsInfoLoad.instance.setContext(getActivity());
            initContactsAdapter();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (null != _filteredContacts && position < _filteredContacts.size()) {
            PaymentContactDeviceInfo contact = _filteredContacts.get(position);
            if (null != _callback)
                _callback.onContactPick(contact);
        }
    }

    class MyContactsListAdapter extends BaseAdapter {
        Context _context;

        public MyContactsListAdapter(Context context) {
            _context = context;
        }

        @Override
        public int getCount() {
            return _filteredContacts.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        public class ContactViewHolder {
            public RoundImageView photo;
            public TextView name;
            public TextView email;
            public TextView initials;
            public PaymentContactDeviceInfo info;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Get current menu item
            PaymentContactDeviceInfo contact = _filteredContacts.get(position);
            ContactViewHolder holder;

            if (convertView == null) {
                LayoutInflater mInflater = (LayoutInflater)
                        _context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

                convertView = mInflater.inflate(R.layout.contact_item_view, null);

                holder = new ContactViewHolder();
                holder.photo = (RoundImageView) convertView.findViewById(R.id.contact_profile_image);
                holder.name = (TextView) convertView.findViewById(R.id.contact_name);
                holder.email = (TextView) convertView.findViewById(R.id.contact_email);
                holder.initials = (TextView) convertView.findViewById(R.id.profile_initials);
                holder.info = contact;
                convertView.setTag(holder);
            }
            else {
                holder = (ContactViewHolder)convertView.getTag();
            }

            // Name
            holder.name.setText(contact.name);
            // Email
            holder.email.setText(contact.email);
            // Photo
            Bitmap profileImage = ContactsInfoLoad.instance.getContactDevicePhoto(getActivity(), contact.contactId);

            if (profileImage != null) {
                holder.photo.setImageBitmap(profileImage);
                holder.initials.setVisibility(View.GONE);
            }
            else {
                // Show default profile image with initials
                holder.photo.setImageResource(R.drawable.transaction_default_profile_image);
                holder.initials.setText(Globals.getNameInitials(contact.name));
                holder.initials.setVisibility(View.VISIBLE);
            }

            return convertView;
        }
    }

    public interface OnContactPickListener {
        void onContactPick(PaymentContactDeviceInfo contact);
    }
}
