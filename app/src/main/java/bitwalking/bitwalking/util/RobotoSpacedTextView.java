package bitwalking.bitwalking.util;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * Created by Marcus on 9/20/16.
 */
public class RobotoSpacedTextView extends TextView {
    public RobotoSpacedTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
//        init(defStyle);
    }

    public RobotoSpacedTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
//        init(0);
    }

    public RobotoSpacedTextView(Context context) {
        super(context);
//        init(0);
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            super.setText(SpacedTextView.applySpacing(text), type);
        } else {
            super.setText(text, type);
        }
    }

//    private void init(int textStyle) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
//            setLetterSpacing(0.12f);
//
//        Typeface tf = FontCache.get(FontCache.FontTypeEnum., getContext());
//
//        switch (textStyle) {
//            case 1: // bold
//                tf = FontCache.get(FontCache.FontTypeEnum.DOLCE_VITA_HEAVY_BOLD, getContext()); break;
//            case 2: // italic
//                tf = FontCache.get(FontCache.FontTypeEnum.DOLCE_VITA_LIGHT, getContext()); break;
//            case 0: // regular
//            default:
//                break;//tf = Typeface.createFromAsset(getContext().getAssets(), "fonts/O.ttf");
//        }
//
//        setTypeface(tf);
//    }
//
//    public void setTypeface(Typeface tf, int style) {
//        if (style == Typeface.BOLD) {
//            super.setTypeface(FontCache.get(FontCache.FontTypeEnum.DOLCE_VITA_HEAVY_BOLD, getContext()));
//        }
//        else if(style == Typeface.ITALIC)
//        {
//            super.setTypeface(FontCache.get(FontCache.FontTypeEnum.DOLCE_VITA_LIGHT, getContext()));
//        }
//        else
//        {
//            super.setTypeface(FontCache.get(FontCache.FontTypeEnum.DOLCE_VITA_REGULAR, getContext()));
//        }
//    }
}
