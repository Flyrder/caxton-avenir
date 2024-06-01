// (C) 2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.forge;

import java.util.function.Supplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import xyz.flirora.caxton.command.ClientCommandSource;

public record ClientCommandSourceResourceLocationEdition(ServerCommandSource underlying)
    implements ClientCommandSource {
  @Override
  public void sendFeedback(Supplier<Text> message) {
    underlying.sendFeedback(message, false);
  }

  @Override
  public void sendError(Text message) {
    underlying.sendError(message);
  }

  @Override
  public MinecraftClient getClient() {
    // Best we can do. :shrug:
    return MinecraftClient.getInstance();
  }
}
