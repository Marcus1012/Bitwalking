package bitwalking.bitwalking.notifications;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import bitwalking.bitwalking.activityes.MainActivity;
import bitwalking.bitwalking.util.Globals;
import bitwalking.bitwalking.util.Logger;

/**
 * Created by Marcus on 6/8/16.
 */
public class BitwalkingFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = BitwalkingFirebaseMessagingService.class.getSimpleName();

    @Override
        public void onMessageReceived(RemoteMessage remoteMessage) {
        Logger.instance().Log(Logger.DEBUG, TAG, "new fcm message, to = " + remoteMessage.getTo());
        Logger.instance().Log(Logger.DEBUG, TAG, "notification sound = " + remoteMessage.getNotification().getSound());

        sendNotification(remoteMessage.getNotification(), remoteMessage.getData());
    }

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param notification FCM notification received.
     */
    private void sendNotification(RemoteMessage.Notification notification, Map<String, String> data) {
        if (null == notification)
            return;

        Intent intent = createOpenAppIntent(data.get("extra_open"), data.get("extra_data"), data.get("open_uri"));

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(Globals.getNotificationIcon())
                .setContentTitle(notification.getTitle())
                .setContentText(notification.getBody())
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            notificationBuilder.setColor(0xFF000000);
        }

//        if (android.os.Build.VERSION.SDK_INT >= 16)
//            notificationBuilder.setStyle(new Notification.BigTextStyle().bigText(notification.getBody()));

        if (null != notification.getSound())
            notificationBuilder.setSound(defaultSoundUri);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }

    private Intent createOpenAppIntent(String extraOpen, String extraData, String uri) {
        Intent intent = new Intent(this, MainActivity.class);
        if (null != extraOpen) {
            intent.putExtra(extraOpen, true);
        }

        if (null != extraData) {
            intent.putExtra("extra_data", extraData);
        }

        if (null != uri) {
            intent.putExtra(Globals.BITWALKING_OPEN_URI, uri);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }
}
