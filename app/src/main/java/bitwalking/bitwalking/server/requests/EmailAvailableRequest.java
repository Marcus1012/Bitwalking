package bitwalking.bitwalking.server.requests;

/**
 * Created by Marcus on 6/21/16.
 */
public class EmailAvailableRequest extends JsonRequest {
    private String emailAddress;

    public EmailAvailableRequest(String emailAddress) {
        this.emailAddress = emailAddress;
    }
}
