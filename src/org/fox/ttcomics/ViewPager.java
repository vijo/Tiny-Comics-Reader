package org.fox.ttcomics;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class ViewPager extends android.support.v4.view.ViewPager {
    public ViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected boolean canScroll(View v, boolean checkV, int dx, int x, int y) {
    	if (v instanceof ImageViewTouch) {
    		ImageViewTouch ivt = (ImageViewTouch) v;
    		try {
    			return ivt.canScroll(dx);
    		} catch (NullPointerException e) {
    			// bad image, etc
    			return false;
    		}
    	} else {
    		return super.canScroll(v, checkV, dx, x, y);
    	}    	
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
    	return super.onTouchEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
    	return super.onInterceptTouchEvent(event);
    }
}