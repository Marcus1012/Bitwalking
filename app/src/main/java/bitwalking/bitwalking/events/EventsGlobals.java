package bitwalking.bitwalking.events;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

import bitwalking.bitwalking.server.responses.EventsListResponse;

/**
 * Created by Marcus on 9/28/16.
 */
public class EventsGlobals {
    public static final String EVENTS_IMAGES_FOLDER = "events_images";

    public static final String EVENT_ID_KEY = "Event.ID";
    public static final String EVENTS_INFO_JSON_KEY = "Events.Info.Json";
    public static final String EVENTS_SECTION_TITLE_KEY = "Events.Section.Title";
    public static final String EVENTS_SECTION_SUB_TITLE_KEY = "Events.Section.SubTitle";
    public static final String EVENTS_SECTION_TEXT_KEY = "Events.Section.Text";
    public static final String EVENTS_SECTION_START_DATE_KEY = "Events.Section.StartDate";
    public static final String EVENTS_SECTION_END_DATE_KEY = "Events.Section.EndDate";
    public static final String EVENTS_SECTION_GEOGRAPHY_KEY = "Events.Section.Geography";

    public static DateFormat getEventDateDisplayFormat() {
        return new SimpleDateFormat("hha, EEEE MMM dd, yyyy", Locale.ENGLISH);
    }

    public static EventsListResponse.EventSection getSectionByName(EventsListResponse.EventInfo eventInfo, String sectionName) {
        EventsListResponse.EventSection section = null;
        for (EventsListResponse.EventSection s : eventInfo.extendedInformation) {
            if (s.sectionTitle.contentEquals(sectionName)) {
                section = s;
                break;
            }
        }

        return section;
    }
}
