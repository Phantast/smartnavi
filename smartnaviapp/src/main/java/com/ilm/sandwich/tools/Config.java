package com.ilm.sandwich.tools;

import android.app.SearchManager;
import android.provider.BaseColumns;

/**
 * @author Christian Henke
 *         www.smartnavi-app.com
 */
public class Config {
    //app-Rate Dialog
    public final static String APP_PNAME = "com.ilm.sandwich";
    public final static int DAYS_UNTIL_PROMPT = 2;
    public final static int LAUNCHES_UNTIL_PROMPT = 2;
    // places api
    public static final String PLACES_API_URL = "https://maps.googleapis.com/maps/api/place";
    public static final String PLACES_API_KEY = "AIzaSyAT3ahsjBZZtWZMzcMy-AJffVfVGLZPdMw";
    public static final String[] COLUMNS = {BaseColumns._ID, SearchManager.SUGGEST_COLUMN_TEXT_1,};
    // statistics
    public static final String SMARTNAVI_API_URL = "http://5.39.91.189:3000/users";
    //permission integers
    public static final int PERMISSION_REQUEST_FINE_LOCATION = 0;
    public static final int PERMISSION_WRITE_EXTERNAL_STORAGE = 1;
    public static final int PLACES_SEARCH_QUERY_CHARACTER_LIMIT = 3;
    public static boolean PLACES_API_UNDER_LIMIT = true;
    //other
    public static boolean backgroundServiceActive = false;
}
