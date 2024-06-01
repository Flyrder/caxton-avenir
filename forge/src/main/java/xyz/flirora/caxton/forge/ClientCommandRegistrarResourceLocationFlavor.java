// (C) 2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.forge;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.datafixers.util.Either;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import xyz.flirora.caxton.command.ClientCommand;
import xyz.flirora.caxton.command.ClientCommandRegistrar;
import xyz.flirora.caxton.command.GenericArgumentBuilderFactory;

import java.util.Objects;

public record ClientCommandRegistrarResourceLocationFlavor(
        CommandDispatcher<ServerCommandSource> dispatcher) implements ClientCommandRegistrar {
    @Override
    public void registerClientCommand(GenericArgumentBuilderFactory builder, ClientCommand command) {
        Objects.requireNonNull(this.dispatcher, "Please call setDispatcher first!");
        dispatcher.register(builder.create(context -> command.run(context, new ClientCommandSourceResourceLocationEdition(context.getSource()))));
    }

    private static int reportResult(Either<Text, Text> result, CommandContext<ServerCommandSource> context) {
        return result.map(
                err -> {
                    context.getSource().sendError(err);
                    return 0;
                },
                msg -> {
                    context.getSource().sendFeedback(() -> msg, false);
                    return 1;
                }
        );
    }
}
