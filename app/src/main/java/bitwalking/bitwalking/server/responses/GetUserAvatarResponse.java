package bitwalking.bitwalking.server.responses;

/**
 * Created by Marcus on 6/20/16.
 */
public class GetUserAvatarResponse extends BasicServerResponse {

    public GetUserAvatarResponse(BasicServerResponse base) {
        super(base);
    }

    public AvatarInfo getAvatarInfo() {
        return getPayload(AvatarInfo.class);
    }

    public class AvatarInfo {
        public String updateTimestamp;
        public String creationTimestamp;
        public String imageUri;
    }
}
