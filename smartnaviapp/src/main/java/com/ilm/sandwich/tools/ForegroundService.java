package com.ilm.sandwich.tools;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.ilm.sandwich.BackgroundService;
import com.ilm.sandwich.R;
import com.ilm.sandwich.sensors.Core;

public class ForegroundService extends Service implements Core.onStepUpdateListener {

    public static String CHANNEL_ID = "SmartNaviBackgroundServiceChannel";
    public static String ACTION_START_FOREGROUND_SERVICE = "START";

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForegroundService();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.i("BackgroundService", "SmartNavi foreground service onDestroy().");
        super.onDestroy();
    }

    @SuppressLint("NewApi")
    private void startForegroundService() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "SmartNaviBackgroundServiceChannel",
                NotificationManager.IMPORTANCE_DEFAULT);

        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(serviceChannel);
        Intent notificationIntent = new Intent(this, BackgroundService.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getApplicationContext().getResources().getString(R.string.tx_72))
                .setContentText(getApplicationContext().getResources().getString(R.string.tx_73))
                .setSmallIcon(R.drawable.ic_stat_maps_directions_walk)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onStepUpdate(int event) {
    }
}
