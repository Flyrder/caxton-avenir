package xyz.flirora.caxton.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import xyz.flirora.caxton.CaxtonModClient;
import xyz.flirora.caxton.command.CaxtonCommands;
import xyz.flirora.caxton.render.CaxtonShaders;
import xyz.flirora.caxton.render.CaxtonVertexFormats;

public class CaxtonModIdentifierFlavor implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CaxtonModClient.init(new PlatformHooksIdentifierFlavor());

        ModContainer modContainer = FabricLoader.getInstance().getModContainer(CaxtonModClient.MOD_ID).get();
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            CaxtonCommands.register(new ClientCommandRegistrarIdentifierFlavor(dispatcher, registryAccess));
        });
        CaxtonModClient.LOGGER.info("Registering built-in resource packs");
        for (String id : CaxtonModClient.BUILTIN_PACKS) {
            ResourceManagerHelper.registerBuiltinResourcePack(new Identifier(CaxtonModClient.MOD_ID, id), modContainer, Text.translatable("caxton.resourcePack." + id), ResourcePackActivationType.NORMAL);
        }
        CaxtonModClient.LOGGER.info("Registering core shaders");
        CoreShaderRegistrationCallback.EVENT.register(context -> {
            context.register(
                    CaxtonShaders.TEXT_ID,
                    VertexFormats.POSITION_COLOR_TEXTURE_LIGHT,
                    shader -> {
                        CaxtonShaders.caxtonTextShader = shader;
                    });
            context.register(
                    CaxtonShaders.TEXT_SEE_THROUGH_ID,
                    VertexFormats.POSITION_COLOR_TEXTURE_LIGHT,
                    shader -> {
                        CaxtonShaders.caxtonTextSeeThroughShader = shader;
                    });
            context.register(
                    CaxtonShaders.TEXT_OUTLINE_ID,
                    CaxtonVertexFormats.POSITION_COLOR_COLOR_TEXTURE_LIGHT,
                    shader -> {
                        CaxtonShaders.caxtonTextOutlineShader = shader;
                    });
        });
    }
}
