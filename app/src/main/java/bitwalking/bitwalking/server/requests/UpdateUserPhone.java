package bitwalking.bitwalking.server.requests;

import bitwalking.bitwalking.user_info.TelephoneInfo;

/**
 * Created by Marcus on 6/22/16.
 */
public class UpdateUserPhone extends JsonRequest {
    private String currentPassword;
    private TelephoneInfo newPhone;

    public UpdateUserPhone(String currentPassword, TelephoneInfo newPhone) {
        this.currentPassword = currentPassword;
        this.newPhone = newPhone;
    }
}