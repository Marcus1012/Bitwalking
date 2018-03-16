package bitwalking.bitwalking.steps.steps_info;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.util.Date;

import bitwalking.bitwalking.util.Globals;
import bitwalking.bitwalking.steps.telephony_info.TelephonyData;
import bitwalking.bitwalking.util.Logger;

/**
 * Created by Marcus on 1/28/16.
 */

public class StepsBulk {
    final static String TAG = StepsBulk.class.getSimpleName();

    protected int _totalSteps;
    TimePeriod _period;
    String _source = "not set";
    StepsLocationExtra _location;
    StepsTelephonyExtra _telephony;

    //region Ctor

    public StepsBulk(StepsBulk other) {
        this(other._period._startTime, other._period._endTime, other._totalSteps);

        _source = other._source;
    }

    public StepsBulk(long startTimeMs, long endTimeMs, int steps) {
        _period = new TimePeriod(startTimeMs, endTimeMs);
        _totalSteps = steps;
    }

    //endregion

    //region Steps

    public void addSteps(long endTime, int steps) {
        if (steps >= 0 && endTime > _period.getEndTime()) {
            _totalSteps += steps;
            _period.setEndTime(endTime);
        }
    }

    public StepsBulk splitBulk(long splitTime, int minSteps) {
        // Check if split is valid
        if (splitTime <= _period.getStartTime() ||
            splitTime >= _period.getEndTime()) {

            DateFormat df = Globals.getFullDateFormat();
            Logger.instance().Log(Logger.DEBUG, TAG, String.format("cannot split bulk: [%s], split time = %s", this.toString(), df.format(new Date(splitTime))));
            // invalid - nothing to split, leave bulk as is and return null
            return null;
        }

        // Split
        // Calc left Bulk
        long splitOffset = splitTime - _period.getStartTime();
        int splitSteps = Math.min(
                (int) (((float) splitOffset / (float) _period.getDuration()) * _totalSteps),
                _totalSteps);

        StepsBulk splitedBulk = null;
        if (splitSteps >= minSteps) {
            splitedBulk = new StepsBulk(_period.getStartTime(), splitTime, splitSteps);
            splitedBulk.setSource(_source);
            // Update current bulk (right)
            _period.setStartTime(splitTime);
            _totalSteps -= splitSteps;
        }

        return splitedBulk;
    }

    //endregion

    //region Location

    public void setLocation(StepsLocationExtra location) {
        _location = location;
    }

    public StepsLocationExtra getLocation() { return _location; }

    //endregion

    //region Telephony

    public void setTelephony(StepsTelephonyExtra telephony) {
        _telephony = telephony;
    }

    //endregion

    //region Getters

    public long getStartTime()              { return _period.getStartTime();    }
    public long getEndTime()                { return _period.getEndTime();      }
    public int getTotalSteps()              { return _totalSteps;               }
    public long getDuration()               { return _period.getDuration();     }
    public String getSource()               { return _source;                   }
    public void setSource(String source)    { _source = source;                 }

    //endregion

    @Override
    public String toString() {
        DateFormat df = Globals.getFullDateFormat();
        return String.format("[%s]-[%s]:%d (%s)",
                df.format(new Date(getStartTime())),
                df.format(new Date(getEndTime())),
                _totalSteps,
                _source);
    }

    public String toJsonString() {
        DateFormat df = Globals.getUTCDateFormat();
        String startDateString = df.format(new Date(getStartTime()));
        String endDateString = df.format(new Date(getEndTime()));

        StepsBulkJson stepsJson = new StepsBulkJson(
                startDateString, endDateString, getTotalSteps(),
                getSource());

        if (null != getLocation()) {
            stepsJson.setLocation(getLocation());
        }

        if (null != _telephony) {
            TelephonyData t = new TelephonyData(
                    null,
                    _telephony.telephonyData.mcc,
                    _telephony.telephonyData.mnc,
                    _telephony.telephonyData.cellId,
                    _telephony.telephonyData.lac
            );
            stepsJson.setTelephony(t);
        }

        return new Gson().toJson(stepsJson);
    }

    public static StepsBulk fromJsonString(String json) {
        StepsBulk stepsBulk = null;
        try {
            StepsBulkJson stepsJsonInfo = new Gson().fromJson(json, StepsBulkJson.class);
            DateFormat df = Globals.getUTCDateFormat();
            Date startDate = df.parse(stepsJsonInfo.start);
            Date endDate = df.parse(stepsJsonInfo.end);

            stepsBulk = new StepsBulk(startDate.getTime(), endDate.getTime(), stepsJsonInfo.steps);
            stepsBulk.setSource(stepsJsonInfo.source);

            stepsBulk._location = stepsJsonInfo.location;
            if (null != stepsJsonInfo.network)
                stepsBulk._telephony = new StepsTelephonyExtra(stepsJsonInfo.network.toTelephonyData());
        }
        catch (Exception e) {
            Logger.instance().Log(Logger.ERROR, TAG, "failed to parse json steps bulk");
        }

        return stepsBulk;
    }

    public static class StepsBulkJson {
        String start;
        String end;
        int steps;
        String source;
        StepsLocationExtra location;
        TelephonyDataString network;

        public StepsBulkJson(String start, String end, int steps, String source) {
            this.start = start;
            this.end = end;
            this.steps = steps;
            this.source = source;
        }

        public void setLocation(StepsLocationExtra location) {
            this.location = location;
        }

        public void setTelephony(TelephonyData data) { network = new TelephonyDataString(data); }

        public class TelephonyDataString {
            String mcc;
            String mnc;
            String lac;
            String cellId;

            public TelephonyDataString(String mcc, String mnc, String lac, String cellId) {
                this.mcc = mcc;
                this.mnc = mnc;
                this.lac = lac;
                this.cellId = cellId;
            }

            public TelephonyDataString(TelephonyData telephonyData) {
                this(String.valueOf(telephonyData.mcc), String.valueOf(telephonyData.mnc), String.valueOf(telephonyData.lac), String.valueOf(telephonyData.cellId));
            }

            TelephonyData toTelephonyData() {
                return new TelephonyData(null,
                        Integer.parseInt(mcc),
                        Integer.parseInt(mnc),
                        Integer.parseInt(lac),
                        Integer.parseInt(cellId));
            }
        }
    }

    public static class StepsBulkSerializer implements JsonSerializer<StepsBulk> {
        @Override
        public JsonElement serialize(StepsBulk src, Type typeOfSrc, JsonSerializationContext context) {
            DateFormat df = Globals.getUTCDateFormat();
            String startDateString = df.format(new Date(src.getStartTime()));
            String endDateString = df.format(new Date(src.getEndTime()));

            StepsBulkJson stepsJson = new StepsBulkJson(
                    startDateString, endDateString, src.getTotalSteps(),
                    src.getSource());

            if (null != src.getLocation()) {
                stepsJson.setLocation(src.getLocation());
            }

            if (null != src._telephony) {
                TelephonyData t = new TelephonyData(
                        null,
                        src._telephony.telephonyData.mcc,
                        src._telephony.telephonyData.mnc,
                        src._telephony.telephonyData.cellId,
                        src._telephony.telephonyData.lac
                );
                stepsJson.setTelephony(t);
            }

            return context.serialize(stepsJson);
        }
    }

    public class StepsBulkDeserializer implements JsonDeserializer<StepsBulk> {
        @Override
        public StepsBulk deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context)
                throws JsonParseException {
            StepsBulk stepsBulk = null;
            try {
                StepsBulkJson stepsJsonInfo = new Gson().fromJson(json, StepsBulkJson.class);
                DateFormat df = Globals.getUTCDateFormat();
                Date startDate = df.parse(stepsJsonInfo.start);
                Date endDate = df.parse(stepsJsonInfo.end);

                stepsBulk = new StepsBulk(startDate.getTime(), endDate.getTime(), stepsJsonInfo.steps);
                stepsBulk.setSource(stepsJsonInfo.source);

                stepsBulk._location = stepsJsonInfo.location;
                if (null != stepsJsonInfo.network)
                    stepsBulk._telephony = new StepsTelephonyExtra(stepsJsonInfo.network.toTelephonyData());
            }
            catch (Exception e) {
                Logger.instance().Log(Logger.ERROR, TAG, "failed to parse json steps bulk");
            }

            return stepsBulk;
        }
    }
}
