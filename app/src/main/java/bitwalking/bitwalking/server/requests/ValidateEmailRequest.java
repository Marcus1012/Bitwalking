package bitwalking.bitwalking.server.requests;

/**
 * Created by Marcus on 6/21/16.
 */
public class ValidateEmailRequest extends JsonRequest {
    private String emailAddress;
    private String validationToken;

    public ValidateEmailRequest(String emailAddress, String validationToken) {
        this.emailAddress = emailAddress;
        this.validationToken = validationToken;
    }
}
