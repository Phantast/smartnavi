package com.ilm.sandwich.tools;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;

import com.ilm.sandwich.BuildConfig;
import com.ilm.sandwich.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

/**
 * Debug-only helper that captures logcat output for the current process
 * to a file in internal storage. Shows a notification with a share action
 * so you can easily export logs.
 *
 * Usage: call DebugLogHelper.dumpAndNotify(context) from onStop/onDestroy.
 * Only active in debug builds; no-ops in release.
 */
public class DebugLogHelper {

    private static final String TAG = "DebugLogHelper";
    private static final String CHANNEL_ID = "debug_log_channel";
    private static final int NOTIFICATION_ID = 9999;
    private static final String LOG_FILENAME = "smartnavi_debug.log";
    private static final int MAX_LOG_LINES = 2000;
    private static boolean crashHandlerInstalled = false;

    /**
     * Installs a global uncaught exception handler that dumps logcat before the
     * process dies. Call once from your launcher activity's onCreate.
     * Chains to the default handler (Crashlytics, system dialog, etc.) afterward.
     */
    public static void installCrashHandler(final Context appContext) {
        if (!BuildConfig.DEBUG || crashHandlerInstalled) return;
        crashHandlerInstalled = true;

        final Thread.UncaughtExceptionHandler defaultHandler =
                Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                try {
                    // Dump logcat synchronously before process dies
                    dumpLogcat(appContext);
                } catch (Exception ignored) {
                }
                // Chain to default handler (Crashlytics, system crash dialog)
                if (defaultHandler != null) {
                    defaultHandler.uncaughtException(t, e);
                }
            }
        });

        Log.d(TAG, "Debug crash handler installed");
    }

    /**
     * Dumps recent logcat for this process to a file and shows a share notification.
     * No-ops in release builds.
     */
    public static void dumpAndNotify(Context context) {
        if (!BuildConfig.DEBUG) return;

        try {
            File logFile = dumpLogcat(context);
            if (logFile != null) {
                showShareNotification(context, logFile);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to dump logcat", e);
        }
    }

    /**
     * Dumps logcat to a file without showing a notification.
     * Returns the log file path, or null on failure.
     */
    public static File dumpLogcat(Context context) {
        if (!BuildConfig.DEBUG) return null;

        try {
            int pid = android.os.Process.myPid();
            // Get recent logcat lines for our process
            Process process = Runtime.getRuntime().exec(new String[]{
                    "logcat", "-d",           // dump and exit
                    "-t", String.valueOf(MAX_LOG_LINES), // last N lines
                    "--pid=" + pid            // only our process
            });

            File logFile = new File(context.getFilesDir(), LOG_FILENAME);
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
                 FileOutputStream fos = new FileOutputStream(logFile)) {

                // Write header
                String header = "=== SmartNavi Debug Log ===\n"
                        + "Time: " + new java.util.Date() + "\n"
                        + "PID: " + pid + "\n"
                        + "Version: " + BuildConfig.VERSION_NAME
                        + " (" + BuildConfig.VERSION_CODE + ")\n"
                        + "Device: " + Build.MANUFACTURER + " " + Build.MODEL
                        + " (API " + Build.VERSION.SDK_INT + ")\n"
                        + "===========================\n\n";
                fos.write(header.getBytes());

                String line;
                while ((line = reader.readLine()) != null) {
                    fos.write((line + "\n").getBytes());
                }
                fos.flush();
            }
            process.waitFor();

            Log.d(TAG, "Logcat dumped to " + logFile.getAbsolutePath());
            return logFile;
        } catch (Exception e) {
            Log.e(TAG, "Failed to dump logcat", e);
            return null;
        }
    }

    private static void showShareNotification(Context context, File logFile) {
        NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        // Create notification channel (required for API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Debug Logs",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Shows debug log share notifications");
            nm.createNotificationChannel(channel);
        }

        // Build share intent using FileProvider
        android.net.Uri logUri = FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".debuglogprovider",
                logFile);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_STREAM, logUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Intent chooser = Intent.createChooser(shareIntent, "Share debug log");
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, chooser,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("SmartNavi Debug Log")
                .setContentText("Tap to share log file")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        nm.notify(NOTIFICATION_ID, builder.build());
    }
}
