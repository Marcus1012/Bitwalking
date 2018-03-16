package bitwalking.bitwalking.server.responses;

import bitwalking.bitwalking.user_info.BalanceInfo;

/**
 * Created by Marcus on 6/21/16.
 */
public class GetUserBalanceResponse extends BasicServerResponse {

    public GetUserBalanceResponse(BasicServerResponse base) {
        super(base);
    }

    public BalanceInfo getBalance() {
        return getPayload(BalanceInfo.class);
    }
}