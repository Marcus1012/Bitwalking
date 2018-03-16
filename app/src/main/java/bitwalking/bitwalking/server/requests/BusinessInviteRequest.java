package bitwalking.bitwalking.server.requests;

import com.google.gson.Gson;

import bitwalking.bitwalking.BitwalkingApp;
import bitwalking.bitwalking.settings.InviteBusinessActivity;

/**
 * Created by Marcus on 6/22/16.
 */
public class BusinessInviteRequest extends BasicServerRequest {
    private InviteBusinessActivity.BusinessInfo business;

    public BusinessInviteRequest(InviteBusinessActivity.BusinessInfo business) {
        this.business = business;
    }

    @Override
    public byte[] getBody() {
        byte[] bytes = null;

        try {
            bytes = new Gson().toJson(business).getBytes("UTF8");
        }
        catch (Exception e) {
            BitwalkingApp.getInstance().trackException(new Exception("Failed to convert BusinessInviteRequest", e));
        }

        return bytes;//Globals.stringToBytes(new Gson().toJson(business));
    }
}
