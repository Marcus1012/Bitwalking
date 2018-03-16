package bitwalking.bitwalking.steps.telephony_info;

import bitwalking.bitwalking.BitwalkingApp;
import bitwalking.bitwalking.util.Globals;

/**
 * Created by Marcus on 5/25/16.
 */
public class TelephonyData {
    public String timestamp;
    public int mcc;
    public int mnc;
    public int cellId;
    public int lac;

    public TelephonyData(String timestamp, int mcc, int mnc, int cid, int lac) {
        this.timestamp = timestamp;
        this.mcc = mcc;
        this.mnc = mnc;
        this.cellId = cid;
        this.lac = lac;
    }

    public void updateTimestamp(String timestamp) { this.timestamp = timestamp; }
    public long getTimestampLong() {
        long t = 0;
        try {
            t = Globals.getUTCDateFormat().parse(timestamp).getTime();
        }
        catch (Exception e) {
            BitwalkingApp.getInstance().trackException("TelephonyData: failed to parse timestamp", e);
        }

        return t;
    }

    @Override
    public boolean equals(Object o) {
        TelephonyData other = (TelephonyData)o;
        return (other.mcc == mcc && other.mnc == mnc && other.cellId == cellId && other.lac == lac);
    }
}
