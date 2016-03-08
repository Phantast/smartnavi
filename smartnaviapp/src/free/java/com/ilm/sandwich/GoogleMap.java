package com.ilm.sandwich;

import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;

import com.ilm.sandwich.tools.Config;


/**
 * @author Christian Henke
 *         www.smartnavi-app.com
 */
public class GoogleMap extends AppCompatActivity {

    public static boolean backgroundServiceShallBeOnAgain = false;
    public static Handler listHandler;
    public static Bitmap drawableDest;
    public static MatrixCursor cursor;
    public static Handler changeSuggestionAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Config.usingGoogleMaps = true;
        setContentView(R.layout.activity_googlemap);
    }


}