#ifdef GL_FRAGMENT_PRECISION_HIGH
   // Default precision
   precision highp float;
#else
   precision mediump float;
#endif

uniform vec4 fvAmbient;
uniform vec4 fvSpecular;
uniform vec4 fvDiffuse;
uniform float fSpecularPower;

uniform sampler2D baseMap;
uniform sampler2D bumpMap;

varying vec2 Texcoord;
varying vec3 ViewDirection;
varying vec3 LightDirection;

void main( void )
{
   vec3  fvLightDirection = -normalize( LightDirection );
   vec3  fvNormal         = normalize( ( texture2D( bumpMap, Texcoord ).xyz * 2.0 ) - 1.0 );
   vec3  fvNormal2         = texture2D( bumpMap, Texcoord ).xyz;
   float fNDotL           = dot( fvNormal, fvLightDirection ); 
   
   vec3  fvReflection     = normalize( ( ( 2.0 * fvNormal ) * fNDotL ) - fvLightDirection ); 
   vec3  fvViewDirection  = normalize( ViewDirection );
   float fRDotV           = max( 0.0, dot( fvReflection, fvViewDirection ) );
   
   vec4  fvBaseColor      = 0.90 + 0.10 * texture2D( baseMap, Texcoord );
   //fvBaseColor      = vec4(1.0, 1.0, 1.0, 1.0);
      
   vec4  fvTotalAmbient   = fvAmbient * fvBaseColor; 
   //vec4  fvTotalDiffuse   = fvDiffuse * fNDotL * fvBaseColor;
   // Force diffuse color 
   vec4  fvTotalDiffuse   = vec4(0.85, 0.74, 0.25, 1.0) * fNDotL * fvBaseColor;
   vec4  fvTotalSpecular  = fvSpecular * ( pow( fRDotV, fSpecularPower ) );
     
   gl_FragColor = ( fvTotalAmbient + fvTotalDiffuse + fvTotalSpecular * 0.85 );
   // Force opacity
   gl_FragColor.a = 1.0;
   
       
}
