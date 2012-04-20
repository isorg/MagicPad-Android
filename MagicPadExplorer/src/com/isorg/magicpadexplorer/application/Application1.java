package com.isorg.magicpadexplorer.application;

import java.util.Timer;
import java.util.TimerTask;
import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.isorg.magicpadexplorer.MagicPadDevice;
import com.isorg.magicpadexplorer.R;
import com.isorg.magicpadexplorer.algorithm.CalibrationAlgorithm;
import com.isorg.magicpadexplorer.algorithm.FingerTipAlgorithm;
import com.isorg.magicpadexplorer.algorithm.ImageReaderAlgorithm;
import com.isorg.magicpadexplorer.algorithm.OtsuAlgorithm;
import com.isorg.magicpadexplorer.algorithm.QuartAlgorithm;
import com.isorg.magicpadexplorer.algorithm.SwapAlgorithm;




public class Application1 extends Activity {
	
	// For the GUI
	private TextView tvImagerReader, tvCalibration, tvOtsu, tvFingerTip, tvQuartAlgo, tvSwapAlgo = null;
	
	// Refresh the data
	private Timer mTimer;
	private static final int FRAME_PERIOD = 75;
	
	// For the BT
	private MagicPadDevice magicPadDevice;
	private final String address = getIntent().getExtras().getString("address");
	
	// process pipeline
	private ImageReaderAlgorithm imageReader = null;
	private CalibrationAlgorithm calibration = null;
	private OtsuAlgorithm otsu = null;
	private FingerTipAlgorithm fingerTip = null;
	private QuartAlgorithm quartAlgo = null;
	private SwapAlgorithm swapAlgo = null;
	
	//For debug
	private String TAG = "Application1";
	private Boolean D = true;
	
	
	
	
	// Message handler
    final Handler handlerStatus = new Handler() {
        @Override
		public void handleMessage(Message msg) {        	
            int co = msg.arg1;
            if(co == 1) {
            	if (D) Log.d(TAG, "Connected\n");
            	readFrames();
            } else if(co == 2) {
            	if (D) Log.d(TAG, "Disconnected\n");
            } else if(co == 3) {
            	if(D) Log.d(TAG, "imageReader[0] = " + imageReader.getOutput().data[0]);
            	tvImagerReader.setText("imageReader [0] = " + String.valueOf(imageReader.getOutput().data[0]));
            	tvCalibration.setText("calibration [0] = " + String.valueOf(calibration.getOutput().data[0]));
            	tvOtsu.setText("Object detected = " + String.valueOf(otsu.isObjectDetected()));
            	tvFingerTip.setText("finger tip x = " + String.valueOf( fingerTip.getPosX()) + "finger tip Y = " + String.valueOf(fingerTip.getPosY()));
            	tvQuartAlgo.setText("quart = " + String.valueOf(quartAlgo.getQuart() ));
            	tvSwapAlgo.setText( "swap = " + String.valueOf(swapAlgo.getSwapMotion()));
            }
        }
    };    
	
	
	/**********     METHODS      *********/

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.app1);
		
		tvImagerReader = (TextView) findViewById(R.tv.imagereader);
		tvCalibration = (TextView) findViewById(R.tv.calibration);
		tvOtsu = (TextView) findViewById(R.tv.otsu);
		tvFingerTip = (TextView) findViewById(R.tv.fingertip);
		tvQuartAlgo = (TextView) findViewById(R.tv.quartalgo);
		tvSwapAlgo = (TextView) findViewById(R.tv.swapalgo);

		magicPadDevice = new MagicPadDevice(handlerStatus);
		mTimer = new Timer();
		
		// BT connexion 
		if (D) Log.d(TAG, "Address : " + address);
		
        imageReader = new ImageReaderAlgorithm();
        imageReader.setInput(magicPadDevice);
        
        calibration = new CalibrationAlgorithm();
        calibration.setInput(imageReader);
        
        otsu = new OtsuAlgorithm();
        otsu.setInput(calibration);
        
        fingerTip = new FingerTipAlgorithm();
        fingerTip.setOtsuInput(otsu);
        fingerTip.setInput(calibration);
        
        quartAlgo = new QuartAlgorithm();
        quartAlgo.setInput(fingerTip);
        
        swapAlgo = new SwapAlgorithm();
        swapAlgo.setInput(otsu);
        
	}
	
	
    @Override
	protected void onResume() {
		magicPadDevice.connect(address);
		readFrames();
		Toast.makeText(this, "Please, wait for the Bluetooth connexion", 6000).show();
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
	
	
    /**********    METHODS TO READ THE MAGIC PAD FRAME     **********/
    
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
    	fingerTip.update();
    	quartAlgo.update();
    	swapAlgo.update();
    	
    	if( imageReader.getOutput() == null )
    		{
	    		if (D) Log.d(TAG, "imageReader.getOutPut is null (the first time)" );
	    		return;
    		}
    	
    	// Send message back
		Message msg = handlerStatus.obtainMessage();
		msg.arg1 = 3;
		
		Bundle b = new Bundle();
		b.putByteArray("frame", imageReader.getOutput().data);
		
		msg.setData(b);
		handlerStatus.sendMessage(msg);
    }
}
