// (C) 2023-2024 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.forge;

import java.nio.file.Path;
import java.util.Optional;
import net.minecraft.resource.*;
import net.minecraft.text.Text;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforgespi.locating.IModFile;
import xyz.flirora.caxton.CaxtonModClient;
import xyz.flirora.caxton.command.CaxtonCommands;

@Mod(CaxtonModClient.MOD_ID)
public class CaxtonModResourceLocationFlavor {
  public CaxtonModResourceLocationFlavor(IEventBus modBus) {
    modBus.addListener(this::initCaxtonMod);
    NeoForge.EVENT_BUS.addListener(CaxtonModResourceLocationFlavor::registerCommands);
    modBus.addListener(this::addPackFinders);
  }

  private void initCaxtonMod(AddClientReloadListenersEvent event) {
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
                new ResourcePackInfo(
                    CaxtonModClient.MOD_ID + ":" + id,
                    Text.translatable("caxton.resourcePack." + id),
                    ResourcePackSource.BUILTIN,
                    Optional.empty()),
                new ResourcePackProfile.PackFactory() {
                  @Override
                  public ResourcePack open(ResourcePackInfo info) {
                    return new DirectoryResourcePack(info, sourcePath);
                  }

                  @Override
                  public ResourcePack openWithOverlays(
                      ResourcePackInfo info, ResourcePackProfile.Metadata metadata) {
                    return new DirectoryResourcePack(info, sourcePath);
                  }
                },
                ResourceType.CLIENT_RESOURCES,
                new ResourcePackPosition(false, ResourcePackProfile.InsertionPosition.TOP, false));
        event.addRepositorySource(profileAdder -> profileAdder.accept(pack));
      }
    }
  }

  private static void registerCommands(RegisterClientCommandsEvent event) {
    CaxtonCommands.register(
        new ClientCommandRegistrarResourceLocationFlavor(
            event.getDispatcher(), event.getBuildContext()));
  }
}
