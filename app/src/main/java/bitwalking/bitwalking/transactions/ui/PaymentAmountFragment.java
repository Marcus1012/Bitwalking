package bitwalking.bitwalking.transactions.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.math.BigDecimal;

import bitwalking.bitwalking.BuildConfig;
import bitwalking.bitwalking.R;
import bitwalking.bitwalking.transactions.PaymentRequestNotification;
import bitwalking.bitwalking.util.MyDigitsKeyboard;

/**
 * Created by Marcus on 11/16/16.
 */

public class PaymentAmountFragment extends Fragment {

    private static final int ALLOWED_DIGITS_AFTER_DOT = 2;
    private static final int ALLOWED_DIGITS_BEFORE_DOT = 5;

    private MyDigitsKeyboard _digits;
    private TextView _amountText;
    private String _currentAmount = "";

    private OnSendRequestAmountListener _callback;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.send_request_amount_layout, container, false);

        _amountText = (TextView) v.findViewById(R.id.send_request_amount);

        _digits = (MyDigitsKeyboard) v.findViewById(R.id.send_request_digits);
        _digits.addListener(new MyDigitsKeyboard.OnDigitListener() {
            @Override
            public void onAddDigit(String digit) {
                if (digit.contentEquals(".")) {
                    if (_currentAmount.length() == 0) {
                        _currentAmount = "0.";
                    }
                    else if (!_currentAmount.contains(".")) {
                        _currentAmount += ".";
                    }
                    else {
                        // ignore, '.' already entered
                    }
                }
                else if (digit.contentEquals("0")) {
                    if (_currentAmount.contentEquals("0")) {
                        // ignore, allow only one leading 0
                    }
                    else {
                        tryAddDigit(digit);
                    }
                }
                else if (_currentAmount.contentEquals("0")) {
                    _currentAmount = digit;
                }
                else {
                    tryAddDigit(digit);
                }

                refreshAmountUI();
            }

            private void tryAddDigit(String digit) {
                int dotIndex = _currentAmount.indexOf('.');
                if (dotIndex >= 0 && (dotIndex + ALLOWED_DIGITS_AFTER_DOT) < _currentAmount.length()) {
                    // ignore
                }
                else if (dotIndex < 0 && _currentAmount.length() >= ALLOWED_DIGITS_BEFORE_DOT) {
                    // ignore
                }
                else {
                    _currentAmount += digit;
                }
            }

            @Override
            public void onDeleteDigit() {
                if (_currentAmount.length() > 0) {
                    _currentAmount = _currentAmount.substring(0, _currentAmount.length() - 1);
                }

                refreshAmountUI();
            }
        });

        v.findViewById(R.id.payment_request_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != _callback) {
                    _callback.onRequest(currentAmount());
                }
            }
        });

        v.findViewById(R.id.payment_send_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != _callback) {
                    _callback.onSend(currentAmount());
                }
            }
        });

        refreshAmountUI();

        if (BuildConfig.DEBUG) {
            v.findViewById(R.id.payment_request_button).setLongClickable(true);
            v.findViewById(R.id.payment_request_button).setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    PaymentRequestNotification.buildNotification(PaymentAmountFragment.this.getActivity(),
                            "Stas sent you a payment request",
                            "Stas sent you a payment request for 25W$.");
                    return true;
                }
            });
        }

        return v;
    }

    private BigDecimal currentAmount() {
        return new BigDecimal(_currentAmount.isEmpty() ? "0" : _currentAmount);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            _callback = (OnSendRequestAmountListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnSendRequestAmountListener");
        }
    }

    private void refreshAmountUI() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String temp = (_currentAmount.isEmpty()) ? "0" : _currentAmount;
                _amountText.setText(" " + temp);
            }
        });
    }

    public interface OnSendRequestAmountListener {
        void onSend(BigDecimal amount);
        void onRequest(BigDecimal amount);
    }
}
