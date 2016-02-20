package com.ilm.sandwich.tools;

/**
 * This class helps to detect TouchEvents on GoogleMap
 * and disables the followMe function if user moves the map
 * Thanks to Stackoverflow: Gaucho
 */

import android.content.Context;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import com.ilm.sandwich.GoogleMap;

public class TouchableWrapper extends FrameLayout {

    int touchCounter = 0;

    public TouchableWrapper(Context context) {
        super(context);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                touchCounter++;
                if (touchCounter >= 10) { //Count for enough motion before disabling followMe
                    GoogleMap.listHandler.sendEmptyMessage(15); //set followMe=false
                    touchCounter = 0;
                }
                break;
        }
        return super.dispatchTouchEvent(event);
    }
}
