package com.isorg.magicpadexplorer.application;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.isorg.magicpadexplorer.MagicPadDevice;
import com.isorg.magicpadexplorer.R;
import com.isorg.magicpadexplorer.algorithm.CalibrationAlgorithm;
import com.isorg.magicpadexplorer.algorithm.ImageReaderAlgorithm;
import com.isorg.magicpadexplorer.algorithm.OtsuAlgorithm;

public class SmartSwitchApplication extends ApplicationActivity {
	
	// For the GUI
	private ImageView draw = null;
	private boolean state, previousObjectDetected = false;
	
	//For debug
	private String TAG = "SmartSwitchApplication";
	private boolean D = true;
	
	
	
	
	// Message handler
    final Handler handlerStatus = new Handler() {
        @Override
		public void handleMessage(Message msg) {
            if(msg.arg1 == 1) {
            	if (D) Log.d(TAG, "Connected");
            } else if(msg.arg1 == 2) {
            	if (D) Log.d(TAG, "Disconnected");
    			Toast.makeText(SmartSwitchApplication.this, R.string.probleme_with_bluetooth, 80000).show();
            } else if(msg.arg1 == 3) {           	
        
            	
            	if (state) {
            		draw.setImageResource(R.drawable.on_button);
            		MediaPlayer mediaPlayer = MediaPlayer.create(SmartSwitchApplication.this, R.raw.onbutton);
            		mediaPlayer.start();
            	}
            	else{
            		draw.setImageResource(R.drawable.off_button);
            		MediaPlayer mediaPlayer = MediaPlayer.create(SmartSwitchApplication.this, R.raw.offbutton);
            		mediaPlayer.start();
            	}
        	}
        }
    };    
	
	
	/**********     METHODS      *********/

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.smart_switch_layout);
		
		draw = (ImageView) findViewById(R.id.draw);

		magicPadDevice = new MagicPadDevice(handlerStatus);
		
		// BT connexion 
		address = getIntent().getExtras().getString("address");
		if (D) Log.d(TAG, "Address : " + address);
		
        imageReader = new ImageReaderAlgorithm();
        imageReader.setInput(magicPadDevice);
        
        calibration = new CalibrationAlgorithm();
        calibration.setInput(imageReader);
        
        otsu = new OtsuAlgorithm();
        otsu.setInput(calibration);
        
	}
	
	
    @Override
	protected void onResume() {
		magicPadDevice.connect((address));
		super.onResume();
	}
    
   
	@Override
	protected void onPause() {
		magicPadDevice.close();
		super.onPause();
	}

    



	// Read a frame and update the processing pipeline
	@Override
    protected void TimerMethod() {    	
    	// send read frame command
    	magicPadDevice.sendCommand(MagicPadDevice.COMMAND_FRAME);
    	
    	// update pipeline
    	imageReader.update();
    	calibration.update();
    	otsu.update();
    	
    	if( imageReader.getOutput() == null )
    	{	if (D) Log.d(TAG, "imageReader.getOutPut is null (the first times)" );
    		
    		return;
    	}
    	
    	if(D) Log.d(TAG, "objet detected = " + otsu.isObjectDetected() + "\n" + "threshold =" + otsu.getThreshold());

		if (otsu.isObjectDetected() && !previousObjectDetected) {
			state = !state;
			previousObjectDetected = true;
			Message msg = handlerStatus.obtainMessage();
			msg.arg1 = 3;
			handlerStatus.sendMessage(msg);
    	}
		else if (!otsu.isObjectDetected() && previousObjectDetected)
			previousObjectDetected = false;
    }
}
