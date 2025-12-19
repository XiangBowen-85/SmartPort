package com.example.smartport;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

public class NotifyHelper {
    public static final String CHANNEL_ID = "smartport_status";
    public static final int NOTIF_ID = 1001;

    public static void ensureChannel(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID,
                    "SmartPort Status",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            ch.setDescription("Estado de solicitudes y sensores");
            NotificationManager nm = ctx.getSystemService(NotificationManager.class);
            nm.createNotificationChannel(ch);
        }
    }
}
