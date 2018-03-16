package bitwalking.bitwalking.server;

import bitwalking.bitwalking.server.responses.BasicServerResponse;

/**
 * Created by Marcus on 6/20/16.
 */
public interface OnServerResponse {
    void onBasicResponse(BasicServerResponse response, int id);
}

