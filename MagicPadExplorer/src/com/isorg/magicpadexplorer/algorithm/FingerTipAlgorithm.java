package com.isorg.magicpadexplorer.algorithm;

import android.util.Log;

/*
 * Find finger tip position in image
 */
public class FingerTipAlgorithm extends ImageAlgorithm {	
	// Debugging
    private static final String TAG = "FingerTipAlgorithm";
    private static final boolean D = false;
    
	// Finger tip coordinate
	private Double mX = 0.0;
	private Double mY = 0.0;
	
	// damping coefficient
	private double mDamping = 0.8;
	
	// otsu algorithm input
	OtsuAlgorithm mOtsu = null;
	
	public void setOtsuInput(OtsuAlgorithm otsu) {
		mOtsu = otsu;
	}
	
	public Double getPosX() { return mX; }
	public Double getPosY() { return mY; }

	@Override
	public void process() {
		if(D) Log.d(TAG, "process()");
		
		if(mInputFrame == null) return;
		
		// get threshold
		double thres = 128;
		if(mOtsu != null) {
			mOtsu.update();
			thres = mOtsu.getThreshold();			
			if(!mOtsu.isObjectDetected()) {
				mX = Double.NaN;
				mY = Double.NaN;
				//if(D) Log.d(TAG, "otsu says no object: nan");
				return;
			} else {
				mX = 0.0;
				mY = 0.0;
			}
		}		
				
		// fingertip computation
		int W = 0;		// weight	
		int rowFlag = 3;	// max number of line that contributes
		double x = 0;
		double y = 0;
		for(int i=0; i<10; i++)
        {
            boolean emptyRow = true;
            for(int j=0; j<10; j++)
            {
                int w = mInputFrame.data[i*10 + j] & 0xff;

                if(rowFlag>=0 && w < thres)
                {
                    if(emptyRow == true)
                    {
                        emptyRow = false;
                        rowFlag--;
                    }
                    x += (255-w) * (j-5) / 10.0;
                    y += (255-w) * (i-5) / 10.0;
                    W += (255-w);
                }
            }
        }

        // Normalize barycenter
		if(W == 0) {
			mX = 0.0;
			mY = 0.0;
		} else {
			mX += (x/W - mX)*mDamping;
			mY += (y/W - mY)*mDamping;
			if(D) Log.d(TAG, mX + " " + mY + " " + W);
		}
		
	}
	
	public void update() {
		if(D) Log.d(TAG, "update()");
		super.update();
	}
	

}
