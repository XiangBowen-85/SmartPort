package com.example.smartport;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class WeightRequestService extends Service {

    public static final String EXTRA_TEXT = "extra_text";
    public static final String EXTRA_TITLE = "extra_title";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String title = "SmartPort";
        String text = "Procesando...";

        if (intent != null) {
            if (intent.hasExtra(EXTRA_TITLE)) title = intent.getStringExtra(EXTRA_TITLE);
            if (intent.hasExtra(EXTRA_TEXT))  text = intent.getStringExtra(EXTRA_TEXT);
        }

        NotifyHelper.ensureChannel(this);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NotifyHelper.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)   // ✅请确保这个图标存在
                .setContentTitle(title)
                .setContentText(text)
                .setOnlyAlertOnce(true);

        startForeground(NotifyHelper.NOTIF_ID, builder.build());
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
