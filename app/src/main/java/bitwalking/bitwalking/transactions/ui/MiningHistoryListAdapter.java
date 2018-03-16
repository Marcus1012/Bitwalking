package bitwalking.bitwalking.transactions.ui;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import bitwalking.bitwalking.R;
import bitwalking.bitwalking.mining_history.MiningHistory;
import bitwalking.bitwalking.util.Globals;
import bitwalking.bitwalking.util.Logger;

/**
 * Created by Marcus on 12/22/16.
 */

public class MiningHistoryListAdapter extends BaseAdapter {
    private static final String TAG = MiningHistoryListAdapter.class.getSimpleName();

    Context _context;
    ArrayList<MiningHistory> _histories;
    ArrayList<String> _separators;
    ArrayList<GenericItem> _items;
    MiningClickListener _listener;

    public MiningHistoryListAdapter(Context context, ArrayList<MiningHistory> histories, MiningClickListener listener) {
        _context = context;
        _listener = listener;
        appendHistory(histories);
    }

    public void appendHistory(List<MiningHistory> histories) {
        if (null == histories) {
            _histories = new ArrayList<>();
            generateItemsArray(_histories);
        } else {
            if (null == _histories) {
                _histories = new ArrayList<>(histories);
            } else {
                _histories.addAll(histories);
            }

            Collections.sort(_histories);
            Collections.reverse(_histories);
            generateItemsArray(_histories);
        }
    }

    private void generateItemsArray(ArrayList<MiningHistory> histories) {
        _items = new ArrayList<>();

        if (null == histories || histories.size() == 0)
            return;

        Date currDate = null;
        String currMonth = "", prevMonth = "";

        // First mining creation time
        try {
            currDate = _histories.get(0).getDate();
            prevMonth = android.text.format.DateFormat.format("MMMM yyyy", currDate).toString();
            _items.add(new GenericItem(ItemType.mining, 0));
        } catch (Exception e) {}

        // Add rest of transactions
        for (int i = 1; i < _histories.size(); ++i ) {
            try {
                currDate = _histories.get(i).getDate();
                currMonth = android.text.format.DateFormat.format("MMMM yyyy", currDate).toString();

                if (!currMonth.contentEquals(prevMonth)) {
                    // Add month separator
                    prevMonth = currMonth;
                    _items.add(new GenericItem(ItemType.separator, _separators.size()));
                    _separators.add(currMonth);
                }

                // Add transaction
                _items.add(new GenericItem(ItemType.mining, i));
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
            if (v.getTag() instanceof MiningViewHolder && null != _listener) {
                _listener.onMiningClick(((MiningViewHolder)v.getTag()).mining);
            }
        }
    };

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = null;
        GenericItem item = _items.get(position);
        switch (item.type) {
            case mining: {
                view = getMiningView(item.index, convertView, parent);
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

    public class MiningViewHolder {
        public TextView day; //transaction_title
        public TextView amount; //transaction_amount
        MiningHistory mining;
    }

    private View getMiningView(int position, View convertView, ViewGroup parent) {
        // Get current menu item
        MiningHistory mining = _histories.get(position);
        MiningViewHolder holder;

        if (convertView == null) {
            LayoutInflater mInflater = (LayoutInflater)
                    _context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

            convertView = mInflater.inflate(R.layout.mining_info_item, null);

            holder = new MiningViewHolder();
            holder.day = (TextView) convertView.findViewById(R.id.transactiontype);
            holder.amount = (TextView) convertView.findViewById(R.id.transaction_amount);
            holder.mining = mining;
            convertView.setTag(holder);
        }
        else {
            holder = (MiningViewHolder)convertView.getTag();
        }

        loadFillPaymentHolder(holder, mining);

        return convertView;
    }

    private void loadFillPaymentHolder(MiningViewHolder holder, MiningHistory mining) {
        // Amount
        holder.amount.setText("+ " + Globals.bigDecimalToNiceString(mining.getMining()) + " W$");

        // Day
        holder.day.setText(String.valueOf(mining.getDay()));
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
        mining,
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

    public interface MiningClickListener {
        void onMiningClick(MiningHistory payment);
    }
}