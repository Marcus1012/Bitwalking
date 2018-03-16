package bitwalking.bitwalking.server.requests;

/**
 * Created by Marcus on 6/28/16.
 */
public class VerifyPhoneRequest extends JsonRequest {
    private String code;
    private String pushToken;

    public VerifyPhoneRequest(String code, String pushToken) {
        this.code = code;
        this.pushToken = pushToken;
    }
}