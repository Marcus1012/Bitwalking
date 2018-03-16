package bitwalking.bitwalking.steps.steps_validation;

/**
 * Created by Marcus on 12/6/15.
 */
public class StepsValidationParameters {

    public static final int SAMPLING_INTERVAL_US = 1 * 60 * 1000 * 1000; // 1 min

    public static final int MIN_STEPS_TO_TRIGGER_LOCATION = 100;
    public static final int MIN_STEPS_FOR_WALK = 10;
    public static final int MIN_STEPS_TIME_FOR_WALK = 1 * 60 * 1000; // 1 min - ms

    public static final float MAX_STEPS_PER_SECOND = 2.5f; // ?

    public static final long MAX_WALK_TIME_PER_DAY_MS = 8 * 60 * 60 * 1000;//8 * 60 * 60 * 1000; // 1 or 8

    public static final float MIN_WALK_FFT = 0.5f; // ?
    public static final float MAX_WALK_FFT = 2.5f;
    public static final float MIN_RUN_FFT = MAX_WALK_FFT;
    public static final float MAX_RUN_FFT = 4.f; // ?

    public final static long MIN_TIME_BETWEEN_STEP_MS = 250L;
    public final static long MAX_TIME_BETWEEN_STEP_MS = 1000L;
    public final static long AVG_TIME_BETWEEN_STEP_MS = (MIN_TIME_BETWEEN_STEP_MS + MAX_TIME_BETWEEN_STEP_MS) / 2;
    public final static long ONE_SECOND_IN_MS = 1000L;
}
