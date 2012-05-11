package com.isorg.magicpadexplorer.application;


import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Range;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
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
	//private TextView tvImagerReader, tvCalibration, tvOtsu, tvFingerTip, tvQuartAlgo, tvSwapAlgo, tvRotationAlgo = null;

	//For debug
	private String TAG = "ConnexionTest";
	private Boolean D = true;
	
	private Vue mVue;
	private int fpsCnt = 0;
	private long lastTime = 0;
	private double fps = 0;
	
	
	
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
            	mVue.setFrame(calibration.getOutput().data);
            	mVue.setThreshold( (int) otsu.getThreshold());
            	mVue.setAng(rotationAlgo.getAngle());
            	mVue.setObjectDetected(otsu.isObjectDetected());
            	// framerate
            	if(++fpsCnt >= 10) {
            		fpsCnt = 0;
            		long t = System.currentTimeMillis() - lastTime;
            		fps = 10*1000/t;
            		lastTime = System.currentTimeMillis();
            		mVue.setFps(fps);
            	}
            	
            	/*tvImagerReader.setText("imageReader [0] = " + String.valueOf(imageReader.getOutput().data[0]));
            	tvCalibration.setText("calibration [0] = " + String.valueOf(calibration.getOutput().data[0]  & 0xff));
            	tvOtsu.setText("Object detected = " + String.valueOf(otsu.isObjectDetected()));
            	tvFingerTip.setText("finger tip x = " + String.valueOf( fingerTip.getPosX()) + "finger tip Y = " + String.valueOf(fingerTip.getPosY()));
            	tvQuartAlgo.setText("quart = " + String.valueOf(quartAlgo.getQuart() ));
            	tvSwapAlgo.setText( "swap = " + String.valueOf(swapAlgo.getSwapMotion()));
            	tvRotationAlgo.setText("rotate = " + String.valueOf(rotationAlgo.getAngle() ));*/
            }
        }
    };    
	
	
	/**********     METHODS      *********/

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);

		mVue = new Vue(this);
		setContentView(mVue);
		
		// For the title bar
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_bar_layout);
		tvTitleBar = (TextView) findViewById(R.tv.title_bar);
		tvTitleBar.setText(getResources().getString(R.string.connexion_name));
		tvConnexionState = (TextView) findViewById(R.id.connexion_state);
		tvConnexionState.setText(getResources().getString(R.string.disconnected));
		ivConnexionState = (ImageView) findViewById(R.id.connexion_state_drawable);
		
		
		/*
		// Get the textView from the layout
		tvImagerReader = (TextView) findViewById(R.tv.imagereader);
		tvCalibration = (TextView) findViewById(R.tv.calibration);
		tvOtsu = (TextView) findViewById(R.tv.otsu);
		tvFingerTip = (TextView) findViewById(R.tv.fingertip);
		tvQuartAlgo = (TextView) findViewById(R.tv.quartalgo);
		tvSwapAlgo = (TextView) findViewById(R.tv.swapalgo);
		tvRotationAlgo = (TextView) findViewById(R.tv.rotationalgo);
		*/

		
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
	
	
	private class Vue extends SurfaceView  implements SurfaceHolder.Callback {

		private ConnexionTestThread mThread; 
		private byte[] mFrame = null;
		private int mThreshold;
        private int PSZ = 40; // pixel size
        private double mFps = 0;
        private double mAngle = 0;
        private boolean mObjectDetected = false;
        
        
        private boolean opticalFlowView = false;
        
        private int paddingCenter = 20;
        private int paddingHeight = 20;
		private int paddingWidth = 15;
		private int xText = 100;
		private int yText = 60;
		
		private int leftButton, topButton, rightButton, bottomButton;

				
		public void setAng(double a) {
			mAngle = a;
		}
		
		public Vue (Context context) {
			super(context);
			getHolder().addCallback(this);
			mThread = new ConnexionTestThread (getHolder(), this);
		}

        public void setFrame(byte[] frame)
        {
        	mFrame = frame;
        	invalidate();
        }
        
        public void setThreshold (int threshold) {
        	mThreshold = threshold;
        }
        
        public void setFps (double fps) {
        	mFps = fps;
        }
        
        public void setObjectDetected (boolean od) {
        	mObjectDetected = od;
        }
        
        
		@Override
		public void onDraw (Canvas c) {					
			Paint paint = new Paint();
			int width = c.getWidth();
			int height = c.getHeight();
			
			// To keep the black background
			paint.setColor(Color.BLACK);
			c.drawRect(0, 0, c.getWidth(), c.getHeight(), paint);

			//-------------------------//
			//   DRAW THE RIGHT PART   //
			if (mFrame != null) {
				c.save();
				c.translate(width/2+33, 100);
        		int value = 0;

                Mat bigFlow = rotationAlgo.getFlow();
                Mat flow = new Mat(bigFlow, new Range(10, 20), new Range(10, 20) );
                
                // In the value grid view
                if (!opticalFlowView) {
		        	for(int co=0; co<flow.cols(); co++) {
		        		for(int ro=0; ro<flow.rows(); ro++) {
		        			// draw pixel
		        			value = (mFrame[co*10 + ro] & 0xff);
		        			
		        			if(value >= mThreshold) 
		        				value = 255;
		        			else 
		        				value = ( int ) (value * 255.0 / mThreshold) ;
		        		
		        			paint.setStyle(Style.FILL);
		        			paint.setARGB(255, value, value, value);
		        			c.drawRect(ro*PSZ, co*PSZ, (ro+1)*PSZ, (co+1)*PSZ, paint);
		        			
		        			// Draw pixel value
		        			if (value > mThreshold) paint.setColor(Color.RED);
		        			else paint.setColor(Color.GREEN);
		        			c.drawText(String.valueOf(value), (ro*PSZ + 10), (int)((co+0.5)*PSZ), paint);
		        		}
		        	}
				}
                // In the optical flow view
                else {
					for(int co=0; co<flow.cols(); co++) {
		        		for(int ro=0; ro<flow.rows(); ro++) {
		        			// draw pixel
		        			value = (mFrame[co*10 + ro] & 0xff);
		        			
		        			if(value >= mThreshold) 
		        				value = 255;
		        			else 
		        				value = ( int ) (value * 255.0 / mThreshold) ;
		        		
		        			paint.setStyle(Style.FILL);
		        			paint.setARGB(255, value, value, value);
		        			c.drawRect(ro*PSZ, co*PSZ, (ro+1)*PSZ, (co+1)*PSZ, paint);
		        			
		        			paint.setStyle(Style.STROKE);
		        			paint.setStrokeWidth(1);
		        			paint.setColor(Color.BLACK);
		        			c.drawRect(ro*PSZ, co*PSZ, (ro+1)*PSZ, (co+1)*PSZ, paint);
		        			
		        			// draw the flow
			                paint.setAntiAlias(true);
			                float vectorSize = 10;
			                paint.setColor(Color.GREEN);


		        			Point center = new Point(ro*PSZ + PSZ/2, co*PSZ +PSZ/2);

	                        // vector
	                        Point flowPoint = new Point(flow.get(co, ro)[0] , flow.get(co, ro)[1]);
	                        Point delta = new Point(center.x + vectorSize*flowPoint.x
	                        		,center.y + vectorSize*flowPoint.y);
	
	                        
	                        // draw line
	                        paint.setStrokeWidth(3);
	                        c.drawLine( (float) center.x, (float) center.y, (float) delta.x, (float) delta.y, paint);
		        		}
		        	}	
				}
			}
			c.restore();
			
			if (opticalFlowView) drawGreenButton(getResources().getString(R.string.switch_to_value_grid), c);
			else drawGreenButton(getResources().getString(R.string.switch_to_flow), c);
			
			//------------------------//
			//   DRAW THE LEFT PART   //
			c.save();
			c.translate(width/2-300, 110);
			paint.setStyle(Style.FILL);
			paint.setColor(Color.WHITE);
			paint.setTextSize(15);
			
			c.drawText("Framerate = " + mFps, 0, 0, paint);
			c.drawText("Average = " + this.mThreshold, 0, 40, paint);
			c.drawText("Object detected = " + mObjectDetected, 0, 80, paint);
			c.drawText("Angle = " + mAngle, 0, 120, paint);

			
			c.restore();
		}

		private void drawGreenButton(String text, Canvas c) {
			Paint paint = new Paint();
			paint.setAntiAlias(true);
			paint.setTextSize(18);

			Rect rect = new Rect();
			paint.getTextBounds(text, 0, text.length() , rect);
			
			
			Drawable blue_button = getResources().getDrawable(R.drawable.green_button_for_twist);
			leftButton = c.getWidth()/2 + rect.left-paddingWidth + xText;
			topButton = rect.top-paddingHeight + yText;
			rightButton = c.getWidth()/2 + rect.right + paddingWidth + xText;
			bottomButton = rect.bottom + paddingHeight + yText;
			blue_button.setBounds( leftButton,topButton ,rightButton , bottomButton);
			blue_button.draw(c);
			
			paint.setStyle(Paint.Style.STROKE);
			paint.setColor(Color.WHITE);
			c.drawText(text, c.getWidth()/2 + xText, yText, paint);			
		}

		@Override
		public boolean onTouchEvent(MotionEvent event) {
		    final int action = event.getAction();
		    final float x = event.getX();
		    final float y = event.getY();
		 
		    if (action == MotionEvent.ACTION_DOWN && x<rightButton && x>leftButton && y<bottomButton && y > topButton) {
		    	opticalFlowView = !opticalFlowView;
	        	if (D) Log.d(TAG, "event = down");
	        	return true;
		    }
		    return super.onTouchEvent(event);
		}
		
		

		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

		public void surfaceCreated(SurfaceHolder holder) {
			mThread.setRunning(true);
			mThread.start();
		}

		public void surfaceDestroyed(SurfaceHolder holder) {
			boolean retry = true;
			mThread.setRunning(false);
			while (retry) {
				try {
					mThread.join();
					retry = false;
				} catch (InterruptedException e) {}
			}
		}
		
	}
	
	
	private class ConnexionTestThread extends Thread {
		
		private boolean mRun = false;
		private SurfaceHolder mHolder;
		private Vue mVue;
		
		@Override
		public void run() {
			Canvas c;
			while (mRun) {
				c = null;
				try {
					c = mHolder.lockCanvas(null);
					synchronized (mHolder) {
						if (D) Log.d(TAG, "Starting onDraw");
						mVue.onDraw(c);
					}
				} finally {
					if (c!= null) mHolder.unlockCanvasAndPost(c);
				}
			}
		}
		
		public void setRunning (boolean r) {
			mRun = r;
		}

		public ConnexionTestThread (SurfaceHolder h, Vue v) {
			mHolder = h;
			mVue = v;
		}
	}

}
