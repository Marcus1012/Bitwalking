package bitwalking.bitwalking.util;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.EditText;

/**
 * Created by Marcus on 3/29/16.
 */
public class DolceVitaEditText extends EditText {
    public DolceVitaEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(defStyle);
    }

    public DolceVitaEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(0);
    }

    public DolceVitaEditText(Context context) {
        super(context);
        init(0);
    }

    private void init(int textStyle) {
        Typeface tf = FontCache.get(FontCache.FontTypeEnum.DOLCE_VITA_REGULAR, getContext());

        switch (textStyle) {
            case 1: // bold
                tf = FontCache.get(FontCache.FontTypeEnum.DOLCE_VITA_HEAVY_BOLD, getContext());

            case 2: // italic
                tf = FontCache.get(FontCache.FontTypeEnum.DOLCE_VITA_LIGHT, getContext());

            case 0: // regular
            default:
                break;
        }

        setTypeface(tf);
    }

    public void setTypeface(Typeface tf, int style) {
        if (style == Typeface.BOLD) {
            super.setTypeface(FontCache.get(FontCache.FontTypeEnum.DOLCE_VITA_HEAVY_BOLD, getContext()));
        }
        else if(style == Typeface.ITALIC)
        {
            super.setTypeface(FontCache.get(FontCache.FontTypeEnum.DOLCE_VITA_LIGHT, getContext()));
        }
        else
        {
            super.setTypeface(FontCache.get(FontCache.FontTypeEnum.DOLCE_VITA_REGULAR, getContext()));
        }
    }
}