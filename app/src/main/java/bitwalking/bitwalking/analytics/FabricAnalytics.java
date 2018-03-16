package bitwalking.bitwalking.analytics;

import com.crashlytics.android.answers.Answers;

import bitwalking.bitwalking.BuildConfig;
import io.fabric.sdk.android.Fabric;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.ContentViewEvent;
import com.crashlytics.android.answers.CustomEvent;

import android.content.Context;

/**
 * Created by Marcus on 23/02/2017.
 */

public class FabricAnalytics extends AnalyticsInterface {
    private Context _context;

    public FabricAnalytics(Context context) {
        _context = context;
        configAnalytics();
    }

    private void initFabric() {
        if (BuildConfig.enableCrashlytics) {
            final Fabric fabric = new Fabric.Builder(_context)
                    .kits(new Answers(), new Crashlytics())
                    .build();

            Fabric.with(fabric);
        }
    }

    @Override
    protected void configDebugAnalytics() {
        initFabric();
    }

    @Override
    protected void configReleaseAnalytics() {
        initFabric();
    }

    @Override
    public void trackEvent(String category, String action, String label) {
        if (Fabric.isInitialized()) {
            Answers instance = Answers.getInstance();
            if (null != instance) {
                Answers.getInstance().logCustom(new CustomEvent(category)
                        .putCustomAttribute("action", action)
                        .putCustomAttribute("label", label));
            }
        }
    }

    @Override
    public void trackScreenView(String screenName) {
        if (Fabric.isInitialized()) {
            Answers instance = Answers.getInstance();
            if (null != instance) {
                Answers.getInstance().logContentView(new ContentViewEvent()
                        .putContentName(screenName)
                        .putContentType("screen"));
            }
        }
    }

    @Override
    public void trackException(Exception e) {
        if (Fabric.isInitialized())
            Crashlytics.logException(e);
    }

    @Override
    public void trackUncaughtException(Exception e) {
        trackException(e);
    }
}
