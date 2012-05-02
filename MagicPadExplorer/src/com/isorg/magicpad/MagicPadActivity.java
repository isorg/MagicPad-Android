package com.isorg.magicpad;

import java.util.Timer;
import java.util.TimerTask;

import com.android.lemon.LemonViewerActivity;
import com.android.lemon.DeviceListActivity;
import com.android.lemon.model.LemonModel;
import com.isorg.magicpad.CalibrationAlgorithm;
import com.isorg.magicpad.FingerTipAlgorithm;
import com.isorg.magicpad.ImageReaderAlgorithm;
import com.isorg.magicpad.MagicPadDevice;
import com.isorg.magicpad.OtsuAlgorithm;
import com.isorg.magicpad.QuartAlgorithm;
import com.isorg.magicpad.SwapAlgorithm;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MagicPadActivity extends Activity {
	// Debug
	private static final boolean D = true; // false to disable debug log call
	private static final String TAG = "MagicPADActivity";	
	
	// GUI
	private TextView logview, infoview;
	private ScrollView logScrollView;
	private CustomDrawableView myView;
		
	// MagicPAD and BlueTooth
	private MagicPadDevice magicPadDevice;
	private static final int REQUEST_CONNECT_DEVICE = 1;
	private static final int REQUEST_ENABLE_BT = 3;
	private static final int FRAME_PERIOD = 50;
	
	// process pipeline
	private ImageReaderAlgorithm imageReader = null;
	private CalibrationAlgorithm calibration = null;
	private OtsuAlgorithm otsu = null;
	private FingerTipAlgorithm fingerTip = null;
	private QuartAlgorithm quartAlgo = null;
	private SwapAlgorithm swapAlgo = null;
	
	private Timer mTimer;
	
	private int fpsCnt = 0;
	private long lastTime = 0;
	private double fps = 0;
	
	// Message handler
    final Handler handlerStatus = new Handler() {
        public void handleMessage(Message msg) {
            int co = msg.arg1;
            if(co == 1) {
            	//logview.append("Connected\n");
            	readFrames();
            } else if(co == 2) {
            	//logview.append("Disconnected\n");
            } else if(co == 3) {     
            	byte[] input = msg.getData().getByteArray("frame");
            	LemonModel.setPressureMap( input, 10, 10 );
            	//Otsu
            	/*double otsu = msg.getData().getDouble("otsu");
            	myView.setThreshold(otsu);
            	
            	// position
            	double x = msg.getData().getDouble("posX");
            	double y = msg.getData().getDouble("posY");
            	if(otsu > 220.0) {
            		myView.setXY(0, 0);            		
            	} else {           	
            	   	myView.setXY(x, y);
            	}
            	
            	y = Math.round(y*100)/100.0;
            	x = Math.round(x*100)/100.0;
            	
            	// frame
            	myView.setFrame(msg.getData().getByteArray("frame"));
            	
            	// framerate
            	if(++fpsCnt >= 10) {
            		fpsCnt = 0;
            		long t = System.currentTimeMillis() - lastTime;
            		fps = 10*1000/t;
            		lastTime = System.currentTimeMillis();
            	}
            	//logview.setText("FPS: " + fps + " Mean: " + String.valueOf(m));
            	
            	String txt = "Framerate: " + fps + '\n' 
            				+ "Otsu: " + otsu + '\n'
            				+ "Position: " + String.valueOf(x) + ", " + String.valueOf(y);            	
            	
            	infoview.setText(txt);  */    
            	 
            }
        }
    };    
    
    // handler for messages coming from MagicPad
    final Handler handlerEvent = new Handler() {    	
        public void handleMessage(Message msg) {
        	int swap = msg.getData().getInt("swap", SwapAlgorithm.SWAP_NONE); // NONE if not sent
        	int quart = msg.getData().getInt("quadrant", QuartAlgorithm.QUART_NONE); // NONE if not sent

        	if(swap == SwapAlgorithm.SWAP_CLICK) {
        		handleClick(quart);
        	} else if(swap != SwapAlgorithm.SWAP_NONE) {
        		handleSwap(swap);
        	}
        	
        	handleQuart(quart);
        }
        
        // handle swap events:
        // Display an arrow in the logview
        public void handleSwap(int s) {
        	addToLog(SwapAlgorithm.SWAP_SYMBOL[s]);
        }
        
        // handle click events:
        // Display message in logview
        public void handleClick(int quart) {
        	addToLog("\nClick: " + SwapAlgorithm.SWAP_CLICK_QUART[quart] + "\n");
        }
        
        // handle quadrant selection events
        // Display rectangle around selected quadrant
        public void handleQuart(int q) {
        	myView.setQuart(q);
        }
    };
    
    public class CustomDrawableView extends View {
        private byte[] mFrame = null;
        private double mThreshold = 0, mX = 0, mY = 0;
        private int mQuart = QuartAlgorithm.QUART_NONE;
        private int PSZ = 45; // pixel size
        
        public CustomDrawableView(Context context) {
        	super(context);        	
        }
        
        public void setFrame(byte[] frame)
        {
        	mFrame = frame;
        	invalidate();
        }
        
        public void setThreshold(double thres) {
        	mThreshold = thres;
        }
        
        public void setXY(double x, double y) {
        	mX = x;
        	mY = y;
        }
        
        public void setQuart(int q) {
        	mQuart = q;
        }
        
        protected void onDraw(Canvas canvas) {
        	if(false) return;
	        	if(mFrame != null) {
	        		Paint p = new Paint();
	        		
	        		Paint red = new Paint();
	        		red.setColor(Color.RED);
	        		Paint green = new Paint();
	        		green.setColor(Color.GREEN);
	        		Paint pt;
	        		
	        		int value = 0;
		        	for(int i=0; i<10; i++) {
		        		for(int j=0; j<10; j++) {
		        			// draw pixel
		        			value = (int)(mFrame[i*10 + j] & 0xff);
		        			p.setARGB(255, value, value, value);
		        			canvas.drawRect(j*PSZ, i*PSZ, (j+1)*PSZ, (i+1)*PSZ, p);
		        			// draw pixel value
		        			pt = value > mThreshold ? red : green;
		        			canvas.drawText(String.valueOf(value), (int)(j*PSZ + 10), (int)((i+0.5)*PSZ), pt);
		        		}
		        	}
		        	
		        	// draw barycenter
		        	red.setStrokeWidth(5);
		        	canvas.drawCircle((float)(PSZ*(10*mX + 5)), (float)(PSZ*(10*mY + 5)), (float)20.0, red);
		        	
		        	// draw quart
		        	Paint paint = red;
		        	paint.setColor(Color.BLUE);
		        	Path path = new Path();
		        	
		        	if(mQuart == QuartAlgorithm.QUART_BOTTOM_LEFT) {
		        		//canvas.drawRect(0*PSZ, 5*PSZ, 5*PSZ, 10*PSZ, paint);
		        		path.addCircle(2*PSZ, 7*PSZ, 20, Path.Direction.CW);
		        	} else if(mQuart == QuartAlgorithm.QUART_BOTTOM_RIGHT) {
		        		//canvas.drawRect(5*PSZ, 5*PSZ, 10*PSZ, 10*PSZ, paint);
		        		path.addCircle(8*PSZ, 8*PSZ, 20, Path.Direction.CW);
		        	} else if(mQuart == QuartAlgorithm.QUART_TOP_LEFT) {
		        		//canvas.drawRect(0*PSZ, 0*PSZ, 5*PSZ, 5*PSZ, paint);
		        		path.addCircle(2*PSZ, 2*PSZ, 20, Path.Direction.CW);
		        	} else if(mQuart == QuartAlgorithm.QUART_TOP_RIGHT) {
		        		//canvas.drawRect(5*PSZ, 0*PSZ, 10*PSZ, 5*PSZ, paint);	
		        		path.addCircle(8*PSZ, 2*PSZ, 20, Path.Direction.CW);
		        	}
		        	canvas.drawPath(path, paint);
	        	}
	        	
	        }
        }
    
    // Add a line of text to the log.
    public void addToLog(String s) {
    	//logview.append(s + '\n');
    	logview.append(s + ' ');
    	logScrollView.post(new Runnable()
        {
            public void run()
            {
            	logScrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
    }
    
    		    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*setContentView(R.layout.main);
        Log.d(TAG, "MagicPAD startup");
        
        // adding matrix view
        myView = new CustomDrawableView(this);
        LinearLayout mainLayout = (LinearLayout)findViewById(R.id.mainLayout);
        mainLayout.addView(myView);          
        
        infoview = (TextView)findViewById(R.id.infoview);  
                
        logScrollView = (ScrollView)findViewById(R.id.logScrollView);
        logview = (TextView)findViewById(R.id.logview);*/
        //addToLog("\u21e6 \u21e7 \u21e8 \u21e9 \u25ce\n");
        
        magicPadDevice = new MagicPadDevice(handlerStatus);
        
        // Create image processing pipeline:
        // MagicPadDevice --> ImageReaderAlgorithm --> CalibrationAlgorithm
        //														|
        //							   OtsuAlgorithm <----------|
        //				____________________|					|
        //				|				    v					|
        //				|		  FingerTipAlgorithm <----------|
        //				|	  ______________|________			
        //				|	 |						 |			
        //				v	 v						 v			
        //			SwapAlgorithm				QuadAlgorithm			
        
        imageReader = new ImageReaderAlgorithm();
        imageReader.setInput(magicPadDevice);
        
        calibration = new CalibrationAlgorithm();
        calibration.setInput(imageReader);
        
        /*otsu = new OtsuAlgorithm();
        otsu.setInput(calibration);
        
        fingerTip = new FingerTipAlgorithm();
        fingerTip.setOtsuInput(otsu);
        fingerTip.setInput(calibration);
        
        quartAlgo = new QuartAlgorithm();
        quartAlgo.setInput(fingerTip);
        
        swapAlgo = new SwapAlgorithm();
        swapAlgo.setInput(otsu);
        */
        mTimer = new Timer();
        
        // Show 'connect to a device' message
        /*AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(this);

        dlgAlert.setMessage("This is an alert with no consequence");
        dlgAlert.setTitle("App Title");
        dlgAlert.setPositiveButton("OK", null);
        dlgAlert.setCancelable(true);
        dlgAlert.create().show();*/
               
        // Start BT and connect to MagicPad
        if(enableBluetooth())
        {
        	// Already ON: launch BT device list
        	connectToDevice();
        }             
    }
    /*
    @Override
	public void onStop() {
		super.onStop();
		mTimer.cancel();
		magicPadDevice.close();
		if(D) Log.d(TAG, "Closing " + TAG);		
		finish();
	}*/
	
	// Start reading frames at regular intervals
	private void readFrames()
    {
    	mTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				TimerMethod();
			}
		}, 0, FRAME_PERIOD);
    }
	
	// Read a frame and update the processing pipeline
    private void TimerMethod() {    	
    	// send read frame command
    	magicPadDevice.sendCommand(MagicPadDevice.COMMAND_FRAME);
    	magicPadDevice.sendCommand(MagicPadDevice.COMMAND_FRAME);
    	
    	// update pipeline
    	//quartAlgo.update();
    	//swapAlgo.update();
    	calibration.update();
    	    	
    	// Debug >>>
		// Send message back to the view
		Message msg = handlerStatus.obtainMessage();
		msg.arg1 = 3;
		
		Bundle b = new Bundle();
		//b.putDouble("otsu", otsu.getThreshold());
		//b.putDouble("posX", fingerTip.getPosX());
		//b.putDouble("posY", fingerTip.getPosY());
		if(calibration.getOutput() != null) {
			b.putByteArray("frame", calibration.getOutput().data);
		} else {
			b.putByteArray("frame", null);
		}		
		msg.setData(b);
		handlerStatus.sendMessage(msg);		
		// <<< DEBUG
		
		/// Event messages
		/*msg = handlerEvent.obtainMessage();
		Bundle be = new Bundle();		
		// Quart		
		be.putInt("quadrant", quartAlgo.getQuart());		
		// Swap
		be.putInt("swap", swapAlgo.getLastSwapMotion());
		// send message
		msg.setData(be);
		handlerEvent.sendMessage(msg);*/				
    }
	
	// Enable BT
	private boolean enableBluetooth()
	{
		// Turn ON BT
		if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
		    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		    return false;
		}
		return true;
	}

	// Connect to a device
	private void connectToDevice()
	{
		//Intent serverIntent = new Intent(this, DeviceListActivity.class);
		//startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
	}

	// Called after activity requested something
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
			// When DeviceListActivity returns with a device to connect
			if(resultCode == Activity.RESULT_OK) {
				// Get the device MAC address
				String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				if(D) Log.d(TAG, "BT list choose: " + address);
				magicPadDevice.connect(address);
			}
			else
			{
				Toast.makeText(this, "R.string.bt_no_device_found", Toast.LENGTH_SHORT).show();
	            finish();
			}
			break;

        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if(resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so connect to a MagicPAD           	
            	connectToDevice();
            	if(D) Log.d(TAG, "BT enabled: continue");
            } else {
                // User did not enable Bluetooth or an error occurred
            	if(D) Log.d(TAG, "BT not enabled");
                Toast.makeText(this, "R.string.bt_not_enabled_leaving", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
	
	public boolean onKeyUp(int keyCode, KeyEvent event)
    {
        //if (keyCode == KeyEvent.KEYCODE_MENU)
		if (keyCode == KeyEvent.KEYCODE_BACK)
        {
            if(D) Log.d(TAG, "Menu key pressed: exit");
    		mTimer.cancel();
    		magicPadDevice.close();
    		if(D) Log.d(TAG, "Closing " + TAG);		
    		finish();
    		return true;
        }
		return false;
    }
}