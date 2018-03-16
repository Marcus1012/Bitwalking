package bitwalking.bitwalking.mining_history;

import java.util.ArrayList;

/**
 * Created by Marcus on 12/24/16.
 */

public interface HistoryLoadedListener {
    void onHistory(ArrayList<MiningHistory> history);
}
