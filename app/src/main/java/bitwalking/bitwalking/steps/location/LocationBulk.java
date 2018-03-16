package bitwalking.bitwalking.steps.location;

import android.location.Location;

import bitwalking.bitwalking.steps.steps_info.TimePeriod;

/**
 * Created by Marcus on 2/1/16.
 */
public class LocationBulk extends Location {
    TimePeriod _period;

    public LocationBulk(Location location, long startTime, long endTime) {
        super(location);

        _period = new TimePeriod(startTime, endTime);
    }

    public long getStartTime() { return _period.getStartTime(); }
    public long getEndTime() { return _period.getEndTime(); }
    public long getDuration() { return _period.getDuration(); }
}
