package com.isorg.magicpad;



import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import com.isorg.magicpad.BtInterface;
import com.isorg.magicpad.MagicPadFrame;

import android.os.Handler;
import android.util.Log;

/*
 * MagicPadFrame
 */
public class MagicPadDevice {
	// Debug
	private static final boolean D = false; 
	private static final String TAG = "MagicPadDevice";
	
	/// BT interface
	private BtInterface bt;
	// string that must be contained in the device name
	public static final String DEVICE_NAME_MUST_HAVE = "Magic PAD";
	
	// Protocol
	public final static byte COMMAND_VERSION = 'V';
	public final static byte COMMAND_NAME = 'N'; // not implemented in fpga
	public final static byte COMMAND_ID = 'I'; // not implemented in fpga
	public final static byte COMMAND_FRAME = 'A';
	private ProtocolThread protocolThread;
	
	/// Device buffer
	private PipedInputStream pipeData;	
	private PipedInputStream pipeCmdIn;
	private PipedOutputStream pipeCmdOut;
	
	// MagicPAD version number
	private String deviceVersion = "0.0.0";
	
	// MagicPAD last frame
	private long mTime = 0;
	private MagicPadFrame deviceFrame = null;
	
    // Constructor
    public MagicPadDevice(Handler hstatus) {
    	
    	//deviceFrame = new MagicPadFrame();
    	
    	// BlueTooth interface
    	bt = new BtInterface(hstatus);
    	
    	// connect pipes to receive data from BT    	  	
    	pipeCmdOut = new PipedOutputStream();      	
    	try
    	{
    		pipeCmdIn = new PipedInputStream(pipeCmdOut);
    	}
    	catch(IOException e)
    	{
    		if(D) Log.d(TAG, "Cannot connect pipes.");
    		e.printStackTrace();
    	}    	
    	
    	try
    	{
    		pipeData = new PipedInputStream(bt.getPipe());  
    	} 
    	catch(IOException e)
    	{
    		if(D) Log.d(TAG, "Cannot connect pipes.");
    		e.printStackTrace();
    	}
    	    	
	}
    
    // update last changed time
    private void changed() {
    	mTime = System.currentTimeMillis();
    }
    
    public long getMTime() { return mTime; }
	
    // Connect to a device given a BlueTooth MAC address
	public void connect(String address) {				
		bt.connect(address);
		
    	// start protocol thread
    	protocolThread = new ProtocolThread();
    	protocolThread.start();    
    	
    	// send version command
    	//sendCommand(COMMAND_VERSION);    	
	}
	
	// send a command to the device
	public void sendCommand(int command)
	{
		if(!bt.isConnected()) {
			if(D) Log.d(TAG, "BT not connected");
			return;
		}
		
		// add 'command' in pipe to keep track of what is asked to the device
		try
		{
			pipeCmdOut.write(command);
		}
		catch(IOException e)
    	{
    		if(D) Log.d(TAG, "Cannot write command to pipe: " + command);
    		e.printStackTrace();
    	}
		
		// send BT
		byte cmd[] = new byte[1];
		cmd[0] = (byte) (command & 0xff);
		bt.sendData(cmd);
	}

	// Get device version MAJOR.MINOR.PATCH
	public String getVersion() {
		synchronized (deviceVersion) {
			return deviceVersion;
		}
	}
	
	// Get device last frame
	public MagicPadFrame getLastFrame() {
		if(D) Log.d(TAG, "getLastFrame() at t=" + mTime);
		if(deviceFrame == null) return null;
		synchronized (deviceFrame) {
			return deviceFrame;
		}
	}
	
	/*
	 * Protocol thread
	 */
	private class ProtocolThread extends Thread {				
		@Override public void run() {			
			int cmd = 0;
			
			while(!interrupted()) {
				try {					
					// read command
					cmd = pipeCmdIn.read();
				} catch (IOException e) {
					if(D) Log.d(TAG, "Cannot read pipeCmdIn");
					e.printStackTrace();
				}
				
				try
					{
					switch(cmd)
					{
					case COMMAND_ID:
						// Unsupported
						break;
						
					case COMMAND_NAME:
						// Unsupported
						break;
						
					case COMMAND_VERSION:							
						// read 3 bytes
						while(pipeData.available() < 3);
						
						byte version[] = new byte[3];
						pipeData.read(version, 0, 3);				
						
						// build version string
						int major = version[0] & 0xff;
						int minor = version[1] & 0xff;
						int patch = version[2] & 0xff;
						
						synchronized (deviceVersion) {
							deviceVersion = String.valueOf(major) + '.' + String.valueOf(minor) + '.' + String.valueOf(patch);
							if(D) Log.d(TAG, "Device Version is: " + deviceVersion);
							changed();
						}					
						break;
						
					case COMMAND_FRAME:
						// read 102 bytes
						while(pipeData.available() < 102);
						
						byte frame[] = new byte[102];
						pipeData.read(frame, 0, 102);
						
						// check first and last byte
						if(frame[0]!='S' || frame[101]!='E')
						{
							if(D) Log.d(TAG, "Incorrect frame " + frame[0] + ' ' + frame[101]);
						}
						else
						{
							// save frame 
							if(deviceFrame != null) {
								synchronized (deviceFrame) {
									deviceFrame = new MagicPadFrame();
									System.arraycopy(frame, 1, deviceFrame.data, 0, 100);
								}
							}
							else
							{
								deviceFrame = new MagicPadFrame();
								System.arraycopy(frame, 1, deviceFrame.data, 0, 100);
							}
							
							changed();
							if(D) Log.d(TAG, "frame received");
						}				
						
						break;
					}			
				} catch (IOException e) {
					if(D) Log.d(TAG, "Cannot read pipeData");
					e.printStackTrace();
				}				
			}
		}
	}
	
	public void close() {
		if(D) Log.d(TAG, "Closing " + TAG);
		if(protocolThread != null) {
			protocolThread.interrupt();
		}
				
		if(bt != null) {
			bt.close();
		}
		
	}
		
}
