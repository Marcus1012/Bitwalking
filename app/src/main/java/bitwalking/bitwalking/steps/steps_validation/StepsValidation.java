package bitwalking.bitwalking.steps.steps_validation;

import android.os.Handler;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

import bitwalking.bitwalking.util.Globals;
import bitwalking.bitwalking.steps.steps_info.StepsBulk;
import bitwalking.bitwalking.steps.steps_sources.StepsSource;
import bitwalking.bitwalking.util.Logger;

/**
 * Created by Marcus on 10/30/15.
 */
public class StepsValidation implements StepsSource.OnNewStepsListener {

    //region Members

    private static final String TAG = "StepsValidation";

    // Last validated step timestamp
//    private long _lastStepsTimestamp = -1;
//    private long _walkStartTimestamp = -1;
//    private int _pendingSteps = 0;
    private StepsBulk _pendingBulk = null;
    private Handler _dispatchPendingTimeout = null;
    private static final int _PENDING_STEPS_TIMEOUT = 2000;
    private boolean _walkStarted = false;

    // Verified Steps Listenersx
    private ArrayList<VerifiedStepsListener> _listeners;

    //endregion

    //region Methods

    public StepsValidation() {
        _listeners = new ArrayList<>();
    }

    public void addListener(VerifiedStepsListener listener) {
        if (_listeners!= null && !_listeners.contains(listener))
            _listeners.add(listener);
    }

    public void removeListener(VerifiedStepsListener listener) {
        if (_listeners != null)
            _listeners.remove(listener);
    }

    private void onNewVerifiedSteps(StepsBulk bulk) {
        // dispatch new number of steps
        for (VerifiedStepsListener l : _listeners) {
            if (l != null)
                l.onVerifiedSteps(bulk);
        }
    }

    //endregion

    //region Steps

//    @Override
//    public synchronized void onSystemSteps(int steps, long timestamp) {
//        Logger.instance().Log(Logger.DEBUG, TAG, String.format("onSystemSteps - [%d]", steps));
//
//        if (0 == steps) {
//            dispatchPendingSteps();
//            return;
//        }
//
//        boolean stepsValid = true;
//        // TODO: see how we can manage the startRecording + end timestamps
//        if (_lastStepsTimestamp > 0) {
//            // Check if new steps not part of pending
//            if (_lastStepsTimestamp + (steps * MAX_TIME_BETWEEN_STEP) < timestamp) {
//                Logger.instance().Log(Logger.DEBUG, TAG, String.format("handle pending steps [%d]", _pendingSteps));
//                dispatchPendingSteps();
//
//                _walkStarted = false;
//            }
//
//            stepsValid = checkStepsPerTime(steps, _lastStepsTimestamp, timestamp);
//        }
//
//        if (stepsValid) {
//            _lastStepsTimestamp = timestamp;
//            _pendingSteps += steps;
//            Logger.instance().Log(Logger.DEBUG, TAG, String.format("add [%d] steps to pending [%d]", steps, _pendingSteps));
//
//            if (_pendingSteps > StepsValidationParameters.MIN_STEPS_FOR_WALK) {
//                dispatchPendingSteps(StepsValidationParameters.MIN_STEPS_FOR_WALK);
//                _walkStarted = true;
//            }
//
//            if (_walkStarted)
//                resetStopAnimationTimer();
//        }
//        else {
//            Logger.instance().Log(Logger.DEBUG, TAG, String.format("steps not valid! drop [%d]", steps));
//        }
//    }

//    @Override
//    public synchronized void onSystemSteps(int steps, long timestamp) {
//        Logger.instance().Log(Logger.DEBUG, TAG, String.format("onSystemSteps - [%d]", steps));
//
//        if (0 == steps) {
//            dispatchPendingSteps();
//            return;
//        }
//
//        // TODO: this is an estimated startRecording timestamp, consider using different method
//        long stepsEstimatedStartTime = timestamp - (steps * (MAX_TIME_BETWEEN_STEP + MIN_TIME_BETWEEN_STEP) / 2);
//
//        if (_walkStarted) {
//            // Check if new steps not part of pending
//            if (_lastStepsTimestamp + ((steps + 1) * MAX_TIME_BETWEEN_STEP) < timestamp) {
//                // New steps are part of a new walk, dispatch old steps and init new walk parameters
//
//                if (_walkConfirmed &&
//                    checkStepsPerTime(_pendingSteps, _walkStartTimestamp, _lastStepsTimestamp)) {
//                    Logger.instance().Log(Logger.DEBUG, TAG, String.format("dispatch pending steps [%d], new walk", _pendingSteps));
//                    dispatchPendingSteps();
//                }
//                else {
//                    Logger.instance().Log(Logger.DEBUG, TAG, String.format("drop pending steps [%d], new walk", _pendingSteps));
//                }
//
//                _walkStartTimestamp = stepsEstimatedStartTime;
//                _pendingSteps = steps;
//                _walkConfirmed = false;
//            }
//            else {
//                // Steps are part of current walk, update steps and times
//                _pendingSteps += steps;
//
//                if (_pendingSteps >= StepsValidationParameters.MIN_STEPS_FOR_WALK) {
//                    if (checkStepsPerTime(_pendingSteps, _walkStartTimestamp, timestamp)) {
//                        Logger.instance().Log(Logger.DEBUG, TAG, String.format("dispatch pending steps [%d], got min steps", _pendingSteps));
//                        dispatchPendingSteps();
//                        resetPendingStepsTimer();
//                    }
//                    else {
//                        Logger.instance().Log(Logger.DEBUG, TAG, String.format("drop pending steps [%d], tried to dispatch min steps", _pendingSteps));
//                    }
//
//                    _walkStartTimestamp = timestamp;
//                    _pendingSteps = 0;
//                    _walkConfirmed = true;
//                }
//            }
//        }
//        else {
//            // New walk
//            Logger.instance().Log(Logger.DEBUG, TAG, String.format("startRecording new walk, [%d] steps", _pendingSteps));
//            _walkStarted = true;
//            _walkConfirmed = false;
//
//            _walkStartTimestamp = stepsEstimatedStartTime;
//            _pendingSteps = steps;
//        }
//
//        _lastStepsTimestamp = timestamp;
//    }

    private boolean checkStepsPerTime(int steps, long startTime, long endTime) {
        double deltaTime = (endTime - startTime) / (double)StepsValidationParameters.ONE_SECOND_IN_MS;
        int maxStepsPerDelta = (int)(deltaTime * StepsValidationParameters.MAX_STEPS_PER_SECOND);

        Logger.instance().Log(Logger.DEBUG, TAG, String.format("[%d] steps in [%.3fs]", steps, deltaTime));
        return (maxStepsPerDelta >= steps);
    }

    @Override
    public void onNewSteps(ArrayList<StepsBulk> newStepBulks) {
        for (StepsBulk bulk : newStepBulks)
            verifyStepsBulk(bulk);
    }

    private void printBulk(String name, StepsBulk bulk) {
        DateFormat df = Globals.getFullDateFormat();
        String log = name + "- null";

        if (null != bulk)
            log = String.format("%s - [%s][%s]~[%d]",
                name,
                df.format(new Date(bulk.getStartTime())),
                df.format(new Date(bulk.getEndTime())),
                bulk.getTotalSteps());

        Logger.instance().Log(Logger.DEBUG, TAG, log);
    }

    private synchronized void verifyStepsBulk(StepsBulk bulk) {
        if(null == bulk)
            return;

        if (bulk.getTotalSteps() > 0)
            onNewVerifiedSteps(bulk);

        return;

//        int steps = bulk.getTotalSteps();
//        Logger.instance().Log(Logger.DEBUG, TAG, String.format("onNewSteps - [%d]", steps));
//
//        if (0 == steps) {
//            dispatchPendingSteps();
//            return;
//        }
//
//        printBulk("pending", _pendingBulk);
//        printBulk("new bulk", bulk);
//
//        if (_walkStarted) {
//            // Check if new steps not part of pending
//            if (_pendingBulk.getEndTime() + ((steps + 1) * StepsValidationParameters.MAX_TIME_BETWEEN_STEP_MS) < bulk.getEndTime()) {
//                Logger.instance().Log(Logger.DEBUG, TAG, String.format("new steps are new walk, dispatch old and start new [%d]", bulk.getTotalSteps()));
//                // New steps are  a new walk, dispatch old steps and init new walk parameters
//                addToPending(bulk, true);
//            }
//            else {
//                Logger.instance().Log(Logger.DEBUG, TAG, String.format("new steps are part of current walk, add [%d]", bulk.getTotalSteps()));
//                // Steps are part of current walk, update steps and times
//                addToPending(bulk, false);
//            }
//        }
//        else {
//            // New walk
//            Logger.instance().Log(Logger.DEBUG, TAG, String.format("startRecording new walk, [%d] steps", bulk.getTotalSteps()));
//            _walkStarted = true;
//
//            addToPending(bulk, false);
//        }
//
//        resetPendingStepsTimer();
    }

    private synchronized void dispatchPendingSteps() {

        if (null != _pendingBulk && _pendingBulk.getTotalSteps() > 0) {
            Logger.instance().Log(Logger.DEBUG, TAG, String.format("dispatch pending - [%d]", _pendingBulk.getTotalSteps()));
            onNewVerifiedSteps(_pendingBulk);
            _pendingBulk = null;
        }
    }

    private void addToPending(StepsBulk bulk, boolean dispatchCurrent) {
        if (dispatchCurrent && null != _pendingBulk) {
            dispatchPendingSteps();
        }

        if (null == _pendingBulk) {
            _pendingBulk = new StepsBulk(bulk);
        }
        else {
            _pendingBulk.addSteps(bulk.getEndTime(), bulk.getTotalSteps());
        }
    }

    private void resetPendingStepsTimer() {
        if (_dispatchPendingTimeout != null) {
            _dispatchPendingTimeout.removeCallbacksAndMessages(null);
            _dispatchPendingTimeout = null;
        }

        _dispatchPendingTimeout = new Handler();
        _dispatchPendingTimeout.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (null != _pendingBulk) {
                    Logger.instance().Log(Logger.DEBUG, TAG,
                        String.format("dispatch pending steps [%d], timeout", _pendingBulk.getTotalSteps()));
                }

                dispatchPendingSteps();
                _walkStarted = false;
            }
        }, _PENDING_STEPS_TIMEOUT);
    }

//    private synchronized void verifyStepsBulk(StepsBulk bulk) {
//        if(null == bulk)
//            return;
//
//        int steps = bulk.getTotalSteps();
//        Logger.instance().Log(Logger.DEBUG, TAG, String.format("onNewSteps - [%d]", steps));
//
//        if (0 == steps) {
//            dispatchPendingSteps();
//            return;
//        }
//
//        long stepsStartTime = bulk.getStartTime();
//
//        if (_walkStarted) {
//            // Check if new steps not part of pending
//            if (_lastStepsTimestamp + ((steps + 1) * StepsValidationParameters.MAX_TIME_BETWEEN_STEP) < bulk.getEndTime()) {
//                // New steps are part of a new walk, dispatch old steps and init new walk parameters
//
//                if (_walkConfirmed &&
//                    checkStepsPerTime(_pendingSteps, _walkStartTimestamp, _lastStepsTimestamp)) {
//
//                    Logger.instance().Log(Logger.DEBUG, TAG, String.format("dispatch pending steps [%d], new walk", _pendingSteps));
//                    dispatchPendingSteps();
//                }
//                else {
//                    Logger.instance().Log(Logger.DEBUG, TAG, String.format("drop pending steps [%d], new walk", _pendingSteps));
//                }
//
//                _walkStartTimestamp = stepsStartTime;
//                _pendingSteps = steps;
//                _walkConfirmed = false;
//            }
//            else {
//                // Steps are part of current walk, update steps and times
//                _pendingSteps += steps;
//
//                if (_pendingSteps >= StepsValidationParameters.MIN_STEPS_FOR_WALK) {
//                    if (checkStepsPerTime(_pendingSteps, _walkStartTimestamp, bulk.getEndTime())) {
//                        Logger.instance().Log(Logger.DEBUG, TAG, String.format("dispatch pending steps [%d], got min steps", _pendingSteps));
//                        dispatchPendingSteps();
//                        resetPendingStepsTimer();
//                    }
//                    else {
//                        Logger.instance().Log(Logger.DEBUG, TAG, String.format("drop pending steps [%d], tried to dispatch min steps", _pendingSteps));
//                    }
//
//                    _walkStartTimestamp = bulk.getEndTime();
//                    _pendingSteps = 0;
//                    _walkConfirmed = true;
//                }
//            }
//        }
//        else {
//            // New walk
//            Logger.instance().Log(Logger.DEBUG, TAG, String.format("startRecording new walk, [%d] steps", _pendingSteps));
//            _walkStarted = true;
//            _walkConfirmed = false;
//
//            _walkStartTimestamp = stepsStartTime;
//            _pendingSteps = steps;
//        }
//
//        _lastStepsTimestamp = bulk.getEndTime();
//    }

    //endregion
}
