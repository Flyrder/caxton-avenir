// (C) 2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.forge;

import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.resource.*;
import net.minecraft.text.Text;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.forgespi.locating.IModFile;
import xyz.flirora.caxton.CaxtonModClient;
import xyz.flirora.caxton.command.CaxtonCommands;
import xyz.flirora.caxton.render.CaxtonShaders;
import xyz.flirora.caxton.render.CaxtonVertexFormats;

@Mod(CaxtonModClient.MOD_ID)
public class CaxtonModResourceLocationFlavor {
  public CaxtonModResourceLocationFlavor() {
    MinecraftForge.EVENT_BUS.addListener(CaxtonModResourceLocationFlavor::registerCommands);
    var modBus = FMLJavaModLoadingContext.get().getModEventBus();
    modBus.addListener(this::addPackFinders);
    modBus.addListener(this::addShaders);
    CaxtonModClient.init(new PlatformHooksResourceLocationFlavor());
  }

  private void addPackFinders(AddPackFindersEvent event) {
    if (event.getPackType() == ResourceType.CLIENT_RESOURCES) {
      CaxtonModClient.LOGGER.info("Registering built-in resource packs");
      IModFile file = ModList.get().getModFileById(CaxtonModClient.MOD_ID).getFile();
      for (String id : CaxtonModClient.BUILTIN_PACKS) {
        Path sourcePath = file.findResource("resourcepacks/" + id);
        var pack =
            ResourcePackProfile.create(
                CaxtonModClient.MOD_ID + ":" + id,
                Text.translatable("caxton.resourcePack." + id),
                false,
                new ResourcePackProfile.PackFactory() {
                  @Override
                  public ResourcePack open(String name) {
                    return new DirectoryResourcePack(name, sourcePath, false);
                  }

                  @Override
                  public ResourcePack openWithOverlays(
                      String name, ResourcePackProfile.Metadata metadata) {
                    return new DirectoryResourcePack(name, sourcePath, false);
                  }
                },
                ResourceType.CLIENT_RESOURCES,
                ResourcePackProfile.InsertionPosition.TOP,
                ResourcePackSource.BUILTIN);
        event.addRepositorySource(profileAdder -> profileAdder.accept(pack));
      }
    }
  }

  private void addShaders(RegisterShadersEvent event) {
    try {
      CaxtonModClient.LOGGER.info("Registering core shaders");
      ResourceFactory factory = event.getResourceProvider();
      event.registerShader(
          new ShaderProgram(
              factory, CaxtonShaders.TEXT_ID, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT),
          shader -> {
            CaxtonShaders.caxtonTextShader = shader;
          });
      event.registerShader(
          new ShaderProgram(
              factory,
              CaxtonShaders.TEXT_SEE_THROUGH_ID,
              VertexFormats.POSITION_COLOR_TEXTURE_LIGHT),
          shader -> {
            CaxtonShaders.caxtonTextSeeThroughShader = shader;
          });
      event.registerShader(
          new ShaderProgram(
              factory,
              CaxtonShaders.TEXT_OUTLINE_ID,
              CaxtonVertexFormats.POSITION_COLOR_COLOR_TEXTURE_LIGHT),
          shader -> {
            CaxtonShaders.caxtonTextOutlineShader = shader;
          });
    } catch (IOException e) {
      throw new RuntimeException("could not add shaders", e);
    }
  }

  private static void registerCommands(RegisterClientCommandsEvent event) {
    CaxtonCommands.register(
        new ClientCommandRegistrarResourceLocationFlavor(event.getDispatcher()));
  }
}
