package bitwalking.bitwalking.server.responses;

/**
 * Created by Marcus on 6/21/16.
 */
public class UpdateUserAvatarResponse extends BasicServerResponse {

    public UpdateUserAvatarResponse(BasicServerResponse base) {
        super(base);
    }

    public GetUserAvatarResponse.AvatarInfo getAvatarInfo() {
        return getPayload(GetUserAvatarResponse.AvatarInfo.class);
    }
}
