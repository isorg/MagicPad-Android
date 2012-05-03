// rendermonkey default textured phong fragment shader
#ifdef GL_FRAGMENT_PRECISION_HIGH
   // Default precision
   precision highp float;
#else
   precision mediump float;
#endif

uniform vec3 fvLightPosition;
uniform vec4 fvAmbient;
uniform vec4 fvSpecular;
uniform vec4 fvDiffuse;
uniform float fSpecularPower;

uniform sampler2D baseMap;
uniform int hasTexture;

varying vec2 Texcoord;
//varying vec3 ViewDirection;

uniform float fTime;

void main( void )
{
   vec4  fvBaseColor = vec4(1.0,1.0,1.0,1.0);  
   if ( hasTexture >= 0 )
   	fvBaseColor      = texture2D( baseMap, Texcoord.xy);
   	
     
   vec4  fvTotalAmbient   = fvAmbient * fvBaseColor;  
   vec4  fvTotalDiffuse   = fvDiffuse * fvBaseColor; 

  
   gl_FragColor = ( fvTotalAmbient + fvTotalDiffuse ) / 2.0;
   gl_FragColor.a = fvTotalDiffuse.a;
       
}