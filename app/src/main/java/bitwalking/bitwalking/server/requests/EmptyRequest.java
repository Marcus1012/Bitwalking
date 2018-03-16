package bitwalking.bitwalking.server.requests;

/**
 * Created by Marcus on 6/21/16.
 */
public class EmptyRequest extends BasicServerRequest {
    private EmptyRequest() {}

    private static EmptyRequest _instance;

    public static EmptyRequest instance() {
        if (null == _instance)
            _instance = new EmptyRequest();

        return _instance;
    }

    @Override
    public byte[] getBody() {
        return null;
    }
}
