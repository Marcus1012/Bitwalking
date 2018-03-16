package bitwalking.bitwalking.mvi.events;

import java.util.List;

import bitwalking.bitwalking.server.responses.EventsListResponse;

/**
 * Created by alexey on 25.08.17.
 */

public class StateEvents {
    private final List<EventsListResponse.EventInfo> events;
    private final boolean loading;
    private final Throwable error;

    public StateEvents(boolean loading,List<EventsListResponse.EventInfo> events,Throwable error){
        this.loading = loading;
        this.error = error;
        this.events = events;
    }


    public List<EventsListResponse.EventInfo> getEvents() {
        return events;
    }

    public boolean isLoading() {
        return loading;
    }

    public Throwable getError() {
        return error;
    }


}
