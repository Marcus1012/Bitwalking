package bitwalking.bitwalking.server.responses;

import bitwalking.bitwalking.user_info.MeInfo;

/**
 * Created by Marcus on 6/20/16.
 */
public class GetMeResponse extends BasicServerResponse {

    public GetMeResponse(BasicServerResponse base) {
        super(base);
    }

    public MeInfo getMe() {
        return getPayload(MeInfo.class);
    }
}