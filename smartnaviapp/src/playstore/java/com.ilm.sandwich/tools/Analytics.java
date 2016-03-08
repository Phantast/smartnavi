package com.ilm.sandwich.tools;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;

/**
 * Class for Tracking
 * Check wether user accepts tracking and sends events to Fabric
 * created by: Christian
 */
public class Analytics {

    private boolean trackingAllowed;

    public Analytics(boolean trackingAllowed){
        this.trackingAllowed = trackingAllowed;
    }

    public void trackEvent(String product, String action){
        if(trackingAllowed){
            Answers.getInstance().logCustom(new CustomEvent(product).putCustomAttribute("Action", action));
        }else{
            //Tracking is not allowed by user;
        }
    }
}

