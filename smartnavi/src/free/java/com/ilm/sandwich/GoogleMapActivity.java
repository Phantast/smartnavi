package com.ilm.sandwich;

import android.os.Bundle;
import android.os.Handler;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Window;


/**
 * @author Christian Henke
 *         www.smartnavi-app.com
 */
public class GoogleMapActivity extends SherlockFragmentActivity {

    public static boolean backgroundServiceShallBeOnAgain = false;
    public static Handler listHandler;
    public static Handler changeSuggestionAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        super.onCreate(savedInstanceState);
        Config.usingGoogleMaps = true;
        setContentView(R.layout.googlemap_layout);
    }


}