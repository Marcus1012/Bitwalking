package bitwalking.bitwalking.server.requests;

import bitwalking.bitwalking.user_info.TelephoneInfo;

/**
 * Created by Marcus on 6/21/16.
 */
public class ResetPasswordRequest extends JsonRequest {
//    private TelephoneInfo phone;
    private String email;

//    public ResetPasswordRequest(TelephoneInfo phone) {
//        this.phone = phone;
//    }

    public ResetPasswordRequest(String email) {
        this.email = email;
    }
}
