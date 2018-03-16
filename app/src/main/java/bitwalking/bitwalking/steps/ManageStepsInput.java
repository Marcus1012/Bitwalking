package bitwalking.bitwalking.steps;

/**
 * Created by Marcus on 1/29/16.
 */
public class ManageStepsInput {

    static private ManageStepsInput _instance;

    public static ManageStepsInput getInstance() {
        if (null == _instance)
            _instance = new ManageStepsInput();

        return _instance;
    }
}
