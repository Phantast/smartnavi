package org.osmdroid.bonuspack.overlays;

import android.graphics.Point;
import android.graphics.drawable.Drawable;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.OverlayItem;

/**
 * An OverlayItem to use in ItemizedOverlayWithBubble<br>
 * - more complete: can contain an image and a sub-description that will be displayed in the bubble, <br>
 * - and flexible: attributes are modifiable<br>
 * <p/>
 * Known Issues:<br>
 * - Bubble offset is not perfect on h&xhdpi resolutions, due to an osmdroid issue on marker drawing<br>
 * - Bubble offset is at 0 when using the default marker => set the marker on each item!<br>
 *
 * @author M.Kergall
 * @see ItemizedOverlayWithBubble
 */
@Deprecated
public class ExtendedOverlayItem extends OverlayItem {

    private String mTitle, mSnippet; // now, they are modifiable
    private String mSubDescription; //a third field that can be displayed in the infowindow, on a third line
    private Drawable mImage; //that will be shown in the infowindow.
    //private GeoPoint mGeoPoint //unfortunately, this is not so simple...
    private Object mRelatedObject; //reference to an object (of any kind) linked to this item.
    private float mAlpha;

    public ExtendedOverlayItem(String aTitle, String aDescription, GeoPoint aGeoPoint) {
        super(aTitle, aDescription, aGeoPoint);
        mTitle = aTitle;
        mSnippet = aDescription;
        mSubDescription = null;
        mImage = null;
        mRelatedObject = null;
        mAlpha = 1.0f; //default = opaque
    }

    public void setSnippet(String snippet) {
        mSnippet = snippet;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public String getDescription() {
        return mSnippet;
    }

    public String getSubDescription() {
        return mSubDescription;
    }

    public void setSubDescription(String aSubDescription) {
        mSubDescription = aSubDescription;
    }

    public Drawable getIcon() {
        return mMarker;
    }

    /**
     * set the marker icon
     */
    public void setIcon(Drawable icon) {
        setMarker(icon);
    }

    public float getAlpha() {
        return mAlpha;
    }

    public void setAlpha(float alpha) {
        mAlpha = alpha;
        mMarker.setAlpha((int) (alpha * 255));
    }

    public Drawable getImage() {
        return mImage;
    }

    /**
     * set the image to be shown in the infowindow - this is not the marker icon.
     */
    public void setImage(Drawable anImage) {
        mImage = anImage;
    }

    public Object getRelatedObject() {
        return mRelatedObject;
    }

    public void setRelatedObject(Object o) {
        mRelatedObject = o;
    }

    /**
     * From a HotspotPlace and drawable dimensions (width, height), return the hotspot position.
     * Could be a public method of HotspotPlace or OverlayItem...
     */
    public Point getHotspot(HotspotPlace place, int w, int h) {
        Point hp = new Point();
        if (place == null)
            place = HotspotPlace.BOTTOM_CENTER; //use same default than in osmdroid.
        switch (place) {
            case NONE:
                hp.set(0, 0);
                break;
            case BOTTOM_CENTER:
                hp.set(w / 2, 0);
                break;
            case LOWER_LEFT_CORNER:
                hp.set(0, 0);
                break;
            case LOWER_RIGHT_CORNER:
                hp.set(w, 0);
                break;
            case CENTER:
                hp.set(w / 2, -h / 2);
                break;
            case LEFT_CENTER:
                hp.set(0, -h / 2);
                break;
            case RIGHT_CENTER:
                hp.set(w, -h / 2);
                break;
            case TOP_CENTER:
                hp.set(w / 2, -h);
                break;
            case UPPER_LEFT_CORNER:
                hp.set(0, -h);
                break;
            case UPPER_RIGHT_CORNER:
                hp.set(w, -h);
                break;
        }
        return hp;
    }

    /**
     * Open the infowindow on the item.
     * Centers the map view on the item if panIntoView is true. <br>
     */
    public void showBubble(InfoWindow bubble, MapView mapView, boolean panIntoView) {
        //offset the bubble to be top-centered on the marker:
        Drawable marker = getMarker(0 /*OverlayItem.ITEM_STATE_FOCUSED_MASK*/);
        int markerWidth = 0, markerHeight = 0;
        if (marker != null) {
            markerWidth = marker.getIntrinsicWidth();
            markerHeight = marker.getIntrinsicHeight();
        } //else... we don't have the default marker size => don't use default markers!!!
        Point markerH = getHotspot(getMarkerHotspot(), markerWidth, markerHeight);
        Point bubbleH = getHotspot(HotspotPlace.TOP_CENTER, markerWidth, markerHeight);
        bubbleH.offset(-markerH.x, -markerH.y);

        bubble.open(this, this.getPoint(), bubbleH.x, bubbleH.y);
        if (panIntoView)
            mapView.getController().animateTo(getPoint());
    }
}
