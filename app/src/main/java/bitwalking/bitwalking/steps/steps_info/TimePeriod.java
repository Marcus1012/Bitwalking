package bitwalking.bitwalking.steps.steps_info;

/**
 * Created by Marcus on 2/1/16.
 */

public class TimePeriod {
    protected long _startTime;
    protected long _endTime;

    public TimePeriod(long startTime, long endTime) {
        _startTime = startTime;
        _endTime = endTime;
    }

    public long getStartTime() { return _startTime; }
    public void setStartTime(long time) { _startTime = time; }
    public long getEndTime() { return _endTime; }
    public void setEndTime(long time) { _endTime = time; }
    public long getDuration() { return _endTime - _startTime; }
}