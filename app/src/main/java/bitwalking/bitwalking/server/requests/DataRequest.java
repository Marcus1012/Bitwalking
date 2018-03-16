package bitwalking.bitwalking.server.requests;

import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * Created by Marcus on 6/21/16.
 */
public class DataRequest extends BasicServerRequest {

    //region Constants

    public static final String BOUNDARY = "bitwalkingboundary-001";

    public static final String CONTENT_TYPE_IMAGE = "image/png";
    public static final String CONTENT_TYPE_PLAIN_TEXT = "text/plain";

    //endregion

    private byte[] data;
    private String _contentType;
    private String _contentDispositionName;
    private String _fileName;
    private static final String CRLF = "\r\n";

    public DataRequest(String contentType, String contentDispositionName, String fileName, byte[] data) {
        this._contentType = contentType;
        this._contentDispositionName = contentDispositionName;
        this._fileName = fileName;
        this.data = data.clone();
    }

    @Override
    public byte[] getBody() {
        String top = String.format(
                "--%s" + CRLF +
                "Content-Disposition: form-data; name=\"" + _contentDispositionName + "\"; filename=\"" + _fileName + "\"" + CRLF +
                "Content-Type: " + _contentType + CRLF +
                "Content-Transfer-Encoding: binary" + CRLF + CRLF, BOUNDARY);

        String bottom = String.format(CRLF + "--%s--" +  CRLF, BOUNDARY);

        byte[] bodyData = new byte[top.length() + data.length + bottom.length()];

        // Copy top
        int index = 0;
        for (; index < top.length(); ++index)
            bodyData[index] = (byte)top.charAt(index);
        // Copy binary data
        System.arraycopy(data, 0, bodyData, index, data.length);
        index += data.length;
        // Copy end
        for (int i = 0; i < bottom.length(); ++index, ++i)
            bodyData[index] = (byte)bottom.charAt(i);

        return bodyData;
    }
}
