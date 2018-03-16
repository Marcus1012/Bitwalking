package bitwalking.bitwalking.mining_history;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import bitwalking.bitwalking.AppPreferences;
import bitwalking.bitwalking.server.api.ServerApi;
import bitwalking.bitwalking.server.responses.UserTodayResponse;

/**
 * Created by Marcus on 12/21/16.
 */

public class GetMiningHistoryServer extends AsyncTask<ArrayList<Date>, Void, Void> {
    Context _context;
    ArrayList<MiningHistory> _result;
    int _counter;
    HistoryLoadedListener _callback;

    public GetMiningHistoryServer(Context context, HistoryLoadedListener callback) {
        _context = context;
        _callback = callback;
    }

    @Override
    protected Void doInBackground(ArrayList<Date>... params) {
        ArrayList<Date> dates = params[0];
        _result = new ArrayList<>();
        _counter = dates.size();

        if (dates.size() > 0) {
            for (int i = 0; i < dates.size(); ++i) {
                Date start = dates.get(i);
                final Date end = new Date(start.getTime() + TimeUnit.HOURS.toMillis(24));

                ServerApi.getMining(
                        start,
                        end,
                        AppPreferences.getUserId(_context),
                        AppPreferences.getUserSecret(_context),
                        new ServerApi.MiningListener() {
                            @Override
                            public void onMining(Date start, UserTodayResponse.MiningInfo miningInfo) {
                                synchronized (_result) {
                                    _counter--;

                                    if (null != miningInfo) {
                                          _result.add(new MiningHistory(start, miningInfo.mining));
                                    }else{
                                        Log.d("Mining LOG","NULL "+start.toString() + "    "+end.toString());
                                    }

                                    if (0 == _counter && null != _callback) {
                                        // Done
                                        _callback.onHistory(_result);
                                    }
                                }
                            }
                        });
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
