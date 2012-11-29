package com.isorg.magicpadexplorer.algorithm;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import com.isorg.magicpadexplorer.MagicPadFrame;
import android.util.Log;


/*
 * Calibrate input frame to reduce dead pixels effect and increase dynamic range.  
 *
 */
public class CalibrationAlgorithm extends ImageAlgorithm {
	// Debug
	private static final boolean D = false; // false to disable debug log call
	private static final String TAG = "CalibrationAlgorithm";	
	
	// image array size
	private static final int IMG_SIZE = 100;
	private static final int IMG_ROW = 10;
	private static final int IMG_COL = 10;
	
	// threshold to decide if pixel is dead or alive
	private static final int CALIBRATIONFILTER_ALIVE_THRESHOLD = 15;
	
	
	
	// In order to be easily updated on PC and on Android, this algo follows the same processing than the computer software,
	// even if it creates some parts of code useless or redundant.
	// This algo uses matrix of OpenCV (Mat), like the algorithm calibration of MagicPad-PC.
	
	// Current frame
	private Mat frame;			// in int
	private Mat frame32F;		// the same frame in float in order to be more accurate
	private Mat frame32FBig;	// the same frame with 1 pixel added on each sides
	
	// Contains the max value of each pixel
	private Mat referenceFrame;
	
	// Contains mapping of dead (0) and alive (1) pixels
	private Mat alive;
	private Mat aliveBig;		// the same frame with 1 pixel added on each sides
	
	// Final frame
	private Mat calibratedFrame;
	
	// Gaussian interpolation mask
	private Mat mask;
	
	// Variables to setup calibration :
	// cf. CalibrationAlgorithm()	
	private float decay;
	private int periodicity;
    private int periodCounter;
    
    // decay is a percentage
    // By default, decay = 0.95
    // If you want to change it, we advise you to stay between 0.90 and 0.99
    public float getDecay()  {return decay;}
    public void setDecay(float d) {
    	if ( (d>0.0) && (d<1.0) )
    		decay = d;	
    }
    
    // By default, periodicity = 15
    // More periodicity is low, more the reference frame will be updated often
    public int getPeriodicity() {return periodicity;}
    public void setPeriodicity(int p) {
    	if ( (p>0) && (p<100) )
    		periodicity = p;	
    }
    
	
	// working pixel mask: true=working, false=dead
    // Others algo of pipeline need this array.
	private Pixel mMask[] = new Pixel[IMG_SIZE];	
	
	public Pixel[] getMask() {
		return mMask;
	}
	
	
	/*
	 *  Pixel 
	 */
	public class Pixel {
		// alive=working or dead pixel
		public boolean alive = false;
	}
	
	
	/*
	 *  constructor
	 */
	public CalibrationAlgorithm() {
		// initialize arrays for others algo
		for(int idx=0; idx<IMG_SIZE; idx++)
		{
			mMask[idx] = new Pixel();
			mMask[idx].alive = true;
		}
		
		// Variables to setup calibration :
		// Every "periodicity", we calculate a reference frame with maximum value of each pixel.
		// If ambient light decreases, this maximum needs to be decreased too.
		// In this case, we subtract "decay" of his value to it.		
		decay = (float) 0.95;
	    periodicity = 15;
	    periodCounter = 0;
	    
	    frame32F = new Mat();
	    frame32FBig = new Mat();
	    
	    referenceFrame = new Mat();
		
	    aliveBig = new Mat();
	    alive = new Mat();
	    
		calibratedFrame = new Mat(IMG_ROW, IMG_COL, CvType.CV_8U, Scalar.all(0));
	    
		// Gaussian interpolation mask
		mask = new Mat(3, 3, CvType.CV_32F, Scalar.all(0));
		mask.put(0, 0, 0.707);  mask.put(0, 1, 1.0);  mask.put(0, 2, 0.707);	//     .707   1.0   .707
		mask.put(1, 0, 1.0);    mask.put(1, 1, 0.0);  mask.put(1, 2, 1.0);		//      1.0   0.0    1.0
		mask.put(2, 0, 0.707);  mask.put(2, 1, 1.0);  mask.put(2, 2, 0.707);    //     .707   1.0   .707
	}
	
	
	@Override
	public void process() {
		if(D) Log.d(TAG, "process() at t=" + mTime);
		if(mInputFrame == null) {
			return;
		}
		
		// Current frame
		frame = new Mat(IMG_ROW, IMG_COL, CvType.CV_8U, Scalar.all(0));
		for (int i=0 ; i<IMG_ROW ; i++)
		{
			for (int j=0 ; j<IMG_COL ; j++)
			{
				frame.put(i, j, mInputFrame.data[i*(IMG_COL) + j] & 0xff);
			}
		}
		
		
	    // build frame in 32F if needed
	    if( (frame32F.empty()) || (frame32F.cols() != frame.cols()) || (frame32F.rows() != frame.rows()) )  //|| ( frame32F.size() != frame.size() ) )
	    {
	        frame32FBig = new Mat( frame.rows()+2, frame.cols()+2, CvType.CV_32F, Scalar.all(0) );
	        frame32F = frame32FBig.submat(1, frame.rows()+1, 1, frame.cols()+1);
	    }
	    
		frame.convertTo(frame32F, CvType.CV_32F);
		
		// build reference frame if needed
	    if(( referenceFrame.empty() ) || (referenceFrame.cols() != frame.cols()) || (referenceFrame.rows() != frame.rows()) )  //( referenceFrame.size() != frame.size() ) ) 
	    {
	        referenceFrame =  new Mat( frame.rows(), frame.cols(), CvType.CV_32F );

	        aliveBig = new Mat( frame.rows()+2, frame.cols()+2, CvType.CV_8U, Scalar.all(0) );
	        alive = aliveBig.submat( 1, frame.rows()+1, 1, frame.cols()+1 );

	        frame.convertTo( referenceFrame, CvType.CV_32F );
	    }

	    // Run the 'max' algorithm once every 'Periodicity' calls
	    if(periodCounter == 0)
	        computeReferenceFrame();
	    
	    periodCounter = (periodCounter+1) % periodicity;
	    

	    // Calibrate frame frame32F = (frame32F * 255) / referenceFrame
	    Core.divide(frame32F, referenceFrame, frame32F, 255);

	    // Interpolate dead/defective pixels
	    interpolate();

	    // Final conversion to <unsigned char> type
	    frame32F.convertTo(calibratedFrame, CvType.CV_8U);
	    
		
	    // Convert Matrix to byte[] to respect the pipeline format
	    mOutputFrame = new MagicPadFrame();
	    byte bConvert[] = new byte[IMG_SIZE];
	    calibratedFrame.get(0, 0, bConvert);
	    mOutputFrame.data = bConvert;
	}
	
	private void computeReferenceFrame()
	{
		// Update maximum
		Mat refWithDecay = new Mat(referenceFrame.rows(), referenceFrame.cols(), CvType.CV_32F, Scalar.all(0));
		for (int i=0 ; i<referenceFrame.rows() ; i++)
	    {
	    	for (int j=0 ; j<referenceFrame.cols() ; j++)
	    	{
	    		refWithDecay.put(i, j, referenceFrame.get(i, j)[0] * decay);
	    	}
	    }
		Core.max(refWithDecay, frame32F, referenceFrame);
		
		// Prevent referenceFram from having 0-value pixels
		// that would cause errors during calculation
		for (int i=0 ; i<referenceFrame.rows() ; i++)
	    {
	    	for (int j=0 ; j<referenceFrame.cols() ; j++)
	    	{
	    		if (referenceFrame.get(i, j)[0] == 0)
	    			referenceFrame.put(i, j, 1);
	    	}
	    }
		
		
		// Check dead/alive pixels
	    alive.setTo( Scalar.all(255) );
	    for (int i=0 ; i<referenceFrame.rows() ; i++)
	    {
	    	for (int j=0 ; j<referenceFrame.cols() ; j++)
	    	{
	    		// 0:dead, 1:alive
	    		if (referenceFrame.get(i, j)[0] > CALIBRATIONFILTER_ALIVE_THRESHOLD)
	    		{
	    			alive.put(i, j, 1 );
	    			mMask[i*IMG_COL + j].alive = true;		// We need to update the two arrays : alive and mMask
	    		}
	    		else
	    		{
	    			alive.put(i, j, 0);
	    			mMask[i*IMG_COL + j].alive = false;		// We need to update the two arrays : alive and mMask
	    		}
	    	}
	    }

	    // D19 diodes are always dead
	    // We need to update the two arrays : alive and mMask
	    alive.put(3, 3, 0);
	    alive.put(3, 6, 0);
	    alive.put(6, 3, 0);
	    alive.put(6, 6, 0);
	    mMask[3*IMG_COL + 3].alive = false;
	    mMask[6*IMG_COL + 3].alive = false;
	    mMask[3*IMG_COL + 6].alive = false;
	    mMask[6*IMG_COL + 6].alive = false;
	    		
	}
	
	
	/*
	 *  interpolate pixel from neighbor
	 */
	private void interpolate() {
		
		for(int i=1 ; i<frame32FBig.rows()-1 ; ++i)
	    {
	        for(int j=1 ; j<frame32FBig.cols()-1 ; ++j)
	        {
	            // Check if pixel is dead
	        	if(aliveBig.get(i, j)[0] == 0)
	            {
	                // Dead pixel: build interpolation matrix
	                Mat neighbor = new Mat( 3, 3, CvType.CV_32F, Scalar.all(0) );
	                
	                aliveBig.submat(i-1, i+2, j-1, j+2).convertTo( neighbor, CvType.CV_32F );

	                // Compute interpolated value
	                // 'M' is the same as 'mask' but with 0-coefficient over dead pixels
	                Mat M = new Mat(3, 3, CvType.CV_32F, Scalar.all(0));
	                Core.multiply(mask, neighbor, M);	                
	                
	                // 'roi' Region of interest is the 3x3 region centered on the dead pixel
	                Mat roi = frame32FBig.submat(i-1, i+2, j-1, j+2);
	                Scalar sum = Core.sumElems(M);
	                if(sum.val[0] >= 0.707)
	                {
	                    frame32FBig.put(i, j, M.dot( roi) / sum.val[0]) ;
	                }
	                // If there is no neighbor to interpolate, the pixel value is 0
	                else
	                {
	                    frame32FBig.put(i, j, 0);
	                }
	            }
	        }
	    }
	}
}
