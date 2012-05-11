package com.isorg.magicpadexplorer.algorithm;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.util.Log;

/*
 * Detect swap movement and clicks
 * Swap movement consist fo a left-right motion or a up-down motion.
 * Motion direction is important too.
 * 
 * Click event are detected whenever the user approaches the sensor.  
 */
public class SwapAlgorithm extends ImageAlgorithm {
	// Debugging
    private static final String TAG = "SwapAlgorithm";
    private static final boolean D = true;
    
    // Swap direction codes
    public static final int SWAP_NONE = 0;
    public static final int SWAP_LEFT_TO_RIGHT = 1;
    public static final int SWAP_RIGHT_TO_LEFT = 2;
    public static final int SWAP_BOTTOM_TO_TOP = 3;
    public static final int SWAP_TOP_TO_BOTTOM = 4;
    public static final int SWAP_CLICK = 5;
    
    private int mSwap = SWAP_NONE;
    
    // Swap direction symbols
    public static final String[] SWAP_SYMBOL = {"", "\u21e8", "\u21e6", "\u21e7", "\u21e9", "\u25ce"};
    //public static final String[] SWAP_SYMBOL = {"-", "L", "R", "T", "B", "x"};
    public static final String[] SWAP_CLICK_QUART = {"MIDDLE", "TOP_LEFT", "BOTTOM_LEFT", "TOP_RIGHT", "BOTTOM_RIGHT"};
    
    // Swap position codes
    private static final int SIDE_NONE = 0;
    private static final int SIDE_LEFT = 1;
    private static final int SIDE_RIGHT = 2;
    private static final int SIDE_TOP = 3;
    private static final int SIDE_BOTTOM = 4;
    private int mSide = 0;
    
    // Swap position list
    private List<Integer> mQueue = new ArrayList<Integer>();
    
    // Timout motion (in miliseconds)
    private static final int MAX_MOTION_TIME_MS = 1500;
    private long mMotionStartTime = 0;
    
    // click detection
    private boolean mClick = false;
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
    
    public boolean isClicked() {
    	return mClick;
    }
    
	@Override
	public void process() {
		
		if(mInputFrame == null) {
			return;
		}		
		
		mSide = sideDetection();

		mSwap = sideToSwap(mSide);
		
		mClick = clickDetection();
		if(mClick && mSwap==SWAP_NONE) {
			mSwap = SWAP_CLICK;
		}
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
		if(entrySide==SIDE_TOP && exitSide==SIDE_BOTTOM) return SWAP_TOP_TO_BOTTOM;
		if(entrySide==SIDE_BOTTOM && exitSide==SIDE_TOP) return SWAP_BOTTOM_TO_TOP;
		
		return SWAP_NONE;
	}
	
	// Detect the side of the object
	private int sideDetection() {		
		// object detection
		boolean obj = ((OtsuAlgorithm)mInputAlgorithm).isObjectDetected();
		if(!obj) return SIDE_NONE;
		
		// side detection
		int L=0, R=0, T=0, B=0;
		//int W = 0; // number of object pixels
		// top left
		for(int r=0; r<5; r++)
			for(int c=0; c<5; c++)
				if(mInputFrame.data[10*r+c] > 0) {
					L++; T++; //W++;
				}	
		// top right
		for(int r=0; r<5; r++)
			for(int c=5; c<10; c++)
				if(mInputFrame.data[10*r+c] > 0) {
					R++; T++; //W++;
				}	
		// bottom left
		for(int r=5; r<10; r++)
			for(int c=0; c<5; c++)
				if(mInputFrame.data[10*r+c] > 0) {
					L++; B++; //W++;
				}		
		// bottom right
		for(int r=5; r<10; r++)
			for(int c=5; c<10; c++)
				if(mInputFrame.data[10*r+c] > 0) {
					R++; B++; //W++;
				}
		//if(D) Log.d(TAG, "LBTR:" + L + " " + B + " " + T + " " + R);
		
		// find majority
		int side = SIDE_NONE;
		if(L>=B && L>=T && L>=R && L>15) {
			side = SIDE_LEFT;			
		} else if(B>=L && B>=T && B>=R && B>15) {
			side = SIDE_BOTTOM;			
		} else if(T>=L && T>=B && T>=R && T>15) {
			side = SIDE_TOP;			
		} else if(R>=L && R>=B && R>=T && R>15) {
			side = SIDE_RIGHT;
		}
				
		return side;
	}
	
	/*
	 * Return true if a click event is detected
	 */
	public boolean clickDetection() {
	
		// No object -> reset queue
		if(! ((OtsuAlgorithm)mInputAlgorithm).isObjectDetected()) {
			mClickQueue.clear();
			return false;
		}
		
		// An object -> add to queue and trim old samples
		// we keep 8 samples which is 5+3 to let swap algorithm decide if it is swaping
		/*MagicPadFrame frame = ((OtsuAlgorithm)mInputAlgorithm).getOutput();
		int remaingRows = 5; // number of rows to consider 
		double topMean = 0; // top-obeject mean
		int topWeight = 0; // top object number of pixels
		for(int r=0; r<10; ++r) {
			
			// sum of the line
			int sw = 0;
			for(int c=0; c<10; ++c) {
				if(frame.data[10*r+c] > 0) {
					topMean = mInputFrame.data[10*r+c];
					sw++;
				}
			}
			topWeight += sw;
			
			//
			if(sw > 0) remaingRows--;
			if(remaingRows == 0) break;
		}
		if(topWeight > 0) topMean /= topWeight;
		else topMean = 255;*/
		
		
		double oa = ((OtsuAlgorithm)mInputAlgorithm).getObjectMean();
		//double oa = topMean;
		mClickQueue.add(oa);
		while(mClickQueue.size() > 8) {
			mClickQueue.remove(0);
		}
		
		/*
		String s = "";
		for(int i=0; i<mClickQueue.size(); i++) {
			s += mClickQueue.get(i).intValue() + " ";
		}*/
		//if(D) Log.d(TAG, ">>> " + s);
		
		if(mClickQueue.size() < 8) return false;
		
		// Decode stream to find click	
		double start=0, drop=0, comeback=0;
		boolean click = false;
		
		start = mClickQueue.get(0);
		drop = mClickQueue.get(1);
		comeback = mClickQueue.get(2);
		click = click || clickDecode(start, drop, comeback);
				
		start = mClickQueue.get(0);
		drop = Math.min(mClickQueue.get(1), mClickQueue.get(2));
		comeback = mClickQueue.get(3);
		click = click || clickDecode(start, drop, comeback);
		
		start = mClickQueue.get(0);
		drop = Math.min(mClickQueue.get(1), mClickQueue.get(2));
		drop = Math.min(drop, mClickQueue.get(3));
		comeback = mClickQueue.get(4);
		click = click || clickDecode(start, drop, comeback);
				
		int istart = (int)start;
		int idrop = (int)drop;
		int pdrop = (int)(100*drop/start);
		int icomeback = (int)comeback;
		int pcomeback = (int)(100*comeback/start);
		if(D && (pdrop<90 || pcomeback<90)) Log.d(TAG, "" + istart + " " + idrop + " (" + pdrop + ") " + icomeback + " (" + pcomeback + ")");
		
		// decode click
		if(click) {
			if(D) Log.d(TAG, "CLICK:" + click);
			mClickQueue.clear();
			return true;
		} else {
			return false;
		}
	}
	
	// Decode stream to find click
	// A click is defined by a drop of average value and a come back
	// The drop must be > than 75% percent and the come back must reach at
	// least 85% of the original
	public boolean clickDecode(double start, double drop, double comeback) {
		double DROP_PERCENTAGE = 0.60; 
		double COMEBACK_PERCENTAGE = 0.80;
		
		//
		// start ----               
		//           \               ----- comeback ---
		//            \             /
		//             \____drop___/
		//
		if(drop <= (start*DROP_PERCENTAGE) &&
		   (comeback >= start*COMEBACK_PERCENTAGE)) {
			return true;
		}
		
		
		//
		// start ----               ----- comeback ---
		//           \             / 
		//            \____drop___/
		//
		DROP_PERCENTAGE = 0.70; 
		COMEBACK_PERCENTAGE = 1.00;
		if(drop <= (start*DROP_PERCENTAGE) &&
				   (comeback >= start*COMEBACK_PERCENTAGE)) {
					return true;
				}
		
		return false;
	}

}
