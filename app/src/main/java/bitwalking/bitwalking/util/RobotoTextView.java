package bitwalking.bitwalking.util;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.TextView;

import bitwalking.bitwalking.R;

/**
 * Created by Marcus on 3/28/16.
 */
public class RobotoTextView extends TextView {

    public RobotoTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(defStyle);
    }

    public RobotoTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(getStyle(context,attrs));
    }

    public RobotoTextView(Context context) {
        super(context);

    }

    private int getStyle(Context context, AttributeSet attrs){
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.AppCompatTextView,
                0, 0);
        int style=0;

        try {
            style=attrs.getAttributeIntValue("http://schemas.android.com/apk/res/android", "textStyle", Typeface.NORMAL);

        } finally {
            a.recycle();
        }
        return style;
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            super.setText(SpacedTextView.applySpacing(text), type);
        } else {
            super.setText(text, type);
        }
    }

    private void init(int textStyle) {
       /* if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            setLetterSpacing(0.12f);*/

        Typeface tf = FontCache.get(FontCache.FontTypeEnum.ROBOTO_REGULAR, getContext());

        switch (textStyle) {
            case 1: // bold
                tf = FontCache.get(FontCache.FontTypeEnum.ROBOTO_BOLD, getContext());
                 break;
            case 2: // italic
                tf = FontCache.get(FontCache.FontTypeEnum.ROBOTO_ITALIC, getContext());
                break;
            case 3: // bold italic
                tf = FontCache.get(FontCache.FontTypeEnum.ROBOTO_BOLD_ITALIC, getContext());
                break;
            case 0: // regular
            default:
                break;//tf = Typeface.createFromAsset(getContext().getAssets(), "fonts/Op");
        }

        setTypeface(tf);
    }

    public void setTypeface(Typeface tf, int style) {
       if (style == Typeface.BOLD) {
            super.setTypeface(FontCache.get(FontCache.FontTypeEnum.ROBOTO_BOLD, getContext()));
        }
        else if(style == Typeface.ITALIC)
        {
            super.setTypeface(FontCache.get(FontCache.FontTypeEnum.ROBOTO_ITALIC, getContext()));
        }
        else if (style==Typeface.BOLD_ITALIC){
           super.setTypeface(FontCache.get(FontCache.FontTypeEnum.ROBOTO_BOLD_ITALIC, getContext()));
        }
        else
        {
            super.setTypeface(FontCache.get(FontCache.FontTypeEnum.ROBOTO_REGULAR, getContext()));
        }

        super.setTypeface(FontCache.get(FontCache.FontTypeEnum.ROBOTO_REGULAR, getContext()));
    }
}