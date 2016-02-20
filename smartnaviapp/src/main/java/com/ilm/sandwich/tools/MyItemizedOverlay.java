package com.ilm.sandwich.tools;

import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;

import org.osmdroid.ResourceProxy;
import org.osmdroid.api.IMapView;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.OverlayItem.HotspotPlace;

import java.util.ArrayList;

/**
 * Used for OsmMap
 */
public class MyItemizedOverlay extends ItemizedOverlay<OverlayItem> {

    private ArrayList<OverlayItem> overlayItemList = new ArrayList<OverlayItem>();

    public MyItemizedOverlay(Drawable pDefaultMarker, ResourceProxy pResourceProxy) {
        super(pDefaultMarker, pResourceProxy);
    }

    public void addItem(GeoPoint p, String title, String snippet) {
        OverlayItem newItem = new OverlayItem(title, snippet, p);
        newItem.setMarkerHotspot(HotspotPlace.CENTER);
        overlayItemList.add(newItem);
        populate();
    }

    @Override
    public boolean onSnapToItem(int arg0, int arg1, Point arg2, IMapView arg3) {
        return false;
    }

    @Override
    protected OverlayItem createItem(int arg0) {
        return overlayItemList.get(arg0);
    }

    @Override
    public int size() {
        return overlayItemList.size();
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent arg0, MapView arg1) {
        return super.onSingleTapConfirmed(arg0, arg1);
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e, MapView mapView) {
        return super.onSingleTapUp(e, mapView);
    }


}
