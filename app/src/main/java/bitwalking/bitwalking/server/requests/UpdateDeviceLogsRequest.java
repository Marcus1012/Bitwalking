package bitwalking.bitwalking.server.requests;

import android.graphics.Bitmap;

import bitwalking.bitwalking.util.Globals;

/**
 * Created by Marcus on 10/11/16.
 */
public class UpdateDeviceLogsRequest extends DataRequest {
    public UpdateDeviceLogsRequest(byte[] logs) {
        super(DataRequest.CONTENT_TYPE_PLAIN_TEXT, "logfile", "file.txt", logs);
    }
}

