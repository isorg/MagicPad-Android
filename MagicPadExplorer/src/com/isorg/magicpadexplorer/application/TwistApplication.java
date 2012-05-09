package com.isorg.magicpadexplorer.application;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Range;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.isorg.magicpadexplorer.MagicPadDevice;
import com.isorg.magicpadexplorer.R;
import com.isorg.magicpadexplorer.algorithm.CalibrationAlgorithm;
import com.isorg.magicpadexplorer.algorithm.ImageReaderAlgorithm;
import com.isorg.magicpadexplorer.algorithm.OtsuAlgorithm;
import com.isorg.magicpadexplorer.algorithm.RotationAlgorithm;

public class TwistApplication extends ApplicationActivity {
	
	private Vue mVue;
	private boolean opticalFlowView = false;
	
	//For debug
	private String TAG = "TwistApplication";
	private boolean D = true;
	
	// Message handler
    final Handler handlerStatus = new Handler() {
        @Override
		public void handleMessage(Message msg) {
            if(msg.arg1 == 1) {
            	if (D) Log.d(TAG, "Connected");
            } else if(msg.arg1 == 2) {
            	if (D) Log.d(TAG, "Disconnected");
    			Toast.makeText(TwistApplication.this, "Problem with Bluetooth connexion", 80000).show();
            } else if(msg.arg1 == 3) {	
            	mVue.setAng(rotationAlgo.getAngle());
            	mVue.setFrame(calibration.getOutput().data);
            	mVue.setThreshold( (int) otsu.getThreshold());
        	}
        }
    };  

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mVue = new Vue(this);
		setContentView(mVue);
		
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
        
        rotationAlgo = new RotationAlgorithm();
        rotationAlgo.setInput( calibration );
        rotationAlgo.setRotationSpeed(0.10);

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
    	rotationAlgo.update();
    	
    	if (calibration.getOutput() != null) {
	    	if (D) Log.d(TAG, "rotationAlgo.getAngle() = " + rotationAlgo.getAngle());
			Message msg = handlerStatus.obtainMessage();
			msg.arg1 = 3;
			handlerStatus.sendMessage(msg);	
    	}
	}
	
	
	private class Vue extends SurfaceView  implements SurfaceHolder.Callback {

		private PotentiometerThread mThread; 
		private double ang = 0.0;
		private byte[] mFrame = null;
		private int mThreshold;
        private int PSZ = 35; // pixel size

				
		public void setAng(double a) {
			ang = a;
		}
		
		public Vue (Context context) {
			super(context);
			getHolder().addCallback(this);
			mThread = new PotentiometerThread (getHolder(), this);
		}

        public void setFrame(byte[] frame)
        {
        	mFrame = frame;
        	invalidate();
        }
        
        public void setThreshold (int threshold) {
        	mThreshold = threshold;
        }
        
        
		@Override
		public void onDraw (Canvas c) {					
			Paint paint = new Paint();
			int width = c.getWidth();
			int height = c.getHeight();
			
			paint.setColor(Color.BLACK);
			

			c.drawRect(0, 0, c.getWidth(), c.getHeight(), paint);
			
			//if (D) Log.d(TAG, "canvas width " + String.valueOf(c.getWidth()) + "  canvas height " + String.valueOf(c.getHeight()));


			if (!opticalFlowView) {
				Drawable lines = getResources().getDrawable(R.drawable.line_for_twist);
				int left = width/2 - lines.getIntrinsicWidth()/2;
				int top = height/2 - lines.getIntrinsicHeight()/2;
				lines.setBounds( left, top, left + lines.getIntrinsicWidth(), top + lines.getIntrinsicHeight());
				lines.draw(c);
				
				Drawable boutton = getResources().getDrawable(R.drawable.button_for_twist);
				left = width/2 - boutton.getIntrinsicWidth()/2;
				top = height/2 - boutton.getIntrinsicHeight()/2;
				boutton.setBounds( left, top, left + lines.getIntrinsicWidth(), top + lines.getIntrinsicHeight());
				boutton.draw(c);
				
				Drawable reflect = getResources().getDrawable(R.drawable.reflect_for_twist);
				left = width/2 - reflect.getIntrinsicWidth()/2; 
				top = height/2 - reflect.getIntrinsicHeight()/2;
				reflect.setBounds( left, top, left + lines.getIntrinsicWidth(), top + lines.getIntrinsicHeight());
				reflect.draw(c);
				
				
				c.save();
				Bitmap hole = BitmapFactory.decodeResource(this.getResources(), R.drawable.hole_for_twist);
				float begin = -120;
				//  360/240 = 1.5
				c.rotate( (float) (begin + (float) ang/1.5), width/2, height/2);
				
				paint.setColor(Color.GRAY);
				c.drawBitmap(hole,left , top, null); 
	
				c.restore();	
				paint.setAntiAlias(true);
				paint.setStyle(Paint.Style.FILL);
				paint.setTextSize(15);
				paint.setColor(Color.WHITE);
				c.drawText("Switch to the optical flow view.", 30, 30, paint);
				
				/*paint.setColor(Color.CYAN);
				c.drawPoint(20, 70, paint);
				c.drawPoint(20, 120, paint);
				c.drawPoint(250, 70, paint);
				c.drawPoint(250, 120, paint);*/
								
			} else {
        		
				if (mFrame != null) {
	        		int value = 0;
        			c.save();
        			c.translate(width/2 - 5*PSZ, height/2 -  5*PSZ);

	                Mat bigFlow = rotationAlgo.getFlow();
	                Mat flow = new Mat(bigFlow, new Range(10, 20), new Range(10, 20) );

		        	for(int co=0; co<flow.cols(); co++) {
		        		for(int ro=0; ro<flow.rows(); ro++) {
		        			// draw pixel
		        			value = (mFrame[co*10 + ro] & 0xff);
		        			
		        			if(value >= mThreshold) 
		        				value = 255;
		        			else 
		        				value = ( int ) (value * 255.0 / mThreshold) ;
		        		
		        			paint.setARGB(255, value, value, value);
		        			c.drawRect(ro*PSZ, co*PSZ, (ro+1)*PSZ, (co+1)*PSZ, paint);
		        			
		        			// draw pixel value
		        			paint.setColor(Color.BLUE);
		        			
		        			paint.setAntiAlias(false);
		        			paint.setTextSize(10);
		        			c.drawText(String.valueOf(value), (ro*PSZ + 5), (int)((co+0.5)*PSZ), paint);
		        			
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
		        	
	        	c.restore();
	        	
	        	paint.setAntiAlias(true);
				paint.setStyle(Paint.Style.FILL);
				paint.setTextSize(15);
				paint.setColor(Color.WHITE);
				c.drawText("Switch to the potentiometer view.", 30, 30, paint);
				
				
			}
		}

		@Override
		public boolean onTouchEvent(MotionEvent event) {
		    final int action = event.getAction();
		    final float x = event.getX();
		    final float y = event.getY();
		 
		    if (action == MotionEvent.ACTION_DOWN && x<250 && x>20 && y<50 && y > 0) {
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
	
	
	private class PotentiometerThread extends Thread {
		
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

		public PotentiometerThread (SurfaceHolder h, Vue v) {
			mHolder = h;
			mVue = v;
		}
	}
	
	
    /********				Save/restore the State				*******/
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
    	savedInstanceState.putBoolean("OpticalFlowView ", opticalFlowView);
    	super.onSaveInstanceState(savedInstanceState);
    }
    
    
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
    	opticalFlowView = savedInstanceState.getBoolean("OpticalFlowView");
    	super.onRestoreInstanceState(savedInstanceState);
    }

}
