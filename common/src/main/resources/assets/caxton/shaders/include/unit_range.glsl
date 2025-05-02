layout(std140) uniform UnitRangeU {
    float UnitRange;
};

float screenPxRange() {
    // More expensive calculation; perhaps switch based on graphics settings?
    // vec2 screenTexSize = vec2(1.0) / length(vec2(length(dFdx(texCoord0)), length(dFdy(texCoord0))));
    vec2 screenTexSize = vec2(1.0) / fwidth(texCoord0);
    return max(0.5 * dot(vec2(UnitRange), screenTexSize), 1.0);
}