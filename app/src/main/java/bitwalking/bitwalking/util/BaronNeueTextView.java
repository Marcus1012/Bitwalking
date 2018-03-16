package bitwalking.bitwalking.util;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * Created by Marcus on 4/18/16.
 */
public class BaronNeueTextView extends TextView {
    public BaronNeueTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(defStyle);
    }

    public BaronNeueTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(0);
    }

    public BaronNeueTextView(Context context) {
        super(context);
        init(0);
    }

    private void init(int textStyle) {
        Typeface tf = FontCache.get(FontCache.FontTypeEnum.BARON_NEUE_ITALIC, getContext());

        switch (textStyle) {
            case 1: // bold
                tf = FontCache.get(FontCache.FontTypeEnum.BARON_NEUE_BOLD, getContext());
                break;
            case 2: // italic
                tf = FontCache.get(FontCache.FontTypeEnum.BARON_NEUE_ITALIC, getContext());
                break;
            case 0: // regular
            default:
                tf = FontCache.get(FontCache.FontTypeEnum.BARON_NEUE_BOLD_ITALIC, getContext());
                break;
        }

        setTypeface(tf);
    }

    public void setTypeface(Typeface tf, int style) {
        if (style == Typeface.BOLD) {
            super.setTypeface(FontCache.get(FontCache.FontTypeEnum.BARON_NEUE_BOLD, getContext()));
        }
        else if(style == Typeface.ITALIC)
        {
            super.setTypeface(FontCache.get(FontCache.FontTypeEnum.BARON_NEUE_ITALIC, getContext()));
        }
        else
        {
            super.setTypeface(FontCache.get(FontCache.FontTypeEnum.BARON_NEUE_REGULAR, getContext()));
        }

        super.setTypeface(FontCache.get(FontCache.FontTypeEnum.BARON_NEUE_ITALIC, getContext()));
    }
}