package bitwalking.bitwalking.transactions;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import bitwalking.bitwalking.R;
import bitwalking.bitwalking.remote_service.BwService;

/**
 * Created by Marcus on 11/21/16.
 */

public class PaymentRequestNotification {
    public static final String PAY_ACTION = "Payment.Request.Pay";
    public static final String REJECT_ACTION = "Payment.Request.Reject";

    public static void buildNotification(Context context, String smallText, String bigText) {
        Intent dismissIntent = new Intent(context, BwService.class);
        dismissIntent.setAction(PAY_ACTION);
        PendingIntent piPay = PendingIntent.getService(context, 0, dismissIntent, 0);

        Intent snoozeIntent = new Intent(context, BwService.class);
        snoozeIntent.setAction(REJECT_ACTION);
        PendingIntent piReject = PendingIntent.getService(context, 0, snoozeIntent, 0);

        // Constructs the Builder object.
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(getNotificationIcon())
                        .setContentTitle("Payment Request")
                        .setContentText(smallText)
                        .setAutoCancel(true)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(bigText))
                        .addAction (R.drawable.reject_icon, "REJECT", piReject)
                        .addAction (R.drawable.pay_icon, "PAY", piPay);

        NotificationManager notifyManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notifyManager.notify(0, builder.build());
    }

    private static int getNotificationIcon() {
        boolean whiteIcon = (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP);
        return whiteIcon ? R.drawable.notification_icon : R.drawable.notification_icon;
    }
}
