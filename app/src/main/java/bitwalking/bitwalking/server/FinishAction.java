package bitwalking.bitwalking.server;

/**
 * Created by Marcus on 9/9/16.
 */
public class FinishAction {
    private int id;
    OnFinishListener listener;

    public FinishAction(OnFinishListener listener) {
        this(listener, 0);
    }

    public FinishAction(OnFinishListener listener, int id) {
        this.listener = listener;
        this.id = id;
    }

    public void done() {
        if (null != listener)
            listener.onFinish(id);
    }

    public void failed() {
        if (null != listener)
            listener.onFail(id);
    }

    public interface OnFinishListener {
        void onFail(int id);
        void onFinish(int id);
    }
}
