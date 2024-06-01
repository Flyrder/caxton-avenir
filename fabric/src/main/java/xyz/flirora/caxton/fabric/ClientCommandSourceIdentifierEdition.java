// (C) 2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.fabric;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import xyz.flirora.caxton.command.ClientCommandSource;

import java.util.function.Supplier;

public record ClientCommandSourceIdentifierEdition(
        FabricClientCommandSource underlying) implements ClientCommandSource {
    @Override
    public void sendFeedback(Supplier<Text> message) {
        underlying.sendFeedback(message.get());
    }

    @Override
    public void sendError(Text message) {
        underlying.sendError(message);
    }

    @Override
    public MinecraftClient getClient() {
        return underlying.getClient();
    }
}
