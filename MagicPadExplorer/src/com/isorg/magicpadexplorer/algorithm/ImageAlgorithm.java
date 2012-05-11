package com.isorg.magicpadexplorer.algorithm;

import com.isorg.magicpadexplorer.MagicPadFrame;


/*
 * Define generic algorithm
 * 
 * Usage: 
 *  - extend this class: ex public class MyAlgorithm extends ImageAlgorithm { ... }
 *  - Instantiate: MyAlgorithm algo = new MyAlgorithm();
 *  - connect input: algo.setInput(anotherAlgo);
 *  - get output: 
 *  	algo.update(); // update all parents
 *      MagicPadFrame frame = algo.getOutput();
 */
public abstract class ImageAlgorithm {
    
    protected ImageAlgorithm mInputAlgorithm = null;
    protected MagicPadFrame mInputFrame = null;
    protected MagicPadFrame mOutputFrame = null;
    
    // time keeping
    long mTime = 0;
    
    // Input
    public void setInput(ImageAlgorithm input) {
    	mInputAlgorithm = input;
    	
    	changed();
    }
    
    // Output
    public MagicPadFrame getOutput() {
    	return mOutputFrame;
    }
    
    // Update pipeline    
    public void update() {
    	if(mInputAlgorithm != null)
    	{
    		mInputAlgorithm.update();    		
    		if(mInputAlgorithm.getMTime() > getMTime()) {
    			mInputFrame = mInputAlgorithm.getOutput();
    			this.process();
    			changed();
    		}
    	}
    }
    
    // Process data
    public abstract void process();
    
    // Update filter timestamp
    protected void changed() {
    	mTime = System.currentTimeMillis();    	
    }
    
    public long getMTime() {
    	return mTime;
    }
    
}
