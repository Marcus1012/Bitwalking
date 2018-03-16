package bitwalking.bitwalking.events.eventInfo;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.google.gson.Gson;

import bitwalking.bitwalking.events.EventsGlobals;
import bitwalking.bitwalking.BitwalkingApp;
import bitwalking.bitwalking.server.responses.EventsListResponse;

/**
 * Created by Marcus on 9/28/16.
 */
public class EventInfoTabPager extends FragmentPagerAdapter {
    EventsListResponse.EventInfo _event;
    Gson _gson;

    public EventInfoTabPager(FragmentManager fm, EventsListResponse.EventInfo event) {
        super(fm);
        _event = event;

        if (null == _event)
            BitwalkingApp.getInstance().trackException(new Exception("Cannot show event info, event is null"));

        _gson = new Gson();
    }

    public String getSectionName(int index) {
        if (index >= _event.extendedInformation.size()) {
            index = 0;
        }

        return _event.extendedInformation.get(index).sectionTitle;
    }

    @Override
    public Fragment getItem(int index) {
        Fragment fragment = null;

        if (index < _event.extendedInformation.size()) {
            EventsListResponse.EventSection section = _event.extendedInformation.get(index);

            Bundle infoArgs = new Bundle();
            infoArgs.putString(EventsGlobals.EVENTS_SECTION_TITLE_KEY, section.sectionTitle);
            infoArgs.putString(EventsGlobals.EVENTS_INFO_JSON_KEY, _gson.toJson(_event));

            switch (section.sectionType) {
                case text: { // about
                    fragment = new EventInfoTextFragment();
                    break;
                }
                case date_location: { // date & location
                    fragment = new EventInfoDateLocationFragment();
                    break;
                }
            }

            fragment.setArguments(infoArgs);
        }

        return fragment;
    }

    @Override
    public int getCount() {
        return (null == _event) ? 0 : _event.extendedInformation.size();
    }

}