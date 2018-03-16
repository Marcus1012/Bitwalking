package bitwalking.bitwalking.analytics;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.analytics.ExceptionParser;
import com.google.android.gms.analytics.ExceptionReporter;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.StandardExceptionParser;
import com.google.android.gms.analytics.Tracker;

import bitwalking.bitwalking.BuildConfig;
import bitwalking.bitwalking.R;
import bitwalking.bitwalking.util.Logger;

/**
 * Created by Marcus on 5/16/16.
 */
public class GoogleAnalyticsService extends AnalyticsInterface {
    private Tracker _tracker;
    private Context _context;

    public GoogleAnalyticsService(Context context) {
        _context = context;
    }

    private synchronized Tracker getGoogleAnalyticsTracker() {

        if (null == _tracker) {
            configAnalytics();
            Thread.setDefaultUncaughtExceptionHandler(new AnalyticsExceptionReporter(_tracker,
                    Thread.getDefaultUncaughtExceptionHandler(), _context));

        }

        return _tracker;
    }

    @Override
    protected void configDebugAnalytics() {
        GoogleAnalytics analytics = GoogleAnalytics.getInstance(_context);
        analytics.setLocalDispatchPeriod(5 * 60);

        _tracker = analytics.newTracker(R.xml.google_analytics_debug);
    }

    @Override
    protected void configReleaseAnalytics() {
        GoogleAnalytics analytics = GoogleAnalytics.getInstance(_context);
        analytics.setLocalDispatchPeriod(5 * 60);

        _tracker = analytics.newTracker(R.xml.google_analytics_release);
    }

    @Override
    public void trackEvent(String category, String action, String label) {
        Tracker t = getGoogleAnalyticsTracker();

        // Build and send an Event.
        t.send(new HitBuilders.EventBuilder().setCategory(category).setAction(action).setLabel(label).build());
    }

    @Override
    public void trackScreenView(String screenName) {
        Tracker t = getGoogleAnalyticsTracker();

        // Set screen name.
        t.setScreenName(screenName);

        // Send a screen view.
        t.send(new HitBuilders.ScreenViewBuilder().build());

        com.google.android.gms.analytics.GoogleAnalytics.getInstance(_context).dispatchLocalHits();
    }

    @Override
    public void trackException(Exception e) {
        if (e != null) {
            Tracker t = getGoogleAnalyticsTracker();

            t.send(new HitBuilders.ExceptionBuilder()
                .setDescription(
                        new StandardExceptionParser(_context, null)
                                .getDescription(Thread.currentThread().getName(), e) +
                        String.format("[%s]", e.getMessage()))
                .setFatal(false)
                .build()
            );
        }
    }

    @Override
    public void trackUncaughtException(Exception e) {
        // Google analytics already handled this exception
    }

    private class AnalyticsExceptionReporter extends ExceptionReporter {

        public AnalyticsExceptionReporter(Tracker tracker, Thread.UncaughtExceptionHandler originalHandler, Context context) {
            super(tracker, originalHandler, context);
            setExceptionParser(new AnalyticsExceptionParser());
        }
    }

    private class AnalyticsExceptionParser implements ExceptionParser {

        @Override
        public String getDescription(String arg0, Throwable arg1) {
            String exceptionDescription = getExceptionInfo(arg1, "", true) + getCauseExceptionInfo(arg1.getCause());

            if (exceptionDescription.length() > 150)
                exceptionDescription = exceptionDescription.replace("bitwalking", "bw");
            //150 Bytes is the maximum allowed by Analytics for custom dimensions values. Assumed that 1 Byte = 1 Character (UTF-8)
            if(exceptionDescription.length() > 150)
                exceptionDescription = exceptionDescription.substring(0, 149);

            Logger.instance().Log(Logger.INFO, AnalyticsExceptionParser.class.getSimpleName(), exceptionDescription);

            return exceptionDescription;
        }

        private String myElementToString(StackTraceElement element) {
            String fileName = (element.getFileName() != null) ? element.getFileName() : element.getClassName();
            String elementString = String.format("%s:%d;",
                    element.getClassName(), element.getLineNumber());

            return elementString;
        }

        private String getCauseExceptionInfo(Throwable t) {
            String causeDescription = "";
            while(t != null && causeDescription.isEmpty()) {
                causeDescription = getExceptionInfo(t, "bw.bw", false);
                t = t.getCause();
            }
            return causeDescription;
        }

        private String getExceptionInfo(Throwable t, String packageName, boolean includeExceptionName) {
            String exceptionName = "";
            String fileName = "";
            String lineNumber = "";

            for (StackTraceElement element : t.getStackTrace()) {
                String className = element.getClassName().toString().toLowerCase();
                if(packageName.isEmpty() || (!packageName.isEmpty() && className.contains(packageName))){
                    exceptionName = includeExceptionName ? t.toString() : "";
                    fileName = element.getFileName();
                    lineNumber = String.valueOf(element.getLineNumber());
                    return exceptionName + "@" + fileName + ":" + lineNumber;
                }
            }
            return "";
        }
    }
}
