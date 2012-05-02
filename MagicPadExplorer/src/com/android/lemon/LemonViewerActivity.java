
// Lemon Entry point : main activity

package com.android.lemon;

import java.util.ArrayList;
import java.util.List;

import com.android.lemon.model.BasicModel;
import com.android.lemon.model.LemonModel;
import com.android.lemon.model.QuadModel;
import com.android.lemon.model.WaterModel;
import com.android.lemon.obj.ObjLoader;
import com.isorg.magicpad.MagicPadActivity;
import com.isorg.magicpadexplorer.R;
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
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;

/**
 * This demo shows the interaction between the ISORG device controller 
 * and a lemon placed in a 3D scene using OpenGL ES 2.0 
 */
public class LemonViewerActivity extends MagicPadActivity  
{
    private GLSurfaceView mGLSurfaceView;
    private ModelRenderer mRenderer;
    
    ArrayList<BasicModel> mScene1 = new ArrayList<BasicModel>();
    ArrayList<BasicModel> mScene2 = new ArrayList<BasicModel>();
    
    private final static String TAG = "LemonViewer";

    WakeLock mWakeLock;

    @Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Fullscreen window
        requestWindowFeature(Window.FEATURE_NO_TITLE);
                
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
        
        try
		{
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK, TAG);
		}
		catch (Exception e)
		{

		}
    } 
    
    // Register a model to the renderer
    private void addModel(BasicModel model) {
    	mRenderer.AddModel(model);
    }
    
 // Register a list of models to the renderer
    private void addModels(ArrayList<BasicModel> scene) {
   		mRenderer.AddModels(scene);
    }
    
     
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
		}
		return false;
	}

	@Override
    protected void onPause() {
        // Ideally a game should implement onResume() and onPause()
        // to take appropriate action when the activity looses focus
        super.onPause();
        mGLSurfaceView.onPause();
        mWakeLock.release();
    }
	@Override
    protected void onResume() {
        // Ideally a game should implement onResume() and onPause()
        // to take appropriate action when the activity looses focus
        super.onResume();
        mGLSurfaceView.onResume();
        mWakeLock.acquire();
    }

	
}
