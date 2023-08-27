package xyz.flirora.caxton.fabric;

import net.fabricmc.loader.api.FabricLoader;
import xyz.flirora.caxton.PlatformHooks;

public class PlatformHooksIdentifierFlavor implements PlatformHooks {
    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }
}
