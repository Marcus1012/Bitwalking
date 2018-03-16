package bitwalking.bitwalking.server.requests;

import android.graphics.Bitmap;

import bitwalking.bitwalking.util.Globals;

/**
 * Created by Marcus on 6/22/16.
 */
public class UpdateUserAvatar extends DataRequest {
    public UpdateUserAvatar(Bitmap avatar) {
        super(DataRequest.CONTENT_TYPE_IMAGE, "image", "image.png", Globals.bitmapToBytes(avatar));
    }
}
