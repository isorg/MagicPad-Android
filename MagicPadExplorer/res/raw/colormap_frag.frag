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
varying vec3 ViewDirection;
varying vec3 Normal;

void main( void )
{
   vec3  fvLightDirection = -normalize( fvLightPosition );
   vec3  fvNormal         = normalize( Normal );
   float fNDotL           = dot( fvNormal, fvLightDirection ); 
   
   vec3  fvReflection     = normalize( ( ( 2.0 * fvNormal ) * fNDotL ) - fvLightDirection ); 
   vec3  fvViewDirection  = normalize( ViewDirection );
   float fRDotV           = max( 0.0, dot( fvReflection, fvViewDirection ) );
      
    vec4  fvBaseColor = vec4(1.0,1.0,1.0,1.0);  
   if ( hasTexture >= 0 )
   	fvBaseColor      = texture2D( baseMap, Texcoord.xy);
   	
     
   vec4  fvTotalAmbient   = fvAmbient * fvBaseColor;  
   vec4  fvTotalDiffuse   = fvDiffuse * fNDotL * fvBaseColor; 
   vec4  fvTotalSpecular  = fvSpecular * ( pow( fRDotV, fSpecularPower ) );
   //fvTotalSpecular  = vec4(1.0,1.0,1.0,1.0) * ( pow( fRDotV, fSpecularPower ) );
   //vec4  fvTotalSpecular = vec4(1.0,1.0,1.0,0.5);
   
  
   gl_FragColor = ( fvTotalAmbient + fvTotalDiffuse + fvTotalSpecular );
   gl_FragColor.a = 1.0;
       
}