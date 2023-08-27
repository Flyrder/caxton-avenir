package xyz.flirora.caxton;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import xyz.flirora.caxton.command.DumpAtlasCommand;

public class CaxtonModIdentifierFlavor implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CaxtonModClient.init();

        ModContainer modContainer = FabricLoader.getInstance().getModContainer(CaxtonModClient.MOD_ID).get();
        CaxtonModClient.LOGGER.info("Registering /caxtondumpatlas client command");
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            DumpAtlasCommand.register(dispatcher, registryAccess);
        });
        CaxtonModClient.LOGGER.info("Registering built-in resource packs");
        ResourceManagerHelper.registerBuiltinResourcePack(new Identifier(CaxtonModClient.MOD_ID, "inter"), modContainer, Text.translatable("caxton.resourcePack.inter"), ResourcePackActivationType.NORMAL);
        ResourceManagerHelper.registerBuiltinResourcePack(new Identifier(CaxtonModClient.MOD_ID, "opensans"), modContainer, Text.translatable("caxton.resourcePack.opensans"), ResourcePackActivationType.NORMAL);
    }
}
