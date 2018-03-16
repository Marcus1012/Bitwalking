package bitwalking.bitwalking.server.api;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.util.Date;

import bitwalking.bitwalking.steps.steps_info.StepsBulk;
import bitwalking.bitwalking.steps.steps_info.StepsTelephonyExtra;
import bitwalking.bitwalking.steps.telephony_info.TelephonyData;
import bitwalking.bitwalking.util.Globals;
import bitwalking.bitwalking.util.Logger;

/**
 * Created by Marcus on 11/10/16.
 */

public class MySuperBoolean implements Comparable<Boolean> {
    private static final String TAG = MySuperBoolean.class.getSimpleName();
    private final boolean value;

    @SuppressWarnings("unchecked")
    public static final Class<MySuperBoolean> TYPE
            = (Class<MySuperBoolean>) boolean[].class.getComponentType();

    public static final MySuperBoolean TRUE = new MySuperBoolean(true);

    public static final MySuperBoolean FALSE = new MySuperBoolean(false);

    public MySuperBoolean(String string) {
        this(parseBoolean(string));
    }

    public MySuperBoolean(boolean value) {
        this.value = value;
    }

    public MySuperBoolean(int value) {
        this.value = (value == 1) ? true : false;
    }

    public boolean booleanValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        return (o == this) || ((o instanceof MySuperBoolean) && (((MySuperBoolean) o).value == value));
    }

    public int compareTo(Boolean that) {
        return compare(value, that);
    }

    public static int compare(boolean lhs, boolean rhs) {
        return lhs == rhs ? 0 : lhs ? 1 : -1;
    }

    @Override
    public int hashCode() {
        return value ? 6539 : 5425;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    public static boolean getBoolean(String string) {
        if (string == null || string.length() == 0) {
            return false;
        }
        return (parseBoolean(System.getProperty(string)));
    }

    public static boolean parseBoolean(String s) {
        boolean b = false;
        if ("true".equalsIgnoreCase(s) || "1".contentEquals(s))
            b = true;
        return b;
    }

    public static String toString(boolean value) {
        return String.valueOf(value);
    }

    public static MySuperBoolean valueOf(String string) {
        return parseBoolean(string) ? MySuperBoolean.TRUE : MySuperBoolean.FALSE;
    }

    public static MySuperBoolean valueOf(boolean b) {
        return b ? MySuperBoolean.TRUE : MySuperBoolean.FALSE;
    }

    public static class MySuperBooleanSerializer implements JsonSerializer<MySuperBoolean> {
        @Override
        public JsonElement serialize(MySuperBoolean src, Type typeOfSrc, JsonSerializationContext context) {
            return context.serialize(src.booleanValue());
        }
    }

    public static class MySuperBooleanDeserializer implements JsonDeserializer<MySuperBoolean> {
        @Override
        public MySuperBoolean deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context)
                throws JsonParseException {
            MySuperBoolean value = null;
            try {
                Boolean b = new Gson().fromJson(json, Boolean.class);
                if (null != b)
                    value = new MySuperBoolean(b);
            } catch (Exception e) {
            }

            if (null == value) {
                try {
                    Integer n = new Gson().fromJson(json, Integer.class);
                    if (null != n)
                        value = new MySuperBoolean(n);
                } catch (Exception e) {
                }
            }

            return value;
        }
    }
}
