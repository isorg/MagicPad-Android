package com.isorg.magicpadexplorer.application;


import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.isorg.magicpadexplorer.MagicPadDevice;
import com.isorg.magicpadexplorer.R;
import com.isorg.magicpadexplorer.algorithm.CalibrationAlgorithm;
import com.isorg.magicpadexplorer.algorithm.ImageReaderAlgorithm;
import com.isorg.magicpadexplorer.algorithm.OtsuAlgorithm;

public class SmartSwitchApplication extends ApplicationActivity {
	
	// GUI
	private ImageView on_off_image = null;
	private boolean state, previousObjectDetected = false;
	
	// debug
	private String TAG = "SmartSwitchApplication";
	private boolean D = false;
	
	
	
	// Message handler
    final Handler handlerStatus = new Handler() {
        @Override
		public void handleMessage(Message msg) {
            if(msg.arg1 == 1) {
            	if (D) Log.d(TAG, "Connected");
            	tvConnexionState.setText(getResources().getString(R.string.connected));
            	ivConnexionState.setImageDrawable(getResources().getDrawable(R.drawable.ok));
            } else if(msg.arg1 == 2) {
            	if (D) Log.d(TAG, "Disconnected");
    			Toast.makeText(SmartSwitchApplication.this, R.string.probleme_with_bluetooth, 80000).show();
            } else if(msg.arg1 == 3) {
            	// Draw and play sound depending on the state
            	if (state) {
            		on_off_image.setImageResource(R.drawable.on_button);
            		MediaPlayer mediaPlayer = MediaPlayer.create(SmartSwitchApplication.this, R.raw.onbutton);
            		mediaPlayer.start();
            	}
            	else{
            		on_off_image.setImageResource(R.drawable.off_button);
            		MediaPlayer mediaPlayer = MediaPlayer.create(SmartSwitchApplication.this, R.raw.offbutton);
            		mediaPlayer.start();
            	}
        	}
        }
    };    
	
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.smart_switch_layout);
		
		
		// title bar
	    getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_bar_layout);
		tvTitleBar = (TextView) findViewById(R.tv.title_bar);
		tvTitleBar.setText(getResources().getString(R.string.switch_name));
		tvConnexionState = (TextView) findViewById(R.id.connexion_state);
		tvConnexionState.setText(getResources().getString(R.string.disconnected));
		ivConnexionState = (ImageView) findViewById(R.id.connexion_state_drawable);	
		
		
		on_off_image = (ImageView) findViewById(R.id.on_off_image);

		
		// BT connexion 
		magicPadDevice = new MagicPadDevice(handlerStatus);

		address = getIntent().getExtras().getString("address");
		if (D) Log.d(TAG, "Address : " + address);

		
		//Pipeline
        imageReader = new ImageReaderAlgorithm();
        imageReader.setInput(magicPadDevice);
        
        calibration = new CalibrationAlgorithm();
        calibration.setInput(imageReader);
        
        otsu = new OtsuAlgorithm();
        otsu.setInput(calibration);
        
	}
	
	
    @Override
	protected void onResume() {
		magicPadDevice.connect(address);
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
    	
    	// The first frames are always null
    	if( imageReader.getOutput() == null )
    	{	Log.d(TAG, "imageReader.getOutPut is null (the first times)" );
    		return;
    	}
    	
    	if(D) Log.d(TAG, "objet detected = " + otsu.isObjectDetected() + "\n" + "threshold =" + otsu.getThreshold());

    	// Update state
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
