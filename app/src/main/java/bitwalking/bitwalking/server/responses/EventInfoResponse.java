package bitwalking.bitwalking.server.responses;

import com.google.gson.GsonBuilder;

import bitwalking.bitwalking.server.api.MySuperBoolean;

/**
 * Created by Marcus on 9/28/16.
 */
public class EventInfoResponse extends BasicServerResponse {
    public EventInfoResponse(BasicServerResponse base) {
        super(base);
    }

    public EventsListResponse.EventInfo getEventInfo() {
        return getPayload(EventsListResponse.EventInfo.class, EventsListResponse.getEventGsonParser());
    }
}
