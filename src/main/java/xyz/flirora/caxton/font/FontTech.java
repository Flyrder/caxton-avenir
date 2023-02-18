package xyz.flirora.caxton.font;

import com.google.gson.JsonSyntaxException;

public enum FontTech {
    MSDF("msdf"),
    RASTER("raster"),
    ;
    private final String name;

    FontTech(String name) {
        this.name = name;
    }

    public static FontTech fromName(String name) {
        return switch (name) {
            case "msdf" -> MSDF;
            case "raster" -> RASTER;
            default -> throw new JsonSyntaxException("invalid font tech: " + name);
        };
    }

    public String getName() {
        return name;
    }
}
