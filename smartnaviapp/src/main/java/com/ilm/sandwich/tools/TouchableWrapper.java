package com.ilm.sandwich.tools;

/**
 * This class helps to detect TouchEvents on GoogleMap
 * and disables the followMe function if user moves the map
 * Thanks to Stackoverflow: Gaucho
 */

import android.content.Context;
import android.view.MotionEvent;
import android.widget.FrameLayout;

public class TouchableWrapper extends FrameLayout {

    private static final int TOUCH_THRESHOLD = 10;

    public interface OnMapTouchListener {
        void onMapTouched();
    }

    private OnMapTouchListener listener;
    private int touchCounter = 0;

    public TouchableWrapper(Context context) {
        super(context);
    }

    public void setOnMapTouchListener(OnMapTouchListener listener) {
        this.listener = listener;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                touchCounter++;
                if (touchCounter >= TOUCH_THRESHOLD) {
                    if (listener != null) {
                        listener.onMapTouched();
                    }
                    touchCounter = 0;
                }
                break;
        }
        return super.dispatchTouchEvent(event);
    }
}
