package bitwalking.bitwalking.util;

import android.content.Context;
import android.graphics.Typeface;

import java.util.Hashtable;

/**
 * Created by Marcus on 11/5/15.
 */
public class FontCache {
    private static Hashtable<String, Typeface> fontCache = new Hashtable<>();

    public enum FontTypeEnum {
        DOLCE_VITA_LIGHT,
        DOLCE_VITA_REGULAR,
        DOLCE_VITA_HEAVY_BOLD,
        DROIDIGA_REGULAR,
        BARON_NEUE_REGULAR,
        BARON_NEUE_ITALIC,
        BARON_NEUE_BOLD,
        BARON_NEUE_BOLD_ITALIC,

        ROBOTO_REGULAR,
        ROBOTO_BOLD,
        ROBOTO_ITALIC,
        ROBOTO_BOLD_ITALIC,
    }

    public static String getFontName(FontTypeEnum type) {
        String name = "";

        switch (type) {
            case DOLCE_VITA_LIGHT: name = "fonts/Dolce Vita Light.ttf"; break;
            case DOLCE_VITA_REGULAR: name = "fonts/Dolce Vita.ttf"; break;
            case DOLCE_VITA_HEAVY_BOLD: name = "fonts/Dolce Vita Heavy Bold.ttf"; break;
            case DROIDIGA_REGULAR: name = "fonts/Droidiga.otf"; break;
            case BARON_NEUE_REGULAR: name = "fonts/Baron Neue.otf"; break;
            case BARON_NEUE_ITALIC: name = "fonts/Baron Neue Italic.otf"; break;
            case BARON_NEUE_BOLD: name = "fonts/Baron Neue Bold.otf"; break;
            case BARON_NEUE_BOLD_ITALIC: name = "fonts/Baron Neue Bold Italic.otf"; break;

            case ROBOTO_REGULAR: name = "fonts/Roboto-Regular.ttf"; break;
            case ROBOTO_BOLD: name = "fonts/Roboto-Bold.ttf"; break;
            case ROBOTO_ITALIC: name = "fonts/Roboto-Regular.ttf"; break;
            case ROBOTO_BOLD_ITALIC: name = "fonts/Roboto-BoldItalic.ttf"; break;

            default: break;
        }

        return name;
    }

    public static Typeface get(FontTypeEnum type, Context context) {
        return get(getFontName(type), context);
    }

    public static Typeface get(String name, Context context) {
        Typeface tf = fontCache.get(name);
        if(tf == null) {
            try {
                tf = Typeface.createFromAsset(context.getAssets(), name);
            }
            catch (Exception e) {
                return null;
            }
            fontCache.put(name, tf);
        }
        return tf;
    }
}
