package bitwalking.bitwalking;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

// todo: zendesk
//import com.zendesk.sdk.model.access.AnonymousIdentity;
//import com.zendesk.sdk.model.access.Identity;
//import com.zendesk.sdk.network.impl.ZendeskConfig;

import java.util.ArrayList;
import bitwalking.bitwalking.analytics.AnalyticsInterface;
import bitwalking.bitwalking.analytics.FabricAnalytics;
import bitwalking.bitwalking.analytics.GoogleAnalyticsService;
import bitwalking.bitwalking.server.BitwalkingServer;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

/**
 * Created by Marcus on 1/2/16.
 */
public class BitwalkingApp extends Application {
    public static final String TAG = BitwalkingApp.class.getSimpleName();
    private static BitwalkingApp _instance;
    ArrayList<AnalyticsInterface> _analyticsServices;

    // uncaught exception handler variable
    private Thread.UncaughtExceptionHandler _defaultUncaughtHandler;

    // handler listener
    private Thread.UncaughtExceptionHandler _ourUncaughtHandler =
            new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable ex) {
                    for (AnalyticsInterface i : _analyticsServices)
                            i.trackUncaughtException(new Exception(ex));

                    // call default handler
                    _defaultUncaughtHandler.uncaughtException(thread, ex);
                }
            };

    public BitwalkingApp() {
        _defaultUncaughtHandler = Thread.getDefaultUncaughtExceptionHandler();

        // setup handler for uncaught exception
        Thread.setDefaultUncaughtExceptionHandler(_ourUncaughtHandler);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        _instance = this;

        BitwalkingServer.setContext(getApplicationContext());

        initAnalyticsServices();

        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                        .setDefaultFontPath("fonts/Roboto-Regular.ttf")
                        .setFontAttrId(R.attr.fontPath)
                        .build());

        // todo: zendesk
//        ZendeskConfig.INSTANCE.init(this, "https://bitwalking1.zendesk.com", "{applicationId}", "{oauthClientId}");
//        Identity identity = new AnonymousIdentity.Builder().build();
//        ZendeskConfig.INSTANCE.setIdentity(identity);
    }

    private void initAnalyticsServices() {
        _analyticsServices = new ArrayList<>();
        _analyticsServices.add(new GoogleAnalyticsService(getApplicationContext()));
        _analyticsServices.add(new FabricAnalytics(getApplicationContext()));
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    public static synchronized BitwalkingApp getInstance() {
        return _instance;
    }

    /***
     * Tracking screen view
     *
     * @param screenName screen name to be displayed on GA dashboard
     */
    public void trackScreenView(String screenName) {
        for (AnalyticsInterface analytics : _analyticsServices) {
            analytics.trackScreenView(screenName);
        }
    }

    /***
     * Tracking exception
     *
     * @param e exception to be tracked
     */
    public void trackException(Exception e) {
        for (AnalyticsInterface analytics : _analyticsServices) {
            analytics.trackException(e);
        }
    }

    /***
     * Tracking exception
     *
     * @param e exception to be tracked
     */
    public void trackException(String msg, Exception e) {
        for (AnalyticsInterface analytics : _analyticsServices) {
            analytics.trackException(new Exception(msg, e));
        }
    }

    /***
     * Tracking event
     *
     * @param category event category
     * @param action   action of the event
     * @param label    label
     */
    public void trackEvent(String category, String action, String label) {
        for (AnalyticsInterface analytics : _analyticsServices) {
            analytics.trackEvent(category, action, label);
        }
    }
}
