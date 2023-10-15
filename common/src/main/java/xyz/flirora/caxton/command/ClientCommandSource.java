package xyz.flirora.caxton.command;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.function.Supplier;

/**
 * A custom command source for client commands that abstracts between platform differences.
 */
public interface ClientCommandSource {
    void sendFeedback(Supplier<Text> message);

    void sendError(Text message);

    MinecraftClient getClient();
}
