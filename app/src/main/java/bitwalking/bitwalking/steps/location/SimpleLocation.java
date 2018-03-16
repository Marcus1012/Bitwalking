package bitwalking.bitwalking.steps.location;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import bitwalking.bitwalking.BitwalkingApp;
import bitwalking.bitwalking.ServicePreferences;
import bitwalking.bitwalking.steps.steps_info.StepsBulk;
import bitwalking.bitwalking.steps.steps_validation.StepsValidationParameters;

/**
 * Created by Marcus on 12/10/16.
 * This class will maintain a list of last known locations and their timestamps,
 * so it will be able to add locations to steps bulks future to come.
 * In order that last known location to be updated there is a trigger to refresh device's location.
 * The trigger is {@link StepsValidationParameters#MIN_STEPS_TO_TRIGGER_LOCATION} steps and
 * the location update interval is {@link SimpleLocation#LOCATION_UPDATE_INTERVAL}.
 * Location update request is stopped after there are no steps for {@link SimpleLocation#MAX_TIME_FOR_LOCATION_UPDATE_AFTER_STEPS}.
 */

public class SimpleLocation extends LocationSource {

    private static int MAX_LOCATION_HISTORY_ITEMS               = 200;
    private static int LOCATION_REQ_INTERVAL_MS                 = 2 * 60 * 1000;
    private static int LOCATION_UPDATE_INTERVAL_MS              = 2 * 60 * 1000;
    private static int LOCATION_UPDATE_INTERVAL                 = 20000;
    private static int MAX_TIME_FOR_LOCATION_UPDATE_AFTER_STEPS = 2 * 60 * 1000; // 2 minutes
    private ArrayList<LocationSample> _locationHistory;
    private LocationSample _lastAddedLocation = null;
    private boolean _running = false;

    private volatile ScheduledFuture<?> _locationFuture;
    private ScheduledThreadPoolExecutor _periodicExecutor;

    public SimpleLocation(Context context) {
        super(context);
        _periodicExecutor = new ScheduledThreadPoolExecutor(1);
        _locationHistory = new ArrayList<>();
    }

    private LocationListener _locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            checkIfNeedToUpdateLocation();
            if (null != _lastAddedLocation) {
                if (GoogleBestLocationCompare.isBetterLocation(location, _lastAddedLocation.location)) {
                    _lastAddedLocation = new LocationSample(location, new Date().getTime());
                    _locationHistory.add(_lastAddedLocation);
                }
            }
            else {
                _lastAddedLocation = new LocationSample(location, new Date().getTime());
                _locationHistory.add(_lastAddedLocation);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    private Runnable _getLocationSampleRunnable = new Runnable() {
        @Override
        public void run() {
            if (_running) {
                try {
                    addNextLocationSample();
                } catch (Exception e) {
                    BitwalkingApp.getInstance().trackException("failed adding location sample", e);
                } finally {
                }
            }
        }
    };

    private void addNextLocationSample() {
        Location currentLocation = getLocation();

        if (null != currentLocation) {
            // Check if the data is new
            if (null == _lastAddedLocation || !_lastAddedLocation.equals(currentLocation)) {
                synchronized (_locationHistory) {
                    // Check if we have full capacity
                    if (_locationHistory.size() >= MAX_LOCATION_HISTORY_ITEMS)
                        // Remove oldest one
                        _locationHistory.remove(0);

                    // Add new and update latest
                    _lastAddedLocation = new LocationSample(currentLocation, new Date().getTime());
                    _locationHistory.add(_lastAddedLocation);
                }
            }
        }
        else {
            // <------- Log
        }
    }

    private Location getLocation() {
        Location lastKnownLocation = null;

        // Check location permissions - permission request handled in MainActivity
        if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(_context, Manifest.permission.ACCESS_FINE_LOCATION)) {
            LocationManager locationManager = (LocationManager) _context.getSystemService(Context.LOCATION_SERVICE);
            if (locationManager != null) {
                // Get from GPS
                lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastKnownLocation == null) {
                    // Get from network
                    lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    if (lastKnownLocation == null) {
                        // Get alternative location
                        lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
                    }
                }
            }
        }

        return lastKnownLocation;
    }

    @Override
    public void init(Object extra) {

    }

    @Override
    public void start() {
        _locationFuture = _periodicExecutor.scheduleAtFixedRate(
                _getLocationSampleRunnable, 1000, LOCATION_REQ_INTERVAL_MS, TimeUnit.MILLISECONDS);
        _running = true;
    }

    @Override
    public void stop() {
        _running = false;
        if (null != _locationFuture)
            _locationFuture.cancel(false);
    }

    ServicePreferences _servicePrefs;
    private ServicePreferences getServicePrefs() {
        if (null == _servicePrefs) {
            _servicePrefs = new ServicePreferences(_context);
        }

        return _servicePrefs;
    }

    private void checkIfNeedToUpdateLocation() {
        long lastStepsTime = getServicePrefs().getLocationTriggerTime();
        LocationManager locationManager = (LocationManager) _context.getSystemService(Context.LOCATION_SERVICE);
        if (lastStepsTime + MAX_TIME_FOR_LOCATION_UPDATE_AFTER_STEPS > new Date().getTime()) {
            // Check location permissions - permission request handled in MainActivity
            if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(_context, Manifest.permission.ACCESS_FINE_LOCATION)) {
                if (locationManager != null) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_UPDATE_INTERVAL, 0, _locationListener);
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, LOCATION_UPDATE_INTERVAL, 0, _locationListener);
                }
            }
        }
        else {
            locationManager.removeUpdates(_locationListener);
        }
    }

    @Override
    public void addLocationToSteps(StepsBulk bulk, StepsWithLocationListener listener) {
        checkIfNeedToUpdateLocation();

        if (null != listener) {
            ArrayList<StepsBulk> singleBulk = new ArrayList<>();
            singleBulk.add(bulk);
            listener.onStepsLocationReady(singleBulk);
        }
    }

    @Override
    public Location getLastKnownLocation() {
        Location lastKnownLocation = getLocation();

        if (null == lastKnownLocation && _locationHistory.size() > 0) {
            lastKnownLocation = _locationHistory.get(_locationHistory.size() - 1).location;
        }

        return lastKnownLocation;
    }

    private class LocationSample {

        public LocationSample(Location location, long timestamp) {
            this.location = location;
            this.timestamp = timestamp;
        }

        Location location;
        long timestamp;

        @Override
        public boolean equals(Object o) {
            Location otherLocation = null;
            if (o instanceof LocationSample)
                otherLocation = ((LocationSample) o).location;
            else if (o instanceof Location)
                otherLocation = (Location) o;

            if (null == otherLocation)
                return false;

            return  otherLocation.getLongitude() == location.getLongitude() &&
                    otherLocation.getLatitude() == location.getLatitude();
        }
    }
}
