package bitwalking.bitwalking.activityes;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import bitwalking.bitwalking.R;
import bitwalking.bitwalking.mining_history.GetMiningHistoryCached;
import bitwalking.bitwalking.mining_history.GetMiningHistoryServer;
import bitwalking.bitwalking.mining_history.HistoryLoadedListener;
import bitwalking.bitwalking.mining_history.MiningHistory;
import bitwalking.bitwalking.mining_history.MiningHistoryCache;
import bitwalking.bitwalking.mining_history.ui.HistoriesAdapter;
import bitwalking.bitwalking.mining_history.ui.SimpleItemDivider;
import bitwalking.bitwalking.transactions.ContactsInfoLoad;
import bitwalking.bitwalking.transactions.PaymentContactDeviceInfo;
import bitwalking.bitwalking.transactions.PaymentContactServerInfo;
import bitwalking.bitwalking.transactions.PaymentRequest;
import bitwalking.bitwalking.transactions.ui.SendRequestActivity;
import bitwalking.bitwalking.transactions.ui.TransactionsListAdapter;
import bitwalking.bitwalking.util.EndlessScrollListener;
import bitwalking.bitwalking.util.Globals;
import bitwalking.bitwalking.util.Logger;
import bitwalking.bitwalking.util.RoundImageView;

/**
 * Created by Marcus on 9/21/16.
 */
public class WalletActivity extends BwActivity {
    private static final String TAG = WalletActivity.class.getSimpleName();

    private TransactionsListAdapter _transactionsAdapter;
    private HistoriesAdapter _historyAdapter;
    private PopupWindow _popupWindow;

    // Mining History
    private static final int MINING_DAYS_PER_LOAD = 10;
    MiningHistoryCache _cachedHistory;
    ArrayList<MiningHistory> _displayedHistories;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wallet_activity_new);

        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
        bindToBwService();

        ContactsInfoLoad.instance.setContext(this);

        if (Globals.TRANSACTIONS_ON || Globals.MINING_HISTORY_ON) {
            if (Globals.TRANSACTIONS_ON) {
                findViewById(R.id.wallet_send_request_button).setVisibility(View.VISIBLE);
            }

            CollapsingToolbarLayout toolbar = (CollapsingToolbarLayout)findViewById(R.id.wallet_balance_layout);
            AppBarLayout.LayoutParams params =
                    (AppBarLayout.LayoutParams) toolbar.getLayoutParams();
            params.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL | AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED);
        }

        _displayedHistories = new ArrayList<>();
    }

    public void onExitClick(View v) {
        onBackPressed();
    }

    public void onSendRequest(View v) {
        startActivity(new Intent(WalletActivity.this, SendRequestActivity.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAllPayments();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
    }

    @Override
    protected void onBwServiceConnected() {
       /* if (null != _serviceApi && _boundToService) {
            try {
                UserInfo userInfo = AppPreferences.getUserInfo(WalletActivity.this);
                ((TextView)findViewById(R.id.wallet_balance)).setText(" " + Globals.bigDecimalToNiceString(userInfo.getBalanceInfo().getBalance()));
            }
            catch (Exception e) {
                BitwalkingApp.getInstance().trackException(e);
            }
        }*/
    }

    @Override
    protected void onBwServiceDisconnected() {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindBwService();
    }

//    ArrayList<PaymentRequest> _fakePayments;
//    private void buildFakePayments() {
//        _fakePayments = new ArrayList<>();
//        PaymentRequest payment = new PaymentRequest();
//        payment.id = "123";
//        payment.updateTimestamp = Globals.getUTCDateFormat().format(new Date());
//        payment.creationTimestamp = Globals.getUTCDateFormat().format(new Date());
//        payment.type = "payment";
//        payment.sender = "stas@bitwalking.com";
//        payment.recipient = "eyal@bitwalking.com";
//        payment.transactionId = "12";
//        payment.status = PaymentRequest.PaymentStatus.paid;
//        payment.transfer = new ArrayList<>();
//        PaymentRequest.PaymentTransferInfo p = new PaymentRequest.PaymentTransferInfo();
//        p.currency = "xwd";
//        p.amount = new BigDecimal("20.45");
//        payment.transfer.add(p);
//
//        _fakePayments.add(payment);
//        _fakePayments.add(payment);
//        _fakePayments.add(payment);
//        _fakePayments.add(payment);
//        _fakePayments.add(payment);
//        _fakePayments.add(payment);
//        _fakePayments.add(payment);
//        _fakePayments.add(payment);
//        _fakePayments.add(payment);
//        _fakePayments.add(payment);
//        _fakePayments.add(payment);
//
//
//        payment.status = PaymentRequest.PaymentStatus.pending;
//        _fakePayments.add(payment);
//        _fakePayments.add(payment);
//    }

    private void refreshAllPayments() {
        if (Globals.MINING_HISTORY_ON) {
            getHistoryAdapter(); // init

            Date yesterday = Globals.getZeroTimeDate(getOneDayBefore(new Date()));
            LoadHistory(yesterday, MINING_DAYS_PER_LOAD);
        }

        if (Globals.TRANSACTIONS_ON) {
            // TODO: the code below is old and might not work, integrate transactions history with mining history above :)
//            buildFakePayments();
//
//            if (null != _fakePayments && _fakePayments.size() > 0) {
//                _transactionsAdapter = new TransactionsListAdapter(WalletActivity.this, _fakePayments, new TransactionsListAdapter.OnPaymentClickListener() {
//                    @Override
//                    public void onPaymentClick(PaymentRequest payment) {
//                        if (payment.status == PaymentRequest.PaymentStatus.pending) {
//                            promptPaymentRequest(payment);
//                        }
//                        else {
//                            promptPaymentInfo(payment);
//                        }
//                    }
//                });
//
//                LinearLayout listViewReplacement = (LinearLayout) findViewById(R.id.wallet_transactions_list);
//                for (int i = 0; i < _transactionsAdapter.getCount(); i++) {
//                    View view = _transactionsAdapter.getView(i, null, listViewReplacement);
//                    listViewReplacement.addView(view);
//                }
//
//                findViewById(R.id.wallet_text).setVisibility(View.GONE);
//            } else {
//                findViewById(R.id.wallet_text).setVisibility(View.VISIBLE);
//            }
        }
    }

    //region Mining History

    private MiningHistoryCache getCachedHistory() {
        if (null == _cachedHistory)
            _cachedHistory = new MiningHistoryCache(getApplicationContext());

        return _cachedHistory;
    }

    private void LoadHistory(Date day, int count) {
        ArrayList<Date> days = getDays(day, count);

        if (0 == _displayedHistories.size()) { // Empty
            getHistoryAdapter().setIsLoading(true);
            loadMoreMiningHistory(days);
        }
        else {
            // first load what we got
            refreshMiningHistory();

            for (MiningHistory mining : _displayedHistories)
                days.remove(mining.getDate());

            if (days.size() > 0)
                loadMoreMiningHistory(days);
        }
    }

    private Date getOneDayBefore(Date day) {
        return new Date(day.getTime() - TimeUnit.HOURS.toMillis(24));
    }

    private void loadMoreMiningHistory(final ArrayList<Date> days) {
        // Get history
        new GetMiningHistoryCached(getApplicationContext(), new HistoryLoadedListener() {
            @Override
            public void onHistory(ArrayList<MiningHistory> histories) {
                ArrayList<Date> daysFromServer = new ArrayList<>(days);

                if (null != histories && histories.size() > 0) {
                    appendMiningHistory(histories);
                    for (MiningHistory mining : histories) {
                        daysFromServer.remove(mining.getDate());
                    }
                }

                if (daysFromServer.size() > 0) {
                    loadFromServer(daysFromServer);
                }
                else {
                    onDoneLoadingHistory();
                }
            }
        }).execute(days);
    }

    private ArrayList<Date> getDays(Date startDay, int count) {
        // Create days array
        Date currDay = Globals.getZeroTimeDate(startDay);
        ArrayList<Date> days = new ArrayList<>();
        for (int i = 0; i < count; ++i) {
            days.add(currDay);
            currDay = getOneDayBefore(currDay);
        }

        return days;
    }

    private void onDoneLoadingHistory() {
        WalletActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _historyAdapter.setIsLoading(false);
            }
        });
    }

    private void loadFromServer(ArrayList<Date> days) {
        // Get history
        new GetMiningHistoryServer(getApplicationContext(), new HistoryLoadedListener() {
            @Override
            public void onHistory(ArrayList<MiningHistory> histories) {
                if (null != histories && histories.size() > 0) {
                    getCachedHistory().append(histories);

                    if (histories.size() > MINING_DAYS_PER_LOAD) {
                        appendMiningHistory(histories.subList(0, histories.size() - MINING_DAYS_PER_LOAD));
                    }
                    else {
                        appendMiningHistory(histories);
                    }

                    onDoneLoadingHistory();
                }
                else {
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {}
                    onDoneLoadingHistory();
                }
            }
        }).execute(days);
    }

    private void initHistoryAdapter(ArrayList<MiningHistory> initHistory) {
        _historyAdapter = new HistoriesAdapter(initHistory);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.wallet_history_list);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.addItemDecoration(new SimpleItemDivider(this));
        recyclerView.setAdapter(_historyAdapter);
        recyclerView.addOnScrollListener(new EndlessScrollListener(linearLayoutManager) {
            @Override
            public boolean onLoadMore(int lastVisibleItem) {
                if (_historyAdapter.isLoading())
                    return true;

                Logger.instance().Log(Logger.DEBUG, TAG, "onLoadMore: lastVisibleItem = " + lastVisibleItem);
                MiningHistory miningHistory = _historyAdapter.getMiningHistory(lastVisibleItem);
                _historyAdapter.setIsLoading(true);

                try {
                    Date startDay = getOneDayBefore(miningHistory.getDate());
                    ArrayList<Date> days = getDays(startDay, MINING_DAYS_PER_LOAD);

                    Logger.instance().Log(Logger.DEBUG, TAG,
                            String.format("onLoadMore: load from %s", Globals.getDateOfBirthDisplayFormat().format(startDay)));
                    loadMoreMiningHistory(days);
                }
                catch (Exception e) {

                }

                return true;
            }
        });
    }

    private void refreshMiningHistory() {
        if (null != _displayedHistories && _displayedHistories.size() > 0) {
           // removeZeroMining(_displayedHistories);
            initHistoryAdapter(_displayedHistories);
        }
    }


    private HistoriesAdapter getHistoryAdapter() {
        if (null == _historyAdapter) {
            initHistoryAdapter(null);
        }

        return _historyAdapter;
    }

    private void appendMiningHistory(final List<MiningHistory> histories) {
        if (null != histories && histories.size() > 0) {
           // removeZeroMining(histories);
            _displayedHistories.addAll(histories);
            getHistoryAdapter().appendHistory(histories);

            WalletActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    getHistoryAdapter().notifyDataSetChanged();
                    findViewById(R.id.wallet_text).setVisibility(View.GONE);
                }
            });
        }
    }

    //endregion

    private void fillPaymentInfoLayout(View v, PaymentRequest payment) {
        // Amount
        for (PaymentRequest.PaymentTransferInfo t : payment.transfer) {
            if (t.currency.equalsIgnoreCase("xwd")) { // Walking $
                ((TextView) v.findViewById(R.id.payment_review_amount)).setText(" " + Globals.bigDecimalToNiceString(t.amount));
                break;
            }
        }

        // Profile
        final RoundImageView profileImage = (RoundImageView) v.findViewById(R.id.payment_review_image);
        final TextView initialsText = (TextView) v.findViewById(R.id.payment_review_initials);

        // User name
        final TextView nameText = (TextView) v.findViewById(R.id.payment_review_name);
        // User email
        final TextView emailText = (TextView) v.findViewById(R.id.payment_review_email);
        // Note
        final TextView noteEdit = (TextView) v.findViewById(R.id.payment_review_note);
        noteEdit.setEnabled(false);
        noteEdit.setText("Some not interesting test note");
        // Background
        View backgroundView = v.findViewById(R.id.payment_info_layout_background);
        backgroundView.setBackgroundColor(0x80D0D0D0);
        backgroundView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != _popupWindow && _popupWindow.isShowing())
                    _popupWindow.dismiss();
            }
        });

        // Status
        try {
            Date statusDate = Globals.getUTCDateFormat().parse(payment.updateTimestamp);
            v.findViewById(R.id.payment_info_status_layout).setVisibility(View.VISIBLE);
            ((TextView) v.findViewById(R.id.payment_info_status_date)).setText(
                    android.text.format.DateFormat.format("HH:mm, MMM dd, yyyy", statusDate).toString());
            ((TextView) v.findViewById(R.id.payment_info_status)).setText(" " + String.valueOf(payment.status) + " ");
        } catch (Exception e) {
        }

        ContactsInfoLoad.instance.getContactDeviceInfo("stas@bitwalking.com", new ContactsInfoLoad.ContactInfoListener() {
            @Override
            public void onDeviceInfo(PaymentContactDeviceInfo info) {
                nameText.setText(info.name);
                emailText.setText(info.email);

                if (info.profileImage != null) {
                    profileImage.setImageBitmap(info.profileImage);
                    initialsText.setVisibility(View.GONE);
                }
                else {
                    // Show default profile image with initials
                    profileImage.setImageResource(R.drawable.transaction_default_profile_image);
                    initialsText.setText(Globals.getNameInitials(info.name));
                    initialsText.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onServerInfo(PaymentContactServerInfo info) {

            }
        });

        //todo: handle which layout to show: cancel or reject/pay - according to the payment initial user
//        if (we_created_payment) {
//            v.findViewById(R.id.payment_request_reject_pay_layout).setVisibility(View.GONE);
//            v.findViewById(R.id.payment_request_cancel_layout).setVisibility(View.VISIBLE);
//        }
//        else {
//            v.findViewById(R.id.payment_request_reject_pay_layout).setVisibility(View.VISIBLE);
//            v.findViewById(R.id.payment_request_cancel_layout).setVisibility(View.GONE);
//        }
    }

    private void fillPaymentRequestLayout(View v, final PaymentRequest payment) {
        // TODO: this is not finished code. Transactions API was not full so there are "fake"/"demo" value left
        // Amount
        for (PaymentRequest.PaymentTransferInfo t : payment.transfer) {
            if (t.currency.equalsIgnoreCase("xwd")) { // Walking $
                ((TextView) v.findViewById(R.id.payment_request_amount)).setText(" -" + Globals.bigDecimalToNiceString(t.amount));
                break;
            }
        }

        // Profile
        final RoundImageView profileImage = (RoundImageView) v.findViewById(R.id.payment_request_image);
        final TextView initialsText = (TextView) v.findViewById(R.id.payment_request_initials);

        // Request text
        final TextView requestText = (TextView) v.findViewById(R.id.payment_request_text);
        // Note
        final TextView noteEdit = (TextView) v.findViewById(R.id.payment_request_note);
        noteEdit.setEnabled(false);
        noteEdit.setText("Some not interesting test note");
        // Background
        View backgroundView = v.findViewById(R.id.payment_info_layout_background);
        backgroundView.setBackgroundColor(0x80D0D0D0);
        backgroundView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != _popupWindow && _popupWindow.isShowing())
                    _popupWindow.dismiss();
            }
        });

        // Status
        try {
            Date statusDate = Globals.getUTCDateFormat().parse(payment.updateTimestamp);
            ((TextView) v.findViewById(R.id.payment_request_date)).setText(
                    android.text.format.DateFormat.format("HH:mm, MMM dd, yyyy", statusDate).toString());
        } catch (Exception e) {
        }

        ContactsInfoLoad.instance.getContactDeviceInfo("stas@bitwalking.com", new ContactsInfoLoad.ContactInfoListener() {
            @Override
            public void onDeviceInfo(PaymentContactDeviceInfo info) {
                BigDecimal wDollarAmount = new BigDecimal("0");
                for (PaymentRequest.PaymentTransferInfo t : payment.transfer) {
                    if (t.currency.equalsIgnoreCase("xwd")) { // Walking $
                        wDollarAmount = t.amount;
                        break;
                    }
                }

                String text = String.format("%s requested %s W$ from you", info.name, Globals.bigDecimalToNiceString(wDollarAmount));
                requestText.setText(text);

                if (info.profileImage != null) {
                    profileImage.setImageBitmap(info.profileImage);
                    initialsText.setVisibility(View.GONE);
                }
                else {
                    // Show default profile image with initials
                    profileImage.setImageResource(R.drawable.transaction_default_profile_image);
                    initialsText.setText(Globals.getNameInitials(info.name));
                    initialsText.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onServerInfo(PaymentContactServerInfo info) {

            }
        });
    }

    private void promptPaymentRequest(PaymentRequest payment) {
        if (null != _popupWindow)
            _popupWindow.dismiss();

        LayoutInflater layoutInflater
                = (LayoutInflater)WalletActivity.this.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        View popupView = layoutInflater.inflate(R.layout.payment_request_popup_layout, null);
        _popupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);

//        popupView.findViewById(R.id.verify_password_cancel).setOnClickListener(this);

        // Fill payment info popup
        fillPaymentRequestLayout(popupView, payment);

        _popupWindow.setOutsideTouchable(true);
        _popupWindow.setTouchable(true);
        _popupWindow.setFocusable(true);
        _popupWindow.setAnimationStyle(R.style.PopupWindowAnimation);
        _popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, -150);
    }

    private void promptPaymentInfo(PaymentRequest payment) {
        if (null != _popupWindow)
            _popupWindow.dismiss();

        LayoutInflater layoutInflater
                = (LayoutInflater)WalletActivity.this.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        View popupView = layoutInflater.inflate(R.layout.payment_info_popup_layout, null);
        _popupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);

//        popupView.findViewById(R.id.verify_password_cancel).setOnClickListener(this);

        // Fill payment info popup
        fillPaymentInfoLayout(popupView, payment);

        _popupWindow.setOutsideTouchable(true);
        _popupWindow.setTouchable(true);
        _popupWindow.setFocusable(true);
        _popupWindow.setAnimationStyle(R.style.PopupWindowAnimation);
        _popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, -150);
    }
}
