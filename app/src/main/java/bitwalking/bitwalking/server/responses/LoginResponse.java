package bitwalking.bitwalking.server.responses;

import bitwalking.bitwalking.user_info.UserInfo;

/**
 * Created by Marcus on 6/21/16.
 */
public class LoginResponse extends BasicServerResponse {

    public LoginResponse(BasicServerResponse base) {
        super(base);
    }

    public UserInfo getInfo() {
        return getPayload(UserInfo.class);
    }
}