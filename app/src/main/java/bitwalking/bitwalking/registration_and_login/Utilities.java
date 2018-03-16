package bitwalking.bitwalking.registration_and_login;

import android.content.Context;
import android.provider.Settings;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.io.UnsupportedEncodingException;
import java.security.AlgorithmParameters;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

import bitwalking.bitwalking.BitwalkingApp;
import bitwalking.bitwalking.util.Globals;
import bitwalking.bitwalking.util.Logger;

/**
 * Created by Marcus on 11/24/15.
 */
public class Utilities {
    private static final String TAG = Utilities.class.getSimpleName();

    public static String getDeviceItentity(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    public static String getTimestamp() {
        return Globals.getUTCDateFormat().format(new Date());
    }

    public static String getSignature(String dataToSign, String key) {
        try {
            byte[] data = dataToSign.getBytes("UTF8");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes("UTF8"), "HmacSHA256"));
            char[] signature = Hex.encodeHex(mac.doFinal(data));
            return new String(signature);
        } catch (Exception exception) {
            return null;
        }
    }

    public static String decodeBase64(String data) throws UnsupportedEncodingException {
        byte[] tmp = Base64.decodeBase64(data.getBytes());
        return new String(tmp, "UTF8");
    }

    public static class AESEncryption {

        public static final String ENCRYPTION_ALGORITHM = "AES/CBC/PKCS5Padding";

        public static String unwrap(String cipherText, String key) throws Exception {
            byte[] dataToDecrypt = Base64.decodeBase64(cipherText.getBytes());
            byte[] initializationVector = new byte[16];
            byte[] data = new byte[dataToDecrypt.length - 16];

            System.arraycopy(dataToDecrypt, 0, initializationVector, 0, 16);
            System.arraycopy(dataToDecrypt, 16, data, 0, dataToDecrypt.length - 16);

            byte[] plainText = decrypt(data, key, initializationVector);
            return new String(plainText);
        }

        public static byte[] decrypt(byte[] cipherBytes, String key, byte[] iv)
                throws Exception {
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            AlgorithmParameters params = AlgorithmParameters.getInstance("AES");
            params.init(new IvParameterSpec(iv));
            cipher.init(Cipher.DECRYPT_MODE, getKey(key), params);
            return cipher.doFinal(cipherBytes);
        }

        private static SecretKeySpec getKey(String key) throws Exception {
            return new SecretKeySpec(Hex.decodeHex(key.toCharArray()), "AES");
        }
    }

    private static boolean hasCharSequence(String text, int minSequenceLength) {
        boolean hasSequence = false;

        if (null != text && text.length() >= minSequenceLength) {
            if (minSequenceLength <= 1)
                return true;

            for (int i = 0; i <= (text.length() - minSequenceLength); ++i) {
                String subText = text.substring(i, i + minSequenceLength);
                boolean isSequence = true;

                // Check if the sub text has sequence
                for (int j = 0; (j < minSequenceLength - 1) && isSequence; ++j) {
                    if (Math.abs(subText.charAt(j) - subText.charAt(j + 1)) > 1) {
                        isSequence = false;
                    }
                }

                if (isSequence) {
                    hasSequence = true;
                    break;
                }
            }
        }

        return hasSequence;
    }

    public static boolean isPasswordValid(String password) {
        boolean valid = true;

        // Password length >= 6
        if (password.length() < 6) {
            valid = false;
        }
        // Has number
        else if (!password.matches(".*\\d.*")) {
            valid = false;
        }
        // Has letter
        else if (!password.matches(".*[a-zA-Z]*")) {
            valid = false;
        }
        // No consecutive
        else if (hasCharSequence(password, 4)) {
            valid = false;
        }

        return valid;
    }

    public static int MIN_AGE = 13;
    public static boolean isValidAge(String birthDate, DateFormat dateFormatter) {
        boolean valid = false;
        try {
            Date bd = dateFormatter.parse(birthDate);
            if (!bd.after(Utilities.getMinimalDateOfBirth())) {
                valid = true;
            }
//
//            Calendar cal = new GregorianCalendar();
//            cal.setTimeZone(TimeZone.getTimeZone("UTC"));
//            int y = cal.get(Calendar.YEAR) - MIN_AGE;
//            int m = cal.get(Calendar.MONTH);
//            int d = cal.get(Calendar.DAY_OF_MONTH);
//            cal.set(y, m, d, 0, 0, 0);
//            cal.set(Calendar.MILLISECOND, 0);
//
//            if (cal.getTime().after(bd))
//                valid = true;
        }
        catch (ParseException e) {
            e.printStackTrace();
        }

        return valid;
    }

    public static Date getMinimalDateOfBirth() {
        Calendar cal = new GregorianCalendar();
//        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        int y = cal.get(Calendar.YEAR) - MIN_AGE;
        int m = cal.get(Calendar.MONTH);
        int d = cal.get(Calendar.DAY_OF_MONTH);
        cal.set(y, m, d, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DAY_OF_MONTH, 1);

        Logger.instance().Log(Logger.VERB, TAG, "getMinimalDateOfBirth: " + Globals.getUTCDateFormat().format(cal.getTime()));

        return cal.getTime();
    }

    public static boolean isEmailValid(String email) {
        // matches regex:
        String emailRegex = "[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})";
        return email.contains("@") && email.matches(emailRegex);
    }

    public static boolean isPasscodeValid(String passscode) {
        return (passscode.length() == 4 && passscode.matches("[0-9]+"));
    }

    public static boolean isPhoneValid(String phone, String countryCode) {
        boolean valid = false;

        ArrayList<String> countries = IsoToPhone.getCountriesByCode(countryCode);

        try {
            for (String country : countries) {
                PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
                Phonenumber.PhoneNumber number = phoneUtil.parse(countryCode + phone, country);
                if (phoneUtil.isValidNumberForRegion(number, country)) {
                    valid = true;
                    break;
                }
            }
        }
        catch (Exception e) {
            BitwalkingApp.getInstance().trackException("isPhoneValid failed", e);
        }

        Logger.instance().Log(Logger.DEBUG, TAG, String.format("(%s) %s - %s", countryCode, phone, (valid) ? "valid" : "invalid"));

        return valid;
    }

    public static boolean isPhoneCodeValid(String code) {
        //TODO: Replace this with your own logic
        String codeOnly = (code.charAt(0) == '+') ? code.substring(1) : code;
        return codeOnly.matches("[1-9\\+][0-9]+") && codeOnly.length() > 0;
    }

    public static boolean isValidFullName(String fullName) {
        String[] names = fullName.split(" ");
        if (names.length > 1 && names[0].length() > 0 && names[1].length() > 0)
            return true;

        return false;
    }

    public static String getProfileImageFileName() {
        return "ProfileImage.jpg";
    }

    public static String getInternalFolderName() {
        return "Bitwalking";
    }
}
