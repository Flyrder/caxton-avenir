package xyz.flirora.caxton.forge;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import xyz.flirora.caxton.PlatformHooks;

public class PlatformHooksResourceLocationFlavor implements PlatformHooks {
    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.isProduction();
    }
}
