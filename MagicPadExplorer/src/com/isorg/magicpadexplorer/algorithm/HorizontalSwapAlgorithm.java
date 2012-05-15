package com.isorg.magicpadexplorer.algorithm;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.util.Log;

public class HorizontalSwapAlgorithm extends ImageAlgorithm {

    private static final String TAG = "HorizontalSwapAlgorithm";
    private static final boolean D = false;
    
    // Swap direction codes
    public static final int SWAP_NONE = 0;
    public static final int SWAP_LEFT_TO_RIGHT = 1;
    public static final int SWAP_RIGHT_TO_LEFT = 2;
    
    private int mSwap = SWAP_NONE;
    
    // Swap position codes
    private static final int SIDE_NONE = 0;
    private static final int SIDE_LEFT = 1;
    private static final int SIDE_RIGHT = 2;
    private int mSide = 0;
    
    // Swap position list
    private List<Integer> mQueue = new ArrayList<Integer>();
    
    // Timout motion (in miliseconds)
    private static final int MAX_MOTION_TIME_MS = 1500;
    private long mMotionStartTime = 0;
    
    // click detection
    //private boolean mClick = false;
    private List<Double> mClickQueue = new LinkedList<Double>();
    
    
    // Set input algorithm: MUST BE an OtsuAlgorithm
    public void setInput(ImageAlgorithm otsuAlgo) {
    	if(otsuAlgo instanceof OtsuAlgorithm) {
    		mInputAlgorithm = otsuAlgo;
    	} else {
    		if(D) Log.d(TAG, "Input algorithm is not an Otsu algorithm");
    	}
    }
    
    // deprecated
    public int getSwapMotion() {
    	return mSwap;
    }
    
    public int getLastSwapMotion() {
    	int s = mSwap;
    	mSwap = SWAP_NONE;
    	return s;
    }
    
    
	@Override
	public void process() {
		if(mInputFrame == null) {
			return;
		}		
		
		mSide = sideDetection();

		mSwap = sideToSwap(mSide);
		
	}
	
	
	// Convert sides list into a swap movement
	private int sideToSwap(int side) {
		
		// movement not finished
		if(side != SIDE_NONE) {
			if(mQueue.isEmpty()) {
				mMotionStartTime = System.currentTimeMillis();
				mQueue.add(side);
			} else if(!mQueue.isEmpty() && side != mQueue.get(mQueue.size()-1) ) {
				mQueue.add(side);
			}
			return SWAP_NONE;
		}
		
		// from here side==NONE is assumed: movement is 
		// finished or has not started yet.
		
		// Empty motion / Too short motion
		// Too long motion
		long motionDuration = System.currentTimeMillis() - mMotionStartTime;
		//if(D) Log.d(TAG, "Motion duration:" + motionDuration);
		if(mQueue.size()<2 || mQueue.size()>4 || motionDuration>MAX_MOTION_TIME_MS) {
			if(D && mQueue.size()>0) Log.d(TAG, "Bad queue size:" + mQueue.size() + " or motion duration:" + motionDuration);
			mQueue.clear();
			return SWAP_NONE;			
		}
		
		// Correct motion length (2-3-4) -> decode
		// entrySide and exitSide are the side from which the movement came and left
		int entrySide = mQueue.get(0);
		int exitSide;
		if(mQueue.size() == 2) exitSide = mQueue.get(1);
		else if(mQueue.size() == 3) exitSide = mQueue.get(2);
		else exitSide = mQueue.get(3);
		mQueue.clear();
						
		// compute swap motion		
		if(entrySide==SIDE_LEFT && exitSide==SIDE_RIGHT) return SWAP_LEFT_TO_RIGHT;
		if(entrySide==SIDE_RIGHT && exitSide==SIDE_LEFT) return SWAP_RIGHT_TO_LEFT;
		
		return SWAP_NONE;
	}
	
	// Detect the side of the object
	private int sideDetection() {		
		// object detection
		boolean obj = ((OtsuAlgorithm)mInputAlgorithm).isObjectDetected();
		if(!obj) return SIDE_NONE;
		
		// side detection
		int L=0, R=0;
		// top left
		for(int r=0; r<5; r++)
			for(int c=0; c<5; c++)
				if(mInputFrame.data[10*r+c] > 0) {
					L++;
				}	
		// top right
		for(int r=0; r<5; r++)
			for(int c=5; c<10; c++)
				if(mInputFrame.data[10*r+c] > 0) {
					R++;
				}	
		// bottom left
		for(int r=5; r<10; r++)
			for(int c=0; c<5; c++)
				if(mInputFrame.data[10*r+c] > 0) {
					L++;
				}		
		// bottom right
		for(int r=5; r<10; r++)
			for(int c=5; c<10; c++)
				if(mInputFrame.data[10*r+c] > 0) {
					R++;
				}
		
		// find majority
		int side = SIDE_NONE;
		if(L>=R && L>15) { 
			side = SIDE_LEFT;			
		} else if(R>=L && R>15) { 
			side = SIDE_RIGHT;
		}
				
		return side;
	}

}
