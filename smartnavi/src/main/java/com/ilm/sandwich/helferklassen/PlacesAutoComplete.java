package com.ilm.sandwich.helferklassen;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ilm.sandwich.Config;
import com.ilm.sandwich.GoogleMapActivity;

import android.database.MatrixCursor;
import android.os.AsyncTask;

public class PlacesAutoComplete extends AsyncTask<String, String, StringBuilder> {

	//private static final String	LOG_TAG				= "PlacesAutocomplete";
	private static final String	TYPE_AUTOCOMPLETE	= "/autocomplete";
	private static final String	OUT_JSON			= "/json";

	@Override
	protected StringBuilder doInBackground(String... input) {

		HttpURLConnection conn = null;
		StringBuilder jsonResults = new StringBuilder();
		
		
		String randomAPI;
		if (Math.random() > 0.5) {
			//Log.d("Zufall_Places", "API MA");
			randomAPI = Config.PLACES_API_KEY_MA;
		} else {
			//Log.d("Zufall_Places", "API CH");
			randomAPI = Config.PLACES_API_KEY_CH;
		}
		
		
		try {

			StringBuilder sb = new StringBuilder(Config.PLACES_API_URL + TYPE_AUTOCOMPLETE + OUT_JSON);
			sb.append("?sensor=true&key=" + randomAPI);
		//	sb.append("&components=country:" + Locale.getDefault().getLanguage());
			sb.append("&location=" + Rechnung.startLat + "," + Rechnung.startLon);
			sb.append("&radius=1000");
			sb.append("&input=" + URLEncoder.encode(input[0], "utf8"));

			URL url = new URL(sb.toString());
			conn = (HttpURLConnection) url.openConnection();
			InputStreamReader in = new InputStreamReader(conn.getInputStream());

			// Load the results into a StringBuilder
			int read;
			char[] buff = new char[1024];
			while ((read = in.read(buff)) != -1) {
				jsonResults.append(buff, 0, read);
			}
			
			
			
			/*
			// Fallback
			JSONObject jsonObj = new JSONObject(jsonResults.toString());
			String status = (String) jsonObj.get("status");
			 
			if (status.equals("OVER_QUERY_LIMIT")) {
				// API key change
				Config.PLACES_API_KEY = Config.PLACES_API_KEY_CH;
				Config.PLACES_API_FALLBACK++;
			}
			*/
			
		} catch (MalformedURLException e) {
			//Log.e(LOG_TAG, "Error processing Places API URL", e);
		} catch (IOException e) {
			//Log.e(LOG_TAG, "Error connecting to Places API", e);
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}

		return jsonResults;
	}

	@Override
	protected void onPostExecute(StringBuilder jsonResults) {
		try {
			// Create a JSON object hierarchy from the results
			JSONObject jsonObj = new JSONObject(jsonResults.toString());
			JSONArray predsJsonArray = jsonObj.getJSONArray("predictions");

			// builds MatrixCursor for autocomplete search view
			MatrixCursor cursor = new MatrixCursor(Config.COLUMNS);
			for (int i = 0; i < predsJsonArray.length(); i++) {
				int pos = i + 1;
				cursor.addRow(new String[] { "" + pos, predsJsonArray.getJSONObject(i).getString("description") });
			}
			
			GoogleMapActivity.cursor = cursor;
			GoogleMapActivity.changeSuggestionAdapter.sendEmptyMessage(0);
			/*
			Karte.mSuggestionsAdapter = new SuggestionsAdapter(Karte.sbContext, cursor);
			Karte.searchView.setSuggestionsAdapter(Karte.mSuggestionsAdapter);
			Karte.searchView.refreshDrawableState();
			Karte.suggestionsInProgress = false;
			*/
			//Log.d("updateAutocompleteAdapter", "Cursor was set");

		} catch (JSONException e) {
			//Log.e(LOG_TAG, "Cannot process JSON results", e);
		}
	}
}