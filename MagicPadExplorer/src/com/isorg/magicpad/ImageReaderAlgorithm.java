package com.isorg.magicpad;

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
    		mOutputFrame = mInputDevice.getLastFrame();
    		this.process();
    		changed();
    	}
    	else
    	{
    		if(D) Log.d(TAG, "   already up to date");
    	}
    }
    
    // Read frame from MagicPadDevice
    @Override
    public void process() {
    	if(D) Log.d(TAG, "process() at t=" + mTime);
    }    
}
