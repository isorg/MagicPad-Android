package com.isorg.magicpadexplorer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedOutputStream;
import java.util.Set;
import java.util.UUID;

import com.isorg.magicpadexplorer.application.SmartSwitchApplication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class BtInterface {
	// Debug
	private static final boolean D = true; // false to disable debug log call
	private static final String TAG = "BtInterface";	

	// BT
	private BluetoothSocket socket = null;
	private InputStream receiveStream = null;
	private OutputStream sendStream = null;
	
	// messages
	Handler handler;
	private PipedOutputStream pipeOut;
	
	// The following UUID is for Serial Port Bluetooth device
	private static final UUID SerialPortServiceClass_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	
	private boolean mConnected = false;
	
	private ReceiverThread receiverThread;

	public BtInterface(Handler hstatus) {	
		handler = hstatus;
		pipeOut = new PipedOutputStream();
		
		Set<BluetoothDevice> setpairedDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
		BluetoothDevice[] pairedDevices = (BluetoothDevice[]) setpairedDevices.toArray(new BluetoothDevice[setpairedDevices.size()]);
		
		for(BluetoothDevice device : pairedDevices) {
			// Look for BT friendly name that contains 'Magic PAD'
			if(device.getName().contains("Magic PAD")) {
				try {					
					socket = device.createRfcommSocketToServiceRecord(SerialPortServiceClass_UUID);
					receiveStream = socket.getInputStream();
					sendStream = socket.getOutputStream();
				} catch (IOException e) {
					if(D) Log.e("BtInterface", "Cannot connect to device");
					e.printStackTrace();
				}
				break;
			}
		}		
		
		//receiverThread = new ReceiverThread(h);
		receiverThread = new ReceiverThread();
	}
	
	public PipedOutputStream getPipe()
	{
		return pipeOut;
	}
	
	// tell if a device is connected
	public boolean isConnected()
	{
		return mConnected;
	}
	
	// send raw data bytes
	public void sendData(byte[] data) {
		if(mConnected) {
			try {
				sendStream.write(data);
			} catch (IOException e) {
				if(D) Log.d(TAG, "Cannot write data");
				e.printStackTrace();
			}
		}
	}
	
	// send a string of data
	public void sendData(String data) {
		sendData(data, false);
	}
	
	// send a string of data
	public void sendData(String data, boolean deleteScheduledData) {
		try {
			sendStream.write(data.getBytes());
	        sendStream.flush();
		} catch (IOException e) {
			if(D) Log.d(TAG, "Cannot send data");
			e.printStackTrace();
		}
	}
	
	public void connect(String address)
	{
		Set<BluetoothDevice> setpairedDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
		BluetoothDevice[] pairedDevices = (BluetoothDevice[]) setpairedDevices.toArray(new BluetoothDevice[setpairedDevices.size()]);
		
		for(BluetoothDevice device : pairedDevices) {
			// Find BT device with correct MAC address and connect to it
			if(device.getAddress().equals(address)) {
				connect(device);
				break;
			}
		}	
		
	}

	public void connect(BluetoothDevice device) {
		
		// Open BT connection with device
		try {
			socket = device.createRfcommSocketToServiceRecord(SerialPortServiceClass_UUID);
			receiveStream = socket.getInputStream();
			sendStream = socket.getOutputStream();
		} catch (IOException e) {
			if(D) Log.e("BtInterface", "Cannot connect to device " + device.getName());
			e.printStackTrace();
		}
		
		mConnected = true;   
		
		if (D) Log.d(TAG, "Connexion réussie 1");
		
		new Thread() {
			@Override public void run() {			
				try {
					// block until connection is made
					socket.connect();
					
					Message msg = handler.obtainMessage();
					msg.arg1 = 1;
	                handler.sendMessage(msg);
	                
	                mConnected = true;
	                
					receiverThread.start();
					
				} catch (IOException e) {
					if(D) Log.v(TAG, "Connection Failed : " + e.getMessage());
					e.printStackTrace();
					Message msg = handler.obtainMessage();
					msg.arg1 = 2;
	                handler.sendMessage(msg);
				}
			}
		}.start();
		if (D) Log.d(TAG, "Connexion réussie 2");

	}

	public void close() {
		if(D) Log.d(TAG, "Closing " + TAG);
		try {
			receiverThread.interrupt();
			socket.close();
			mConnected = false;						
		} catch (IOException e) {
			if(D) Log.d(TAG, "While closing " + TAG);
			e.printStackTrace();
		}
	}
	
	/*
	 * ReceiverThread - thread waiting for incoming data
	 */
	private class ReceiverThread extends Thread {
		/*Handler handler;
		
		ReceiverThread(Handler h) {
			handler = h;
		}
		*/
		@Override public void run() {	
			
			if(D) Log.d(TAG, TAG + " ID: " + getId());
					
			while(!interrupted()) {
				try {
					if(receiveStream.available() > 0) {
						// send data into the pipe
						byte buffer[] = new byte[128];
						int k = receiveStream.read(buffer, 0, 128);						
						if(k > 0) {							
							pipeOut.write(buffer, 0, k);
						}
					}
				} catch (IOException e) {
					if(D) Log.d(TAG, "Cannot read BT buffer or write into pipe");
					e.printStackTrace();
				}
			}
		}
	}	
}
