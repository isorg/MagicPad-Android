package com.isorg.magicpadexplorer.application;

import java.io.File;
import java.util.ArrayList;

import com.isorg.magicpadexplorer.R;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.Bitmap.Config;
import android.graphics.PorterDuff.Mode;
import android.graphics.Shader.TileMode;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ImageView.ScaleType;


import com.isorg.magicpadexplorer.MagicPadDevice;
import com.isorg.magicpadexplorer.algorithm.CalibrationAlgorithm;
import com.isorg.magicpadexplorer.algorithm.ImageReaderAlgorithm;
import com.isorg.magicpadexplorer.algorithm.OtsuAlgorithm;
import com.isorg.magicpadexplorer.algorithm.SwapAlgorithm;



public class PhotosBrowserApplication extends ApplicationActivity {

	//For debug
	String TAG = "PhotosBrowserApplication";
	boolean D = true;
	
	private CoverFlow coverFlow; 
	private Handler mHandler = new Handler();

	
	
	// Message handler
    final Handler handlerStatus = new Handler() {
        @Override
		public void handleMessage(Message msg) {
            if(msg.arg1 == 1) {
            	if (D) Log.d(TAG, "Connected");
            } else if(msg.arg1 == 2) {
            	if (D) Log.d(TAG, "Disconnected");
    			Toast.makeText(PhotosBrowserApplication.this, R.string.probleme_with_bluetooth, 80000).show();
            } else if(msg.arg1 == 3) {	
            	if (D) Log.d(TAG, "swapAlgo.getSwapMotion = " + swapAlgo.getSwapMotion());
            	if (D && swapAlgo.getSwapMotion() != 0) Log.d(TAG, "swap detected = " + swapAlgo.getSwapMotion());
            	if (swapAlgo.getSwapMotion() == SwapAlgorithm.SWAP_LEFT_TO_RIGHT) {
            		mHandler.postDelayed(nextPictureLeft, 0);
            	} else if (swapAlgo.getSwapMotion() == SwapAlgorithm.SWAP_RIGHT_TO_LEFT) {
            		mHandler.postDelayed(nextPictureRight, 0);
            	}
        	}
        }
    };  
    
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Setup the coverflow
	    ImageAdapter coverImageAdapter = new ImageAdapter(this);     
	    coverImageAdapter.createReflectedImages();
	     
	    coverFlow = new CoverFlow(this);
	    coverFlow.setSpacing(-25);
	    coverFlow.setSelection(4, true);
	    coverFlow.setAnimationDuration(1000);     
	    coverFlow.setAdapter(coverImageAdapter);
	    //coverFlow.setAdapter(new ImageAdapter(this));  
	     
	    setContentView(coverFlow);
	     
	    mHandler.removeCallbacks(nextPictureRight);
	    mHandler.removeCallbacks(nextPictureLeft);
	    //mHandler.postDelayed(nextPicture, 1000);
	    
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
        
        swapAlgo = new SwapAlgorithm();
        swapAlgo.setInput(otsu);
	}
	
	
    private Runnable nextPictureRight = new Runnable() {	
    	public void run() {    					
			// Emulate a left/right swap
			coverFlow.onKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT, null);
						
			mHandler.removeCallbacks(this);
			//mHandler.postDelayed(this, 5000);
    	}
    };
    
    private Runnable nextPictureLeft = new Runnable() {	
    	public void run() {    					
			// Emulate a left/right swap
			coverFlow.onKeyDown(KeyEvent.KEYCODE_DPAD_LEFT, null);
						
			mHandler.removeCallbacks(this);
			//mHandler.postDelayed(this, 5000);
    	}
    };
    
    public class ImageAdapter extends BaseAdapter {
        int mGalleryItemBackground;
        private Context mContext;
           
        /*private Integer[] mImageIds = {
          R.drawable.kasabian_kasabian,
          R.drawable.starssailor_silence_is_easy,
          R.drawable.killers_day_and_age,
          R.drawable.garbage_bleed_like_me,
          R.drawable.death_cub_for_cutie_the_photo_album,
          R.drawable.kasabian_kasabian,
          R.drawable.massive_attack_collected,
          R.drawable.muse_the_resistance,
          R.drawable.starssailor_silence_is_easy
        }; */

        private ImageView[] mImages;
        
    	ArrayList<String> strFile = new ArrayList<String>();
    	File[] files;
    	File[] fName;
    	String path;
    	
        public ImageAdapter(Context c) {
       	 mContext = c;
       	 findImages();
       	 //mImages = new ImageView[mImageIds.length];
       	 mImages = new ImageView[strFile.size()];
        }

        public boolean createReflectedImages() {
       	 //The gap we want between the reflection and the original image
       	 final int reflectionGap = 4;
       	 
       	 //
       	 final int viewSize = 350;
    
       	 int index = 0;
       	 //for (int imageId : mImageIds) {
       	 for( String imageId : strFile ) {    		 
       		 //Bitmap raw = BitmapFactory.decodeResource(getResources(), imageId);    		 
       		 Bitmap raw = BitmapFactory.decodeFile( path + "/" + imageId );
       	     Log.d(TAG, "factoring: " + path + "/" + imageId);
       		 Bitmap originalImage = Bitmap.createScaledBitmap(raw, viewSize, viewSize, false);
       		 int width = originalImage.getWidth();
       		 int height = originalImage.getHeight();
           
       		 //This will not scale but will flip on the Y axis
       		 Matrix matrix = new Matrix();
       		 //matrix.preScale((int)((1.0 * viewSize)/width) , (int)((-1.0 * viewSize)/height));
       		 matrix.preScale(1, -1);
      
   			//Create a Bitmap with the flip matrix applied to it.
   			//We only want the bottom half of the image
   			Bitmap reflectionImage = Bitmap.createBitmap(originalImage, 0, height/2, width, height/2, matrix, false);
   			   			       
   			//Create a new bitmap with same width but taller to fit reflection
   			Bitmap bitmapWithReflection = Bitmap.createBitmap(width , (height + height/2), Config.ARGB_8888);
   			 
   			//Create a new Canvas with the bitmap that's big enough for
   			//the image plus gap plus reflection
   			Canvas canvas = new Canvas(bitmapWithReflection);
   			
   			//Draw in the original image
   			canvas.drawBitmap(originalImage, 0, 0, null);
   			
   			//Draw in the gap
   			Paint deafaultPaint = new Paint();
   			canvas.drawRect(0, height, width, height + reflectionGap, deafaultPaint);
   			
   			//Draw in the reflection
   			canvas.drawBitmap(reflectionImage, 0, height + reflectionGap, null);
   			
   			//Create a shader that is a linear gradient that covers the reflection
   			Paint paint = new Paint(); 
   			LinearGradient shader = new LinearGradient(
   					0, 
   					originalImage.getHeight(), 
   					0, 
   					bitmapWithReflection.getHeight() + reflectionGap, 
   					0x90ffffff, 
   					0x00ffffff, 
   					TileMode.CLAMP); 
   			
   			//Set the paint to use this shader (linear gradient)
   			paint.setShader(shader); 
   			
   			//Set the Transfer mode to be porter duff and destination in
   			paint.setXfermode(new PorterDuffXfermode(Mode.DST_IN)); 
   			
   			//Draw a rectangle using the paint with our linear gradient
   			canvas.drawRect(0, height, width, bitmapWithReflection.getHeight() + reflectionGap, paint); 
   			      
   			ImageView imageView = new ImageView(mContext);
   			imageView.setImageBitmap(bitmapWithReflection);
   			imageView.setLayoutParams(new CoverFlow.LayoutParams(viewSize, viewSize+viewSize/2));
   			imageView.setScaleType(ScaleType.MATRIX);
   			mImages[index++] = imageView;
   		}
       	 
   		return true;
   	}
        
        private void findImages()
        {
        	String pictureFolder = getExternalFilesDir(Environment.DIRECTORY_PICTURES).getName();
    	    files = Environment.getExternalStorageDirectory().listFiles();
    	    
    	    if (D)Log.d(TAG, "" + getExternalFilesDir(Environment.DIRECTORY_PICTURES).getName() );
    		//files = getExternalFilesDir(Environment.DIRECTORY_PICTURES).getName();
    		
    	    if (D)Log.d(TAG, "files.length: " + files.length);
    		for(int i=0; i<files.length ;i++)
    		{
    			if(files[i].isDirectory())// If file is Directory
    			{
    				//Add files in DCIM to fName Array
    				if(files[i].getName().equals( pictureFolder )) {
    					fName = files[i].listFiles();//Add Array
    					path = files[i].getPath().toString();//Get PATH
    					if (D) Log.d(TAG, "" + path);
    				}	
    			}	
    		}
        	    			
    		Log.d(TAG, "fName.length: " + fName.length);
    		for(int j=0; j<fName.length; j++)
    		{
    			Log.d(TAG, "fName.getName: " + fName[j].getName());
    			if( fName[j].getName().toString().indexOf(".jpg") >= 0 
    					|| fName[j].getName().toString().indexOf(".png") >= 0
    					|| fName[j].getName().toString().indexOf(".gif") >= 0 )
    			{
    				strFile.add(fName[j].getName().toString());
    				Log.d("CoverFlowExample", "Adding: " + fName[j].getName().toString());
    			}
    			
    		}
        }

        public int getCount() {
            //return mImageIds.length;
       	 //return strFile.size();
       	 return mImages.length;
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
       	 //Use this code if you want to load from resources
       	 /*ImageView i = new ImageView(mContext);
   		i.setImageResource(mImageIds[position]);
   		i.setLayoutParams(new CoverFlow.LayoutParams(130, 130));
   		i.setScaleType(ImageView.ScaleType.CENTER_INSIDE); 
   	 
   		//Make sure we set anti-aliasing otherwise we get jaggies
   		BitmapDrawable drawable = (BitmapDrawable) i.getDrawable();
   		drawable.setAntiAlias(true);
   		return i;*/         
   	  
       	 // Use this code to use reflected images
       	 return mImages[position];
        }
        
      /** Returns the size (0.0f to 1.0f) of the views 
       * depending on the 'offset' to the center. */      
         public float getScale(boolean focused, int offset) { 
           /* Formula: 1 / (2 ^ offset) */ 
             return Math.max(0, 1.0f / (float)Math.pow(2, Math.abs(offset))); 
         } 

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


	@Override
	protected void TimerMethod() {
		// send read frame command
    	magicPadDevice.sendCommand(MagicPadDevice.COMMAND_FRAME);
    	
    	// update pipeline
    	imageReader.update();
    	calibration.update();
    	otsu.update();
    	swapAlgo.update();
    	
    	if (D) Log.d(TAG, "swapAlgo.getSwapMotion = " + swapAlgo.getSwapMotion());
    	if (swapAlgo.getSwapMotion() != 0) {
			Message msg = handlerStatus.obtainMessage();
			msg.arg1 = 3;
			handlerStatus.sendMessage(msg);	
    	}
    	
	}

}
