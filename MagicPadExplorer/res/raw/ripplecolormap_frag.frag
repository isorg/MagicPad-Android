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
uniform float fTime;
//uniform float frequency;
//uniform float amp;
uniform sampler2D baseMap;
uniform samplerCube cubeMap;

varying vec2 Texcoord;
varying vec3 ViewDirection;
varying vec3 Normal;

void main( void )
{
   vec3  fvLightDirection = -normalize( fvLightPosition );
   vec3  fvNormal         = normalize( Normal.xyz  );
   float fNDotL           = dot( fvNormal, fvLightDirection ); 
   
   vec3  fvReflection     = normalize( ( ( 2.0 * fvNormal ) * fNDotL ) - fvLightDirection ); 
   vec3  fvViewDirection  = normalize( ViewDirection );
   float fRDotV           = max( 0.0, dot( fvReflection, fvViewDirection ) );
     
   vec4  fvBaseColor      = texture2D( baseMap, Texcoord.xy) * textureCube(cubeMap, (  fvViewDirection )   );
    
   vec4  fvTotalAmbient   = fvAmbient * fvBaseColor / 2.0; 
   vec4  fvTotalDiffuse   = fvDiffuse * fNDotL * fvBaseColor; 
   vec4  fvTotalSpecular  = fvSpecular * ( pow( fRDotV, fSpecularPower ) );
   //fvTotalSpecular  = vec4(1.0,1.0,1.0,1.0) * ( pow( fRDotV, fSpecularPower ) );
   //vec4  fvTotalSpecular = vec4(1.0,1.0,1.0,0.5);
   
  
   gl_FragColor = ( fvTotalAmbient + fvTotalDiffuse + fvTotalSpecular );
   //gl_FragColor.rgb = fvNormal.xyz;
   //gl_FragColor.r = 1.0;
   gl_FragColor.a = 0.65;
   
   
   /*
   vec2 tc = Texcoord.xy;
  vec2 p = -1.0 + 2.0 * tc;
  float len = length(p);
  vec2 uv = tc + (p/len)*cos(len*12.0-fTime*4.0)*0.03;
  vec3 col = texture2D(baseMap,uv).xyz;
  gl_FragColor = vec4(col,1.0);
       */
}