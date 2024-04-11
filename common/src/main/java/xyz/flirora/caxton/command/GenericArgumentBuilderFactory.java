package xyz.flirora.caxton.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.command.CommandRegistryAccess;

/**
 * Functionally equivalent to {@code <S> Function<Command<S>, LiteralArgumentBuilder<S>>}.
 * <p>
 * Construct a {@link LiteralArgumentBuilder} that calls {@link ArgumentBuilder#executes(Command)} on the provided {@code command} at the end.
 */
@FunctionalInterface
public interface GenericArgumentBuilderFactory {
    <S> LiteralArgumentBuilder<S> create(Command<S> command, CommandRegistryAccess registryAccess);
}
