package bitwalking.bitwalking.server.requests;

import bitwalking.bitwalking.user_info.TelephoneInfo;

/**
 * Created by Marcus on 6/21/16.
 */
public class UserRegisterRequest extends JsonRequest {
    private String country;
    private String password;
    private TelephoneInfo phone;
    private String fullName;
    private String email;
    private String dateOfBirth;
    private String registrationToken;

    public UserRegisterRequest(String country, String password, TelephoneInfo phone,
               String fullName, String email, String dateOfBirth, String registrationToken) {
        this.country = country;
        this.password = password;
        this.phone = phone;
        this.fullName = fullName;
        this.email = email;
        this.dateOfBirth = dateOfBirth;
        this.registrationToken = registrationToken;
    }
}
