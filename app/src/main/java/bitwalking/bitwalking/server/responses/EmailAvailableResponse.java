package bitwalking.bitwalking.server.responses;

/**
 * Created by Marcus on 6/21/16.
 */
public class EmailAvailableResponse extends BasicServerResponse {

    public EmailAvailableResponse(BasicServerResponse base) {
        super(base);
    }

    public EmailAvailable getAnswer() {
        return getPayload(EmailAvailable.class);
    }

    public class EmailAvailable {
        public String emailAddress;
        public boolean available;
    }
}
