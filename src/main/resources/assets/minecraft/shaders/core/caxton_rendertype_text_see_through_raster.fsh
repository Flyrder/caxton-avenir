#version 150

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float UnitRange;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0).rrrr * vertexColor * ColorModulator;
    if (color.r < 0.05) discard;
    fragColor = color * ColorModulator;
}
