package bitwalking.bitwalking.util;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by Marcus on 11/18/15.
 */
public class CustomViewPager extends ViewPager {
    private boolean _pagingEnabled = true;

    public CustomViewPager(Context context) {
        super(context);
    }

    public CustomViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return _pagingEnabled && super.onTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return _pagingEnabled && super.onInterceptTouchEvent(event);
    }

    public void setPagingEnabled(boolean b) {
        _pagingEnabled = b;
    }
}