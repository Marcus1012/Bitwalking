package bitwalking.bitwalking.events.eventInfo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import bitwalking.bitwalking.events.EventsGlobals;
import bitwalking.bitwalking.R;
import bitwalking.bitwalking.server.responses.EventsListResponse;

/**
 * Created by Marcus on 9/29/16.
 */
public class EventInfoSectionFragment extends Fragment {
    protected EventsListResponse.EventInfo _eventInfo;
    protected EventsListResponse.EventSection _section;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View rootView = inflater.inflate(R.layout.event_info_text_section, container, false);

        // Init event info
        String eventInfoJson = getArguments().getString(EventsGlobals.EVENTS_INFO_JSON_KEY, "");

        _eventInfo = EventsListResponse.getEventGsonParser().fromJson(eventInfoJson, EventsListResponse.EventInfo.class);

        String sectionName = getArguments().getString(EventsGlobals.EVENTS_SECTION_TITLE_KEY, "");
        _section = EventsGlobals.getSectionByName(_eventInfo, sectionName);

        setJoinButton(rootView, _eventInfo);

        return rootView;
    }

    private void setJoinButton(View rootView, EventsListResponse.EventInfo eventInfo) {
        if (null != eventInfo) {
            Button joinButton = (Button)rootView.findViewById(R.id.event_info_join_button);

            switch (eventInfo.status) {
                case ongoing:
                case pending: {
                    if (null == _eventInfo.me.joinTimestamp) {
                        joinButton.setText("JOIN EVENT");
                        joinButton.setTextColor(0xFF30BECA);
                        joinButton.setBackgroundResource(R.drawable.event_join_button_empty_background);
                    }
                    else {
                        joinButton.setText("ACTIVE EVENT");
                        joinButton.setTextColor(0xFFFFFFFF);
                        joinButton.setBackgroundResource(R.drawable.event_join_button_filled_background);
                    }
                    break;
                }
                case finished:
                case completed: {
                    joinButton.setText("EVENT DATA");
                    joinButton.setTextColor(0xFFFFFFFF);
                    joinButton.setBackgroundResource(R.drawable.event_join_button_filled_background);
                    break;
                }
            }
        }
    }
}