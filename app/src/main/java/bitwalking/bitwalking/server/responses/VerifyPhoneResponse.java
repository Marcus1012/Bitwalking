package bitwalking.bitwalking.server.responses;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Marcus on 6/28/16.
 */
public class VerifyPhoneResponse extends BasicServerResponse {

    public VerifyPhoneResponse(BasicServerResponse base) {
        super(base);
    }

    public SessionTokenInfo getSessionInfo() {
        return getPayload(SessionTokenInfo.class);
    }

    public class SessionTokenInfo {
        @SerializedName("user-secret")
        public String userSecret;
    }
}

