package bitwalking.bitwalking.server.requests;

import com.google.gson.GsonBuilder;

import java.util.ArrayList;

import bitwalking.bitwalking.BitwalkingApp;
import bitwalking.bitwalking.steps.steps_info.StepsBulk;

/**
 * Created by Marcus on 6/22/16.
 */
public class UpdateUserSteps extends BasicServerRequest {
    private ArrayList<StepsBulk> stepUpdates;

    public UpdateUserSteps(ArrayList<StepsBulk> stepUpdates) {
        this.stepUpdates = stepUpdates;
    }

    public ArrayList<StepsBulk> getStepsBulks() { return stepUpdates; }

    @Override
    public byte[] getBody() {
        final GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(StepsBulk.class, new StepsBulk.StepsBulkSerializer());

        byte[] bytes = null;

        try {
            bytes = gsonBuilder.create().toJson(this).getBytes("UTF8");
        }
        catch (Exception e) {
            BitwalkingApp.getInstance().trackException(new Exception("Failed to convert UpdateUserSteps", e));
        }

        return bytes;//Globals.stringToBytes(gsonBuilder.create().toJson(this));
    }
}
