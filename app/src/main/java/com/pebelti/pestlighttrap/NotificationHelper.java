package com.pebelti.pestlighttrap;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class NotificationHelper {

    private static final String CHANNEL_ID_CRITICAL = "pest_trap_critical";
    private static final String CHANNEL_ID_WARNING = "pest_trap_warning";
    private static final String CHANNEL_ID_INFO = "pest_trap_info";

    private final Context context;
    private final NotificationManager notificationManager;

    public NotificationHelper(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannels();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Channel untuk notifikasi kritis (baterai lemah, trap penuh)
            NotificationChannel criticalChannel = new NotificationChannel(
                    CHANNEL_ID_CRITICAL,
                    "Peringatan Kritis",
                    NotificationManager.IMPORTANCE_HIGH
            );
            criticalChannel.setDescription("Notifikasi penting seperti baterai lemah atau trap penuh");
            criticalChannel.enableVibration(true);
            criticalChannel.setVibrationPattern(new long[]{0, 500, 200, 500});

            // Channel untuk notifikasi peringatan
            NotificationChannel warningChannel = new NotificationChannel(
                    CHANNEL_ID_WARNING,
                    "Peringatan",
                    NotificationManager.IMPORTANCE_HIGH
            );
            warningChannel.setDescription("Notifikasi peringatan seperti efisiensi rendah");
            warningChannel.enableVibration(true);

            // Channel untuk notifikasi info/sukses
            NotificationChannel infoChannel = new NotificationChannel(
                    CHANNEL_ID_INFO,
                    "Informasi",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            infoChannel.setDescription("Notifikasi informasi status perangkat");

            notificationManager.createNotificationChannel(criticalChannel);
            notificationManager.createNotificationChannel(warningChannel);
            notificationManager.createNotificationChannel(infoChannel);
        }
    }

    /**
     * Menampilkan notifikasi pop-up di HP pengguna.
     *
     * @param notificationId ID unik untuk notifikasi (agar bisa di-update/replace)
     * @param title          Judul notifikasi
     * @param message        Isi pesan notifikasi
     * @param type           Tipe: "CRITICAL", "WARNING", "SUCCESS", "INFO"
     * @param iconResId      Resource ID untuk ikon kecil
     */
    public void showNotification(int notificationId, String title, String message, String type, int iconResId) {
        String channelId;
        int priority;

        switch (type) {
            case "CRITICAL":
                channelId = CHANNEL_ID_CRITICAL;
                priority = NotificationCompat.PRIORITY_HIGH;
                break;
            case "WARNING":
                channelId = CHANNEL_ID_WARNING;
                priority = NotificationCompat.PRIORITY_HIGH;
                break;
            case "SUCCESS":
            case "INFO":
            default:
                channelId = CHANNEL_ID_INFO;
                priority = NotificationCompat.PRIORITY_DEFAULT;
                break;
        }

        // Intent untuk membuka aplikasi saat notifikasi di-tap
        Intent intent = new Intent(context, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, notificationId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(iconResId)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(priority)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // Untuk notifikasi kritis, gunakan heads-up notification (pop-up)
        if ("CRITICAL".equals(type) || "WARNING".equals(type)) {
            builder.setDefaults(NotificationCompat.DEFAULT_ALL);
            builder.setFullScreenIntent(pendingIntent, true);
        }

        notificationManager.notify(notificationId, builder.build());
    }
}
