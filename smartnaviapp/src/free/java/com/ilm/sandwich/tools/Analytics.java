package com.ilm.sandwich.tools;

/**
 * Class for Tracking
 * Normally this class checks wether user accepts tracking and sends events to Fabric
 * but as this is for the free build flavor Crashlytics is not included
 * created by: Christian
 */
public class Analytics {

    private boolean trackingAllowed;

    public Analytics(boolean trackingAllowed) {
        this.trackingAllowed = trackingAllowed;
    }

    public void trackEvent(String eventName, String eventType) {
        //We are in free version (for F-Droid) which does not include Crashlytics
        return;
    }
}

