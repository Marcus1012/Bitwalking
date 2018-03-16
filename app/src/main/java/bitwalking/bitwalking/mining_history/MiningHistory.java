package bitwalking.bitwalking.mining_history;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

/**
 * Created by Marcus on 12/21/16.
 */

public class MiningHistory implements Comparable<MiningHistory> {
    private Date day;
    private BigDecimal mining;

    MiningHistory(Date day, BigDecimal mining) {
        this.day = day;
        this.mining = mining;
    }

    public Date getDate() { return day; }

    public String getDay() {
        return new SimpleDateFormat("dd MMM yyyy").format(day);
    }

    public BigDecimal getMining() {
        return this.mining.setScale(2, BigDecimal.ROUND_DOWN);
    }

//    public String getMiningText() {
//        return Globals.bigDecimalToNiceString(getMining());
//    }

    @Override
    public int compareTo(MiningHistory other) {
        return day.compareTo(other.day);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MiningHistory))
            return false;

        MiningHistory other = (MiningHistory)o;

        return other.day.equals(day) && other.mining.equals(mining);
    }

    public static ArrayList<MiningHistory> removeDuplicates(ArrayList<MiningHistory> src) {
        Collections.sort(src);
        MiningHistory last = null;
        ArrayList<MiningHistory> filtered = new ArrayList<>();
//&& (src.get(i).getMining().doubleValue()>0)
        for (int i = 0; i < src.size(); ++i) {
            if ((null == last || !last.equals(src.get(i))) ) {
                last = src.get(i);
                filtered.add(last);
            }
        }

        return filtered;
    }


}
