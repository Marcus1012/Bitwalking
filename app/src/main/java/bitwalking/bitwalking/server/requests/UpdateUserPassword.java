package bitwalking.bitwalking.server.requests;

/**
 * Created by Marcus on 6/22/16.
 */
public class UpdateUserPassword extends JsonRequest {
    private String currentPassword;
    private String newPassword;

    public UpdateUserPassword(String currentPassword, String newPassword) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
    }
}
