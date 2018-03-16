package bitwalking.bitwalking.analytics;

import bitwalking.bitwalking.BuildConfig;

/**
 * Created by Marcus on 5/16/16.
 */
public abstract class AnalyticsInterface {

    public void configAnalytics() {
        if (BuildConfig.DEBUG)
            configDebugAnalytics();
        else
            configReleaseAnalytics();
    }

    protected abstract void configDebugAnalytics();
    protected abstract void configReleaseAnalytics();
    public abstract void trackEvent(String category, String action, String label);
    public abstract void trackScreenView(String screenName);
    public abstract void trackException(Exception e);
    public abstract void trackUncaughtException(Exception e);
}
