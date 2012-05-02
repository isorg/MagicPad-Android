
// class to manage a simple rotation control

package com.android.lemon;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.FloatMath;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MotionEvent;


class TouchSurfaceView extends GLSurfaceView {

    public TouchSurfaceView(Context context) {
        super(context);
    }

	@Override public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX();
        float y = e.getY();
        switch (e.getAction()) {
        case MotionEvent.ACTION_DOWN:			// one touch: drag
			if (mode == NONE)
				mode = DRAG;
			mPreviousX = -1.0f;
            mPreviousY = -1.0f;
			break;
		case MotionEvent.ACTION_POINTER_DOWN:	// two touches: zoom
			mPreviousX = -1.0f;
            mPreviousY = -1.0f;
			break;
		case MotionEvent.ACTION_UP:				// no mode
			mode = NONE;
			oldDist = -1.0f;
			mPreviousX = -1.0f;
            mPreviousY = -1.0f;
			break;
		case MotionEvent.ACTION_POINTER_UP:		// no mode
			oldDist = -1.0f;
			mPreviousX = -1.0f;
            mPreviousY = -1.0f;
			break;
        case MotionEvent.ACTION_MOVE:
        	if (e.getPointerCount() > 1 ) {
        		if (oldDist <= 0.0f)
        			oldDist = spacing(e);
        		if (oldDist > 10.0f) {
        			newDist = spacing(e);
        			if (newDist > 10.0f) {
        				float scale = newDist /oldDist; // scale
        				// 	scale in the renderer
        				//renderer.changeScale(scale);
        				Log.d("Zoom: ", "" + scale );
        				mRenderer.mScale *= scale;

        				oldDist = newDist;
        			}
				} 
        		mPreviousX = -1.0f;
	            mPreviousY = -1.0f;
			}
			else if ((e.getPointerCount() == 1)){
				if (mPreviousX < 0.0f)
				{
					mPreviousX = x;	mPreviousY = y;
				}
				
	            float dx = x - mPreviousX;
	            float dy = y - mPreviousY;
	            
	            // Todo : call a method instead
	            mRenderer.mAngleX += dx * TOUCH_SCALE_FACTOR;
	            mRenderer.mAngleY += dy * TOUCH_SCALE_FACTOR;
	            
	            mPreviousX = x;
	            mPreviousY = y;
			}
        } 
          
        return true;
    }
	
	// touch events
	private final int NONE = 0;
	private final int DRAG = 0;
	private final int ZOOM = 0;
	
	// pinch to zoom
	float oldDist = 100.0f;
	float newDist;

	int mode = 0;

	
	// finds spacing
	private float spacing(MotionEvent event) {
		float x = event.getX(0) - event.getX(1);
		float y = event.getY(0) - event.getY(1);
		return FloatMath.sqrt(x * x + y * y);
	}
    
	@Override
	public void setRenderer(Renderer renderer) {
		super.setRenderer(renderer);
		mRenderer = (ModelRenderer) renderer;
        //setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
	}

    private final float TOUCH_SCALE_FACTOR = 180.0f / 320;
    private ModelRenderer mRenderer;
    private float mPreviousX;
    private float mPreviousY;

}
