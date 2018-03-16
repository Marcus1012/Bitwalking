package bitwalking.bitwalking.mvi.notification;


public class NotificationState {
    private final boolean loading;
   // private final boolean loadingFiresPage;
    private final Throwable error;
    private final  boolean loadMore;

    public NotificationState(boolean loading,boolean loadMore,Throwable error) {
        this.loading = loading;
        this.error = error;
        this.loadMore = loadMore;
    }

    public boolean isLoading() {
        return loading;
    }

    public Throwable getError() {
        return error;
    }
}
