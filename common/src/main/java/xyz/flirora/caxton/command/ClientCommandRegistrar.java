package xyz.flirora.caxton.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.datafixers.util.Either;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.function.BiFunction;

/**
 * Hooks for platform-independent registration of client commands.
 */
public interface ClientCommandRegistrar {
    /**
     * Registers a new client command.
     *
     * @param builder A {@link GenericArgumentBuilderFactory} that creates a literal argument builder with the correct type parameters.
     * @param command A {@link ClientCommand} that executes the command.
     */
    void registerClientCommand(GenericArgumentBuilderFactory builder, ClientCommand command);
}
