package xyz.flirora.caxton.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

/**
 * Functionally equivalent to {@code <S,> Supplier<LiteralArgumentBuilder<S>>}.
 */
@FunctionalInterface
public interface GenericArgumentBuilderFactory {
    <S> LiteralArgumentBuilder<S> create();
}
