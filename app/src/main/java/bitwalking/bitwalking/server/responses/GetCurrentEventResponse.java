package bitwalking.bitwalking.server.responses;

import bitwalking.bitwalking.user_info.CurrentEventInfo;

/**
 * Created by Marcus on 9/30/16.
 */
public class GetCurrentEventResponse extends BasicServerResponse {

    public GetCurrentEventResponse(BasicServerResponse base) {
        super(base);
    }

    public CurrentEventInfo getCurrentEventInfo() {
        UserCurrentEvent userEvent = getPayload(UserCurrentEvent.class);

        return (null == userEvent) ? null : userEvent.currentEventObject;
    }

    public class UserCurrentEvent {
        CurrentEventInfo currentEventObject;
    }
}