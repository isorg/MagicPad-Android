package com.isorg.magicpadexplorer.application;

import java.util.TimerTask;
import android.content.Context;
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

public class VumeterApplication extends ApplicationActivity {
	
	//For debugging
	boolean D = true;
	String TAG = "VumeterApplication";
	
	private Vue mVue;

	
	// Message handler
    final Handler handlerStatus = new Handler() {
        @Override
		public void handleMessage(Message msg) {
            if(msg.arg1 == 1) {
            	if (D) Log.d(TAG, "Connected");
            } else if(msg.arg1 == 2) {
            	if (D) Log.d(TAG, "Disconnected");
    			Toast.makeText(VumeterApplication.this, "Problem with Bluetooth connexion", 80000).show();
            } else if(msg.arg1 == 3) {            	
        
            	if(D) Log.d(TAG, "imageReader[0] = " + imageReader.getOutput().data[0]);
            	
            	int average = 0;
            	for (int i =0; i < calibration.getOutput().data.length; i++)
            		average += calibration.getOutput().data[i]  & 0xff;
            	
            	average /= calibration.getOutput().data.length;
            	
            	if (D) Log.d(TAG, "moyenne = " + String.valueOf(average));

            	if (average >= 249) mVue.setState(0);
            	else if ( (average >= (255-42)) &&  (average < 249)) mVue.setState(1);
            	else if ( (average >= (255-42*2)) &&  (average < (255-42))) mVue.setState(2);
            	else if ( (average >= (255-42*3)) &&  (average < (255-42*2))) mVue.setState(3);
            	else if ( (average >= (255-42*4)) &&  (average < (255-42*3))) mVue.setState(4);
            	else if ( (average >= (255-42*5)) &&  (average < (255-42*4))) mVue.setState(5);
            	else if ( (average >= (255-42*6)) &&  (average < (255-42*5))) mVue.setState(6);

        	}
        }
    };  
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        
        mVue = new Vue (this);
        setContentView(mVue);
        
		magicPadDevice = new MagicPadDevice(handlerStatus);
		
		// BT connexion 
		address = getIntent().getExtras().getString("address");
		if (D) Log.d(TAG, "Address : " + address);
		
        imageReader = new ImageReaderAlgorithm();
        imageReader.setInput(magicPadDevice);
        
        calibration = new CalibrationAlgorithm();
        calibration.setInput(imageReader);
        
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

 
	// Read a frame and update the processing pipeline
    protected void TimerMethod() {    	
    	// send read frame command
    	magicPadDevice.sendCommand(MagicPadDevice.COMMAND_FRAME);
    	
    	// update pipeline
    	imageReader.update();
    	calibration.update();
    	
    	if( calibration.getOutput() == null )
    	{	if (D) Log.d(TAG, "calibration.getOutPut is null (the first times)" );
    		return;
    	}
    	

		Message msg = handlerStatus.obtainMessage();
		msg.arg1 = 3;
		handlerStatus.sendMessage(msg);
	}
    

	private class Vue extends SurfaceView  implements SurfaceHolder.Callback{

		private VumeterThread mThread;
		
		private int state = 0;

		
		public Vue (Context context) {
			super(context);
			getHolder().addCallback(this);
			mThread = new VumeterThread (getHolder(), this);
		}
		
		
		public void setState (int s) {
			if (s >=0 && s <=6)
				state = s;
		}
		
		@Override
		public void onDraw (Canvas c) {		
			
			Paint paint = new Paint();
			int width = c.getWidth();
			int height = c.getHeight();
			int marginTopAndBottom = (int) height/11;
			int marginBetweenBars = (int) height/37;
			int barHeight = (int) height/8;
			
			paint.setColor(Color.BLACK);
			c.drawRect(0, 0, c.getWidth(), c.getHeight(), paint);
			
			if (D) Log.d(TAG, "canvas width " + String.valueOf(c.getWidth()) + "  canvas height " + String.valueOf(c.getHeight()));
			//c.drawRect(c.getWidth()/3, c.getHeight()/3,  c.getWidth()*2/3, c.getHeight()*2/3, paint);

			//First green bar
			Drawable greenBar = getResources().getDrawable(R.drawable.barreverte);
			greenBar.setBounds(width*10/22, marginTopAndBottom, width*12/22, marginTopAndBottom + barHeight);
			if (state >=1 ) greenBar.setAlpha(100);
			greenBar.draw(c);
			
			//Second bar
			Drawable greenYellowBar = getResources().getDrawable(R.drawable.barrevertejaune);
			greenYellowBar.setBounds(width*9/22, marginTopAndBottom + barHeight + marginBetweenBars, width*13/22, marginTopAndBottom + 2*barHeight + marginBetweenBars);
			if (state >=2 ) greenYellowBar.setAlpha(100);
			greenYellowBar.draw(c);
			
			//3th yellow bar
			Drawable yellowBar = getResources().getDrawable(R.drawable.barrejaune);
			yellowBar.setBounds(width*8/22, marginTopAndBottom + barHeight*2 + marginBetweenBars*2, width*14/22, marginTopAndBottom + 3*barHeight + marginBetweenBars*2);
			if (state >=3 ) yellowBar.setAlpha(100);
			yellowBar.draw(c);
			
			//4th orange bar
			Drawable orangeBar = getResources().getDrawable(R.drawable.barreorange);
			orangeBar.setBounds(width*7/22, marginTopAndBottom + barHeight*3 + marginBetweenBars*3, width*15/22, marginTopAndBottom + 4*barHeight + marginBetweenBars*3);
			if (state >=4 ) orangeBar.setAlpha(100);
			orangeBar.draw(c);
			
			//5th orange red bar
			Drawable orangeRedBar = getResources().getDrawable(R.drawable.barreorangerouge);
			orangeRedBar.setBounds(width*6/22, marginTopAndBottom + barHeight*4 + marginBetweenBars*4, width*16/22, marginTopAndBottom + 5*barHeight + marginBetweenBars*4);
			if (state >=5 ) orangeRedBar.setAlpha(100);
			orangeRedBar.draw(c);
			
			//6th red bar
			Drawable redBar = getResources().getDrawable(R.drawable.barrerouge);
			redBar.setBounds(width*5/22, marginTopAndBottom + barHeight*5 + marginBetweenBars*5, width*17/22, marginTopAndBottom + 6*barHeight + marginBetweenBars*5);
			if (state == 6 ) redBar.setAlpha(100);
			redBar.draw(c);
			
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
	
	
	private class VumeterThread extends Thread {
		
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

		public VumeterThread (SurfaceHolder h, Vue v) {
			mHolder = h;
			mVue = v;
		}
	}

}
