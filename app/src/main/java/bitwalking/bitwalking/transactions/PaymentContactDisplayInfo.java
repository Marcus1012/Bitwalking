package bitwalking.bitwalking.transactions;

import android.graphics.Bitmap;

/**
 * Created by Marcus on 11/21/16.
 */

public class PaymentContactDisplayInfo {
    public String email;
    public String fullName;
    public Bitmap image;

    public PaymentContactDisplayInfo(PaymentContactDeviceInfo deviceContactInfo) {
        email = deviceContactInfo.email;
        fullName = deviceContactInfo.name;
        image = deviceContactInfo.profileImage;
    }
}
