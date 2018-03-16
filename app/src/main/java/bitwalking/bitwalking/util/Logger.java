package bitwalking.bitwalking.util;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.StreamCorruptedException;

import bitwalking.bitwalking.BuildConfig;

/**
 * Created by Marcus on 4/26/16.
 */
public class Logger {

    public static final int VERB    = 0;
    public static final int DEBUG   = 10;
    public static final int INFO    = 20;
    public static final int WARN    = 30;
    public static final int ERROR   = 40;
    public static final int ASSERT  = 50;

    private int _level = (BuildConfig.DEBUG) ? VERB : ERROR;
    private static Logger _instance = null;

    String _localLogFile = "log.log";
    File _logFile = null;
    FileWriter _logWriter = null;

    public void initLogFile(Context context) {
        try {
            _logFile = new File(context.getFilesDir() + "/" + _localLogFile);

            if (!_logFile.exists())
                _logFile.createNewFile();

            _logWriter = new FileWriter(_logFile, true);
        }
        catch (Exception e) {
        }
    }

    private Logger() {

    }

    public static Logger instance() {
        if (null == _instance)
            _instance = new Logger();

        return _instance;
    }

    public void setLevel(int level) { _level = level; }

    public void Log(int level, String tag, String msg) {
        if (level >= _level) {
            if (level >= ASSERT) {
                // none
            }
            else if (level >= ERROR) {
                Log.e(tag, msg);
            }
            else if (level >= WARN) {
                Log.w(tag, msg);
            }
            else if (level >= INFO) {
                Log.i(tag, msg);
            }
            else if (level >= DEBUG) {
                Log.d(tag, msg);
            }
            else if (level >= VERB) {
                Log.v(tag, msg);
            }
        }

        if (null != _logWriter) {
            try {
                _logWriter.append(String.format("%s: %s\n", tag, msg));
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void Log(int level, String tag, String msg, Throwable t) {
        if (level >= _level) {
            if (level >= ERROR) {
                Log.e(tag, msg, t);
            }
        }
    }
}
