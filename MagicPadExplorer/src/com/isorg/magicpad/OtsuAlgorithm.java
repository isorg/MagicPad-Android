package com.isorg.magicpad;

import java.util.Arrays;

import android.util.Log;

/*
 * This class implements Otsu's segmentation algorithm
 */
public class OtsuAlgorithm extends ImageAlgorithm {
	// Debugging
    private static final String TAG = "OtsuAlgorithm";
    private static final boolean D = false;
    
    private static final int OBJECT_DETECTION_THRESHOLD = 220;    
	private double mThreshold = 0;	 
	private double mMean = 0;
	private double mObjMean = 0;

	@Override
	public void process() {
		if(D) Log.d(TAG, "process() at t=" + mTime);
		
		if(mInputFrame == null) {
			return;
		}
		
		// compute threshold
		mThreshold = otsu();
		
		// compute mean
		mMean = 0;
		for(int idx=0; idx<mInputFrame.data.length; ++idx)
			mMean += mInputFrame.data[idx] & 0xff;
		mMean /= mInputFrame.data.length;			
		
		// segmentation
		segmentation();
	}

	/*
	 * Return otsu's threshold
	 */
	public double getThreshold() {
		return mThreshold;
	}
	
	/*
	 * Tell if an object is present 
	 */
	public boolean isObjectDetected() {
		
		return (mThreshold < OBJECT_DETECTION_THRESHOLD) /*&& (mMean < (255*0.90))*/;
	}
	
	/*
	 * Get the average value of the object area
	 */
	public double getObjectMean() {
		return mObjMean;
	}
	
	/*
	 * Compute Otsu threshold
	 */
	private double otsu() {
		int np;
	    int thresholdValue = 255;
	    int[] ihist = new int[256];
	    
	    int i, j, k;	// various counters
	    int n, n1, n2, gmin, gmax;
	    double m1, m2, sum, csum, fmax, sb;

	    // set ihist to '0'
	    Arrays.fill(ihist, 0);

	    gmin = 255; 
	    gmax = 0;

	    for(i=0; i<10; i++) {
	        for (j=0; j<10; j++) {
	        	np = mInputFrame.data[i*10 + j] & 0xff;
	            ihist[np]++;
	            if(np > gmax) gmax = np;
	            if(np < gmin) gmin = np;
	            np++;
	        }
	    }

	    // set up everything
	    sum = csum = 0.0;
	    n = 0;

	    for (k = 0; k <= 255; k++) {
	        sum += (double) k * (double) ihist[k];
	        n   += ihist[k];
	    }

	    if(n == 0) {
	        // if n has no value, there is problems...
	        return 160;
	    }

	    // do the otsu global thresholding method
	    fmax = -1.0;
	    n1 = 0;
	    for (k = 0; k < 255; k++) {
	        n1 += ihist[k];
	        if (n1 == 0) { continue; }
	        n2 = n - n1;
	        if (n2 == 0) { break; }
	        csum += (double) k *ihist[k];
	        m1 = csum / n1;
	        m2 = (sum - csum) / n2;
	        sb = (double) n1 *(double) n2 *(m1 - m2) * (m1 - m2);
	        /* bbg: note: can be optimized. */
	        if (sb > fmax) {
	            fmax = sb;
	            thresholdValue = k;
	        }
	    }

	    return thresholdValue;
	}
	
	/*
	 * Inverted binary segmentation:
	 * 	input > threshold -> output=1;
	 */
	private void segmentation() {
		if(mInputFrame == null) {
			return;
		}
		
		mOutputFrame = new MagicPadFrame();
		
		int W = 0;
		mObjMean = 0;
		
		for(int idx=0; idx<mInputFrame.data.length; idx++) {
			if((mInputFrame.data[idx] & 0xff) > mThreshold) {
				mOutputFrame.data[idx] = 0;
			} else {
				mOutputFrame.data[idx] = 1;
				
				W++;
				mObjMean += mInputFrame.data[idx] & 0xff;
			}
		}
		
		if(W>0) mObjMean /= W;
		else mObjMean = 255.0;
	}
}
