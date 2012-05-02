
// Model material

package com.android.lemon;

import java.nio.ByteBuffer;
import com.android.lemon.obj.ObjMaterial;
import com.isorg.magicpadexplorer.R;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;

public class Material {
	public float[] matrix = null;
	public int textureID = -1;
	public int bumpID = -1;
	public int cubeID = -1;
	public int vertexIndexStart = -1;
	public int vertexIndexCount = -1;
	private Bitmap mTexture = null;
	private Bitmap mBump = null;
	
	public Material(ObjMaterial obj, int start, int count) {
		// set up the matrix we'll be passing to the shaders
		matrix = new float[] {
			obj.ambient[0], obj.ambient[1], obj.ambient[2], obj.alpha,
			obj.diffuse[0], obj.diffuse[1], obj.diffuse[2], obj.alpha,
			obj.specular[0], obj.specular[1], obj.specular[2], obj.alpha,
			obj.shininess, 0.0f, 0.0f, 0.0f,
		};
		// store the vertices this material should be applied to
		vertexIndexStart = start;
		vertexIndexCount = count;
		
		// we can't load this into texture memory until later
		mTexture = obj.texture;
		mBump = obj.bump;
	}
	
	public Material(Bitmap bitm, float r, float g, float b, float a, int start, int count) {
		// set up the matrix we'll be passing to the shaders
		matrix = new float[] {
			r, g, b, a,
			r, g, b, a,
			0.0f, 0.0f, 0.0f, 0.0f,
			0.0f, 0.0f, 0.0f, 0.0f,
		};
		// store the vertices this material should be applied to
		vertexIndexStart = start;
		vertexIndexCount = count;
		
		// we can't load this into texture memory until later
		mTexture = bitm;
		mBump = null;
	}
	
    // Create a simple cubemap 
    private int createSimpleTextureCubemap( )
    {
        int[] textureId = new int[1];

        // Generate a texture object
        GLES20.glGenTextures ( 1, textureId, 0 );

        // Bind the texture object
        GLES20.glBindTexture ( GLES20.GL_TEXTURE_CUBE_MAP, textureId[0] );
    
        // Load default cubemap
        Bitmap img = null;
        img = BitmapFactory.decodeResource(ModelRenderer.GetContext().getResources(), R.drawable.cloudmap);
        ByteBuffer fcbuffer = null;
        fcbuffer = ByteBuffer.allocateDirect(img.getHeight() * img.getWidth() * 4);
        img.copyPixelsToBuffer(fcbuffer);
        fcbuffer.position(0);
        
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_X, 0, GLES20.GL_RGBA,
                img.getWidth(),img.getHeight() , 0,GLES20.GL_RGBA ,GLES20.GL_UNSIGNED_BYTE, fcbuffer);
        
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_CUBE_MAP_NEGATIVE_X, 0, GLES20.GL_RGBA,
                img.getWidth(),img.getHeight() , 0,GLES20.GL_RGBA ,GLES20.GL_UNSIGNED_BYTE, fcbuffer);
        
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_Y, 0, GLES20.GL_RGBA,
                img.getWidth(),img.getHeight() , 0,GLES20.GL_RGBA ,GLES20.GL_UNSIGNED_BYTE, fcbuffer);
        
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y, 0, GLES20.GL_RGBA,
          img.getWidth(),img.getHeight() , 0,GLES20.GL_RGBA ,GLES20.GL_UNSIGNED_BYTE, fcbuffer);
        
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_Z, 0, GLES20.GL_RGBA,
                img.getWidth(),img.getHeight() , 0,GLES20.GL_RGBA ,GLES20.GL_UNSIGNED_BYTE, fcbuffer);
        
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z, 0, GLES20.GL_RGBA,
                img.getWidth(),img.getHeight() , 0,GLES20.GL_RGBA ,GLES20.GL_UNSIGNED_BYTE, fcbuffer);
        
        fcbuffer = null;
        img.recycle();

        // Set the filtering mode
        GLES20.glTexParameteri ( GLES20.GL_TEXTURE_CUBE_MAP, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST );
        GLES20.glTexParameteri ( GLES20.GL_TEXTURE_CUBE_MAP, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST );

        return textureId[0];
    }
	
	public void loadTexture() {
		if ((mTexture == null) && (mBump == null))
			return;
		
		//http://www.opengl.org/wiki/Common_Mistakes
		if (mTexture != null) {
			int[] textures = new int[1];
			GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
			GLES20.glGenTextures(1, textures, 0);
			textureID = textures[0];
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureID);

			// parameters
				GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
						GLES20.GL_NEAREST);
				GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
						GLES20.GL_TEXTURE_MAG_FILTER,
						GLES20.GL_LINEAR);

				GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
						GLES20.GL_REPEAT);
				GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
						GLES20.GL_REPEAT);

			GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mTexture, 0);
			mTexture.recycle();
			mTexture = null;
		} 
		
		if (mBump != null) {
			int[] textures = new int[1];
			GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
			GLES20.glGenTextures(1, textures, 0);
			bumpID = textures[0];
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, bumpID);

			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
					GLES20.GL_NEAREST);
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
					GLES20.GL_LINEAR);

			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
					GLES20.GL_REPEAT);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
					GLES20.GL_REPEAT);

			GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mBump, 0);
			mBump.recycle();
			mBump = null;
		}
		
		// Force a default cube map to each model
		cubeID = createSimpleTextureCubemap();
	}
	
}
