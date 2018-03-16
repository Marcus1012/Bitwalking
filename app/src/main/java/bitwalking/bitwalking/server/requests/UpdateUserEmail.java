package bitwalking.bitwalking.server.requests;

/**
 * Created by Marcus on 6/22/16.
 */
public class UpdateUserEmail extends JsonRequest {
    private String currentPassword;
    private String newEmail;

    public UpdateUserEmail(String currentPassword, String newEmail) {
        this.currentPassword = currentPassword;
        this.newEmail = newEmail;
    }
}