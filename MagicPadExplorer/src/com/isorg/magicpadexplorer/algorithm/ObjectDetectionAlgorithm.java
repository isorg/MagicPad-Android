package com.isorg.magicpadexplorer.algorithm;


import android.util.Log;

/*
 * Detect if an object is present in the input image
 */
public class ObjectDetectionAlgorithm extends ImageAlgorithm {
	// Debugging
    private static final String TAG = "ObjectDetectionAlgorithm";
    private static final boolean D = true;
    
	// percentage of frame coverage
	private double mObjThreshold = 0.98;
	private double mMean = 0;

	@Override
	public void process() {
		//if(D) Log.d(TAG, "process() at t=" + mTime);
		mMean = mean();		
	}
	
	
	// set object detection threshold in percentage of coverage
	public void setObjectThreshold(double thres) {
		if(thres>=0 && thres<=1) {
			mObjThreshold = thres;
		}
	}
	
    // return > 0 if an object is present in the image
    public boolean isObjectPresent()
    {
    	return mMean < (255.0 * mObjThreshold);
    }

    // debug
    public double mean()
    {   	
    	if(mInputFrame == null) return -1;
    	
    	if(mInputFrame.data.length == 0) return 0;
    	
    	double m = 0;
    	for(int idx=0; idx<mInputFrame.data.length; idx++)
    	{
    		m += (double)(mInputFrame.data[idx] & 0xff);
    	}
    	
    	return m/mInputFrame.data.length;
    }

}
