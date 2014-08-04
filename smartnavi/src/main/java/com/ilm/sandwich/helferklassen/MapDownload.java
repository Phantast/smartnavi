/**
 * 
 */
package com.ilm.sandwich.helferklassen;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

/**
 * @author Christian
 * 
 */
public class MapDownload extends AsyncTask<String, String, String> {

	private String[] server = new String[4];
	private String randomServer;

	/**
	 * @param args
	 */
	public void initialize(double lat, double lon, int zoom, boolean closeup) {
		server[0] = "http://otile1.mqcdn.com/tiles/1.0.0/map/";
		server[1] = "http://otile2.mqcdn.com/tiles/1.0.0/map/";
		server[2] = "http://otile3.mqcdn.com/tiles/1.0.0/map/";
		server[3] = "http://otile4.mqcdn.com/tiles/1.0.0/map/";
		int random = (int) (Math.random() * 3);
		randomServer = server[random];
		Log.d("egal", "Nehme Server Nr: " + random);
	}

	@Override
	protected String doInBackground(String... params) {
		try {
			String urlStr = randomServer + 5 + "/" + 16 + "/" + 10 + ".jpg";      //     z     y    x
			URL url = new URL(urlStr);
			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			InputStream in = new BufferedInputStream(urlConnection.getInputStream());
			File newFileDir = new File(Environment.getExternalStorageDirectory().toString() + "/osmdroid/tiles/MapquestOSM/" + 5 + "/" + 16);
			newFileDir.mkdirs();
			File newFile = new File(newFileDir, 10 + ".jpg.tile");
			OutputStream output = new FileOutputStream(newFile);
			int read;
			while ((read = in.read()) != -1) {
				output.write(read);
				output.flush();
			}
			output.close();
			urlConnection.disconnect();
		} catch (Exception e) {
			Log.e("egal", e.getMessage());
			e.printStackTrace();
		}
		return null;
	}
}
