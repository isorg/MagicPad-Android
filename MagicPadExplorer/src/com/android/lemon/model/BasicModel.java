package com.android.lemon.model;

//import java.io.IOException;
//import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
//import java.util.Vector;

import com.android.lemon.Material;
import com.android.lemon.ModelRenderer;
import com.android.lemon.obj.ObjFace;
import com.android.lemon.obj.ObjLoader;
import com.android.lemon.obj.ObjModel;
import com.isorg.magicpadexplorer.R;

import android.content.Context;
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
import android.opengl.GLES20;
//import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

public class BasicModel {
	
	
	public BasicModel(Context ctx, String objFileName, Boolean keepObj) {
		
		// Load an obj file if given
		if (objFileName != null)
		{
			mObj = ObjLoader.loadObject(ctx, objFileName);
			Log.i(TAG, "Starting obj processing");
			genVertexArrays(mObj);
			mLongestAxisLength = Math.max(Math.max(mObj.minmax[1]-mObj.minmax[0],
											mObj.minmax[3]-mObj.minmax[2]),
											mObj.minmax[5]-mObj.minmax[4]);
			Log.i(TAG, "Finished obj processing: "+
					mObj.faces.size()+ " faces, "+
					mObj.vertices.length+" vertices");
		}
		
		if (keepObj == false)
			mObj = null;
	}
	
	// Use the current loaded program and get handles on attribs
	protected void useShader(int shader) {
    	// Todo shader param is not used !!
		
        GLES20.glUseProgram(mProgram);
        checkGlError("glUseProgram");
        
        // get handles for the vertex attributes
        mrm_VertexHandle = GLES20.glGetAttribLocation(mProgram, "rm_Vertex");
        checkGlError("glGetAttribLocation rm_Vertex");

        mrm_NormalHandle = GLES20.glGetAttribLocation(mProgram, "rm_Normal");
        checkGlError("glGetAttribLocation rm_Normal");

        mrm_TexCoord0Handle = GLES20.glGetAttribLocation(mProgram, "rm_TexCoord0");
        checkGlError("glGetAttribLocation rm_TexCoord0");
        
        // get handles for the light and eye positions
        mfvLightPositionHandle = GLES20.glGetUniformLocation(mProgram, "fvLightPosition");
        checkGlError("glGetUniformLocation fvLightPosition");

        mfvEyePositionHandle = GLES20.glGetUniformLocation(mProgram, "fvEyePosition");
        checkGlError("glGetUniformLocation fvEyePosition");

        // get handles for the transform matrices
        mmatViewProjectionHandle = GLES20.glGetUniformLocation(mProgram, "matViewProjection");
        checkGlError("glGetUniformLocation matViewProjection");

        mmatViewProjectionInverseTransposeHandle = GLES20.glGetUniformLocation(mProgram, "matViewProjectionInverseTranspose");
        checkGlError("glGetUniformLocation matViewProjectionInverseTranspose");
        
        // get handles for the material properties

        mfvAmbientHandle = GLES20.glGetUniformLocation(mProgram, "fvAmbient");
        checkGlError("glGetUniformLocation fvAmbient");

        mfvDiffuseHandle = GLES20.glGetUniformLocation(mProgram, "fvDiffuse");
        checkGlError("glGetUniformLocation fvDiffuse");

        mfvSpecularHandle = GLES20.glGetUniformLocation(mProgram, "fvSpecular");
        checkGlError("glGetUniformLocation fvSpecular");

        mfSpecularPowerHandle = GLES20.glGetUniformLocation(mProgram, "fSpecularPower");
        checkGlError("glGetUniformLocation fSpecularPower");

        mfTimeHandle = GLES20.glGetUniformLocation(mProgram, "fTime");
        checkGlError("glGetUniformLocation fTime");
        
    }
	

	public void setVisible(Boolean state) 	{
		mVisible = state;
	}
	
	public Boolean isVisible() 	{
		return( mVisible );
	}
	
	
	public void DrawFrame()
	{
		useShader(0);

		float[] mmatModel = new float[16];
		
        // bind the vertex arrays
        GLES20.glVertexAttribPointer(mrm_VertexHandle, 3, GLES20.GL_FLOAT, false,
        							 0, this.mVerticesBuffer);
        checkGlError("glVertexAttribPointer mrm_VertexHandle");
        GLES20.glEnableVertexAttribArray(mrm_VertexHandle);
        checkGlError("glEnableVertexAttribArray mrm_VertexHandle");
        
        // Normals
        if (this.mNormalsBuffer != null)
        {
        	GLES20.glVertexAttribPointer(mrm_NormalHandle, 3, GLES20.GL_FLOAT, false,
				 				     	0, this.mNormalsBuffer);
        	checkGlError("glVertexAttribPointer mrm_NormalHandle");
        	GLES20.glEnableVertexAttribArray(mrm_NormalHandle);
        	checkGlError("glEnableVertexAttribArray mrm_NormalHandle");
        }
        
        // Uvs
        if (mTexCoordsBuffer != null)  
        {
        	GLES20.glVertexAttribPointer(mrm_TexCoord0Handle, 2, GLES20.GL_FLOAT, false,
        						0, this.mTexCoordsBuffer);
        	checkGlError("glVertexAttribPointer mrm_TexCoord0Handle");
        	GLES20.glEnableVertexAttribArray(mrm_TexCoord0Handle);
        	checkGlError("glEnableVertexAttribArray mrm_TexCoord0Handle");
        }

        // bind the light and eye positions
		GLES20.glUniform3f(mfvLightPositionHandle,
							ModelRenderer.mfvLightPosition[0], 
							ModelRenderer.mfvLightPosition[1], 
							ModelRenderer.mfvLightPosition[2]);
		checkGlError("glUniform3f mfvLightPositionHandle");
		
		GLES20.glUniform3f(mfvEyePositionHandle,
							ModelRenderer.mfvEyePosition[0], 
							ModelRenderer.mfvEyePosition[1], 
							ModelRenderer.mfvEyePosition[2]);
		checkGlError("glUniform3f mfvEyePositionHandle");

		
        // Clamp angles
		if (ModelRenderer.mAngleY < -20.0f)
			ModelRenderer.mAngleY = -20.0f;
		else if (ModelRenderer.mAngleY > 150.0f)
			ModelRenderer.mAngleY = 150.0f;  
		
		// set up the transform matrices
        Matrix.setRotateM(mmatModel, 0, -ModelRenderer.mAngleY, 1.0f, 0, 0);
        Matrix.rotateM(mmatModel, 0, ModelRenderer.mAngleX, 0, 1.0f, 0);
        Matrix.scaleM(mmatModel, 0, ModelRenderer.miScale*ModelRenderer.mScale, ModelRenderer.miScale*ModelRenderer.mScale, ModelRenderer.miScale*ModelRenderer.mScale);
        Matrix.translateM(mmatModel, 0, posx, posy, posz);
        Matrix.multiplyMM(ModelRenderer.mmatModelView, 0, ModelRenderer.mmatView, 0, mmatModel, 0);
        Matrix.multiplyMM(ModelRenderer.mmatViewProjection, 0, ModelRenderer.mmatProjection, 0, ModelRenderer.mmatModelView, 0);  // XXX - TODO - this is a lie, need to rename these
        Matrix.invertM(ModelRenderer.mmatViewProjectionInverse, 0, ModelRenderer.mmatViewProjection, 0);
        Matrix.transposeM(ModelRenderer.mmatViewProjectionInverseTranspose, 0, ModelRenderer.mmatViewProjectionInverse, 0);
        
        // bind the transform matrices
        GLES20.glUniformMatrix4fv(mmatViewProjectionHandle, 1, false, ModelRenderer.mmatViewProjection, 0);
        checkGlError("glUniformMatrix4fv mmatViewProjectionHandle");
        GLES20.glUniformMatrix4fv(mmatViewProjectionInverseTransposeHandle, 1, false, ModelRenderer.mmatViewProjectionInverseTranspose, 0);
        checkGlError("glUniformMatrix4fv mmatViewProjectionInverseTransposeHandle");

        // Inc time (not real based time)
        fTime+= 0.08f;
        GLES20.glUniform1f(GLES20.glGetUniformLocation(mProgram, "fTime"), fTime);
        
        // loop through the materials
        for (int i=0; i<this.mMaterials.length; i++) {
        	Material material = this.mMaterials[i];
        	  
        	GLES20.glUniform1i(GLES20.glGetUniformLocation(mProgram, "hasTexture"), material.textureID);

        	// turn on the color texture if there is one
        	if (material.textureID != -1) {
                GLES20.glEnableVertexAttribArray(mrm_TexCoord0Handle);
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                //GLES20.glEnable(GLES20.GL_TEXTURE_2D);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, material.textureID);
                
                GLES20.glUniform1i(GLES20.glGetUniformLocation(mProgram, "baseMap"), 0);
                 
        
        	}   
        	
        	// turn on the bump texture if there is one
        	if (material.bumpID != -1) {
                //GLES20.glEnableVertexAttribArray(mrm_TexCoord0Handle);
                GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, material.bumpID);
                
                GLES20.glUniform1i(GLES20.glGetUniformLocation(mProgram, "bumpMap"), 1);
        	}
        	
        	
        	// turn on the Cubemap texture if there is one
        	if (material.cubeID != -1) {
        		// Bind the texture
                GLES20.glActiveTexture ( GLES20.GL_TEXTURE2 );
                GLES20.glBindTexture ( GLES20.GL_TEXTURE_CUBE_MAP, material.cubeID );
                
                GLES20.glUniform1i(GLES20.glGetUniformLocation(mProgram, "cubeMap"), 2);
        	}

        	 
        	// bind the materials
        	GLES20.glUniform4f(mfvAmbientHandle, material.matrix[0], material.matrix[1], material.matrix[2],  material.matrix[3]);
        	checkGlError("glUniform4f mfvAmbientHandle");
        	GLES20.glUniform4f(mfvDiffuseHandle, material.matrix[4], material.matrix[5], material.matrix[6],  material.matrix[7]);
        	checkGlError("glUniform4f mfvDiffuseHandle");
        	GLES20.glUniform4f(mfvSpecularHandle, material.matrix[8], material.matrix[9], material.matrix[10],  material.matrix[11]);
        	checkGlError("glUniform4f mfvSpecularHandle");
        	GLES20.glUniform1f(mfSpecularPowerHandle, material.matrix[12]);
        	checkGlError("glUniform1f mfSpecularPowerHandle");
        	 
            // draw all the faces with this material
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, material.vertexIndexStart, material.vertexIndexCount);
            checkGlError("glDrawArrays");
        }
	}
	
	// Show debug info
	private void checkGlError(String op) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, op + ": glError " + error);
            //throw new RuntimeException(op + ": glError " + error);
        }
    }
	 
    // Model must be initialized
	public void Init( ) {
		if (mInitDone == true)
			return;
		
		mProgram = ModelRenderer.createProgram(R.raw.colormap_vert, R.raw.colormap_frag); 
        if (mProgram == 0) {
            throw new RuntimeException("Error compiling the shader programs");
         } 
        
        // Load texture into memory
        this.loadTextures();
        
        mInitDone = true;
	}
	
	// generate linear lists of matched vertices, uvs and normals
	private void genVertexArrays(ObjModel obj) {
		if (obj.faces.size()==0) 
			return;	
		
		// number of faces * 3 vertices/face * 3 coords per vertex * 4 bytes per float
		int bufSize = obj.faces.size()*3*3*4;
		// number of faces * 3 vertices/face * 2 coords per vertex * 4 bytes per float
		int texCoordBufSize = obj.faces.size()*3*2*4;
		
		ByteBuffer vbb = ByteBuffer.allocateDirect(bufSize);
		vbb.order(ByteOrder.nativeOrder());
		mVerticesBuffer = vbb.asFloatBuffer();
		
		ByteBuffer nbb = ByteBuffer.allocateDirect(bufSize);
		nbb.order(ByteOrder.nativeOrder());
		mNormalsBuffer = nbb.asFloatBuffer();
		 
		mTexCoordsBuffer = null;
		if ((obj.texCoords != null) && (obj.texCoords.length > 0))
		{
			ByteBuffer tbb = ByteBuffer.allocateDirect(texCoordBufSize);
			tbb.order(ByteOrder.nativeOrder());
			mTexCoordsBuffer = tbb.asFloatBuffer();
		}
	    
	    // sort the faces by material so we can render all the faces with that material at once
	    Collections.sort(obj.faces, new ObjFace.byMaterial());
	    
	    ArrayList<Material> materials = new ArrayList<Material>(obj.materials.size());
	    String objMaterial = obj.faces.get(0).material;
	    int materialBeginsAtFace = 0;
	    int materialFaceCount = 0;
	     
		for (int nFace=0; nFace<obj.faces.size(); nFace++) {
			ObjFace face = obj.faces.get(nFace);
			
			// check for a different material first
			if (!face.material.equals(objMaterial)) {
				// store the old one
				if (materialFaceCount > 0)
					materials.add(new Material(obj.materials.get(objMaterial),
													 materialBeginsAtFace*3,
													 materialFaceCount*3)); 
				  
				// track the new one
				objMaterial = face.material; 
				materialBeginsAtFace = nFace; 
				materialFaceCount = 1;
			} else {  
				materialFaceCount++;
			}
			
			// now add the vertices/texcoords/normals to the buffers
			for (int nVert=0; nVert<3; nVert++) {
				// copy 3 coords from vertices[face.vertexIndices[nVert]*3]
				mVerticesBuffer.put(obj.vertices, face.vertexIndices[nVert]*3, 3);
				mNormalsBuffer.put(obj.normals, face.normalIndices[nVert]*3, 3);
				if ((obj.texCoords != null) && (obj.texCoords.length > 0))
					mTexCoordsBuffer.put(obj.texCoords, face.texCoordIndices[nVert]*2, 2);
			}
		}
		// store the last material
		if (materialFaceCount > 0)
			materials.add(new Material(obj.materials.get(objMaterial),
											 materialBeginsAtFace*3,
											 materialFaceCount*3));
		mMaterials = new Material[materials.size()];
		materials.toArray(mMaterials);
		
		mVerticesBuffer.rewind();
		if (mNormalsBuffer != null)
			mNormalsBuffer.rewind();
		if (mTexCoordsBuffer != null)
			mTexCoordsBuffer.rewind();
	}
	
	// Load all textures contained in the materials (bump, color, cubemap)
	public void loadTextures() {
		Log.i(TAG, "Loading "+mMaterials.length+" textures");
		for (int i=0; i<mMaterials.length; i++)
			mMaterials[i].loadTexture();
		Log.i(TAG, "Finished loading textures");
	}
	
	
	protected Boolean mInitDone = false;
	public float mLongestAxisLength = 1.0f;
	public Material[] mMaterials = null;
	
	// OpengGL buffers 
	public FloatBuffer mVerticesBuffer = null;
	public FloatBuffer mNormalsBuffer = null;
	public FloatBuffer mTexCoordsBuffer = null;
	// Shader program
	protected int mProgram;
	
	// Model Translation
	float posx = 0.0f;
	float posy = -80.0f;		// Force entire scene to be under 0 : Lemon is just positionned on 0 to simplify the rotation
	float posz = 0.0f;
 
	// Shaders attributes
	private int mrm_VertexHandle;
    private int mrm_NormalHandle;
    private int mrm_TexCoord0Handle;
    private int mmatViewProjectionHandle;
    private int mmatViewProjectionInverseTransposeHandle;
    private int mfvLightPositionHandle;
    private int mfvEyePositionHandle;
  	private int mfvAmbientHandle;
  	private int mfvDiffuseHandle;
  	private int mfvSpecularHandle;
  	private int mfSpecularPowerHandle;
  	private int mfTimeHandle;

  	Boolean mVisible = true;
	static float fTime = 0.0f;
  	
    private static String TAG = "BasicModel";
    // Keep a reference on original obj model
    ObjModel mObj;
}
