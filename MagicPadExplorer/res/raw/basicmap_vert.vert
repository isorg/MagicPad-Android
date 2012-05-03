// rendermonkey default textured phong vertex shader
uniform mat4 matViewProjectionInverseTranspose;
uniform mat4 matViewProjection;
uniform vec3 fvLightPosition;
uniform vec3 fvEyePosition;

uniform float posx;
uniform float posy;
uniform float scalex;
uniform float scaley;

attribute vec4 rm_Vertex;
//attribute vec4 rm_Normal;
attribute vec2 rm_TexCoord0;

varying vec2 Texcoord;
//varying vec3 ViewDirection;
 

uniform float fTime;

void main( void )
{
   gl_Position = /*matViewProjection **/ rm_Vertex;
   gl_Position.x = ( gl_Position.x  ) * scalex + ( posx *2.0 ) - 1.0;
   gl_Position.y = ( gl_Position.y )* scaley - ( posy *2.0 ) + 1.0 + 0.01 * sin(fTime);
   Texcoord    = rm_TexCoord0.xy;
    
   //vec4 fvObjectPosition = matViewProjection * rm_Vertex;
   
   //ViewDirection  = fvEyePosition - fvObjectPosition.xyz;
   
}