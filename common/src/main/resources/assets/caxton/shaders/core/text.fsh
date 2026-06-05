#version 330

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
#moj_import <minecraft:fog.glsl>
#endif

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <caxton:sdf.glsl>
#moj_import <caxton:unit_range.glsl>

uniform sampler2D Sampler0;

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
in float sphericalVertexDistance;
in float cylindricalVertexDistance;
#endif

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    #ifdef IS_OUTLINE
    float opacity = sdf0(Sampler0, texCoord0, screenPxRange(texCoord0));
    #else
    float opacity = msdf(Sampler0, texCoord0, screenPxRange(texCoord0));
    #endif
    vec4 texColor = vec4(1.0, 1.0, 1.0, opacity);

    #ifdef IS_SEE_THROUGH
    vec4 color = texColor * vertexColor;
    #else
    vec4 color = texColor * vertexColor * ColorModulator;
    #endif
    if (color.a < 0.1) {
        discard;
    }

    #ifdef IS_SEE_THROUGH
    fragColor = color * ColorModulator;
    #elif defined(IS_GUI)
    fragColor = color;
    #else
    fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
    #endif
}
