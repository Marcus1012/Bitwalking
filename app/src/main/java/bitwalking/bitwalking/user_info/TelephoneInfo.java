package bitwalking.bitwalking.user_info;

/**
 * Created by Marcus on 7/15/16.
 */
public class TelephoneInfo {
    public String countryCode;
    public String number;
    public String msisdn;

    public TelephoneInfo(String countryCode, String number) {
        this.countryCode = (countryCode.charAt(0) == '+') ? countryCode.substring(1) : countryCode;
        this.number = number;
    }

    public static String phoneToMSISDN(String code, String number) {
        code = (code.charAt(0) == '+') ? code.substring(1) : code;
        while (number.length() > 1 && number.charAt(0) == '0')
            number = number.substring(1);

        return code + number;
    }


    public boolean hasPhone(){
        return (number!=null && !number.isEmpty());
    }
}
