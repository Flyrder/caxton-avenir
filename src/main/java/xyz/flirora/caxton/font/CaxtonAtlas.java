package xyz.flirora.caxton.font;

public class CaxtonAtlas {
    public static int getX(long packed) {
        return (int) (packed & 0x1FFF);
    }

    public static int getY(long packed) {
        return (int) ((packed >> 13) & 0x1FFF);
    }

    public static int getW(long packed) {
        return (int) ((packed >> 26) & 0x1FFF);
    }

    public static int getH(long packed) {
        return (int) ((packed >> 39) & 0x1FFF);
    }

    public static int getPage(long packed) {
        return (int) (packed >>> 52);
    }
}
