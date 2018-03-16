package bitwalking.bitwalking.steps;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;

import bitwalking.bitwalking.util.Globals;
import bitwalking.bitwalking.steps.steps_info.StepsBulk;
import bitwalking.bitwalking.util.Logger;

/**
 * Created by Marcus on 1/6/16.
 */
public class UserStepsData {

    private final static String TAG = "UserStepsData";

    private final static long ONE_SECOND_IN_MILLISECOND = 1000;
    private final static long ONE_HOUR_IN_MILLISECOND = 1 * 60 * 60 * 1000;
    private final static long ONE_DAY_IN_MILLISECOND = 24 * ONE_HOUR_IN_MILLISECOND;
    private final static long ONE_WEEK_IN_MILLISECOND = 7 * ONE_DAY_IN_MILLISECOND;

    private ArrayList<StepsBulk> _lastUserSteps;

    public UserStepsData() {
        _lastUserSteps = new ArrayList<>();
    }

    public UserStepsData(String json) {
        if (null != json)
            fromJson(json);
        else
            _lastUserSteps = new ArrayList<>();
    }

    public synchronized int getStepsLastDay() {
        return getStepsUntilDate(new Date().getTime() - ONE_DAY_IN_MILLISECOND);
    }

    public synchronized int getStepsLastWeek() {
        return getStepsUntilDate(new Date().getTime() - ONE_WEEK_IN_MILLISECOND);
    }

    private int getStepsUntilDate(long lastDateTime) {
        int steps = 0;

        for (int i = _lastUserSteps.size() - 1; i >= 0; --i) {
            if (_lastUserSteps.get(i).getEndTime() > lastDateTime)
                steps += _lastUserSteps.get(i).getTotalSteps();
            else
                break;
        }

        return steps;
    }

    public int getStepsLastSecond() {
        return getStepsUntilDate(new Date().getTime() - ONE_SECOND_IN_MILLISECOND);
    }

    /**
     * Adds new bulk of steps to users data. Steps must be represented in a period of time.
     *
     * @param newStepsBulk
     *          new steps bulk contains [startRecording time],[end time],[steps]
     */
    public synchronized void addNewSteps(StepsBulk newStepsBulk) {

        long weekBack = newStepsBulk.getEndTime() - ONE_WEEK_IN_MILLISECOND;
        int deleteIndex = 0;
        for (; deleteIndex < _lastUserSteps.size(); ++deleteIndex) {
            if (_lastUserSteps.get(deleteIndex).getEndTime() >= weekBack)
                break;
        }

        while (deleteIndex > 0) {
            _lastUserSteps.remove(0);
            Logger.instance().Log(Logger.DEBUG, TAG, "Remove old bulk");
            deleteIndex--;
        }

        Logger.instance().Log(Logger.DEBUG, TAG, String.format("New bulk: [%d] [%d]", newStepsBulk.getTotalSteps(), newStepsBulk.getDuration()));
        _lastUserSteps.add(newStepsBulk);
    }

    public long getLastDayWalkDuration() {
        return getWalkDurationUntilDate(System.currentTimeMillis() - ONE_DAY_IN_MILLISECOND);
    }

    private long getWalkDurationUntilDate(long lastDateTime) {
        long duration = 0;

        Logger.instance().Log(Logger.DEBUG, TAG, "walk duration: lastDateTime = " + lastDateTime);

        for (int i = _lastUserSteps.size() - 1; i >= 0; --i) {
            if (_lastUserSteps.get(i).getEndTime() > lastDateTime)
                duration += _lastUserSteps.get(i).getDuration();
            else {
                Logger.instance().Log(Logger.DEBUG, TAG, "bulk out of date time - " + _lastUserSteps.get(i).getEndTime());
                break;
            }
        }

        return duration;
    }

    public ArrayList<UserDaySteps> getStepsPerDay() {
        ArrayList<UserDaySteps> _steps = new ArrayList<>();

        // Get zero time of current day
        long currentDayEndTime = Globals.getZeroTimeDate(new Date(_lastUserSteps.get(0).getEndTime())).getTime();
        // Add 24 hours in millisecond
        currentDayEndTime += ONE_DAY_IN_MILLISECOND;
        int currentDaySteps = 0;

        for (StepsBulk bulk : _lastUserSteps) {
            if (bulk.getEndTime() < currentDayEndTime)
                currentDaySteps += bulk.getTotalSteps();
            else {
                // New day
                _steps.add(new UserDaySteps(new Date(currentDayEndTime - 1), currentDaySteps));
                currentDayEndTime += ONE_DAY_IN_MILLISECOND;
                currentDaySteps = 0;
            }
        }

        return _steps;
    }

    public class UserDaySteps {
        private Date _date;
        private int _steps;

        public UserDaySteps (Date date, int steps) {
            _date = date;
            _steps = steps;
        }

        public int getSteps() { return _steps; }
        public Date getDate() { return _date; }
    }

    public String toJson() {
        Gson gson = new Gson();
        String json = gson.toJson(_lastUserSteps);

        return json;
    }

    private void fromJson(String json) {
        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<StepsBulk>>() {}.getType();
        _lastUserSteps = gson.fromJson(json, type);
    }
}
