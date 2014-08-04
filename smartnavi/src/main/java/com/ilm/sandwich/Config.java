package com.ilm.sandwich;

import android.app.SearchManager;
import android.provider.BaseColumns;

public class Config {

	//debug
	public static boolean			debugModus			= false;
	
	//google or OSM
	public static boolean			usingGoogleMaps;
	
	//app-Rate Dialog
	public final static 			String APP_PNAME 	= "com.ilm.sandwich";
	public final static int 		DAYS_UNTIL_PROMPT 	= 2; // 2
	public final static int 		LAUNCHES_UNTIL_PROMPT = 3; // 3
	
	// autocomplete
	public static final String		PLACES_API_URL		= "https://maps.googleapis.com/maps/api/place";
	public static final String		PLACES_API_KEY_MA	= "AIzaSyCh3nOv_wMsNwfbJa0vMSFII4x_p4hhZh8";
	public static final String		PLACES_API_KEY_CH	= "AIzaSyAT3ahsjBZZtWZMzcMy-AJffVfVGLZPdMw";
	public static String			PLACES_API_KEY		= PLACES_API_KEY_MA;
	public static int				PLACES_API_FALLBACK	= 0;
	public static final String[]	COLUMNS				= { BaseColumns._ID, SearchManager.SUGGEST_COLUMN_TEXT_1, };

	// statistics
	public static final String		VSERVER_API_URL		= "http://www.api.2imba.de";
	public static final String		VSERVER_API_HEADER	= "Apache-HttpClient/SmartNaviV1.3/TomJumpedOverTheChickensHome-DoNotSpeakWithTheBusdriver";
	
	//tracking data
	public static boolean			serviceGesehen		= false;
	public static boolean			exportBenutzt		= false;
	public static long 				hintergrundStartzeit = 0;
	public static long				hintergrundEndzeit	= 0;
	public static boolean 			hintergrundAn		= false;
}
