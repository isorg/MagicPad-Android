
// Class to display 2d Quad

package com.android.lemon.model;

//import java.io.IOException;
//import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
/*import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;*/

import com.android.lemon.Material;
import com.android.lemon.ModelRenderer;
//import com.android.lemon.obj.ObjFace;
//import com.android.lemon.obj.ObjModel;
import com.isorg.magicpadexplorer.R;

import android.content.Context;
import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
import android.opengl.GLES20;
/*import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;*/

public class QuadModel extends BasicModel {
		
	float _x = 0.0f;
	float _y = 0.0f;
	float _scalex = 1.0f;
	float _scaley = 1.0f;
	
	public QuadModel(Context ctx, Bitmap bitm, float x, float y, float scale)  {
		super(ctx, null, false);
	
		posx = x;
		posy = y;
		// Scale and sprite aspect ration
		_scalex = scale;
		_scaley = scale * bitm.getHeight() / bitm.getWidth();
			
		float val = 1.0f;
		float vertices[] = {
		         // Vertices for the square
		          -val, -val,  0.0f,  // 0.
		          val, -val,  0.0f,  // 1. 
		          -val,  val,  0.0f,  // 2. 
		          val,  val,  0.0f   // 3. 
		};

		float texture[] = {          
		        //Mapping coordinates for the 4 vertices
		        0.0f, 1.0f,
		        1.0f, 1.0f,
		        0.0f, 0.0f,
		        1.0f, 0.0f, 
		};
		
		// VERTEX ****
		ByteBuffer vbb = ByteBuffer.allocateDirect(4*4*6);
		vbb.order(ByteOrder.nativeOrder());
		mVerticesBuffer = vbb.asFloatBuffer();
		
		mVerticesBuffer.rewind();
		
		// Create 2 triangles for vertex
		mVerticesBuffer.put(vertices, 0, 3);
		mVerticesBuffer.put(vertices, 3, 3);
		mVerticesBuffer.put(vertices, 6, 3);
		
		mVerticesBuffer.put(vertices, 3, 3);
		mVerticesBuffer.put(vertices, 9, 3);
		mVerticesBuffer.put(vertices, 6, 3);
		
		mVerticesBuffer.rewind();
		
		// UVS ****
		ByteBuffer tbb = ByteBuffer.allocateDirect(2*4*6);
		tbb.order(ByteOrder.nativeOrder());
		mTexCoordsBuffer = tbb.asFloatBuffer();
		
		mTexCoordsBuffer.rewind();
		
		// Create 2 triangles for uvs
		mTexCoordsBuffer.put(texture, 0, 2);
		mTexCoordsBuffer.put(texture, 2, 2);
		mTexCoordsBuffer.put(texture, 4, 2);
		
		mTexCoordsBuffer.put(texture, 2, 2);
		mTexCoordsBuffer.put(texture, 6, 2);
		mTexCoordsBuffer.put(texture, 4, 2);
		
		
		mTexCoordsBuffer.rewind();
		
		mMaterials = new Material[1];
		mMaterials[0] = new Material(bitm, 1.0f, 1.0f, 1.0f, 1.0f, 0, 6);
	}
	
	public void Init( ) {
		
		if (mInitDone == true)
			return;
		
		mProgram = ModelRenderer.createProgram(R.raw.basicmap_vert, R.raw.basicmap_frag);
        if (mProgram == 0) {
            throw new RuntimeException("Error compiling the shader programs");
        } 
        
        this.loadTextures();
        
        mInitDone = true;
	}
	
	public void setTexture(ByteBuffer fcbuffer, int width, int height, Boolean filter)
	{
		//mMaterials[0] = new Material(bitm, 1.0f, 1.0f, 1.0f, 1.0f, 0, 6);
		
		GLES20.glBindTexture ( GLES20.GL_TEXTURE_2D, mMaterials[0].textureID );
		
		if (filter == true)
		{
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
					GLES20.GL_NEAREST);
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
					GLES20.GL_TEXTURE_MAG_FILTER,
					GLES20.GL_LINEAR);
		}
		else
		{
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
					GLES20.GL_NEAREST);
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
					GLES20.GL_TEXTURE_MAG_FILTER,
					GLES20.GL_NEAREST);
		}
        
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
        		width,height , 0,GLES20.GL_RGBA ,GLES20.GL_UNSIGNED_BYTE, fcbuffer);
		
		_scaley = _scalex * height / width;
	}
	
	@Override protected void useShader(int shader) {
		super.useShader(shader);

		// Setup shaders params
		GLES20.glUniform1f(GLES20.glGetUniformLocation(mProgram, "posx"), posx);
		GLES20.glUniform1f(GLES20.glGetUniformLocation(mProgram, "posy"), posy);
		GLES20.glUniform1f(GLES20.glGetUniformLocation(mProgram, "scalex"), _scalex);
		GLES20.glUniform1f(GLES20.glGetUniformLocation(mProgram, "scaley"), _scaley);
    }
	 
	@Override public void DrawFrame()
	{
		// Allow blend
		GLES20.glEnable (GLES20.GL_BLEND);
        GLES20.glBlendFunc (GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glDisable (GLES20.GL_DEPTH_TEST);
		//GLES20.glDisable (GLES20.GL_DEPTH_WRITEMASK);
        
        GLES20.glEnable (GLES20.GL_BLEND);
        
		super.DrawFrame();
		
		GLES20.glDisable(GLES20.GL_BLEND);
		GLES20.glEnable (GLES20.GL_DEPTH_TEST);
		//GLES20.glEnable (GLES20.GL_DEPTH_WRITEMASK);
	}
	

	
}
