package com.isorg.magicpadexplorer.application;

import com.isorg.magicpadexplorer.R;
import android.content.Context;
import android.database.Cursor;
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
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView.ScaleType;


import com.isorg.magicpadexplorer.MagicPadDevice;
import com.isorg.magicpadexplorer.algorithm.CalibrationAlgorithm;
import com.isorg.magicpadexplorer.algorithm.HorizontalSwapAlgorithm;
import com.isorg.magicpadexplorer.algorithm.ImageReaderAlgorithm;
import com.isorg.magicpadexplorer.algorithm.OtsuAlgorithm;
import com.isorg.magicpadexplorer.algorithm.SwapAlgorithm;



public class PhotosBrowserApplication extends ApplicationActivity {

	//For debug
	String TAG = "PhotosBrowserApplication";
	boolean D = true;
	
	private CoverFlow coverFlow; 
	private Handler mHandler = new Handler();
	
	// For the PhotosBrowserApplication we made a special swap detection,
	// which just detect the right to left and the left to right swap,
	// in order to improve the robustness
	private HorizontalSwapAlgorithm horizontalSwapAlgo = null;

	
	
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
    			Toast.makeText(PhotosBrowserApplication.this, R.string.probleme_with_bluetooth, 80000).show();
            } else if(msg.arg1 == 3) {	
            	int swap = msg.getData().getInt("swap");
            	if (swap == SwapAlgorithm.SWAP_LEFT_TO_RIGHT) {
            		mHandler.postDelayed(nextPictureLeft, 0);
            	} else if (swap == SwapAlgorithm.SWAP_RIGHT_TO_LEFT) {
            		mHandler.postDelayed(nextPictureRight, 0);
            	}
        	} else if (msg.arg1 == 4) {
            	Toast.makeText(PhotosBrowserApplication.this, getResources().getString(R.string.probleme_with_bluetooth),Toast.LENGTH_LONG).show();
            	finish();
            }
        }
    };  
    
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		
		
		// Setup the coverflow
	    ImageAdapter coverImageAdapter = new ImageAdapter(this);
	     
	    coverFlow = new CoverFlow(this);
	    coverFlow.setSpacing(-25);
	    coverFlow.setSelection(4, true);
	    coverFlow.setAnimationDuration(1000);     
	    coverFlow.setAdapter(coverImageAdapter);
	    setContentView(coverFlow);

	    
	    // title bar
	    getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_bar_layout);
		tvTitleBar = (TextView) findViewById(R.tv.title_bar);
		tvTitleBar.setText(getResources().getString(R.string.photos_browser_name));
		tvConnexionState = (TextView) findViewById(R.id.connexion_state);
		tvConnexionState.setText(getResources().getString(R.string.disconnected));
		ivConnexionState = (ImageView) findViewById(R.id.connexion_state_drawable);
		
	     
	    mHandler.removeCallbacks(nextPictureRight);
	    mHandler.removeCallbacks(nextPictureLeft);
	    
	    
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
        
        horizontalSwapAlgo = new HorizontalSwapAlgorithm();
        horizontalSwapAlgo.setInput(otsu);
	}
	
	
    private Runnable nextPictureRight = new Runnable() {	
    	public void run() {    					
			// Emulate a right/left swap
			coverFlow.onKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT, null);
			mHandler.removeCallbacks(this);
    	}
    };
    
    private Runnable nextPictureLeft = new Runnable() {	
    	public void run() {    					
			// Emulate a left/right swap
			coverFlow.onKeyDown(KeyEvent.KEYCODE_DPAD_LEFT, null);
			mHandler.removeCallbacks(this);
    	}
    };
    

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


	@Override
	protected void TimerMethod() {
		// send read frame command
    	magicPadDevice.sendCommand(MagicPadDevice.COMMAND_FRAME);
    	
    	// update pipeline
    	imageReader.update();
    	calibration.update();
    	otsu.update();
    	horizontalSwapAlgo.update();
    	
    	// Avoid bluetooth issue
    	if (nullFrameCounter >90) {
    		Message msgToLeave = handlerStatus.obtainMessage();
    		msgToLeave.arg1 = 4;
    		handlerStatus.sendMessage(msgToLeave);
    	}

    	// The first frames are always null
    	if (imageReader.getOutput()== null) {
    		Log.d(TAG, "imageReader.getOutPut is null (the first times)");
    		nullFrameCounter++;
    		return;
    	}
    	
    	// Send message back
    	int swap = horizontalSwapAlgo.getLastSwapMotion();
    	if (swap != SwapAlgorithm.SWAP_NONE) {
    		if (D) Log.d(TAG, "swap detected = " + swap);
			Message msg = handlerStatus.obtainMessage();
			msg.arg1 = 3;
			Bundle b = new Bundle();
			b.putInt("swap", swap);
			msg.setData(b);
			handlerStatus.sendMessage(msg);
    	}
    	
	}
    
    
    
    /*
     *  ImageAdapter class
     */
    public class ImageAdapter extends BaseAdapter {
        int mGalleryItemBackground;
        private Context mContext;

        private ImageView[] mImages;
        public final int MAX_PICTURE = 20;
        
    	
        public ImageAdapter(Context c) {
        	mContext = c;
        	findAndProcessImages();
        }

        
        private void findAndProcessImages()
        {
        	Uri contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI; 	// The URI to images stored on SD card (photos, pictures, screenCaptures...)
     	    String[] proj = { MediaStore.Images.Media.DATA };
            Cursor cursor = managedQuery(contentUri, proj, null, null, null);	// cursor contains all pictures
            cursor.moveToFirst();
            
            if (D) Log.d (TAG, "cursor.getCount : " +  cursor.getCount() );
            
            // If there is no pictures, stop application and return an error message
            if (cursor.getCount() <= 1)
            {
            	Toast.makeText(PhotosBrowserApplication.this, getResources().getString(R.string.no_picture),Toast.LENGTH_LONG).show();
            	finish();
            }
            
            if (cursor.getCount() > MAX_PICTURE)		// to avoid to have to much pictures
 	    		mImages = new ImageView[ MAX_PICTURE ];
 	    	else
 	    		mImages = new ImageView[ cursor.getCount() ];
            
            int i=0;
            while ( (i<MAX_PICTURE)  &&  (i<cursor.getCount()) )
            {
         	   if (D) Log.d (TAG, "adding : " + cursor.getString(0));
         	   mImages[i] = reflectedImage( cursor.getString(0));
         	   i++;
         	   cursor.moveToNext();
            }
     		
         }
        
        private ImageView reflectedImage(String path)
        {
        	//The gap we want between the reflection and the original image
          	final int reflectionGap = 4;
          	 
          	final int viewSize = 350;
          	
          	Bitmap raw = BitmapFactory.decodeFile( path );
          	Log.d(TAG, "factoring: " + path );
          	
      		Bitmap originalImage = Bitmap.createScaledBitmap(raw, viewSize, viewSize, false);
      		int width = originalImage.getWidth();
      		int height = originalImage.getHeight();
          
      		//This will not scale but will flip on the Y axis
      		Matrix matrix = new Matrix();
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
  			return imageView;
       }
       

        public int getCount() {
       	 return mImages.length;
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
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
}
