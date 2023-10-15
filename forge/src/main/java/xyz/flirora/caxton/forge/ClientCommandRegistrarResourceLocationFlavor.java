package xyz.flirora.caxton.forge;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.datafixers.util.Either;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import xyz.flirora.caxton.command.ClientCommandRegistrar;
import xyz.flirora.caxton.command.CommonDumpAtlasCommand;
import xyz.flirora.caxton.command.GenericArgumentBuilderFactory;

import java.util.Objects;
import java.util.function.Function;

public record ClientCommandRegistrarResourceLocationFlavor(
        CommandDispatcher<ServerCommandSource> dispatcher) implements ClientCommandRegistrar {
    @Override
    public void registerClientCommand(GenericArgumentBuilderFactory builder, Function<MinecraftClient, Either<Text, Text>> runner) {
        Objects.requireNonNull(this.dispatcher, "Please call setDispatcher first!");
        dispatcher.register(builder.<ServerCommandSource>create().executes(context -> {
            net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();

            return reportResult(CommonDumpAtlasCommand.run(client), context);
        }));
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
