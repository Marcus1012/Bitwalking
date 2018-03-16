package bitwalking.bitwalking.util;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;

import java.util.ArrayList;

import bitwalking.bitwalking.R;

/**
 * Created by Marcus on 6/27/16.
 */
public class MyDigitsKeyboard extends TableLayout {

    private static final int ROW_HEIGHT = 57;
    private ArrayList<OnDigitListener> _listeners = new ArrayList<>();

    public MyDigitsKeyboard(Context context, AttributeSet attrs) {
        super(context, attrs);

        init();
    }

    public MyDigitsKeyboard(Context context) {
        super(context);

        init();
    }

    private int getDpToPixels(int dp) {
        final float scale = getContext().getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    private void init() {
        TableRow.LayoutParams tableRowParams = new TableRow.LayoutParams();
        tableRowParams.setMargins(1, 1, 1, 1);
        tableRowParams.weight = 1;
        tableRowParams.width = 100;
        tableRowParams.height = getDpToPixels(ROW_HEIGHT);
        tableRowParams.gravity = Gravity.CENTER;

        // Row 1
        TableRow currRow = new TableRow(getContext());
        currRow.addView(createDigit("1"), tableRowParams);
        currRow.addView(createDigit("2"), tableRowParams);
        currRow.addView(createDigit("3"), tableRowParams);
        addView(currRow);

        // Row 2
        currRow = new TableRow(getContext());
        currRow.addView(createDigit("4"), tableRowParams);
        currRow.addView(createDigit("5"), tableRowParams);
        currRow.addView(createDigit("6"), tableRowParams);
        addView(currRow);

        // Row 3
        currRow = new TableRow(getContext());
        currRow.addView(createDigit("7"), tableRowParams);
        currRow.addView(createDigit("8"), tableRowParams);
        currRow.addView(createDigit("9"), tableRowParams);
        addView(currRow);

        // Row 4
        currRow = new TableRow(getContext());
        Button b = createDigit(".");
//        b.setVisibility(INVISIBLE);
        currRow.addView(b, tableRowParams);
        currRow.addView(createDigit("0"), tableRowParams);
        currRow.addView(createDeleteButton(), tableRowParams);
        addView(currRow);

        setShrinkAllColumns(true);
    }

    private Button createDigit(final String digit) {
        Button button = new Button(getContext());
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(50, ViewGroup.LayoutParams.MATCH_PARENT);
        button.setLayoutParams(lp);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 31);
        button.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
        button.setTextColor(0xFFFFFFFF);
        button.setBackgroundResource(R.drawable.digit_button_style);
        button.setText(digit);
        button.setGravity(Gravity.CENTER);

        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onDigitClick(digit);
            }
        });

        return button;
    }

    private View createDeleteButton() {
        ImageView img = new ImageView(getContext());
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(70, 60);
        img.setLayoutParams(lp);
        img.setScaleType(ImageView.ScaleType.FIT_CENTER);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            img.setImageDrawable(getResources().getDrawable(R.drawable.backspace_image, getContext().getTheme()));
        } else {
            img.setImageDrawable(getResources().getDrawable(R.drawable.backspace_image));
        }

        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)img.getLayoutParams();
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        img.setLayoutParams(layoutParams);

        RelativeLayout layout = new RelativeLayout(getContext());
        layout.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
        layout.setBackgroundResource(R.drawable.delete_button_style);
        layout.addView(img);

        // Handle on click
        layout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onDigitClick(null);
            }
        });

        return layout;
    }

    private void onDigitClick(String digit) {
        if (null == digit) { // delete
            for (OnDigitListener l : _listeners) {
                if (null != l)
                    l.onDeleteDigit();
            }
        }
        else { //digit
            for (OnDigitListener l : _listeners) {
                if (null != l)
                    l.onAddDigit(digit);
            }
        }
    }

    public void addListener(OnDigitListener listener) {
        if (!_listeners.contains(listener))
            _listeners.add(listener);
    }

    public void removeListener(OnDigitListener listener) {
        _listeners.remove(listener);
    }

    public interface OnDigitListener {
        void onAddDigit(String digit);
        void onDeleteDigit();
    }
}
