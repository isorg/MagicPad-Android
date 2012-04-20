package com.isorg.magicpadexplorer.algorithm;


import android.util.Log;

/*
 * 
 */
public class QuartAlgorithm extends ImageAlgorithm {
	// Debugging
    private static final String TAG = "QuartAlgorithm";
    private static final boolean D = false;
    
    // Quadrant selection
    public static final int QUART_NONE = 0;
    public static final int QUART_TOP_LEFT = 1;
    public static final int QUART_BOTTOM_LEFT = 2;
    public static final int QUART_TOP_RIGHT = 3;
    public static final int QUART_BOTTOM_RIGHT = 4;
    
    //
    private FingerTipAlgorithm mFingerTip = null;
    private int mPrevQuart = QUART_NONE;
    
    // Return the quadrant the finger tip is overing. 
    public int getQuart() {
    	return mPrevQuart;
    }
    
    // Set the input fingertip algorithm
    public void setInput(FingerTipAlgorithm fta) {
    	mFingerTip = fta;
    }
    
	@Override
	public void process() {
		if(D) Log.d(TAG, "process()");
		
		if(mFingerTip == null) return;
		
		Double x = mFingerTip.getPosX();
		Double y = mFingerTip.getPosY();
		
		if(x.isNaN() || y.isNaN()) {
			mPrevQuart = QUART_NONE;
			if(D) Log.d(TAG, "nan");
			return;
		}
		
		int quart = QUART_NONE;			
		double gap = 0.05;
		// clearly in a quart
		if(x >= gap && y >= gap) quart = QUART_BOTTOM_RIGHT;
		else if(x >= gap && y <= -gap) quart = QUART_TOP_RIGHT;
		else if(x <= -gap && y >= gap) quart = QUART_BOTTOM_LEFT;
		else if(x <= -gap && y <= -gap) quart = QUART_TOP_LEFT;
		else {
			// in the middle area or between to quart
			if(mPrevQuart == QUART_TOP_LEFT && (x >= 0 || y >=0)) quart = QUART_NONE;
			else if(mPrevQuart == QUART_BOTTOM_LEFT && (x >= 0 || y <=0)) quart = QUART_NONE;
			else if(mPrevQuart == QUART_TOP_RIGHT && (x <= 0 || y >=0)) quart = QUART_NONE;
			else if(mPrevQuart == QUART_BOTTOM_RIGHT && (x <= 0 || y <=0)) quart = QUART_NONE;
			else quart = mPrevQuart;
		}

		mPrevQuart = quart;		
		if(D) Log.d(TAG, "quart:" + quart);
	}
	
	// Update pipeline
	@Override
    public void update() {
    	if(mFingerTip != null)
    	{
    		mFingerTip.update();    		
    		if(mFingerTip.getMTime() > getMTime()) {
    			this.process();
    			changed();
    		}
    	}
    }
}
