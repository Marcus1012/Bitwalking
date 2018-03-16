package bitwalking.bitwalking.server.responses;

/**
 * Created by Marcus on 6/21/16.
 */
public class InviteBusinessResponse extends BasicServerResponse {

    public InviteBusinessResponse(BasicServerResponse base) {
        super(base);
    }

    public InviteInfo getAnswer() {
        return getPayload(InviteInfo.class);
    }

    public class InviteInfo {
        boolean invited;
    }
}