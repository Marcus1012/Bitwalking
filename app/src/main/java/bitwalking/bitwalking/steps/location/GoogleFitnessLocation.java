package bitwalking.bitwalking.steps.location;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessStatusCodes;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;
import com.google.android.gms.location.LocationServices;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import bitwalking.bitwalking.ServicePreferences;
import bitwalking.bitwalking.util.Globals;
import bitwalking.bitwalking.steps.steps_info.StepsBulk;
import bitwalking.bitwalking.steps.steps_info.StepsLocationExtra;
import bitwalking.bitwalking.steps.steps_validation.StepsValidationParameters;
import bitwalking.bitwalking.util.Logger;

/**
 * Created by Marcus on 2/1/16.
 */
public class GoogleFitnessLocation extends LocationSource {

    final static String TAG = GoogleFitnessLocation.class.getSimpleName();

    GoogleApiClient _client;
    boolean _running;
    private ServicePreferences _servicePrefs;

    public GoogleFitnessLocation(Context context) {
        super(context);
    }

    @Override
    public void init(Object extra) {
        if (null != extra)
            _client = (GoogleApiClient)extra;
    }

    @Override
    public void start() {
        if (null != _client && _client.isConnected()) {
            Fitness.RecordingApi.subscribe(_client, DataType.TYPE_LOCATION_SAMPLE)
                    .setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(Status status) {
                            if (status.isSuccess()) {
                                if (status.getStatusCode() == FitnessStatusCodes.SUCCESS_ALREADY_SUBSCRIBED) {
                                    Logger.instance().Log(Logger.INFO, TAG, String.format("[%s]: SUCCESS_ALREADY_SUBSCRIBED.", "LocationTrack"));
                                } else {
                                    Logger.instance().Log(Logger.INFO, TAG, String.format("[%s]: Successfully subscribed!", "LocationTrack"));
                                }
                            } else {
                                Logger.instance().Log(Logger.INFO, TAG, String.format("[%s]: There was a problem subscribing.", "LocationTrack"));
                            }
                        }
                    });
            _running = true;

            addDebugLog("start recording location");
        }
        else {
            addDebugLog("failed to startRecording, _client is null");
        }
    }

    @Override
    public void stop() {
        _running = false;

        if (null != _client && _client.isConnected()) {
            Fitness.RecordingApi.unsubscribe(_client, DataType.TYPE_LOCATION_SAMPLE);
        }
    }

    @Override
    public void addLocationToSteps(StepsBulk bulk, StepsWithLocationListener listener) {
        new ReadLocationTrackTask(listener).execute(bulk);
    }

    @Override
    public Location getLastKnownLocation() {
        Location location = null;
        if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(_context, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            location = LocationServices.FusedLocationApi.getLastLocation(_client);
        }

        return location;
    }

    private class ReadLocationTrackTask extends AsyncTask<StepsBulk, Void, ArrayList<StepsBulk>> {

        StepsWithLocationListener _listener;
        public ReadLocationTrackTask(StepsWithLocationListener listener) {
            _listener = listener;
        }

        protected ArrayList<StepsBulk> doInBackground(StepsBulk... args) {
            StepsBulk bulk = args[0];
            ArrayList<LocationBulk> locations = getLocations(bulk.getStartTime(), bulk.getEndTime());

            return _addLocationToSteps(bulk, locations);
        }

        protected void onPostExecute(ArrayList<StepsBulk> stepsWithLocations) {
            addDebugLog(String.format("done adding location, %d bulks", stepsWithLocations.size()));
            if (null != _listener)
                _listener.onStepsLocationReady(stepsWithLocations);
        }

        private ArrayList<StepsBulk> _addLocationToSteps(StepsBulk bulk, ArrayList<LocationBulk> locations) {
            ArrayList<StepsBulk> finalSteps = new ArrayList<>();
            long stepsBefore = bulk.getTotalSteps(), stepsAfter = 0;

            // handle no locations
            if (null == locations || 0 == locations.size()) {
                finalSteps.add(bulk);
            }
            // Handle one location
            else if (1 == locations.size()) {
                bulk.setLocation(new StepsLocationExtra(locations.get(0)));
                finalSteps.add(bulk);
            }
            // More than one location for the bulk - should be divided
            else {
                addDebugLog(String.format("Steps bulk has %d locations", locations.size()));
                Logger.instance().Log(Logger.DEBUG, TAG, "before: " + bulk);

                for (int i = 0; i < locations.size() - 1; ++i) {
                    StepsBulk subBulk = bulk.splitBulk(locations.get(i).getEndTime(), StepsValidationParameters.MIN_STEPS_FOR_WALK);
                    if (null != subBulk) {
                        subBulk.setLocation(new StepsLocationExtra(locations.get(i)));
                        finalSteps.add(subBulk);
                    }
                }

                // Add last one
                bulk.setLocation(new StepsLocationExtra(locations.get(locations.size() - 1)));
                finalSteps.add(bulk);

                Logger.instance().Log(Logger.DEBUG, TAG, "after: ");
                for (StepsBulk s : finalSteps) {
                    Logger.instance().Log(Logger.DEBUG, TAG, s.toString());
                }
            }

            for (StepsBulk b : finalSteps) {
                stepsAfter += b.getTotalSteps();
            }

            if (stepsAfter != stepsBefore) {
                addDebugLog(String.format("steps lost during location add!!! [before=%d][after=%d]", stepsBefore, stepsAfter));
            }

            return finalSteps;
        }

        private ArrayList<LocationBulk> getLocations(long startTime, long endTime) {
            DateFormat df = Globals.getFullDateFormat();
            Logger.instance().Log(Logger.DEBUG, TAG, String.format("get locations between [%d]-[%d]=[%d]ms", startTime, endTime, endTime - startTime));
            Logger.instance().Log(Logger.DEBUG, TAG, String.format("get locations between [%s]-[%s]", df.format(new Date(startTime)), df.format(new Date(endTime))));
            ArrayList<LocationBulk> locations = null;

            if (null != _client) {
                final PendingResult<DataReadResult> results = Fitness.HistoryApi.readData(
                        _client,
                        new DataReadRequest.Builder()
                                .read(DataType.TYPE_LOCATION_SAMPLE)
                                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                                .build());

                DataReadResult readDataResult = results.await();
//                steps = countDataSetSteps(readDataResult.getDataSet(DataType.TYPE_STEP_COUNT_DELTA));
                DataSet set = readDataResult.getDataSet(DataType.TYPE_LOCATION_SAMPLE);
                if (set.getDataPoints().size() > 0) {
                    locations = new ArrayList<>();
                    for (DataPoint dp : set.getDataPoints()) {

                        // Add location bulk
                        locations.add(createLocationBulk(dp));
                    }

                    Logger.instance().Log(Logger.DEBUG, TAG, String.format("got %d locations", locations.size()));
                }

            }
            else {
                addDebugLog("client is null, cannot get locations");
            }

            return locations;
        }

        private LocationBulk createLocationBulk(DataPoint dp) {
            // Create location
            Location location = new Location(dp.getDataSource().getStreamName());
            // Init location data
            location.setLatitude(dp.getValue(Field.FIELD_LATITUDE).asFloat());
            location.setLongitude(dp.getValue(Field.FIELD_LONGITUDE).asFloat());
            location.setAltitude(dp.getValue(Field.FIELD_ALTITUDE).asFloat());
            location.setAccuracy(dp.getValue(Field.FIELD_ACCURACY).asFloat()); // meters

            return new LocationBulk(
                    location,
                    dp.getStartTime(TimeUnit.MILLISECONDS),
                    dp.getEndTime(TimeUnit.MILLISECONDS));
        }
    }

    private ServicePreferences getServicePrefs() {
        if (null == _servicePrefs)
            _servicePrefs = new ServicePreferences(_context);

        return _servicePrefs;
    }

    private void addDebugLog(String log) {
        log = "Location: " + log;
        if (Globals.LOG_TO_FILE) {
            getServicePrefs().addLog(log);
        }

        Logger.instance().Log(Logger.DEBUG, TAG, log);
    }
}
