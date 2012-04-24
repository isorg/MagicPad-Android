package com.isorg.magicpadexplorer.application;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.ProgressDialog;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.isorg.magicpadexplorer.MagicPadDevice;
import com.isorg.magicpadexplorer.R;
import com.isorg.magicpadexplorer.algorithm.CalibrationAlgorithm;
import com.isorg.magicpadexplorer.algorithm.ImageReaderAlgorithm;
import com.isorg.magicpadexplorer.algorithm.OtsuAlgorithm;

public class OnOffApplication extends Activity {
	
	// For the GUI
	private ImageView draw = null;
	private boolean state, previousObjectDetected = false;
	
	// Refresh the data
	private Timer mTimer;
	private static final int FRAME_PERIOD = 75;
	
	// For the BT
	private MagicPadDevice magicPadDevice;
	private String address;
	
	// process pipeline
	private ImageReaderAlgorithm imageReader = null;
	private CalibrationAlgorithm calibration = null;
	private OtsuAlgorithm otsu = null;
	
	//For debug
	private String TAG = "OnOffApplication";
	private boolean D = true;
	
	
	
	
	// Message handler
    final Handler handlerStatus = new Handler() {
        @Override
		public void handleMessage(Message msg) {
        	if(msg.arg1 == 3) {            	
        
            	if(D) Log.d(TAG, "imageReader[0] = " + imageReader.getOutput().data[0]);
            	
            	if (state) {
            		draw.setImageResource(R.drawable.onbutton);
            		MediaPlayer mediaPlayer = MediaPlayer.create(OnOffApplication.this, R.raw.onbutton);
            		mediaPlayer.start();
            	}
            	else{
            		draw.setImageResource(R.drawable.offbutton);
            		MediaPlayer mediaPlayer = MediaPlayer.create(OnOffApplication.this, R.raw.offbutton);
            		mediaPlayer.start();
            	}
        	}
        }
    };    
	
	
	/**********     METHODS      *********/

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.on_off_layout);
		
		draw = (ImageView) findViewById(R.id.draw);

		magicPadDevice = new MagicPadDevice(handlerStatus);
		mTimer = new Timer();
		
		// BT connexion 
		address = getIntent().getExtras().getString("address");
		if (D) Log.d(TAG, "Address : " + address);
		//magicPadDevice.connect((address));
		//readFrames();
		
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
		readFrames();
		Toast.makeText(this, "Please, wait for the Bluetooth connexion", 60000).show();
		super.onResume();
	}
    
   
	@Override
	protected void onPause() {
		mTimer.cancel();
		magicPadDevice.close();
		super.onPause();
	}

    
    /********				Save/restore the State				*******/
    /*

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
    	// Save UI state changes to the savedInstanceState.
    	// This bundle will be passed to onCreate if the process is
    	// killed and restarted.
    	savedInstanceState.putBoolean("BTParameters", true);
    	super.onSaveInstanceState(savedInstanceState);
    }
    
    
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
    	BTParameters = savedInstanceState.getBoolean("BTParameters");
    	super.onRestoreInstanceState(savedInstanceState);
    	// Restore UI state from the savedInstanceState.
    	// This bundle has also been passed to onCreate.
    }
     */
	
	
    // ------  METHODS TO READ THE MAGIC PAD FRAME  ------ //
    
	// Start reading frames at regular intervals
	private void readFrames()
    {
    	mTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				TimerMethod();
			}
		}, 0, FRAME_PERIOD);
    }


	// Read a frame and update the processing pipeline
    private void TimerMethod() {    	
    	// send read frame command
    	magicPadDevice.sendCommand(MagicPadDevice.COMMAND_FRAME);
    	
    	// update pipeline
    	imageReader.update();
    	calibration.update();
    	otsu.update();
    	
    	if( imageReader.getOutput() == null )
    	{	if (D) Log.d(TAG, "imageReader.getOutPut is null (the first time)" );
    		
    		return;
    	}
    	
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
    
    
    
    /********    For the indeterminate progress bar    ********/
    /*
    private class waitForConnexion extends AsyncTask<Void, Void, Integer>      {
    	private ProgressDialog pb = new ProgressDialog(OnOffApplication.this);
    	
    	@Override
    	protected void onPreExecute() {
    		//pb.setIndeterminate(true);
    		pb.show();
    	}

    	@Override
    	protected void onPostExecute(Integer result) {
    		pb.dismiss();
    	}

		@Override
		protected Integer doInBackground(Void... arg0) {

    		return 0;
    	}
  }*/


}
