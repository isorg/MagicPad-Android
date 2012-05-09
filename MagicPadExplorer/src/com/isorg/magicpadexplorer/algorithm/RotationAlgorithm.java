package com.isorg.magicpadexplorer.algorithm;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Range;
import org.opencv.core.Scalar;
import org.opencv.video.Video;

import android.util.Log;

public class RotationAlgorithm extends ImageAlgorithm {
	// Debugging
    private static final String TAG = "RotationAlgorithm";
    private static final boolean D = true;
    
    private Mat mInputMat = new Mat(10, 10, CvType.CV_8U );
    
    // Filter internal data
    private double Damping;
    private boolean Clamped;
    
    private double Moment;
    private double Speed;
    private double Angle;
    private double mTargetAngle;

    private double MomentToSpeed;
    private double SpeedToAngle;

    private Point MotionVector;
    
    private final static double ROTATION_FILTER_MOMENT_TO_SPEED = 1.0;
    private double ROTATION_FILTER_SPEED_TO_ANGLE = 0.25;

    // Optical Flow
    private Mat mBigFrame;    // last frame from sensor
    private Mat mBigPrevious; // previous m_frame
    private Mat mBigFlow;     // optical flow

    private Mat mPrevious;     // previous m_frame
    private Mat Flow;         // optical flow
    
    public RotationAlgorithm()
    {
    	reset();
    }
    
    public void setRotationSpeed (double speed) {
    	ROTATION_FILTER_SPEED_TO_ANGLE = speed;
    }
    
	@Override
	public void process() {
		if(mInputFrame == null) {
			return;
		}
				
		buildOpenCVMat();

		computeOpticalFlow();
		
		//if(D) Log.d(TAG, "angle=" + (int)Angle);
	}
	
	private void reset()
	{
		// Setup matrices, and stuff
	    mBigFrame = new Mat( 30, 30, CvType.CV_8U );
	    mBigFrame.setTo( Scalar.all(128) );
	    mInputMat = new Mat(mBigFrame, new Range(10, 20), new Range(10, 20) );

	    mBigPrevious = new Mat( 30, 30, CvType.CV_8U );
	    mBigPrevious.setTo( Scalar.all(128) );
	    mPrevious = new Mat(mBigPrevious, new Range(10, 20), new Range(10, 20) );

	    mBigFlow = new Mat( 30, 30, CvType.CV_32FC2 );
	    mBigFlow.setTo( Scalar.all(0) );
	    Flow = new Mat(mBigFlow, new Range(10, 20), new Range(10, 20) );

	    Damping = 0.95;
	    Clamped = true;

	    Moment = 0.0;
	    Speed = 0.0;
	    Angle = 0.0;
	    mTargetAngle = 0.0;

	    MomentToSpeed = ROTATION_FILTER_MOMENT_TO_SPEED;
	    SpeedToAngle = ROTATION_FILTER_SPEED_TO_ANGLE;
	}
	
	private void buildOpenCVMat()
	{
		for( int i=0; i<10; i++ )
		{
			for( int j=0; j<10; j++ )
			{
				mInputMat.put( i,  j, mInputFrame.data[i*10 + j] );				
			}
		}
		//mInputMat = new Mat(10, 10, CvType.CV_8U, mInputFrame.data );
	}

	
	/**
	 * Compute Optical Flow and Rotation
	 */
	void computeOpticalFlow()
	{
		// TODO: user objectDetectionFilter instead of dirty hack
		/// Check for object in the scene
		// 85% occupation: need to be clearly over the sensor
		boolean objDetect = Core.mean( mInputMat ).val[0] < (255.0 * 0.85);
		
		//
		// Optical Flow computation
		//
		Video.calcOpticalFlowFarneback(
		            mBigPrevious,       // (current-1)^th frame (input)
		            mBigFrame,          // current frame (input)
		            mBigFlow,           // optical flow (output)
		            0.5,                // pyramid scale (0.5=default)
		            2,                  // number of pyramidal levels (1=default)
		            3,                  // winsize
		            3,                  // iteration
		            3,                  // poly neighboor
		            1.1,                // poly sigma
		            0 & Video.OPTFLOW_FARNEBACK_GAUSSIAN );                 // flags
		
		// Save the current frame
		// The current frame is 10-by-10. m_previous points to the central area
		// of m_bigPrevious (30-by-30).
		mInputMat.copyTo( mPrevious );
		
		// If no object is over the sensor then quit, ie: do not update Angle
		//if( !objDetect ) return;
		
		//
		// Compute motion
		//
		if( objDetect )
		{
		    MotionVector = motion( Flow );
		}
		else
		{
		    MotionVector = new Point(0,0);
		}
		
		//
		// Compute Angle
		//
		
		// Moment
		double MAX_MOMENT = 500;
		double m = 0.0;
		
		if( objDetect ) m = moment( Flow );
		
		Moment = clamp( m, -MAX_MOMENT, MAX_MOMENT );
		
		// Speed
		Speed = Moment * MomentToSpeed;
		
		// Angle
		double delta = Speed * SpeedToAngle;
		mTargetAngle += delta;
		
		if( Clamped )
		{
		    mTargetAngle = clamp( mTargetAngle, 0.0, 360.0 );
		}
		else
		{
		   // while( mTargetAngle >= 360.0 ) mTargetAngle -= 360.0;
		   // while( mTargetAngle < 0.0 ) mTargetAngle += 360.0;
		}
		
		Angle += ( mTargetAngle - Angle ) * Damping;
	}
	
	/**
	 * Return the resulting moment of all vectors of 'src'.
	 */
	double moment( Mat src )
	{
	    Point3 center = new Point3(src.cols()/2.0, src.rows()/2.0, 0.0);
	    Point3 m = new Point3(0, 0, 0);
	
	    for(int i=0; i<src.rows(); i++)
	    {
	        for(int j=0; j<src.cols(); j++)
	        {
	            Point f = new Point( src.get(i, j) );
	            //Point3 F = new Point3( f.x, f.y, 0 );
	            
	            Point3 pos = new Point3( center.x - j, center.y - i, 0);
	        
	            // m += F.cross( pos ); 
	            m.x += f.y * pos.z - 0 * pos.y;
        		m.y += 0 * pos.x - f.x * pos.z;
            	m.z += f.x * pos.y - f.y * pos.x;	            
	        }
	    }
	
	    return m.z;
	}
	
	/**
	 * Return the global motion
	 */
	private Point motion( Mat src )
	{
	    Point vec = new Point(0, 0);
	
	    for(int i=0; i<src.rows(); i++)
	    {
	        for(int j=0; j<src.cols(); j++)
	        {
	        	Point p = new Point( src.get(i, j) );
	            vec.x += p.x;
	            vec.y += p.y;
	        }
	    }
	
	    return vec;
	}
	
	private double clamp(double in, double low, double high)
	{		
		if( in < low ) return low;
		if( in > high ) return high;
		return in;
	}
	
	public double getAngle()
	{
		return Angle;
	}
	
	public void setClamped(boolean on)
	{
		Clamped = on;
	}
	
	public Mat getFlow() {
		return mBigFlow;
	}
}
