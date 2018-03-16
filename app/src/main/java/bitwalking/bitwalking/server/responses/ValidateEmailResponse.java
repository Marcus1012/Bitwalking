package bitwalking.bitwalking.server.responses;

/**
 * Created by Marcus on 6/21/16.
 */
public class ValidateEmailResponse extends BasicServerResponse {

    public ValidateEmailResponse(BasicServerResponse base) {
        super(base);
    }

    public EmailValidated getAnswer() {
        return getPayload(EmailValidated.class);
    }

    public class EmailValidated {
        boolean status;
    }
}
