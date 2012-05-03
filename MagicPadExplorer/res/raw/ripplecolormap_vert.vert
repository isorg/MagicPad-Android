// rendermonkey default textured phong vertex shader
uniform mat4 matViewProjectionInverseTranspose;
uniform mat4 matViewProjection;
uniform vec3 fvLightPosition;
uniform vec3 fvEyePosition;

attribute vec4 rm_Vertex;
attribute vec4 rm_Normal;
attribute vec2 rm_TexCoord0;

varying vec2 Texcoord;
varying vec3 ViewDirection;
varying vec3 Normal;


uniform float fTime;
uniform float frequency;
uniform float amp;
 
vec3 computeNormal( vec3 pos, 
                    vec3 tangent, 
                    vec3 binormal,
                    float amp, 
                    float phase, 
                    float freq )
{
	mat3 J;
	
	float dist = sqrt(pos.x*pos.x + pos.z*pos.z);
	if (dist < 0.01)
		return vec3(0.0, 1.0, 0.0);
	float jac_coef = /*amp**/ cos(freq*dist - /*amp **/ phase) / (5.0*dist+0.0001);
	
	// A matrix is an array of column vectors so J[2] is 
	// the third column of J.
	
	J[0][0] = 1.0;
	J[0][1] = jac_coef * pos.x;
	J[0][2] = 0.0;
	
	J[1][0] = 0.0;
	J[1][1] = 1.0;
	J[1][2] = 0.0;

	J[2][0] = 0.0;
	J[2][1] = jac_coef * pos.z;
	J[2][2] = 1.0;
	
	vec3 u = J * tangent;
	vec3 v = J * binormal;
	
	vec3 n = cross(u, v);
	return normalize(n);
}

vec4 displaceVertexFunc( vec4 pos, float amp, float phase, float frequency ) 
{
	vec4 new_pos;
	
	new_pos.x = pos.x;
	new_pos.z = pos.z;
	new_pos.w = pos.w;
	
	float dist = sqrt(pos.x*pos.x + pos.z*pos.z);
	if (dist > 0.01)
	new_pos.y = pos.y + amp * sin( frequency * dist - /*amp **/ phase ) / (5.0*dist+0.0001);
	
	return new_pos;
}


void main( void )
{
   gl_Position = matViewProjection * rm_Vertex;
   Texcoord    = rm_TexCoord0.xy;
    
   /*vec4 fvObjectPosition = matViewProjection * rm_Vertex;
   
   ViewDirection  = fvEyePosition - fvObjectPosition.xyz;
   Normal         = (matViewProjectionInverseTranspose * rm_Normal).xyz;*/
   
  
   //******************************
   
   //float frequency = 0.2;
   //float amp = 100.0; 
   
   vec4 displacedPosition;
	vec3 displacedNormal; 
	
	// 1 - Compute the diplaced position.
	//	 
	if (amp > 0.0)
		displacedPosition = displaceVertexFunc(rm_Vertex, amp, fTime*2.0, frequency );
	else
		displacedPosition = rm_Vertex;  
   	  
	gl_Position = matViewProjection * displacedPosition;	   	
	//vVertex = gl_ModelViewMatrix * displacedPosition;
	 
	vec4 fvObjectPosition = matViewProjection * displacedPosition;
   
   ViewDirection  = fvEyePosition - fvObjectPosition.xyz;
   Normal         = (matViewProjectionInverseTranspose * rm_Normal).xyz;
   
   vec3 norm = normalize( rm_Normal.xyz );//rm_Normal.xyz;
	
	
	// 2 - Compute the displaced normal
	//
	
	// if the engine does not provide the tangent vector you 
	// can compute it with the following piece of of code:
	//
	vec3 tangent; 
	vec3 binormal; 
	
	vec3 c1 = cross(norm, vec3(0.0, 0.0, 1.0)); 
	vec3 c2 = cross(norm, vec3(0.0, 1.0, 0.0)); 
	
	if(length(c1)>length(c2))
	{
		tangent = c1;	
	}
	else
	{
		tangent = c2;	
	}
	
	tangent = normalize(tangent);
	
	binormal = cross(norm, tangent); 
	binormal = normalize(binormal);

	if ( amp>0.0)
		displacedNormal = computeNormal( displacedPosition.xyz, 
	                                 tangent.xyz, 
	                                 binormal, 
	                                 amp,
	                                 fTime*2.0, 
	                                 frequency );
	else 
		displacedNormal = rm_Normal.xyz;
   	
   	vec4 n = vec4(displacedNormal.xyz, 0.0);
   	//Normal = 1.0 + (normalize((matViewProjectionInverseTranspose * n).xyz) / 2.0);
   	Normal = normalize(((matViewProjectionInverseTranspose * n).xyz));
   	//Normal = displacedNormal;
   
}