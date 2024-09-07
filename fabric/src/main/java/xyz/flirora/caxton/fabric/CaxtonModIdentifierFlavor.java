// (C) 2023-2024 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import xyz.flirora.caxton.CaxtonModClient;
import xyz.flirora.caxton.command.CaxtonCommands;
import xyz.flirora.caxton.render.CaxtonShaders;

public class CaxtonModIdentifierFlavor implements ClientModInitializer {
  @Override
  public void onInitializeClient() {
    CaxtonModClient.init(new PlatformHooksIdentifierFlavor());

    ModContainer modContainer =
        FabricLoader.getInstance().getModContainer(CaxtonModClient.MOD_ID).get();
    ClientCommandRegistrationCallback.EVENT.register(
        (dispatcher, registryAccess) -> {
          CaxtonCommands.register(
              new ClientCommandRegistrarIdentifierFlavor(dispatcher, registryAccess));
        });
    CaxtonModClient.LOGGER.info("Registering built-in resource packs");
    for (String id : CaxtonModClient.BUILTIN_PACKS) {
      ResourceManagerHelper.registerBuiltinResourcePack(
          Identifier.of(CaxtonModClient.MOD_ID, id),
          modContainer,
          Text.translatable("caxton.resourcePack." + id),
          ResourcePackActivationType.NORMAL);
    }
    CaxtonModClient.LOGGER.info("Registering core shaders");
    ShaderProgramKeys.getAll().add(CaxtonShaders.TEXT_KEY);
    ShaderProgramKeys.getAll().add(CaxtonShaders.TEXT_SEE_THROUGH_KEY);
    ShaderProgramKeys.getAll().add(CaxtonShaders.TEXT_OUTLINE_KEY);
  }
}
