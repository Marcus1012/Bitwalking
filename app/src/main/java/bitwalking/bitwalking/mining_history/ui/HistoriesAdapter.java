package bitwalking.bitwalking.mining_history.ui;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import bitwalking.bitwalking.R;
import bitwalking.bitwalking.mining_history.MiningHistory;
import bitwalking.bitwalking.util.Globals;
import bitwalking.bitwalking.util.Logger;

/**
 * Created by Marcus on 12/25/16.
 */

public class HistoriesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = HistoriesAdapter.class.getSimpleName();

    private ArrayList<MiningHistory> _historiesList;
    ArrayList<String> _separators;
    ArrayList<GenericItem> _items;
    private boolean _isLoading;
    private GenericItem _loadingItem = new GenericItem(LOADING_TYPE, 0);

    private static final int HISTORY_TYPE = 1;
    private static final int MONTH_TEXT_TYPE = 2;
    private static final int LOADING_TYPE = 3;

    private class GenericItem {
        public int type;
        public int index;

        public GenericItem(int type, int index) {
            this.type = type;
            this.index = index;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof GenericItem))
                return false;

            GenericItem other = (GenericItem)o;

            return other.type == type && other.index == index;
        }
    }

    public class HistoryViewHolder extends RecyclerView.ViewHolder {
        public TextView transactiontype; //transaction_title
        public TextView amount; //transaction_amount

        public HistoryViewHolder(View view) {
            super(view);
            this.transactiontype = (TextView) view.findViewById(R.id.transactiontype);
            this.amount = (TextView) view.findViewById(R.id.transaction_amount);
        }
    }

    public class SeparatorViewHolder extends RecyclerView.ViewHolder {
        public TextView title;

        public SeparatorViewHolder(View view) {
            super(view);
            this.title = (TextView)view.findViewById(R.id.separator_text);;
        }
    }

    public class LoadingViewHolder extends RecyclerView.ViewHolder {
        public ProgressBar progress;

        public LoadingViewHolder(View view) {
            super(view);
            this.progress = (ProgressBar)view.findViewById(R.id.mining_loading_progress);;
        }
    }

    public HistoriesAdapter(List<MiningHistory> historiesList) {
        appendHistory(historiesList);
    }

    public void appendHistory(List<MiningHistory> histories) {
        if (null == histories) {
            _historiesList = new ArrayList<>();
            generateItemsArray(_historiesList);
        } else {
            if (null == _historiesList) {
                _historiesList = new ArrayList<>(histories);
            } else {
                _historiesList.addAll(histories);
            }

            _historiesList = MiningHistory.removeDuplicates(_historiesList);

            Collections.reverse(_historiesList);
            generateItemsArray(_historiesList);
        }
    }



    private void generateItemsArray(ArrayList<MiningHistory> histories) {
        _items = new ArrayList<>();
        _separators = new ArrayList<>();

        if (null == histories || histories.size() == 0)
            return;

        Date currDate = null;
        String currMonth = "", prevMonth = "";

        // First mining creation time
        try {
            currDate = histories.get(0).getDate();
            prevMonth = android.text.format.DateFormat.format("dd MMMM yyyy", currDate).toString();
            _items.add(new GenericItem(HISTORY_TYPE, 0));
        } catch (Exception e) {}

        // Add rest of transactions
        for (int i = 1; i < histories.size(); ++i ) {
            try {
                currDate = histories.get(i).getDate();
                currMonth = android.text.format.DateFormat.format("dd MMMM yyyy", currDate).toString();

                if (!currMonth.contentEquals(prevMonth)) {
                    // Add month separator
                    prevMonth = currMonth;
                    _items.add(new GenericItem(MONTH_TEXT_TYPE, _separators.size()));
                    _separators.add(currMonth);
                }

                // Add transaction
                _items.add(new GenericItem(HISTORY_TYPE, i));
            } catch (Exception e) {
                Logger.instance().Log(Logger.ERROR, TAG, "failed to add generic item");
            }
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == HISTORY_TYPE) {
            View normalView = LayoutInflater.from(parent.getContext()).inflate(R.layout.mining_info_item, null);
            return new HistoryViewHolder(normalView);
        } else if (viewType == MONTH_TEXT_TYPE) {
            View headerRow = LayoutInflater.from(parent.getContext()).inflate(R.layout.transaction_month_separator, null);
            return new SeparatorViewHolder(headerRow);
        }
        else if (viewType == LOADING_TYPE) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.mining_loading_item, null);
            return new LoadingViewHolder(view);
        }

        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        final int itemType = getItemViewType(position);

        if (itemType == HISTORY_TYPE) {
            HistoryViewHolder historyViewHolder = (HistoryViewHolder)holder;
            MiningHistory history = _historiesList.get(_items.get(position).index);
            // Amount
            BigDecimal zero = new BigDecimal("0");
            String amountText;
            int amountColor;
            if (0 == history.getMining().compareTo(zero)) { // 0
                amountText = "0.00";
                amountColor = 0xFF000000;
            }
            else if (0 < history.getMining().compareTo(zero)) { // Positive
                amountText = "+ " + Globals.bigDecimalToNiceString(history.getMining());
                amountColor = 0xFF4434D5;
            }
            else { // Negative
                amountText = "- " + Globals.bigDecimalToNiceString(history.getMining().negate());
                amountColor = 0xFFD53434;
            }

            historyViewHolder.amount.setText(amountText + " W$");
            historyViewHolder.amount.setTextColor(amountColor);

            // transaction type
            historyViewHolder.transactiontype.setText("Daily Mining");
        }
        else if (itemType == MONTH_TEXT_TYPE) {
            ((SeparatorViewHolder)holder).title.setText(" " + _separators.get(_items.get(position).index).toLowerCase());
        }
        else if (itemType == LOADING_TYPE) {
            ((LoadingViewHolder)holder).progress.setIndeterminate(true);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return _items.get(position).type;
    }

    @Override
    public int getItemCount() {
        return _items.size();
    }

    public void setIsLoading(boolean isLoading) {
        if (isLoading == _isLoading)
            return;

        _isLoading = isLoading;

        if (_isLoading) {
            _items.add(_loadingItem);
            notifyItemInserted(_items.size() - 1);
        }
        else {
            _items.remove(_loadingItem);
            notifyItemRemoved(_items.size() - 1);
        }
    }

    public boolean isLoading() { return _isLoading; }

    public MiningHistory getMiningHistory(int position) {
        MiningHistory miningHistory = null;

        if (position < _items.size()) {
            if (_items.get(position).type == HISTORY_TYPE) {
                miningHistory = _historiesList.get(_items.get(position).index);
            }
        }

        return miningHistory;
    }
}