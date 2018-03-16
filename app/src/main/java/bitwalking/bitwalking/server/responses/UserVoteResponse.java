package bitwalking.bitwalking.server.responses;

import bitwalking.bitwalking.server.responses.BasicServerResponse;

/**
 * Created by Marcus on 6/21/16.
 */
public class UserVoteResponse extends BasicServerResponse {

    public UserVoteResponse(BasicServerResponse base) {
        super(base);
    }

    public VoteInfo getVoteInfo() {
        return getPayload(VoteInfo.class);
    }

    public class VoteInfo {
        public String itemId;
        public String updateTimestamp;
        public String creationTimestamp;
    }
}