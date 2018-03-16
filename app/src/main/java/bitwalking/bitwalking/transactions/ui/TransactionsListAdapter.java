package bitwalking.bitwalking.transactions.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import bitwalking.bitwalking.R;
import bitwalking.bitwalking.transactions.PaymentRequest;
import bitwalking.bitwalking.util.Globals;
import bitwalking.bitwalking.util.Logger;

/**
 * Created by Marcus on 10/22/16.
 */

public class TransactionsListAdapter extends BaseAdapter {
    private static final String TAG = TransactionsListAdapter.class.getSimpleName();

    Context _context;
    ArrayList<PaymentRequest> _payments;
    ArrayList<String> _separators;
    ArrayList<GenericItem> _items;
    OnPaymentClickListener _listener;

    public TransactionsListAdapter(Context context, ArrayList<PaymentRequest> payments, OnPaymentClickListener listener) {
        _context = context;
        _listener = listener;
        generateItemsArray(payments);
    }

    private void generateItemsArray(ArrayList<PaymentRequest> payments) {
        _items = new ArrayList<>();

        if (null == payments || payments.size() == 0)
            return;

        _payments = new ArrayList<>(payments);
        Collections.sort(_payments);
        Collections.reverse(_payments);

        DateFormat df = Globals.getUTCDateFormat();
        Date currDate = null;
        String currMonth = "", prevMonth = "";

        // First transaction creation time
        try {
            currDate = df.parse(_payments.get(0).creationTimestamp);
            prevMonth = android.text.format.DateFormat.format("MMMM yyyy", currDate).toString();
            _items.add(new GenericItem(ItemType.transaction, 0));
        } catch (Exception e) {}

        // Add rest of transactions
        for (int i = 1; i < _payments.size(); ++i ) {
            try {
                currDate = df.parse(_payments.get(i).creationTimestamp);
                currMonth = android.text.format.DateFormat.format("MMMM yyyy", currDate).toString();

                if (!currMonth.contentEquals(prevMonth)) {
                    // Add month separator
                    prevMonth = currMonth;
                    _items.add(new GenericItem(ItemType.separator, _separators.size()));
                    _separators.add(currMonth);
                }

                // Add transaction
                _items.add(new GenericItem(ItemType.transaction, i));
            } catch (Exception e) {
                Logger.instance().Log(Logger.ERROR, TAG, "failed to add generic item");
            }
        }
    }

    @Override
    public int getCount() {
        return _items.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    View.OnClickListener _onPaymentItemClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v.getTag() instanceof TransactionViewHolder && null != _listener) {
                _listener.onPaymentClick(((TransactionViewHolder)v.getTag()).payment);
            }
        }
    };

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = null;
        GenericItem item = _items.get(position);
        switch (item.type) {
            case transaction: {
                view = getTransactionView(item.index, convertView, parent);
                view.setOnClickListener(_onPaymentItemClickListener);
                break;
            }
            case separator: {
                view = getSeparatorView(item.index, convertView, parent);
                break;
            }
            default: break;
        }

        return view;
    }

    public class TransactionViewHolder {
        public ImageView profileImage; //transaction_profile_image
        public View defaultProfileImageLayout; //transaction_default_profile_image
        public TextView defaultProfileImageInitials; //transaction_default_profile_initials
        public TextView title; //transaction_title
        public TextView toFrom; //transaction_to_from_text
        public TextView amount; //transaction_amount
        public TextView status; //transaction_status
        PaymentRequest payment;
    }

    private View getTransactionView(int position, View convertView, ViewGroup parent) {
        // Get current menu item
        PaymentRequest payment = _payments.get(position);
        TransactionViewHolder holder;

        if (convertView == null) {
            LayoutInflater mInflater = (LayoutInflater)
                    _context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

            convertView = mInflater.inflate(R.layout.transaction_info_item, null);

            holder = new TransactionViewHolder();
            holder.profileImage = (ImageView) convertView.findViewById(R.id.transaction_profile_image);
            holder.defaultProfileImageLayout = convertView.findViewById(R.id.transaction_default_profile_image);
            holder.defaultProfileImageInitials = (TextView) convertView.findViewById(R.id.transaction_default_profile_initials);
            holder.title = (TextView) convertView.findViewById(R.id.transaction_title);
            holder.toFrom = (TextView) convertView.findViewById(R.id.transaction_to_from_text);
            holder.amount = (TextView) convertView.findViewById(R.id.transaction_amount);
            holder.status = (TextView) convertView.findViewById(R.id.transaction_status);
            holder.payment = payment;
            convertView.setTag(holder);
        }
        else {
            holder = (TransactionViewHolder)convertView.getTag();
        }

        loadFillPaymentHolder(holder, payment);

        return convertView;
    }

    private void loadFillPaymentHolder(TransactionViewHolder holder, PaymentRequest payment) {
        String userInitials = "";
        Bitmap profileImage = null;
        String title = "Sent";
        String toFrom = payment.recipient;

        // profile image
        if (null != profileImage) {
            holder.profileImage.setImageBitmap(profileImage);
            holder.profileImage.setVisibility(View.VISIBLE);
            holder.defaultProfileImageLayout.setVisibility(View.GONE);
        }
        else {
            holder.defaultProfileImageInitials.setText(userInitials);
            holder.profileImage.setVisibility(View.GONE);
            holder.defaultProfileImageLayout.setVisibility(View.VISIBLE);
        }

        // title
        holder.title.setText(title);

        // to from
        holder.toFrom.setText(toFrom);

        // amount
        holder.amount.setText("");
        for (PaymentRequest.PaymentTransferInfo t : payment.transfer) {
            if (t.currency.equalsIgnoreCase("xwd")) { // Walking $
                holder.amount.setText(" " + Globals.bigDecimalToNiceString(t.amount) + " W$");
                break;
            }
        }

        // status
        holder.status.setText(String.valueOf(payment.status));
    }

    public class SeparatorViewHolder {
        public TextView title;
    }

    private View getSeparatorView(int position, View convertView, ViewGroup parent) {
        // Get current menu item
        String separator = _separators.get(position);
        SeparatorViewHolder holder;

        if (convertView == null) {
            LayoutInflater mInflater = (LayoutInflater)
                    _context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

            convertView = mInflater.inflate(R.layout.transaction_month_separator, null);

            holder = new SeparatorViewHolder();
            holder.title = (TextView)convertView.findViewById(R.id.separator_text);
            convertView.setTag(holder);
        }
        else {
            holder = (SeparatorViewHolder)convertView.getTag();
        }

        // event title
        holder.title.setText(separator);

        return convertView;
    }

    private enum ItemType {
        transaction,
        separator
    }

    private class GenericItem {
        public ItemType type;
        public int index;

        public GenericItem(ItemType type, int index) {
            this.type = type;
            this.index = index;
        }
    }

    public interface OnPaymentClickListener {
        void onPaymentClick(PaymentRequest payment);
    }
}
