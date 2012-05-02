package com.android.lemon.obj;

import java.util.ArrayList;
import java.util.HashMap;


//values loaded from the obj/mtl file
public class ObjModel {
	
	// Geometry (null if not used)
	public float[] vertices = null;
	public float[] texCoords = null;
	public float[] normals = null;
	// Face entries
	public ArrayList<ObjFace> faces = null;
	// Materials
	public HashMap<String, ObjMaterial> materials = null;
	// Bounding box
	public float[] minmax = null;
}
