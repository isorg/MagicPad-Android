package com.isorg.magicpadexplorer.application;


import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Range;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Rect;
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
import com.isorg.magicpadexplorer.algorithm.CalibrationAlgorithm.Pixel;
import com.isorg.magicpadexplorer.algorithm.FingerTipAlgorithm;
import com.isorg.magicpadexplorer.algorithm.ImageReaderAlgorithm;
import com.isorg.magicpadexplorer.algorithm.OtsuAlgorithm;
import com.isorg.magicpadexplorer.algorithm.QuartAlgorithm;
import com.isorg.magicpadexplorer.algorithm.RotationAlgorithm;




public class ConnexionTest extends ApplicationActivity {

	// Debug
	private String TAG = "ConnexionTest";
	private Boolean D = false;
	
	// Customized view
	private CustomizedView mView;
	
	private int fpsCnt = 0;
	private long lastTime = 0;
	private double fps = 0;
	private Pixel[] mask = null;
	
	
	
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
            	mView.setFrame(calibration.getOutput().data);
            	mView.setThreshold( (int) otsu.getThreshold());
            	mView.setAng( Math.floor(rotationAlgo.getAngle()*100.0)/100);
            	mView.setObjectDetected(otsu.isObjectDetected());
            	// framerate
            	if(++fpsCnt >= 10) {
            		fpsCnt = 0;
            		long t = System.currentTimeMillis() - lastTime;
            		if (t != 0) {
	            		fps = 10*1000/t;
	            		lastTime = System.currentTimeMillis();
	            		mView.setFps(fps);
            		}
            	}
            	mView.setXY(fingerTip.getPosX(), fingerTip.getPosY());
        		if (quartAlgo.getQuart() != QuartAlgorithm.QUART_NONE)
        			mView.setQuart(quartAlgo.getQuart());
        		else mView.setQuart(QuartAlgorithm.QUART_NONE);
            }
        }
    };    
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);

		mView = new CustomizedView(this);
		setContentView(mView);
		
		// title bar
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_bar_layout);
		tvTitleBar = (TextView) findViewById(R.tv.title_bar);
		tvTitleBar.setText(getResources().getString(R.string.connexion_name));
		tvConnexionState = (TextView) findViewById(R.id.connexion_state);
		tvConnexionState.setText(getResources().getString(R.string.disconnected));
		ivConnexionState = (ImageView) findViewById(R.id.connexion_state_drawable);

		
		magicPadDevice = new MagicPadDevice(handlerStatus);
		
		
		// BT address 
		address = getIntent().getExtras().getString("address");
		if (D) Log.d(TAG, "Address : " + address);
		
		
		// Pipeline
        imageReader = new ImageReaderAlgorithm();
        imageReader.setInput(magicPadDevice);
        
        calibration = new CalibrationAlgorithm();
        calibration.setInput(imageReader);
        mask = calibration.getMask();
        
        otsu = new OtsuAlgorithm();
        otsu.setInput(calibration);
        
        fingerTip = new FingerTipAlgorithm();
        fingerTip.setOtsuInput(otsu);
        fingerTip.setInput(calibration);
        
        quartAlgo = new QuartAlgorithm();
        quartAlgo.setInput(fingerTip);
        
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
    	rotationAlgo.update();
    	
    	// Avoid bluetooth issue
    	if (nullFrameCounter >90) {
    		Toast.makeText(this, getResources().getString(R.string.probleme_with_bluetooth), Toast.LENGTH_SHORT).show();
    		finish();
    	}
    	
    	// The first frames are always null
    	if( imageReader.getOutput() == null ) {
    		Log.d(TAG, "imageReader.getOutPut is null (the first times)" );
    		nullFrameCounter++;
    		return;
		}
    	
    	// Send message back
		Message msg = handlerStatus.obtainMessage();
		msg.arg1 = 3;
		handlerStatus.sendMessage(msg);
    }
	
	
	private class CustomizedView extends SurfaceView  implements SurfaceHolder.Callback {

		private ConnexionTestThread mThread; 
		private byte[] mFrame = null;
		private int mThreshold;
        private int PSZ = 40; // pixel size
        private double  mX = 0, mY = 0;
        private double mFps = 0;
        private double mAngle = 0;
        private boolean mObjectDetected = false;
        private int mQuart = QuartAlgorithm.QUART_NONE;

        // To know if we have to draw the optical flow or the grid
        private boolean opticalFlowView = false;
        
        // To draw the green button and set up the event on it 
        private int paddingHeight = 20;
		private int paddingWidth = 15;
		private int xText = 100;
		private int yText = 60;
		private int leftButton, topButton, rightButton, bottomButton;

		
		public CustomizedView (Context context) {
			super(context);
			getHolder().addCallback(this);
			mThread = new ConnexionTestThread (getHolder()); //, this);
		}
		
		public void setAng(double a) {
			mAngle = a;
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
        
        public void setXY(double x, double y) {
        	mX = x;
        	mY = y;
        }
        
        public void setQuart(int q) {
        	mQuart = q;
        }
        
        
		@Override
		public void onDraw (Canvas c) {					
			Paint paint = new Paint();
			int width = c.getWidth();
			
			// To keep the black background and refresh the draw
			paint.setColor(Color.BLACK);
			c.drawRect(0, 0, c.getWidth(), c.getHeight(), paint);

			//-------------------------//
			//   DRAW THE RIGHT SIDE   //
			//-------------------------//
			if (mFrame != null) {
				c.save();
				c.translate(width/2+33, 140);
        		int value = 0;

        		// To get the flow, calculated by openCV
                Mat bigFlow = rotationAlgo.getFlow();
                Mat flow = new Mat(bigFlow, new Range(10, 20), new Range(10, 20) );
                
                // In the value grid view
                if (!opticalFlowView) {
                	// For each pixel
		        	for(int co=0; co<flow.cols(); co++) {
		        		for(int ro=0; ro<flow.rows(); ro++) {
		        			// draw pixel
		        			value = (mFrame[co*10 + ro] & 0xff);
		        			if(value >= mThreshold) 
		        				value = 255;
		        			else 
		        				value = ( int ) (value * 255.0 / mThreshold) ;
		        		
		        			// Draw the gray rectangle
		        			paint.setStyle(Style.FILL);
		        			paint.setARGB(255, value, value, value);
		        			c.drawRect(ro*PSZ, co*PSZ, (ro+1)*PSZ, (co+1)*PSZ, paint);
		        			
		        			// Write pixel value
		        			if (value > mThreshold) paint.setColor(Color.RED);
		        			else paint.setColor(Color.GREEN);
		        			c.drawText(String.valueOf(value), (ro*PSZ + 10), (int)((co+0.5)*PSZ), paint);
		        			
		        			// Show the dead pixels
		        			if (!mask [co*10 + ro].alive) {
		        				paint.setStyle(Style.STROKE);
		        				paint.setColor(Color.RED);
		        				c.drawRect(ro*PSZ, co*PSZ, (ro+1)*PSZ-1, (co+1)*PSZ-1, paint);
		        			}
		        		}
		        	}
		        	
		        	// draw finger tip
		        	paint.setStyle(Style.STROKE);
		        	paint.setColor(Color.RED);
		        	paint.setStrokeWidth(5);
		        	c.drawCircle((float)(PSZ*(10*mX + 5)), (float)(PSZ*(10*mY + 5)), (float)20.0, paint);
		        	
		        	// draw quart
		        	paint.setColor(Color.BLUE);
		        	Path path = new Path();
		        	
		        	if(mQuart == QuartAlgorithm.QUART_BOTTOM_LEFT) {
		        		path.addCircle(2*PSZ, 7*PSZ, 20, Path.Direction.CW);
		        	} else if(mQuart == QuartAlgorithm.QUART_BOTTOM_RIGHT) {
		        		path.addCircle(8*PSZ, 8*PSZ, 20, Path.Direction.CW);
		        	} else if(mQuart == QuartAlgorithm.QUART_TOP_LEFT) {
		        		path.addCircle(2*PSZ, 2*PSZ, 20, Path.Direction.CW);
		        	} else if(mQuart == QuartAlgorithm.QUART_TOP_RIGHT) {
		        		path.addCircle(8*PSZ, 2*PSZ, 20, Path.Direction.CW);
		        	}
		        	c.drawPath(path, paint);
		        	
	                // Add the caption
	                paint.setColor(Color.RED);
	                paint.setAntiAlias(true);
	                paint.setStyle(Style.FILL);
	                c.drawText(getResources().getString(R.string.red_point), PSZ*3+PSZ/2, PSZ*10 +20, paint);
	                paint.setColor(Color.BLUE);
	                c.drawText(getResources().getString(R.string.blue_point), PSZ*3 + PSZ/2, PSZ*10+40, paint);
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
			
			// Draw the green button with the good text
			if (opticalFlowView) drawGreenButton(getResources().getString(R.string.switch_to_value_grid), c);
			else drawGreenButton(getResources().getString(R.string.switch_to_flow), c);
			
			//------------------------//
			//   DRAW THE LEFT SIDE   //
			c.save();
			c.translate(width/2-300, 150);
			paint.setStyle(Style.FILL);
			paint.setAntiAlias(true);
			paint.setColor(Color.WHITE);
			paint.setTextSize(15);
			
			c.drawText(getResources().getString(R.string.framerate) + mFps, 0, 0, paint);
			c.drawText(getResources().getString(R.string.average) + mThreshold, 0, 40, paint);
			c.drawText(getResources().getString(R.string.object) + mObjectDetected, 0, 80, paint);
			c.drawText(getResources().getString(R.string.angle) + mAngle, 0, 120, paint);
			c.restore();
		}

		private void drawGreenButton(String text, Canvas c) {
			// Set up the paint
			Paint paint = new Paint();
			paint.setAntiAlias(true);
			paint.setStyle(Paint.Style.STROKE);
			paint.setColor(Color.WHITE);
			paint.setTextSize(18);

			// Get the size of the text
			Rect rect = new Rect();
			paint.getTextBounds(text, 0, text.length() , rect);
			
			// Define the left top right and bottom of the button
			Drawable blue_button = getResources().getDrawable(R.drawable.green_button_for_twist);
			leftButton = c.getWidth()/2 + rect.left-paddingWidth + xText;
			topButton = rect.top-paddingHeight + yText;
			rightButton = c.getWidth()/2 + rect.right + paddingWidth + xText;
			bottomButton = rect.bottom + paddingHeight + yText;
			blue_button.setBounds( leftButton,topButton ,rightButton , bottomButton);
			blue_button.draw(c);
			
			// Write the text
			c.drawText(text, c.getWidth()/2 + xText, yText, paint);			
		}

		@Override
		public boolean onTouchEvent(MotionEvent event) {
		    final int action = event.getAction();
		    final float x = event.getX();
		    final float y = event.getY();
		 
		    // If we touch the green button
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
		//private CustomizedView mVue;
		
		@Override
		public void run() {
			Canvas c;
			while (mRun) {
				c = null;
				try {
					c = mHolder.lockCanvas(null);
					synchronized (mHolder) {
						if (D) Log.d(TAG, "Starting onDraw");
						mView.onDraw(c);
					}
				} finally {
					if (c!= null) mHolder.unlockCanvasAndPost(c);
				}
			}
		}
		
		public void setRunning (boolean r) {
			mRun = r;
		}

		public ConnexionTestThread (SurfaceHolder h) {// CustomizedView v) {
			mHolder = h;
			//mView = v;
		}
	}

}
