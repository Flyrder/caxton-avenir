#version 150

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <caxton:sdf.glsl>
#moj_import <caxton:unit_range.glsl>

uniform sampler2D Sampler0;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    float opacity = msdf(Sampler0, texCoord0, screenPxRange(texCoord0));
    vec4 color = vertexColor;
    color.a *= opacity;
    if (color.a < 0.01) discard;
    fragColor = color * ColorModulator;
}
