package bitwalking.bitwalking.server.requests;

/**
 * Created by Marcus on 6/22/16.
 */
public class UpdateUserVote extends JsonRequest {
    private String itemId;

    public UpdateUserVote(String itemId) {
        this.itemId = itemId;
    }
}
