package bitwalking.bitwalking.server.responses;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.reflect.Type;

import bitwalking.bitwalking.util.Logger;

/**
 * Created by Marcus on 6/20/16.
 */
public class BasicServerResponse {
    private static final String TAG = BasicServerResponse.class.getSimpleName();

    private ResponseHeader header;
    protected Object payload;
    private int responseCode;

    public BasicServerResponse(int responseCode) {
        this.responseCode = responseCode;
    }

    public BasicServerResponse(BasicServerResponse other) {
        header = other.header;
        payload = other.payload;
        responseCode = other.responseCode;
    }

    public void setResponseCode(int responseCode) { this.responseCode = responseCode; }
    public int getResponseCode() { return responseCode; }
    public ResponseHeader getHeader() { return header; }
    public <T> T getPayload(Class<T> cls) {
        Gson gson = new Gson();

        String toJson = gson.toJson(payload);

        T t = null;
        try {
            t = gson.fromJson(toJson, cls);
            t.toString();
        }
        catch (Exception e) {
            Logger.instance().Log(Logger.ERROR, TAG, "failed to parse server response payload");
        }

        return t;
    }

    private <T> T parseJson(Type typeOfT, Gson gson) {
        String toJson = gson.toJson(payload);

        T t = null;
        try {
            t = gson.fromJson(toJson, typeOfT);
            t.toString();
        }
        catch (Exception e) {
            Logger.instance().Log(Logger.ERROR, TAG, "failed to parse server response payload");
            e.printStackTrace();
        }

        return t;
    }

    public <T> T getPayload(Type typeOfT) {
        return parseJson(typeOfT, new Gson());
    }

    public <T> T getPayload(Type typeOfT, Gson gson) {
        return parseJson(typeOfT, gson);
    }

    public static class ResponseHeader {
        public String uri;
        public int status;
        public String code;
        public String message;
        public String developerMessage;
        public String moreInfo;
    }
}
