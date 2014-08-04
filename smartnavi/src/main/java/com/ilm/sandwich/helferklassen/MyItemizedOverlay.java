package com.ilm.sandwich.helferklassen;

import java.util.ArrayList;

import org.osmdroid.ResourceProxy;
import org.osmdroid.api.IMapView;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.OverlayItem.HotspotPlace;

import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;

public class MyItemizedOverlay extends ItemizedOverlay<OverlayItem> {

	private ArrayList<OverlayItem> overlayItemList = new ArrayList<OverlayItem>();

	public MyItemizedOverlay(Drawable pDefaultMarker, ResourceProxy pResourceProxy) {
		super(pDefaultMarker, pResourceProxy);
		// TODO Auto-generated constructor stub
	}

	public void addItem(GeoPoint p, String title, String snippet) {
		OverlayItem newItem = new OverlayItem(title, snippet, p);
		newItem.setMarkerHotspot(HotspotPlace.CENTER);
		overlayItemList.add(newItem);
		populate();
	}

	@Override
	public boolean onSnapToItem(int arg0, int arg1, Point arg2, IMapView arg3) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected OverlayItem createItem(int arg0) {
		// TODO Auto-generated method stub
		return overlayItemList.get(arg0);
	}

	@Override
	public int size() {
		// TODO Auto-generated method stub
		return overlayItemList.size();
	}

	@Override
	public boolean onSingleTapConfirmed(MotionEvent arg0, MapView arg1) {
		// TODO Auto-generated method stub
		return super.onSingleTapConfirmed(arg0, arg1);
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e, MapView mapView) {
		// TODO Auto-generated method stub
		return super.onSingleTapUp(e, mapView);
	}
	
	

}
