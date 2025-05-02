#version 150

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <caxton:sdf.glsl>
#moj_import <caxton:unit_range.glsl>

uniform sampler2D Sampler0;

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    float opacity = msdf(Sampler0, texCoord0, screenPxRange());
    vec4 color = vertexColor * ColorModulator;
    color.a *= opacity;
    if (color.a < 0.1) discard;
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
    //     fragColor = vec4(fragColor.rgb * fragColor.a, 1.0);
}
