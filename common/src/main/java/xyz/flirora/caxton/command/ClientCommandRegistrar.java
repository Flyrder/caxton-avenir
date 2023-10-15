package xyz.flirora.caxton.command;

import com.mojang.datafixers.util.Either;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.function.Function;

/**
 * Hooks for platform-independent registration of client commands.
 */
public interface ClientCommandRegistrar {
    /**
     * Registers a new client command.
     *
     * @param builder A {@link GenericArgumentBuilderFactory} that creates a literal argument builder with the correct type parameters.
     * @param runner  A callback that executes a command, returning either the result of {@link Either#right(Object)} for a success of or {@link Either#left(Object)} for an error.
     */
    void registerClientCommand(GenericArgumentBuilderFactory builder, Function<MinecraftClient, Either<Text, Text>> runner);
}
