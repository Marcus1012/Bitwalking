package bitwalking.bitwalking.mining_history.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import bitwalking.bitwalking.R;

/**
 * Created by Marcus on 12/27/16.
 */

public class SimpleItemDivider extends RecyclerView.ItemDecoration {
    private Drawable _divider;

    public SimpleItemDivider(Context context) {
        _divider = ContextCompat.getDrawable(context, R.drawable.line_divider);
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        int left = parent.getPaddingLeft();
        int right = parent.getWidth() - parent.getPaddingRight();

        int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = parent.getChildAt(i);

            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

            int top = child.getBottom() + params.bottomMargin;
            int bottom = top + _divider.getIntrinsicHeight();

            _divider.setBounds(left, top, right, bottom);
            _divider.draw(c);
        }
    }
}