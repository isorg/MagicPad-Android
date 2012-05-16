package com.isorg.magicpadexplorer.application;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.isorg.magicpadexplorer.MagicPadDevice;
import com.isorg.magicpadexplorer.R;
import com.isorg.magicpadexplorer.algorithm.CalibrationAlgorithm;
import com.isorg.magicpadexplorer.algorithm.FingerTipAlgorithm;
import com.isorg.magicpadexplorer.algorithm.ImageReaderAlgorithm;
import com.isorg.magicpadexplorer.algorithm.OtsuAlgorithm;
import com.isorg.magicpadexplorer.algorithm.QuartAlgorithm;
import com.isorg.magicpadexplorer.algorithm.RotationAlgorithm;
import com.isorg.magicpadexplorer.algorithm.SwapAlgorithm;

public abstract class ApplicationActivity extends Activity {
	
	// Refresh the data
	protected Timer mTimer;
	protected static final int FRAME_PERIOD = 75;
	
	// process pipeline
	protected ImageReaderAlgorithm imageReader = null;
	protected CalibrationAlgorithm calibration = null;
	protected OtsuAlgorithm otsu = null;
	protected FingerTipAlgorithm fingerTip = null;
	protected QuartAlgorithm quartAlgo = null;
	protected SwapAlgorithm swapAlgo = null;
	protected RotationAlgorithm rotationAlgo = null;
	
	// title bar
	protected TextView tvTitleBar, tvConnexionState = null;
	protected ImageView ivConnexionState = null;
	
	// bluetooth
	protected MagicPadDevice magicPadDevice;
	protected String address;
	
	// orientation
	private int mOrientation;
	
	// To keep the screen bright
    WakeLock mWakeLock;
    
    
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Know the initial orientation
		mOrientation = getResources().getConfiguration().orientation;

		// To get the wake lock
        try
		{	PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, this.getLocalClassName());
		}
		catch (Exception e){}
	}
	
	
	   @Override
		protected void onResume() {
		    // To start the timer and read the frames
			mTimer = new Timer();
			readFrames();
			// Start the wake lock
	    	mWakeLock.acquire();
	    	// Ask to wait for the bluetooth connexion
			Toast.makeText(this, R.string.wait_for_bluetooth, Toast.LENGTH_SHORT).show();
			super.onResume();
		}
	    
	   
		@Override
		protected void onPause() {
			// Stop the wake lock
			mWakeLock.release();
			mTimer.cancel();
			super.onPause();
		}
		
		
		// Start reading frames at regular intervals
		protected void readFrames()
	    {
	    	mTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					TimerMethod();
				}
			}, 0, FRAME_PERIOD);
	    }


		@Override
		protected void onStop() {
			// if the activity is restarting because orientation has change, nothing to do
			// else, finish the activity in order to avoid some issues with threads
			if (mOrientation != getResources().getConfiguration().orientation) {
				mOrientation = getResources().getConfiguration().orientation;
			} else {
				finish();
			}
			super.onStop();
		}


		// Read a frame and update the processing pipeline
	    protected void TimerMethod() {}

}
