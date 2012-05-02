package com.isorg.magicpadexplorer.application;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.Window;
import android.widget.Toast;

import com.isorg.magicpadexplorer.MagicPadDevice;
import com.isorg.magicpadexplorer.algorithm.CalibrationAlgorithm;
import com.isorg.magicpadexplorer.algorithm.FingerTipAlgorithm;
import com.isorg.magicpadexplorer.algorithm.ImageReaderAlgorithm;
import com.isorg.magicpadexplorer.algorithm.OtsuAlgorithm;
import com.isorg.magicpadexplorer.algorithm.QuartAlgorithm;
import com.isorg.magicpadexplorer.algorithm.RotationAlgorithm;
import com.isorg.magicpadexplorer.algorithm.SwapAlgorithm;

public abstract class ApplicationActivity extends Activity {
	
	//For debugging
	boolean D = true;
	String TAG = "ApplicationActivity";
	
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
	
	// For the BT
	protected MagicPadDevice magicPadDevice;
	protected String address;
	
	//For keep the screen bright
    WakeLock mWakeLock;
    
    
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        try
		{
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, TAG);
		}
		catch (Exception e){
		}
			}
	
	
	   @Override
		protected void onResume() {
			mTimer = new Timer();
	    	mWakeLock.acquire();
			readFrames();
			Toast.makeText(this, "Please, wait for the Bluetooth connexion", Toast.LENGTH_LONG).show();
			super.onResume();
		}
	    
	   
		@Override
		protected void onPause() {
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
		
		// Read a frame and update the processing pipeline
	    protected void TimerMethod() {    	
		}


}
