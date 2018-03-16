package bitwalking.bitwalking.server.responses;

import java.math.BigDecimal;

/**
 * Created by Marcus on 7/16/16.
 */
public class UserTodayResponse extends BasicServerResponse {

    public UserTodayResponse(BasicServerResponse base) {
        super(base);
    }

    public MiningInfo getMiningInfo() {
        return getPayload(MiningInfo.class);
    }

    public class MiningInfo {
        public BigDecimal mining;
    }
}