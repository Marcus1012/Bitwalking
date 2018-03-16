package bitwalking.bitwalking.steps.steps_info;

import android.location.Location;

/**
 * Created by Marcus on 5/25/16.
 */
public class StepsLocationExtra {
    double lat;
    double lon;
    float acc;
    float speed;
    String provider;
    float distance;

    public StepsLocationExtra(Location l, int distance) {
        lat = l.getLatitude();
        lon = l.getLongitude();
        acc = l.getAccuracy();
        speed = l.getSpeed();
        provider = l.getProvider();
        this.distance = distance;
    }

    public StepsLocationExtra(Location location) {
        this(location, 0);
    }

    public void calcSetDistance(Location other) {
        Location loc = new Location(provider);
        loc.setLatitude(lat);
        loc.setLongitude(lon);
        this.distance = loc.distanceTo(other);
    }

    public void calcSetDistance(StepsLocationExtra other) {
        Location loc = new Location(other.provider);
        loc.setLatitude(other.lat);
        loc.setLongitude(other.lon);
        calcSetDistance(loc);
    }
}
