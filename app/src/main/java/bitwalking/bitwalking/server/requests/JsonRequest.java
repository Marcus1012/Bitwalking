package bitwalking.bitwalking.server.requests;

import com.google.gson.GsonBuilder;

import bitwalking.bitwalking.BitwalkingApp;

/**
 * Created by Marcus on 6/21/16.
 */
public class JsonRequest extends BasicServerRequest {
    @Override
    public byte[] getBody() {
        String json = new GsonBuilder().setPrettyPrinting().create().toJson(this);
        byte[] bytes = null;

        try {
            bytes = json.getBytes("UTF8");
        }
        catch (Exception e) {
            BitwalkingApp.getInstance().trackException(new Exception("Failed to convert JsonRequest", e));
        }

        return bytes;//Globals.stringToBytes(json);
    }
}
