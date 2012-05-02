package com.isorg.magicpadexplorer.application;

import java.util.TimerTask;

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
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.isorg.magicpadexplorer.MagicPadDevice;
import com.isorg.magicpadexplorer.R;
import com.isorg.magicpadexplorer.algorithm.CalibrationAlgorithm;
import com.isorg.magicpadexplorer.algorithm.ImageReaderAlgorithm;
import com.isorg.magicpadexplorer.algorithm.RotationAlgorithm;

public class TwistApplication extends ApplicationActivity {
	
	private Vue mVue;
	
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
    	rotationAlgo.update();
    	
    	if (calibration.getOutput() != null) {
	    	if (D) Log.d(TAG, "rotationAlgo.getAngle() = " + rotationAlgo.getAngle());
			Message msg = handlerStatus.obtainMessage();
			msg.arg1 = 3;
			handlerStatus.sendMessage(msg);	
    	}
	}
	
	
	private class Vue extends SurfaceView  implements SurfaceHolder.Callback{

		private PotentiometerThread mThread; 
		private double ang = 0.0;
		private int i = 0;
				
		public void setAng(double a) {
			ang = a;
		}
		
		public Vue (Context context) {
			super(context);
			getHolder().addCallback(this);
			mThread = new PotentiometerThread (getHolder(), this);
		}

		@Override
		public void onDraw (Canvas c) {					
			Paint paint = new Paint();
			int width = c.getWidth();
			int height = c.getHeight();
			
			paint.setColor(Color.BLACK);
			c.drawRect(0, 0, c.getWidth(), c.getHeight(), paint);
			
			//if (D) Log.d(TAG, "canvas width " + String.valueOf(c.getWidth()) + "  canvas height " + String.valueOf(c.getHeight()));


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
			
			paint.setStyle(Paint.Style.FILL);
			paint.setColor(Color.GRAY);
			paint.setAntiAlias(true);
			paint.setTextSize(15);
			c.drawBitmap(hole,left , top, null); 

			c.restore();
			c.drawText("angle : " + rotationAlgo.getAngle(), 30, 30, paint);

			
			
			
			
			/*
	        Bitmap hole = BitmapFactory.decodeResource(getResources(), R.drawable.holeforpotentiometer);
	        Log.d(TAG, "hole : " + hole.getWidth() + " " + hole.getHeight());
	        Matrix mat = new Matrix();
	        mat.postRotate( i );
	        Log.d(TAG, "" + i);
	        c.translate(width/2, height/2);
	        c.scale( (float) 0.6, (float) 0.6);
	        c.drawBitmap(hole, mat, null);
	        */

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
						if (D) Log.d(TAG, "On lance onDraw");
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

}
