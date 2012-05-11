
// The lemon morphing class

package com.android.lemon.model;

/*
import java.io.IOException;
import java.io.InputStream;*/
import java.nio.ByteBuffer;
/*import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;
import java.util.concurrent.locks.Lock;*/

import com.android.lemon.Material;
import com.android.lemon.ModelRenderer;
import com.android.lemon.obj.ObjFace;
//import com.android.lemon.obj.ObjModel;
import com.android.lemon.utils.ConvolutionMatrix;
import com.android.lemon.utils.MatrixUtils;
import com.isorg.magicpadexplorer.R;

import android.content.Context;
import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
import android.graphics.Bitmap.Config;
//import android.graphics.Color;
import android.opengl.GLES20;
//import android.opengl.GLUtils;
//import android.opengl.Matrix;
//import android.util.Log;

public class LemonModel extends BasicModel {
		
	// Capture informations 
	static int 	s_width;			// upscaled width
	static int 	s_height;		// upscaled height
	static byte[] s_capture;		// last given capture
	static Bitmap s_captureHD;	// upscaled bitmap object
	static int[] s_pixels;		// upscaled bitmap pixels
	static final float PRESSURE_TRESH = 0.3f;
	static int s_minyTreshCount = 0;
	static float s_miny = 1.0f;		// minimum y found in capture in range [0 , 1.0f]
	static float s_centery = 1.0f;	// center value of the capture in range [0 , 1.0f]
	static float s_avy = 1.0f;		// average value in range [0 , 1.0f]

	// Vertices & normals to be processed
	float[][] _verts;
	float[][] _norms;
	
	static int s_texid;
	
	// Ripple values : todo create a method
	public static float amp = 0.0f;
	public static float freq = 0.0f;
	
	float vscale = 1.0f;
	float vscaleinc = 0.00f; 
	
	QuadModel debugQuad = null; 
	
	// true if input capture has changed and must be reprocessed
	static Boolean s_bufferChanged = true;
	
	static int[] s_tmppixels = new int[10 * 10];
	static Bitmap s_tmpcapt = null;
	static ByteBuffer s_fcbuffer = null;
	
	public LemonModel(Context ctx, String objFileName)  {
		super(ctx, objFileName, true);
		
	  	// Preallocates
	    _verts = new float[3][3];
	    _norms = new float[mObj.vertices.length / 3][3];
	    
	    // Keep  the material id if available
	    Material material = this.mMaterials[0];
	    s_texid = material.textureID;
	    
	    //Bitmap logo = BitmapFactory.decodeResource(ModelRenderer.GetContext().getResources(), R.drawable.logoisorg);
		//debugQuad = new QuadModel(ctx, logo, 0.8f, 0.3f, 0.3f);
	}
	
	
	@Override protected void useShader(int shader) {
		super.useShader(shader);

    } 
	
	public void Init( ) {
		
		if (mInitDone == true)
			return;
		
		// use bump on lemon
		mProgram = ModelRenderer.createProgram(R.raw.bumpmap_vert, R.raw.bumpmap_frag);
        if (mProgram == 0) {
            throw new RuntimeException("Error compiling the shader programs");
        }
                
        this.loadTextures();
        
        if (debugQuad != null)
			debugQuad.Init();
        
        mInitDone = true;
	}
 
	
	
	// Convert input 10x10 capture into an upscaled bitmap based on the given size
	static Bitmap CreateBitmapFromArray(byte[] input, int outwidth, int outheight)
    {
		if (input == null)
			return null;
		
    	
    	
    	// For average calculation
    	float av = 0.0f;
    	
    	// Center value of the capture in range of [0, 1]
    	s_centery = (input[44] & 0xff) / 255.0f;
    	
    	int k = 0;
    	// Reset min value 
    	s_miny = 1.0f;
    	s_minyTreshCount = 0;
    	//for(int k = 0 ; k < 100 ; k ++)
    	for(int i = 0 ; i < 10 ; i ++)
	    	for(int j = 0 ; j < 10 ; j ++)
	    	{
	    		int ind = (9-j) + (i)*10;
	    		long col = input[ind] & 0xff;
	    		
	    		// Compute min value
	    		if ((col & 0xff)/ 255.0f < s_miny )
	    			s_miny = (col & 0xff) / 255.0f;
	    		if ((col & 0xff)/ 255.0f < PRESSURE_TRESH )
	    			s_minyTreshCount ++;
	    		
	    		// Add for average calculation
	    		av += (col & 0xff) / 255.0f;
	    		
	    		long integCol = (long)(col&0xff);
	    		integCol += (long)( s_tmppixels[k] & 0xff );
	    		integCol /= 2;
	    		
	    		col =  (integCol & 0xff); 
	    		
	    		// A
	    		s_tmppixels[k] |= col & 0xff;
	    		s_tmppixels[k] <<= 8;
		    	// R
	    		s_tmppixels[k] |= col & 0xff;
	    		s_tmppixels[k] <<= 8;
		    	// G
	    		s_tmppixels[k] |= col & 0xff;
	    		s_tmppixels[k] <<= 8;
		    	// B
	    		s_tmppixels[k] |= col & 0xff;
	    		 
	    		//s_tmppixels[k] = Color.argb(col, col, col, col);
		    	
		    	k++;
	    	}
    	
    	// average in range of [0, 1]
    	s_avy = (av / 100.0f);
    	
    	if (s_tmpcapt == null)
    		s_tmpcapt = Bitmap.createBitmap( 10, 10, Config.ARGB_8888);
    	
    	s_tmpcapt.setPixels(s_tmppixels, 0, 10, 0, 0, 10, 10);
    	
    	//Bitmap upscbit = s_tmpcapt;
    	//Bitmap upscbit = applyGaussianBlur(s_tmpcapt);
    	Bitmap upscbit = Bitmap.createScaledBitmap(s_tmpcapt, outwidth, outheight, true);
    	//upscbit = applyGaussianBlur(upscbit);
    	if (s_fcbuffer == null)
    		s_fcbuffer = ByteBuffer.allocateDirect(upscbit.getHeight() * upscbit.getWidth() * 4);
    	
    	upscbit.copyPixelsToBuffer(s_fcbuffer);
    	s_fcbuffer.position(0); 
    	
    	return(upscbit);
    }
	
	static double[][] s_GaussianBlurConfig = new double[][] {
	        { 1, 2, 1 },
	        { 2, 4, 2 },
	        { 1, 2, 1 }
	    };
	static ConvolutionMatrix s_convMatrix = new ConvolutionMatrix(3);
	public static Bitmap applyGaussianBlur(Bitmap src) {
		
		    s_convMatrix.applyConfig(s_GaussianBlurConfig);
		    s_convMatrix.Factor = 16;
		    s_convMatrix.Offset = 0;
		    return ConvolutionMatrix.computeConvolution3x3(src, s_convMatrix);
		
		}

	
	
	// Draw frame
	@Override public void DrawFrame()
	{
		// Get text id if it has changed
		Material material = this.mMaterials[0];
	    s_texid = material.textureID;
	    
		//synchronized (_lock) 
		{
			if ( s_bufferChanged == true )
			{
				// Reset capture info
				s_miny = 1.0f;
				s_centery = 1.0f;
				s_avy = 1.0f;
				
				if (s_captureHD != null)
				{
					s_captureHD.recycle();
					s_captureHD = null;
				}
				// Upscale the capture
				s_captureHD = LemonModel.CreateBitmapFromArray(s_capture, 40, 40);
				if (s_captureHD == null)
				{
					//_pixels = null;
					s_width = 0;
					s_height = 0;
					s_miny = 1.0f;
					s_centery = 1.0f;
					s_avy = 1.0f;
				}
				else
				{
					if ( debugQuad != null )
						debugQuad.setTexture(s_fcbuffer, s_captureHD.getWidth(), s_captureHD.getHeight(), false);
					
					s_width = s_captureHD.getWidth();
					s_height = s_captureHD.getHeight();
					if (s_pixels == null)
						s_pixels = new int[s_captureHD.getHeight() * s_captureHD.getWidth()];
					s_captureHD.getPixels(s_pixels, 0, s_captureHD.getWidth(), 0, 0, s_captureHD.getWidth(), s_captureHD.getHeight() );
				
					// Map the capture on the lemon
					GLES20.glBindTexture ( GLES20.GL_TEXTURE_2D, s_texid );
			        
			        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
			        		s_captureHD.getWidth(),s_captureHD.getHeight() , 0,GLES20.GL_RGBA ,GLES20.GL_UNSIGNED_BYTE, s_fcbuffer);
					
				}
				
			//}

        
				// ripple are triggered by min y 
				//if (s_miny < PRESSURE_TRESH)
				if (s_minyTreshCount > 10)
					vscaleinc = 0.05f;		// Todo : define constant
				else
					vscaleinc = -0.04f;	// Todo : define constant
				
				// Update the ripples effect
				vscale += vscaleinc;
				if (vscale < 0.0f)
					vscale = 0;
				else if (vscale > 1.0f)
					vscale = 1.0f;
				
				// Apply amplitude & frequency
				amp = 300*vscale;
				freq = 0.35f*vscale;
				
				
				// Reset OpenGL buffers position
				mVerticesBuffer.rewind();
				mNormalsBuffer.rewind();
				
				// Todo : optimize the computation OR do it on several frame OR once the capture has changed  
				
				if (( s_pixels != null ) && ( s_width != 0 )&& ( s_height != 0 ))
				{
				
					float[] v1 = new float[3];
					float[] v2 = new float[3];
					//float[] v3 = new float[3];
					float[] normal = new float[3];
					int fcount =  mObj.faces.size();
					
					// Clamp average
					if (s_avy < 0.15f)
						s_avy = 0.15f;
					float sccenter = s_centery;
					sccenter = Math.min( 1.0f, sccenter / s_avy);
					
					for (int nFace=0; nFace<fcount; nFace++) {
						ObjFace face = mObj.faces.get(nFace);
						 
						// now add the vertices/texcoords/normals to the buffers
						for (int nVert=0; nVert<3; nVert++) {
							
							byte col = (byte) 0xff;
							// Read color matching uvs
							
								int uvindex = face.texCoordIndices[nVert]*2;
								float u = mObj.texCoords[uvindex];
								float v = mObj.texCoords[uvindex + 1];
								
								long x = ((long)(1.0f*u*s_width))% (long)s_width;
								long y = ((long)(1.0f*v*s_height))% (long)s_height;
								
								int index = (int) (( x) + y * s_width);
								col = (byte)(s_pixels[index] & 0xff);
							
							
							// Compute the sc (scale value)
							float sc =  Math.max( 0.0f, ((col &0xff ) / 255.0f)  );
							// scale values depending on average lighting  
							sc = Math.min( 1.0f, sc / s_avy);
							
							int vindex = face.vertexIndices[nVert]*3;
							{
								// Use for Y morph : center value controls all vertices and can stretch up&down 
								float val =  (1.0f - sc * sccenter)*  Math.abs(mObj.vertices[vindex+1]);
								
								// Morph each coords
								float scxz = (Math.max( 0.0f, sccenter) - sc) /** (1.0f-sc)*/;
								_verts[nVert][0] = mObj.vertices[vindex] - s_avy * 0.4f * (scxz)*  mObj.vertices[vindex];	
								_verts[nVert][1] =  mObj.vertices[vindex+1] - (1.0f-s_avy) * 0.25f * val;				
								_verts[nVert][2] = mObj.vertices[vindex + 2] - s_avy * 0.4f* (scxz)* mObj.vertices[vindex+2];
							}
							// store 3 vertices
							mVerticesBuffer.put(_verts[nVert], 0, 3);
						}
			
						// Compute face normal
						
						// Vector AB
						v1[0] = _verts[1][0] - _verts[0][0];    
						v1[1] = _verts[1][1] - _verts[0][1];
						v1[2] = _verts[1][2] - _verts[0][2];
			
						// Vector AC
						v2[0] = _verts[2][0] - _verts[0][0];    
						v2[1] = _verts[2][1] - _verts[0][1];
						v2[2] = _verts[2][2] - _verts[0][2];
						
						// Cross AB x AC to get normal 
						MatrixUtils.cross(v1, v2, normal);
						
						// Skip normalize because shader we will do it on shader even if it is not accurate
						//MatrixUtils.normalize(normal);
						
						// Add per vertex normal
						
						int nindex = face.vertexIndices[0];
						_norms[nindex][0] += normal[0];
						_norms[nindex][1] += normal[1];
						_norms[nindex][2] += normal[2];
						
						nindex = face.vertexIndices[1];
						_norms[nindex][0] += normal[0];
						_norms[nindex][1] += normal[1];
						_norms[nindex][2] += normal[2];
						
						nindex = face .vertexIndices[2];
						_norms[nindex][0] += normal[0];
						_norms[nindex][1] += normal[1];
						_norms[nindex][2] += normal[2];
					}
					
					
					int vindex;
					// Store and normalize all normals 
					for (int nFace=0; nFace<fcount; nFace++) {
						ObjFace face = mObj.faces.get(nFace);
						
						// now add the vertices/texcoords/normals to the buffers
						for (int nVert=0; nVert<3; nVert++) {
							vindex = face.vertexIndices[nVert];
							
							normal[0] = _norms[vindex][0];
							normal[1] = _norms[vindex][1];
							normal[2] = _norms[vindex][2];
							
							// Skip normalize because shader we will do it on shader even if it is not accurate
							//MatrixUtils.normalize(normal);
							
							mNormalsBuffer.put(normal, 0, 3);
						}
					} 
					
					mVerticesBuffer.rewind();
					mNormalsBuffer.rewind(); 
				}
				// Force lemon to be positionned at the center of the world
				posy = -0;
				
			    s_bufferChanged = false;
			}
			
		}
		GLES20.glDisable(GLES20.GL_BLEND);
		super.DrawFrame();
		
		if (debugQuad != null)
			debugQuad.DrawFrame();
	}


	static Boolean _lock = true;		// not used
	
	// Notify that an input capture has changed 
	public static void setPressureMap(byte[] input, int width, int height) {
		
		//synchronized (_lock) 
		{
			s_capture = input;
		}
		s_bufferChanged = true;
	}
	

	
}
