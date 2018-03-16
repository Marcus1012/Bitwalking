package bitwalking.bitwalking.transactions;

import android.graphics.Bitmap;

/**
 * Created by Marcus on 11/15/16.
 */

public class PaymentContactDeviceInfo  implements Comparable<PaymentContactDeviceInfo> {
    public String contactId;
    public String name;
    public String email;
    public transient Bitmap profileImage;

    @Override
    public int compareTo(PaymentContactDeviceInfo another) {
        return name.compareToIgnoreCase(another.name);
    }
}
