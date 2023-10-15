package xyz.flirora.caxton.forge;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.datafixers.util.Either;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import xyz.flirora.caxton.PlatformHooks;
import xyz.flirora.caxton.command.CommonDumpAtlasCommand;
import xyz.flirora.caxton.command.GenericArgumentBuilderFactory;

import java.util.Objects;
import java.util.function.Function;

public class PlatformHooksResourceLocationFlavor implements PlatformHooks {
    private CommandDispatcher<ServerCommandSource> dispatcher;

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
