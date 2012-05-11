package com.isorg.magicpadexplorer.algorithm;

import java.util.ArrayList;
import java.util.List;

import com.isorg.magicpadexplorer.MagicPadFrame;

import android.util.Log;


/*
 * Calibrate input frame to reduce dead pixels effect and increase dynamic range.  
 *
 */
public class CalibrationAlgorithm extends ImageAlgorithm {
	// Debug
	private static final boolean V = false; // false to disable debug log call
	private static final boolean D = false; // false to disable debug log call
	private static final String TAG = "CalibrationAlgorithm";	
	
	// image array size
	private static final int IMG_SIZE = 100;
	
	// image max pixel gain threshold
	private static final int MAX_GAIN_THRES = 20;
	
	// in algorithm initialized and ready to calibrate
	private boolean mCalibrated = false;
	
	// working pixel mask: true=working, false=dead
	private Pixel mMask[] = new Pixel[100];
	
	// gain matrix: output = input * gain
	private double mGain[] = new double[IMG_SIZE];
	
	// light matrix
	private double mLight[] = new double[IMG_SIZE];
	
	/*
	 *  Pixel 
	 */
	private class Pixel {
		// alive=working or dead pixel
		public boolean alive = false;
				
		// list of neighbor (idx + factor)
		public List<Neighbor> neighbor = null;
	}
	
	/*
	 * Pixel Neighbor
	 */
	private class Neighbor {
		// Type of neighborhood:
		// V4 are the four closest (Top Bottom Left Right)
		// V8 are the diagonals
		public static final double V4_FACTOR = 1.0;
		public static final double V8_FACTOR = 0.707; // 1/sqrt(2)
		
		public int index = -1;
		public double factor = -1;
		
		public Neighbor(int idx, double f) {
			index = idx; factor = f;
		}
	}
	
	/*
	 *  constructor
	 */
	public CalibrationAlgorithm() {
		// initialize arrays
		for(int idx=0; idx<IMG_SIZE; idx++)
		{
			mMask[idx] = new Pixel();
			mMask[idx].alive = false;
			mGain[idx] = 0.0;
			mCalibrated = false;
		}
	}
	
	/*
	 *  List pixel that are V4 or V8 of 'index'
	 */
	private List<Neighbor> listNeighbor(int index) {
		List<Neighbor> L = new ArrayList<Neighbor>();
		
		int row = index / 10;
		int col = index % 10;
		
		// V4 -> top
		if(row>0 && mMask[index-10].alive) 
			L.add(new Neighbor(index-10, Neighbor.V4_FACTOR));
			
		// V4 -> bottom
		if(row<9 && mMask[index+10].alive) 
			L.add(new Neighbor(index+10, Neighbor.V4_FACTOR));
		
		// V4 -> right
		if(col<9 && mMask[index+1].alive) 
			L.add(new Neighbor(index+1, Neighbor.V4_FACTOR));
		
		// V4 -> left
		if(col>0 && mMask[index-1].alive) 
			L.add(new Neighbor(index-1, Neighbor.V4_FACTOR));
				
		// V8 -> top left
		if(row>0 && col>0 && mMask[index-11].alive) 
			L.add(new Neighbor(index-11, Neighbor.V8_FACTOR));
			
		// V8 -> bottom left
		if(row<9 && col>0 && mMask[index+9].alive) 
			L.add(new Neighbor(index+9, Neighbor.V8_FACTOR));
		
		// V8 -> top right
		if(row>0 && col<9 && mMask[index-9].alive) 
			L.add(new Neighbor(index-9, Neighbor.V8_FACTOR));
		
		// V8 -> bottom right
		if(row<9 && col<9 && mMask[index+11].alive) 
			L.add(new Neighbor(index+11, Neighbor.V8_FACTOR));
		
		return L;
	}
	
	/*
	 *  compute gain + mask
	 */
	public void calibrate() {		
		// compute gain + set dead pixels
		for(int idx=0; idx<IMG_SIZE; idx++) {
			int value = mInputFrame.data[idx] & 0xff;
			mLight[idx] = Math.max(value, 1.0);
			if(V) Log.v(TAG, "idx,value: "+idx+" "+value);
			double gain = 0;
			if(value > 0) {
				gain = 255.0 / value;
				if(gain<=MAX_GAIN_THRES) {
					mMask[idx].alive = true;
					mGain[idx] = gain;
				}
			}	
		}
		
		// force x4 D19 dead pixels
		mMask[3*10+3].alive = false;
		mMask[3*10+6].alive = false;
		mMask[6*10+3].alive = false;
		mMask[6*10+6].alive = false;
		
		// find dead pixels neighbors
		for(int idx=0; idx<IMG_SIZE; idx++) {
			if(!mMask[idx].alive) {
				mMask[idx].neighbor = listNeighbor(idx);
			}
		}
		
		mCalibrated = true;
	}
	
	/*
	 *  interpolate pixel from neighbor
	 */
	private void interpolate() {
		
		mOutputFrame = new MagicPadFrame();
		
		// Apply gain when pixels are alive
		for(int idx=0; idx<IMG_SIZE; idx++) {			
			// 
			if(mMask[idx].alive) {
				// alive: output = input * gain;
				mOutputFrame.data[idx] = (byte)(Math.min(255, Math.max(0, (double)(mInputFrame.data[idx] & 0xff) * mGain[idx])));
			}
		}
		
		// When pixel are dead: interpolate values		
		for(int idx=0; idx<IMG_SIZE; idx++) {
			if(!mMask[idx].alive) {
				// dead: output = Sum(neighbors.(input*gain*factor)) / Sum(neighbors.factor)
				double value = 0;
				double factor = 0;
				//Log.d(TAG, String.valueOf(idx));
				for(Neighbor N : mMask[idx].neighbor) {
					//Log.d(TAG, "--> " + String.valueOf(N.index) + " " + String.valueOf(N.factor) + " * " + String.valueOf((double)(mInputFrame.data[N.index] & 0xff)));
					value += (double)(mOutputFrame.data[N.index] & 0xff) * N.factor;
					factor += N.factor;
				}
				if(factor>0) value = Math.min(255, Math.max(0, value / factor));
				mOutputFrame.data[idx] = (byte)(value);
				
				if(idx == 92) {
					Log.d(TAG, String.valueOf(mOutputFrame.data[idx] & 0xff));
				}
			}
		}
	}
	
	// fix non linearity issues 'cross effect'
	// apply pow(2) to central cross
	// Never used
	/*
	private void nonLinearity() {
		for(int r=0; r<10; r++) {
			for(int c=0; c<10; c++) {
				// is it in the central cross ?
				if( (r>=4 && r<=5 && c>0 && c<9) ||
					(c>=4 && c<=5 && r>0 && r<9) ){
					double x = mInputFrame.data[10*r + c] & 0xff;
					mInputFrame.data[10*r + c] = (byte)(mLight[10*r+c] * Math.sqrt(x/mLight[10*r+c]));
				}
			}
		}
	}*/
	
	@Override
	public void process() {
		if(D) Log.d(TAG, "process() at t=" + mTime);
		if(mInputFrame == null) {
			return;
		}
		
		if(!mCalibrated) { 
			if(D) Log.d(TAG, "Calibrate");
			calibrate();
		}
		
		//nonLinearity();
		interpolate();		
	}
}
