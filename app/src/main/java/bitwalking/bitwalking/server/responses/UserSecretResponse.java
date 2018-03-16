package bitwalking.bitwalking.server.responses;

/**
 * Created by Marcus on 6/21/16.
 */
public class UserSecretResponse extends BasicServerResponse {

    public UserSecretResponse(BasicServerResponse base) {
        super(base);
    }

    public SecretInfo getSecret() {
        return getPayload(SecretInfo.class);
    }

    public class SecretInfo {
        String userSecret;
    }
}
