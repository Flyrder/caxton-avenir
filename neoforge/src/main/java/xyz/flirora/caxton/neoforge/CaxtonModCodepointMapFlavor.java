// (C) 2023-2024 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.neoforge;

import net.minecraft.resource.*;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import xyz.flirora.caxton.CaxtonModClient;
import xyz.flirora.caxton.command.CaxtonCommands;

@Mod(CaxtonModClient.MOD_ID)
public class CaxtonModCodepointMapFlavor {
  public CaxtonModCodepointMapFlavor(IEventBus modBus) {
    modBus.addListener(this::initCaxtonMod);
    NeoForge.EVENT_BUS.addListener(CaxtonModCodepointMapFlavor::registerCommands);
    modBus.addListener(this::addPackFinders);
  }

  private void initCaxtonMod(AddClientReloadListenersEvent event) {
    CaxtonModClient.init(new PlatformHooksCodepointMapFlavor());
  }

  private void addPackFinders(AddPackFindersEvent event) {
    if (event.getPackType() == ResourceType.CLIENT_RESOURCES) {
      CaxtonModClient.LOGGER.info("Registering built-in resource packs");
      for (String id : CaxtonModClient.BUILTIN_PACKS) {
        event.addPackFinders(
            Identifier.of(CaxtonModClient.MOD_ID, "resourcepacks/" + id),
            ResourceType.CLIENT_RESOURCES,
            Text.translatable("caxton.resourcePack." + id),
            ResourcePackSource.BUILTIN,
            false,
            ResourcePackProfile.InsertionPosition.TOP);
      }
    }
  }

  private static void registerCommands(RegisterClientCommandsEvent event) {
    CaxtonCommands.register(
        new ClientCommandRegistrarCodepointMapFlavor(
            event.getDispatcher(), event.getBuildContext()));
  }
}
