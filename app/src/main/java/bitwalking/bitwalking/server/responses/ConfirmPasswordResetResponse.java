package bitwalking.bitwalking.server.responses;

/**
 * Created by Marcus on 6/21/16.
 */
public class ConfirmPasswordResetResponse extends BasicServerResponse {

    public ConfirmPasswordResetResponse(BasicServerResponse base) {
        super(base);
    }

    public ResetInfo getAnswer() {
        return getPayload(ResetInfo.class);
    }

    public class ResetInfo {
        boolean passwordReset;
    }
}