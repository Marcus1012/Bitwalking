package bitwalking.bitwalking.server.requests;

/**
 * Created by Marcus on 6/21/16.
 */
public class ConfirmPasswordResetRequest extends JsonRequest {
    private String email;
    private String validationToken;
    private String newPassword;

    public ConfirmPasswordResetRequest(String email, String validationToken, String newPassword) {
        this.email = email;
        this.validationToken = validationToken;
        this.newPassword = newPassword;
    }
}
