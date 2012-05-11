
// Class to manage water plane with procedural ripple  

package com.android.lemon.model;

/*import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;*/

import com.android.lemon.ModelRenderer;
//import com.android.lemon.obj.ObjFace;
//import com.android.lemon.obj.ObjModel;
import com.isorg.magicpadexplorer.R;

import android.content.Context;
import android.opengl.GLES20;
/*
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;*/

public class WaterModel extends BasicModel {
		
	public WaterModel(Context ctx, String objFileName)  {
		super(ctx, objFileName, false);
		
	}
	
	public void Init( ) {
		
		if (mInitDone == true)
			return;
		
		// Use the ripple shader : the entire effect is done by the shader
		mProgram = ModelRenderer.createProgram(R.raw.ripplecolormap_vert, R.raw.ripplecolormap_frag);
        if (mProgram == 0) {
            throw new RuntimeException("Error compiling the shader programs");
        } 
        
        this.loadTextures();
        
        mInitDone = true;
	}
	
	@Override protected void useShader(int shader) {
		super.useShader(shader);

		// Read ripple attributes from Lemon model 
		GLES20.glUniform1f(GLES20.glGetUniformLocation(mProgram, "frequency"), LemonModel.freq);
		GLES20.glUniform1f(GLES20.glGetUniformLocation(mProgram, "amp"), LemonModel.amp);
    }
	
	
	 
	@Override public void DrawFrame()
	{
		// Enable blend
		GLES20.glEnable (GLES20.GL_BLEND);
        GLES20.glBlendFunc (GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
		
		super.DrawFrame();
		
		GLES20.glDisable(GLES20.GL_BLEND);
	}
	

	
}
