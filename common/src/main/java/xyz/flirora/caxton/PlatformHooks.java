package xyz.flirora.caxton;

public interface PlatformHooks {
    boolean isModLoaded(String modId);

    boolean isDevelopmentEnvironment();
}
