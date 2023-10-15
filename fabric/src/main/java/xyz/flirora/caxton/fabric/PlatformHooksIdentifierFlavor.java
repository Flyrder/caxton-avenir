package xyz.flirora.caxton.fabric;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import xyz.flirora.caxton.PlatformHooks;

public class PlatformHooksIdentifierFlavor implements PlatformHooks {
    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isModAtLeastVersion(String modId, String required) {
        var container = FabricLoader.getInstance().getModContainer(modId);
        if (container.isPresent()) {
            Version currentVersion = container.get().getMetadata().getVersion();
            try {
                Version requiredVersion = Version.parse(required);
                return currentVersion.compareTo(requiredVersion) >= 0;
            } catch (VersionParsingException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }
}
