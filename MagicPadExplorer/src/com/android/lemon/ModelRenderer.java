
// OpengGL 3D Renderer

package com.android.lemon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.android.lemon.model.BasicModel;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

public class ModelRenderer implements GLSurfaceView.Renderer {

    public ModelRenderer(Context context ) {
    	mContext = context; 
        //mModel = model;
    }
    
    static public Context GetContext()
    {
    	return(mContext);
    }
    
    // Register a model to the renderer    
    public void AddModel( BasicModel model ) {
    	mModels.add(model);
    }
    
    // Register a list of models to the renderer
    public void AddModels( ArrayList<BasicModel> models ) {
    	for(int i = 0 ; i < models.size() ; i ++)
    	{
    		BasicModel model = models.get(i);
    		mModels.add(model);
    	}
    }
    
    
    // Unregister a model
    public void RemoveModel( BasicModel model ) {
    	mModels.remove(model);
    }
    
    // Unregister all models
    public void ClearModels(  ) {
    	mModels.clear();
    }
    
 // Show/hide models from a given list 
    public void showModels( ArrayList<BasicModel> models, Boolean state ) {
    	for(int i = 0 ; i < models.size() ; i ++)
    	{
    		BasicModel model = models.get(i);
    		model.setVisible(state);
    	}
    }

    // Show/hide registered models  
    public void showAllModels( Boolean state ) {
    	for(int i = 0 ; i < mModels.size() ; i ++)
    	{
    		BasicModel model = mModels.get(i);
    		model.setVisible(state);
    	}
    }
    
     
    // Draw scene
    public void onDrawFrame(GL10 glUnused) {
        // Ignore the passed-in GL10 interface, and use the GLES20
        // class's static methods instead.
 
    	// Clear backbuffer & depth
        GLES20.glClear( GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        
        GLES20.glDisable(GLES20.GL_BLEND);
        // Render all models
        for( int i = 0 ; i < mModels.size() ; i ++ )
        {
        	BasicModel model = mModels.get(i);
        	if (model.isVisible() == true)
        		model.DrawFrame();	
        }
    }

    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        // Ignore the passed-in GL10 interface, and use the GLES20
        // class's static methods instead.
        GLES20.glViewport(0, 0, width, height);
        float ratio = (float) width / height;
        Matrix.frustumM(mmatProjection, 0, -ratio, ratio, -1, 1, 1.0f, 1000.0f);
    }
 
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
        // Ignore the passed-in GL10 interface, and use the GLES20
        // class's static methods instead.
    	
    	// Initialyse all models
    	for( int i = 0 ; i < mModels.size() ; i ++ )
        {
    		BasicModel model = mModels.get(i);
    		model.Init();
        }
    	
    	System.gc();
    	
    	// Setup default Render states 
        //GLES20.glClearColor(0.2f, 0.4f, 0.6f, 1.0f);
    	GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
    	
        checkGlError("glClearColor");
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        checkGlError("GL_CULL_FACE");
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        checkGlError("GL_DEPTH_TEST");
        
        // Get lemon size
        BasicModel model = mModels.get(0);
        if (mModels.size() > 1)
        	model = mModels.get(1);
        
        miScale = (-mfvEyePosition[2] - 1.0f) * 2 / ( 2*model.mLongestAxisLength );
        
        Matrix.setLookAtM(mmatView, 0, mfvEyePosition[0], mfvEyePosition[1], mfvEyePosition[2], 0f, 0f, 0f, 0f, 1.0f, 0.0f);
    }
    
    public void setAmbientLight(float intensity) {
    	// Todo
    }
    
    
    // Load & compile a shader program
    static private int loadShader(int shaderType, String source) {
    	
        int shader = GLES20.glCreateShader(shaderType);
        checkGlError("glCreateShader "+shaderType);
        if (shader != 0) {
            GLES20.glShaderSource(shader, source);
            checkGlError("glShaderSource "+source);
            GLES20.glCompileShader(shader);
            checkGlError("glCompileShader "+shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            checkGlError("glGetShaderiv "+shader);
            if (compiled[0] == 0) {
            	String err = GLES20.glGetShaderInfoLog(shader);
                Log.e(TAG, "Could not compile shader " + shaderType + ":");
                Log.e(TAG, err);
                GLES20.glDeleteShader(shader);
                checkGlError("glDeleteShader "+shader);
                shader = 0;
            }
        }

        return shader;
    }

    // Read shader source from file
    static private String readShader(int resId) {
    	BufferedReader reader = new BufferedReader(new InputStreamReader(mContext.getResources().openRawResource(resId)));
    	String result = "";
    	String line;
    	try {
			while ((line = reader.readLine()) != null)
				result += line+"\n";
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return result;
    }
    
    // Assemble both vertex & fragment shaders  
    static public int createProgram(int vertexResId, int fragResId) {
    	String strVertexShader = readShader(vertexResId);
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, strVertexShader);
        if (vertexShader == 0) {
            return 0;
        }

        String strFragmentShader = readShader(fragResId);
        int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, strFragmentShader);
        if (pixelShader == 0) {
            return 0;
        }

        // Link shaders
        int program = GLES20.glCreateProgram();
        if (program != 0) {
            GLES20.glAttachShader(program, vertexShader);
            checkGlError("glAttachShader");
            GLES20.glAttachShader(program, pixelShader);
            checkGlError("glAttachShader");
            GLES20.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GLES20.GL_TRUE) {
                Log.e(TAG, "Could not link program: ");
                Log.e(TAG, GLES20.glGetProgramInfoLog(program));
                GLES20.glDeleteProgram(program);
                program = 0;
            }
        }
        return program;
    }

    // Show debug info
    static private void checkGlError(String op) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, op + ": glError " + error);
            throw new RuntimeException(op + ": glError " + error);
        }
    }

    private static String TAG = "ModelRenderer";
    
    // Matrices used in shaders
    static public float[] mmatView = new float[16];
    static public float[] mmatProjection = new float[16];
    static public float[] mmatModelView = new float[16];
    static public float[] mmatViewProjection = new float[16];
    static public float[] mmatViewProjectionInverse = new float[16];
    static public float[] mmatViewProjectionInverseTranspose = new float[16];
    static public float[] mfvLightPosition = {0,0,10};
    static public float[] mfvEyePosition = {0,2,-4};
     
    static private Context mContext;
    
    // Models of the scene
    ArrayList<BasicModel> mModels = new ArrayList<BasicModel>();
    
    
    // Model scale
    static public  float miScale = 1.0f;
    static public  float mScale = 1.0f;
    
    // Warning : Model rotation angles in degrees 
    static public  float mAngleX = 90.0f;
    static public  float mAngleY = 0;
}
