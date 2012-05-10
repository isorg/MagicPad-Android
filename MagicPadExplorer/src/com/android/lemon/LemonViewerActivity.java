
// Lemon Entry point : main activity

package com.android.lemon;

import java.util.ArrayList;
import java.util.List;

import com.android.lemon.model.BasicModel;
import com.android.lemon.model.LemonModel;
import com.android.lemon.model.QuadModel;
import com.android.lemon.model.WaterModel;
import com.android.lemon.obj.ObjLoader;
import com.isorg.magicpadexplorer.MagicPadDevice;
import com.isorg.magicpadexplorer.R;
import com.isorg.magicpadexplorer.application.ApplicationActivity;
import com.isorg.magicpadexplorer.application.ConnexionTest;
//import com.isorg.magicpad.MagicPadActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This demo shows the interaction between the ISORG device controller 
 * and a lemon placed in a 3D scene using OpenGL ES 2.0 
 */
public class LemonViewerActivity extends ApplicationActivity  
{
    private GLSurfaceView mGLSurfaceView;
    private ModelRenderer mRenderer;
    
    ArrayList<BasicModel> mScene1 = new ArrayList<BasicModel>();
    ArrayList<BasicModel> mScene2 = new ArrayList<BasicModel>();
    
    private final static String TAG = "LemonViewer";
    private boolean D = true;


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
    			Toast.makeText(LemonViewerActivity.this, "Problem with Bluetooth connexion", 80000).show();
            } else if(msg.arg1 == 3) {     
            	LemonModel.setPressureMap( calibration.getOutput().data, 10, 10 );
            	if (D) Log.d(TAG, "imageReader = " + imageReader.getOutput().data[0]);
            }
        }
    };
        
    
    @Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        
        
        mRenderer = null; 
    	mGLSurfaceView = null;
    	
    	// Create the 3D scene renderer
    	mRenderer = new ModelRenderer(this);
    	    
    	// Cup and box
    	mScene1.add( new BasicModel(this, "Lemon/cup21-3.obj", false));
    	// Water ripples plane
    	mScene1.add( new WaterModel( this, "Lemon/ripplane3.obj"));
    	// Lemon
    	mScene1.add( new LemonModel( this, "Lemon/lemonld9.obj"));
    	
    	// Logo
    	Bitmap logo = BitmapFactory.decodeResource(ModelRenderer.GetContext().getResources(), R.drawable.logoisorg);
    	mScene1.add( new QuadModel(this, logo, 0.2f, 0.15f, 0.4f));
    	
    	
    	// Register first scene
    	addModels(mScene1);
    	
    	//mScene2.add( new MorphPlaneModel(this, "Lemon/lemonplane.obj"));
    	//addModels(mScene2);
    	
    	// Hide all models
    	mRenderer.showAllModels( false );
    	// Reveals first scene
    	mRenderer.showModels(mScene1, true);
    	
    	// Create 3D OpengGL View
    	mGLSurfaceView = new TouchSurfaceView(this);
        mGLSurfaceView.setEGLContextClientVersion(2);
        mGLSurfaceView.setRenderer(mRenderer);
        setContentView(mGLSurfaceView); 
        
        
        // For the title bar
 		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_bar_layout);
 		tvTitleBar = (TextView) findViewById(R.tv.title_bar);
 		tvTitleBar.setText(getResources().getString(R.string.lemon_name));
 		tvConnexionState = (TextView) findViewById(R.id.connexion_state);
 		tvConnexionState.setText(getResources().getString(R.string.disconnected));
 		ivConnexionState = (ImageView) findViewById(R.id.connexion_state_drawable);
     		
     		
		magicPadDevice = new MagicPadDevice(handlerStatus);
		
		// BT connexion 
		address = getIntent().getExtras().getString("address");
		if (D) Log.d(TAG, "Address : " + address);
		
        imageReader = new com.isorg.magicpadexplorer.algorithm.ImageReaderAlgorithm();
        imageReader.setInput(magicPadDevice);
        
        calibration = new com.isorg.magicpadexplorer.algorithm.CalibrationAlgorithm();
        calibration.setInput(imageReader);
                


		
    } 
    
	// Read a frame and update the processing pipeline
	@Override
    protected void TimerMethod() {    	
    	// send read frame command
    	magicPadDevice.sendCommand(MagicPadDevice.COMMAND_FRAME);
    	
    	// update pipeline
    	imageReader.update();
    	calibration.update();
    	
    	if( imageReader.getOutput() == null )
    		{
	    		if (D) Log.d(TAG, "imageReader.getOutPut is null (the first times)" );
	    		return;
    		}
    	
    	// Send message back
		Message msg = handlerStatus.obtainMessage();
		msg.arg1 = 3;
		
		/*Bundle b = new Bundle();
		b.putByteArray("frame", imageReader.getOutput().data);
		
		msg.setData(b);*/
		handlerStatus.sendMessage(msg);
    }
    
    // Register a model to the renderer
    private void addModel(BasicModel model) {
    	mRenderer.AddModel(model);
    }
    
 // Register a list of models to the renderer
    private void addModels(ArrayList<BasicModel> scene) {
   		mRenderer.AddModels(scene);
    }
    
    /* 
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    //inflater.inflate(R.menu.menu, menu);
	    
	    menu.add(0, Menu.FIRST, 0, "<Scene1>");
	    //menu.add(0, Menu.FIRST + 1, 0, "<Scene2>");
	    
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) 
	    { 
	    case Menu.FIRST: 
	    	mRenderer.showAllModels( false );
	    	mRenderer.showModels(mScene1, true);
	    	return true;
	   /* case Menu.FIRST + 1: 
	    	mRenderer.showAllModels( false );
	    	mRenderer.showModels(mScene2, true);
	    	return true;*/
		//}
		//return false;
	//}
	

	@Override
    protected void onPause() {
        // Ideally a game should implement onResume() and onPause()
        // to take appropriate action when the activity looses focus
		magicPadDevice.close();
        super.onPause();
        mGLSurfaceView.onPause();
    }
	@Override
    protected void onResume() {
        // Ideally a game should implement onResume() and onPause()
        // to take appropriate action when the activity looses focus
		magicPadDevice.connect(address);
        super.onResume();
        mGLSurfaceView.onResume();
    }

	
}
