package bitwalking.bitwalking.events.eventInfo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Date;

import bitwalking.bitwalking.events.EventsGlobals;
import bitwalking.bitwalking.BitwalkingApp;
import bitwalking.bitwalking.R;
import bitwalking.bitwalking.server.responses.EventsListResponse;
import bitwalking.bitwalking.util.Globals;

/**
 * Created by Marcus on 9/28/16.
 */
public class EventInfoDateLocationFragment extends EventInfoSectionFragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container, savedInstanceState);

        try {
            fillText(rootView, _eventInfo);
        }
        catch (Exception e) {
            BitwalkingApp.getInstance().trackException("fillText failed", e);
        }

        return rootView;
    }

    private void fillText(View rootView, EventsListResponse.EventInfo eventInfo) {
        if (null != eventInfo) {
            switch (eventInfo.status) {
                case pending: {
                    fillEventPendingDate(rootView);
                    break;
                }
                case ongoing: {
                    fillEventOngoingDate(rootView);
                    break;
                }
                case finished:
                case completed: {
                    fillEventCompleteDate(rootView);
                    break;
                }
            }
        }

        // set links clickable inside TextView
        TextView sectionText = (TextView)rootView.findViewById(R.id.event_info_text_section_text);
        sectionText.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void fillEventPendingDate(View rootView) {
        // Give icon
        rootView.findViewById(R.id.event_info_section_give_icon).setVisibility(View.GONE);

        // Sub title
        if (null != _section) {
            TextView subTitle = (TextView)rootView.findViewById(R.id.event_info_text_section_sub_title);
            subTitle.setText(_section.sectionTitle);
        }

        // Text
        String text = "";

        // Start time
        text += String.format("Start: %s\n", convertUtcDateToEventDisplay(_eventInfo.startTime));

        // End time
        text += String.format("Ends: %s\n\n", convertUtcDateToEventDisplay(_eventInfo.endTime));

        // Location
        boolean addIn = true;
        String location = "";

        for (EventsListResponse.EventInfo.EventGeography eg : _eventInfo.geography) {
            if (eg.type == EventsListResponse.EventGeographyType.worldwide) {
                location = "";
                break;
            }
            else {
                if (addIn) {
                    addIn = false;
                    location += "in ";
                }
                else {
                    location += ", ";
                }

                location += eg.name;
            }
        }

        text += String.format("This event is open to all users %s.", location);

        TextView sectionText = (TextView)rootView.findViewById(R.id.event_info_text_section_text);
        sectionText.setText(text);
    }

    private void fillEventOngoingDate(View rootView) {
        fillEventPendingDate(rootView);
    }

    private void fillEventCompleteDate(View rootView) {
        // Give icon
        rootView.findViewById(R.id.event_info_section_give_icon).setVisibility(View.VISIBLE);

        // Sub title
        TextView subTitle = (TextView)rootView.findViewById(R.id.event_info_text_section_sub_title);
        subTitle.setText("THIS EVENT ENDED!");

        // Text
        String text = "";

        // User donation
        text += String.format("You donated: <b>%s W$</b><br>", _eventInfo.me.donation);

        // All donation
        text += String.format("Bitwalking community donated: <b>%s W$</b><br><br>", _eventInfo.community.donation);

        TextView sectionText = (TextView)rootView.findViewById(R.id.event_info_text_section_text);
//        sectionText.setText(text);
        sectionText.setText(Html.fromHtml(text));
    }

    private String convertUtcDateToEventDisplay(String fromUtc) {
        String eventDate = fromUtc;

        try {
            Date fromDate = Globals.getUTCDateFormat().parse(fromUtc);
            eventDate = EventsGlobals.getEventDateDisplayFormat().format(fromDate);
        }
        catch (Exception e) {
            BitwalkingApp.getInstance().trackException(e);
        }

        return eventDate;
    }
}
