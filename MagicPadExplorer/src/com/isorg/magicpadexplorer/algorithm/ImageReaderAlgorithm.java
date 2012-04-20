package com.isorg.magicpadexplorer.algorithm;

import com.isorg.magicpadexplorer.MagicPadDevice;

import android.util.Log;

public class ImageReaderAlgorithm extends ImageAlgorithm {
	// Debugging
    private static final String TAG = "ImageReaderAlgorithm";
    private static final boolean D = false;
    
    private MagicPadDevice mInputDevice = null;
    
    // set input device
    public void setInput(MagicPadDevice mpd)
    {
    	mInputDevice = mpd;
    }
    
    // Update pipeline
    @Override
    public void update() {
    	if(D) Log.d(TAG, "update() at t=" + mTime);
    	if(mInputDevice != null && mInputDevice.getMTime() >= getMTime())
    	{
    		if(D) Log.d(TAG, "   work");
    		if( mInputDevice.getLastFrame() == null ) Log.d(TAG, "mInputDevice.frame is null");
    		mOutputFrame = mInputDevice.getLastFrame();
    		this.process();
    		changed();
    	}
    	else
    	{
    		if(D) Log.d(TAG, "   already up to date");
    		if( mInputDevice == null ) Log.d(TAG, "inputdevice null");
    		//if( mInputDevice.getMTime() < getMTime() ) Log.d(TAG, "mInputDevice.getMTime < getMTime");

    	}
    }
    
    // Read frame from MagicPadDevice
    @Override
    public void process() {
    	if(D) Log.d(TAG, "process() at t=" + mTime);
    }    
}
