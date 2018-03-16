package bitwalking.bitwalking.mining_history;

import android.content.Context;
import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Marcus on 12/24/16.
 */

public class GetMiningHistoryCached extends AsyncTask<ArrayList<Date>, Void, Void> {

    Context _context;
    HistoryLoadedListener _callback;
    ArrayList<MiningHistory> _result;

    public GetMiningHistoryCached(Context context, HistoryLoadedListener callback) {
        _context = context;
        _callback = callback;
    }

    @Override
    protected Void doInBackground(ArrayList<Date>... params) {
        ArrayList<Date> dates = params[0];
        _result = new ArrayList<>();

        if (dates.size() > 0) {
            final MiningHistoryCache cache = new MiningHistoryCache(_context);

            for (Date day : dates) {
                MiningHistory history = cache.getDay(day);
                if (null == history) {
                    // skip this date
                }
                else {
                    _result.add(history);
                    // Fake delay
                    try {
                        Thread.sleep(150);
                    } catch (Exception e) {}
                }
            }

            if (null != _callback) {
                // Done
                _callback.onHistory(_result);
            }
        }
        else {
            if (null != _callback) { // Empty
                // Done
                _callback.onHistory(_result);
            }
        }

        return null;
    }

}
