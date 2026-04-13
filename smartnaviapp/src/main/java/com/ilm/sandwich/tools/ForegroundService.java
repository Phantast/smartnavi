package com.ilm.sandwich.tools;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.ilm.sandwich.BackgroundService;
import com.ilm.sandwich.GoogleMap;
import com.ilm.sandwich.R;

public class ForegroundService extends Service {

    private static final String TAG = "ForegroundService";
    public static final String CHANNEL_ID = "SmartNaviForegroundChannel";
    public static final String ACTION_START_FOREGROUND_SERVICE = "START";
    private static final String ACTION_NOTIFICATION_DISMISSED = "com.ilm.sandwich.NOTIFICATION_DISMISSED";
    private static final int NOTIFICATION_ID = 101;

    // Guard against re-posting after the service is stopping
    private volatile boolean isStopping = false;
    private BroadcastReceiver dismissReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");

        // Register a receiver that re-posts the notification when the user swipes it away
        dismissReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Only re-post if the service is genuinely still active and not shutting down
                if (isStopping) {
                    Log.i(TAG, "Notification dismissed but service is stopping, not re-posting");
                    return;
                }
                if (!Config.backgroundServiceActive) {
                    Log.i(TAG, "Notification dismissed but service no longer active, not re-posting");
                    return;
                }
                try {
                    Log.i(TAG, "Notification dismissed by user, re-posting");
                    NotificationManager manager = getSystemService(NotificationManager.class);
                    if (manager != null) {
                        manager.notify(NOTIFICATION_ID, buildNotification());
                    }
                } catch (Exception e) {
                    // Service context may be partially dead — do NOT re-post
                    Log.e(TAG, "Failed to re-post notification, giving up", e);
                }
            }
        };
        IntentFilter filter = new IntentFilter(ACTION_NOTIFICATION_DISMISSED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(dismissReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(dismissReceiver, filter);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand");

        // If this is a restart after a crash, Core and sensors are gone.
        // Don't run as a zombie — just stop.
        if (!Config.backgroundServiceActive) {
            Log.i(TAG, "State invalid (not active), stopping self");
            cleanupNotification();
            stopSelf();
            return START_NOT_STICKY;
        }

        try {
            createNotificationChannel();
            Notification notification = buildNotification();
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
            Log.i(TAG, "startForeground called successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start foreground service", e);
            cleanupNotification();
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "SmartNavi Background Navigation",
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Shows when SmartNavi is providing location in the background");

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
            Log.i(TAG, "Notification channel created");
        } else {
            Log.e(TAG, "NotificationManager is null");
        }
    }

    private Notification buildNotification() {
        Intent notificationIntent = new Intent(this, BackgroundService.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        // PendingIntent that fires when user swipes the notification away
        Intent dismissIntent = new Intent(ACTION_NOTIFICATION_DISMISSED);
        dismissIntent.setPackage(getPackageName());
        PendingIntent deletePendingIntent = PendingIntent.getBroadcast(this,
                0, dismissIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.tx_72))
                .setContentText(getString(R.string.tx_73))
                .setSmallIcon(R.drawable.ic_stat_maps_directions_walk)
                .setContentIntent(pendingIntent)
                .setDeleteIntent(deletePendingIntent)
                .setOngoing(true)
                .setAutoCancel(false)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .build();

        notification.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
        return notification;
    }

    /**
     * Explicitly cancel the notification and prevent any further re-posting.
     */
    private void cleanupNotification() {
        try {
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.cancel(NOTIFICATION_ID);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error cancelling notification", e);
        }
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy — cleaning up");

        // Set stopping flag FIRST to prevent the receiver from re-posting
        isStopping = true;
        Config.backgroundServiceActive = false;

        // Unregister the dismiss receiver before cancelling notification
        // so the cancel doesn't trigger a re-post
        if (dismissReceiver != null) {
            try {
                unregisterReceiver(dismissReceiver);
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering receiver", e);
            }
            dismissReceiver = null;
        }

        // Explicitly cancel the notification
        cleanupNotification();

        // Unregister sensors so they don't keep running after service stops
        if (GoogleMap.mCore != null) {
            GoogleMap.mCore.pauseSensors();
            Log.i(TAG, "Sensors paused");
        }

        // Remove mock location provider
        try {
            BackgroundService.pauseFakeProvider();
            Log.i(TAG, "Fake provider removed");
        } catch (Exception e) {
            Log.e(TAG, "Error removing fake provider", e);
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
