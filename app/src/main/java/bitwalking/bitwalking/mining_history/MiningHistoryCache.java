package bitwalking.bitwalking.mining_history;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Marcus on 12/21/16.
 */

public class MiningHistoryCache {
    private static final String HISTORY_PREFS = "MINING_HISTORY";

    private Context _context;
    private SimpleDateFormat _historyFileDateFormat;
    private SimpleDateFormat _historyKeyDateFormat;
    private Gson _gson;

    public MiningHistoryCache(Context context) {
        _context = context;
        _historyFileDateFormat = new SimpleDateFormat("yyyy_MM");
        _historyKeyDateFormat = new SimpleDateFormat("dd");
        _gson = new Gson();
    }

    public MiningHistory getDay(Date day) {
        MiningHistory history = null;

        if (null != day) {
            String historyJson =
                getDayPrefsFile(day).getString(_historyKeyDateFormat.format(day), null);

            try {
                history = _gson.fromJson(historyJson, MiningHistory.class);
            }
            catch (Exception e) {}
        }

        return history;
    }

    public void append(ArrayList<MiningHistory> moreHistory) {
        if (null != moreHistory) {
            for (MiningHistory history : moreHistory)
                storeHistory(history);
        }
    }

    private void storeHistory(MiningHistory history) {
        if (null != history) {
            getDayPrefsFile(history.getDate())
                .edit()
                .putString(_historyKeyDateFormat.format(history.getDate()), _gson.toJson(history))
                .apply();
        }
    }

    private SharedPreferences getDayPrefsFile(Date day) {
        String dayFileName = String.format("history_%s", _historyFileDateFormat.format(day));
        return _context.getSharedPreferences(dayFileName, Context.MODE_PRIVATE);
    }
}
