package bitwalking.bitwalking.server.responses;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import bitwalking.bitwalking.server.api.MySuperBoolean;
import bitwalking.bitwalking.steps.steps_info.StepsBulk;
import bitwalking.bitwalking.util.Logger;

/**
 * Created by Marcus on 9/26/16.
 */
public class EventsListResponse extends BasicServerResponse {

    public EventsListResponse(BasicServerResponse base) {
        super(base);
    }

    public ArrayList<EventInfo> getEvents() {
        return getPayload(new TypeToken<List<EventInfo>>(){}.getType(), getEventGsonParser());
    }

    public enum EventStatus {
        pending,
        ongoing,
        completed,
        finished
    }

    public enum EventGeographyType {
        worldwide,
        country,
        city
    }

    public enum EventInfoSectionType {
        text,
        date_location
    }

    public class EventInfo implements Comparable<EventInfo>{
        public String id;
        public EventLinks links;
        public EventImages images;
        public ArrayList<String> categories;
        public String title;
        public EventStatus status;
        public EventShare share;
        public MySuperBoolean featured;
        public String creationTimestamp;
        public String updateTimestamp;
        public String startTime;
        public String endTime;
        public ArrayList<EventGeography> geography;
        public EventCommunity community;
        public EventMe me;
        public ArrayList<EventSection> extendedInformation;

        public class EventLinks {
            public String externalUri;
            public String sponsorUri;
        }

        public class EventImages {
            public String banner;
            public String list;
            public String sponsor;
        }

        public class EventGeography {
            public EventGeographyType type;
            public String name;
            public String code;
        }

        public class EventShare {
            public String title;
            public String text;
        }

        public class EventCommunity {
            public BigDecimal donation;
        }

        public class EventMe {
            public String joinTimestamp;
            public BigDecimal donation;
        }

        public String getBannerImageName() {
            return String.format("%s_banner", id);
        }

        public String getListImageName() {
            return String.format("%s_list", id);
        }

        public String getSponsrImageName() {
            return String.format("%s_sponsor", id);
        }

        @Override
        public int compareTo(EventInfo another) {
            int result = startTime.compareTo(another.startTime);

            return result;
        }
    }

    public static Gson getEventGsonParser() {
        final GsonBuilder gsonBuilder = new GsonBuilder();
        return gsonBuilder.registerTypeAdapter(MySuperBoolean.class, new MySuperBoolean.MySuperBooleanDeserializer()).create();
    }

    public class EventSection {
        public EventInfoSectionType sectionType;
        public String sectionTitle;
        public String sectionSubTitle;
        public String sectionText;
    }
}
