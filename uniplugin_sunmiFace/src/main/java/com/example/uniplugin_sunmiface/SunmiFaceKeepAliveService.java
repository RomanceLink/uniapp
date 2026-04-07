package com.example.uniplugin_sunmiface;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

public class SunmiFaceKeepAliveService extends Service {
    private static final String CHANNEL_ID = "sunmi_face_keep_alive";
    private static final int NOTIFICATION_ID = 40963;

    public static void start(Context context) {
        if (context == null) {
            return;
        }
        Intent intent = new Intent(context, SunmiFaceKeepAliveService.class);
        ContextCompat.startForegroundService(context, intent);
    }

    public static void stop(Context context) {
        if (context == null) {
            return;
        }
        Intent intent = new Intent(context, SunmiFaceKeepAliveService.class);
        context.stopService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ensureChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("人脸识别运行中")
                .setContentText("正在保持相机识别页面活跃")
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
        startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null || manager.getNotificationChannel(CHANNEL_ID) != null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Sunmi Face Keep Alive",
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("用于保持人脸识别页面存活");
        manager.createNotificationChannel(channel);
    }
}
