package bitwalking.bitwalking.util;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.EditText;

/**
 * Created by Marcus on 11/17/15.
 */
public class BWEditText extends EditText {
    public BWEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    public BWEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public BWEditText(Context context) {
        super(context);
    }
//    public void setTypeface(Typeface tf, int style) {
//        if (style == Typeface.BOLD) {
//            super.setTypeface(FontCache.get(FontCache.FontTypeEnum.ROBOTO_BOLD, getContext()));
//        }
//        else if(style == Typeface.ITALIC)
//        {
//            super.setTypeface(Typeface.createFromAsset(getContext().getAssets(), "fonts"));
//        }
//        else
//        {
//            super.setTypeface(FontCache.get(FontCache.FontTypeEnum.ROBOTO_REGULAR, getContext()));
//        }
//    }
}