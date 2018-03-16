package bitwalking.bitwalking.server.requests;

import com.google.gson.annotations.Expose;

import bitwalking.bitwalking.user_info.TelephoneInfo;

/**
 * Created by Marcus on 7/7/16.
 */
public class VerifyPasswordRequest extends JsonRequest {
    private TelephoneInfo phone;
    private String email;
    private String password;

    public VerifyPasswordRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

//    public VerifyPasswordRequest(String countryCode, String number, String password) {
//        this.phone = new TelephoneInfo(countryCode, number);
//        this.password = password;
//    }
}
