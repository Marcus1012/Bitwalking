package bitwalking.bitwalking.util;

/**
 * Created by Marcus on 3/28/16.
 */

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ScaleXSpan;
import android.util.AttributeSet;
import android.widget.TextView;

public class SpacedTextView extends TextView {

    private float spacing = Spacing.NORMAL;
    private CharSequence originalText = "";


    public SpacedTextView(Context context) {
        super(context);
    }

    public SpacedTextView(Context context, AttributeSet attrs){
        super(context, attrs);
    }

    public SpacedTextView(Context context, AttributeSet attrs, int defStyle){
        super(context, attrs, defStyle);
    }

    public float getSpacing() {
        return this.spacing;
    }

    public void setSpacing(float spacing) {
        this.spacing = spacing;
        applySpacing();
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        originalText = text;
        applySpacing();
    }

    @Override
    public CharSequence getText() {
        return originalText;
    }

    public static CharSequence applySpacing(CharSequence input) {
        if (null == input)
            return input;

        CharSequence output = input;

        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < input.length(); i++) {
            builder.append(input.charAt(i));

            if (skipSpace(input, i))
                continue;

            if(i+1 < input.length()) {
                builder.append("\u00A0");
            }
        }

        SpannableString finalText = new SpannableString(builder.toString());
        if(builder.toString().length() > 1) {
            for(int i = 1; i < builder.toString().length(); i+=2) {
                finalText.setSpan(new ScaleXSpan((Spacing.NORMAL+1)/10), i, i+1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        return finalText.toString();
    }

    private void applySpacing() {
        if (this == null || this.originalText == null) return;

        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < originalText.length(); i++) {
            builder.append(originalText.charAt(i));

            if (skipSpace(originalText, i))
                continue;

            if(i+1 < originalText.length()) {
                builder.append("\u00A0");
            }
        }
        SpannableString finalText = new SpannableString(builder.toString());
        if(builder.toString().length() > 1) {
            for(int i = 1; i < builder.toString().length(); i+=2) {
                finalText.setSpan(new ScaleXSpan((spacing+1)/10), i, i+1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        super.setText(finalText, BufferType.SPANNABLE);
    }

    private static boolean skipSpace(CharSequence text, int i) {
        boolean skip = false;
        char currChar = text.charAt(i);

        if (0 == i) {
            skip = skip || (currChar == ' ');
        }

        if (i < (text.length() - 1)) {
            char nextChar = text.charAt(i + 1);
            skip = skip || ((currChar == 'w' || currChar == 'W') && (nextChar == 'a' || nextChar == 'A' || nextChar == '\n'));
        }

        return skip;
    }

    public class Spacing {
        public final static float NORMAL = 0;
    }
}