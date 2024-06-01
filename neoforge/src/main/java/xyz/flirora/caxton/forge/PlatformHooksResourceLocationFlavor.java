// (C) 2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.forge;

import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import xyz.flirora.caxton.PlatformHooks;

public class PlatformHooksResourceLocationFlavor implements PlatformHooks {
    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isModAtLeastVersion(String modId, String required) {
        var container = ModList.get().getModContainerById(modId);
        if (container.isPresent()) {
            ArtifactVersion currentVersion = container.get().getModInfo().getVersion();
            ArtifactVersion requiredVersion = new DefaultArtifactVersion(required);
            return currentVersion.compareTo(requiredVersion) >= 0;
        }
        return false;
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.isProduction();
    }
}
