package com.ilm.sandwich.sensors;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handles GPX file export for position tracks.
 * Writes to app-private storage during tracking, then copies to Downloads on close.
 * All file I/O is performed on a background thread.
 */
public class GpxExporter {

    private static final String TAG = "GpxExporter";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final Context context;
    private File posFile;
    private String posFileName;
    private boolean positionsFileNotExisting = true;

    public GpxExporter(Context context) {
        this.context = context.getApplicationContext();
    }

    public void writePosition(double lat, double lon, boolean newStep) {
        executor.execute(() -> positionOutput(lat, lon, newStep));
    }

    public void closeLogFile() {
        executor.execute(() -> {
            if (!positionsFileNotExisting && posFile != null) {
                try (BufferedWriter out = new BufferedWriter(new FileWriter(posFile, true))) {
                    out.newLine();
                    out.write("</trkseg></trk></gpx>");
                } catch (Exception e) {
                    Log.e(TAG, "Failed to close GPX file", e);
                }
                // Copy finished GPX to user-accessible Downloads folder
                copyToDownloads(posFile, posFileName);
            }
            positionsFileNotExisting = true;
        });
    }

    private void positionOutput(double startLat, double startLon, boolean newStep) {
        try {
            File folder = getExportDir();
            if (folder == null) return;

            if (positionsFileNotExisting) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.GERMAN);
                String currentDateAndTime = sdf.format(new Date());
                posFileName = "track_" + currentDateAndTime + ".gpx";
                posFile = new File(folder, posFileName);

                TimeZone tz = TimeZone.getTimeZone("UTC");
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ", Locale.GERMAN);
                df.setTimeZone(tz);
                String nowAsISO = df.format(new Date());

                try (BufferedWriter out = new BufferedWriter(new FileWriter(posFile))) {
                    out.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?><gpx> <trk><name>SmartNavi " + nowAsISO
                            + "</name><number>1</number><trkseg>");
                }
                positionsFileNotExisting = false;
                Log.i(TAG, "GPX file created: " + posFile.getAbsolutePath());
            } else if (newStep) {
                TimeZone tz = TimeZone.getTimeZone("UTC");
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ", Locale.GERMAN);
                df.setTimeZone(tz);
                String nowAsISO = df.format(new Date());

                try (BufferedWriter out = new BufferedWriter(new FileWriter(posFile, true))) {
                    out.newLine();
                    out.write("<trkpt lat=\"" + startLat + "\" lon=\"" + startLon + "\"><time>" + nowAsISO + "</time></trkpt>");
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to write GPX position", e);
        }
    }

    /**
     * Returns the app-private export directory. Works on all API levels, no permission needed.
     */
    private File getExportDir() {
        File dir = context.getExternalFilesDir("gpx");
        if (dir != null && !dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /**
     * Copies the finished GPX file to a user-visible location.
     * API 29+: Uses MediaStore to put it in Downloads/SmartNavi/.
     * API < 29: Copies directly to external storage /SmartNavi/.
     */
    private void copyToDownloads(File sourceFile, String fileName) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // MediaStore approach for scoped storage
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, "application/gpx+xml");
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/SmartNavi");

                Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    try (FileInputStream fis = new FileInputStream(sourceFile);
                         OutputStream os = context.getContentResolver().openOutputStream(uri)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = fis.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                    }
                    Log.i(TAG, "GPX saved to Downloads/SmartNavi/" + fileName);
                }
            } else {
                // Legacy approach for API < 29
                File downloadsDir = new File(Environment.getExternalStorageDirectory(), "SmartNavi");
                if (!downloadsDir.exists()) downloadsDir.mkdirs();
                File destFile = new File(downloadsDir, fileName);
                try (FileInputStream fis = new FileInputStream(sourceFile);
                     java.io.FileOutputStream fos = new java.io.FileOutputStream(destFile)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                }
                Log.i(TAG, "GPX saved to " + destFile.getAbsolutePath());
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to copy GPX to Downloads", e);
        }
    }
}
