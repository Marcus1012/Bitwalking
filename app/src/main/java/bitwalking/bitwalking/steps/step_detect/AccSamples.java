package bitwalking.bitwalking.steps.step_detect;

/**
 * Created by Marcus on 10/24/15.
 */
public class AccSamples implements Comparable {
    private float[] _samples = new float[] { 0.f, 0.f, 0.f };
    private long _timestamp; // nanosecond
    private double _abs = -1;

    public AccSamples() {

    }

    public AccSamples(float[] samples, long timestamp) {
        _samples[0] = samples[0];
        _samples[1] = samples[1];
        _samples[2] = samples[2];
        _timestamp = timestamp;
    }

    public synchronized double getAbs() {
        if (_abs < 0) {
            _abs = Math.sqrt(_samples[0] * _samples[0] + _samples[1] * _samples[1] + _samples[2] * _samples[2]);
        }

        return _abs;
    }

    public synchronized void setSamples(float[] samples, long timestamp) {
        _timestamp = timestamp;
        _samples[0] = samples[0];
        _samples[1] = samples[1];
        _samples[2] = samples[2];
        _abs = -1;
    }

    public synchronized float[] getSamples() { return _samples.clone(); }

    public synchronized long getTimestamp() { return _timestamp; }

    @Override
    public int compareTo(Object another) {
        double compare = getAbs() - ((AccSamples)another).getAbs();
        return (compare > 0) ? 1 : (compare < 0) ? -1 : 0;
    }
}
