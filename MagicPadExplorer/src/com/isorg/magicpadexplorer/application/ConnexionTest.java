package com.isorg.magicpadexplorer.application;


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
import com.isorg.magicpadexplorer.algorithm.FingerTipAlgorithm;
import com.isorg.magicpadexplorer.algorithm.ImageReaderAlgorithm;
import com.isorg.magicpadexplorer.algorithm.OtsuAlgorithm;
import com.isorg.magicpadexplorer.algorithm.QuartAlgorithm;
import com.isorg.magicpadexplorer.algorithm.RotationAlgorithm;
import com.isorg.magicpadexplorer.algorithm.SwapAlgorithm;




public class ConnexionTest extends ApplicationActivity {
	
	// For the GUI
	private TextView tvImagerReader, tvCalibration, tvOtsu, tvFingerTip, tvQuartAlgo, tvSwapAlgo, tvRotationAlgo = null;

	//For debug
	private String TAG = "ConnexionTest";
	private Boolean D = true;
	
	
	
	
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
    			Toast.makeText(ConnexionTest.this, R.string.probleme_with_bluetooth, 80000).show();
            } else if(msg.arg1 == 3) {
            	if(D) Log.d(TAG, "imageReader[0] = " + imageReader.getOutput().data[0]);
            	tvImagerReader.setText("imageReader [0] = " + String.valueOf(imageReader.getOutput().data[0]));
            	tvCalibration.setText("calibration [0] = " + String.valueOf(calibration.getOutput().data[0]  & 0xff));
            	tvOtsu.setText("Object detected = " + String.valueOf(otsu.isObjectDetected()));
            	tvFingerTip.setText("finger tip x = " + String.valueOf( fingerTip.getPosX()) + "finger tip Y = " + String.valueOf(fingerTip.getPosY()));
            	tvQuartAlgo.setText("quart = " + String.valueOf(quartAlgo.getQuart() ));
            	tvSwapAlgo.setText( "swap = " + String.valueOf(swapAlgo.getSwapMotion()));
            	tvRotationAlgo.setText("rotate = " + String.valueOf(rotationAlgo.getAngle() ));
            }
        }
    };    
	
	
	/**********     METHODS      *********/

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.connexion_test_layout);

		
		// For the title bar
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_bar_layout);
		tvTitleBar = (TextView) findViewById(R.tv.title_bar);
		tvTitleBar.setText(getResources().getString(R.string.connexion_name));
		tvConnexionState = (TextView) findViewById(R.id.connexion_state);
		tvConnexionState.setText(getResources().getString(R.string.disconnected));
		ivConnexionState = (ImageView) findViewById(R.id.connexion_state_drawable);
		
		
		// Get the textView from the layout
		tvImagerReader = (TextView) findViewById(R.tv.imagereader);
		tvCalibration = (TextView) findViewById(R.tv.calibration);
		tvOtsu = (TextView) findViewById(R.tv.otsu);
		tvFingerTip = (TextView) findViewById(R.tv.fingertip);
		tvQuartAlgo = (TextView) findViewById(R.tv.quartalgo);
		tvSwapAlgo = (TextView) findViewById(R.tv.swapalgo);
		tvRotationAlgo = (TextView) findViewById(R.tv.rotationalgo);

		
		magicPadDevice = new MagicPadDevice(handlerStatus);
		
		
		// BT connexion 
		address = getIntent().getExtras().getString("address");
		if (D) Log.d(TAG, "Address : " + address);
		
		
		// Pipeline
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
        
        rotationAlgo = new RotationAlgorithm();
        rotationAlgo.setInput(calibration);
        
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
    	fingerTip.update();
    	quartAlgo.update();
    	swapAlgo.update();
    	rotationAlgo.update();
    	
    	if( imageReader.getOutput() == null )
    		{
	    		if (D) Log.d(TAG, "imageReader.getOutPut is null (the first times)" );
	    		return;
    		}
    	
    	// Send message back
		Message msg = handlerStatus.obtainMessage();
		msg.arg1 = 3;
		handlerStatus.sendMessage(msg);
    }
}
