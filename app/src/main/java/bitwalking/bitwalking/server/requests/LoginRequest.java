package bitwalking.bitwalking.server.requests;

import bitwalking.bitwalking.user_info.TelephoneInfo;

/**
 * Created by Marcus on 6/21/16.
 */
public class LoginRequest extends JsonRequest {
    private TelephoneInfo phone;
    private String email;
    private String password;

    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }
    public LoginRequest(TelephoneInfo phone, String password) {
        this.phone = phone;
        this.password = password;
    }
}
