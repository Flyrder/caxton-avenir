package xyz.flirora.caxton.fabric;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.datafixers.util.Either;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import xyz.flirora.caxton.command.ClientCommandRegistrar;
import xyz.flirora.caxton.command.GenericArgumentBuilderFactory;

import java.util.function.BiFunction;

public record ClientCommandRegistrarIdentifierFlavor(
        CommandDispatcher<FabricClientCommandSource> dispatcher) implements ClientCommandRegistrar {
    @Override
    public void registerClientCommand(GenericArgumentBuilderFactory builder, BiFunction<CommandContext<?>, MinecraftClient, Either<Text, Text>> runner) {
        dispatcher.register(builder.create(context -> {
            net.minecraft.client.MinecraftClient client = context.getSource().getClient();

            return reportResult(runner.apply(context, client), context);
        }));
    }

    private static int reportResult(Either<Text, Text> result, CommandContext<FabricClientCommandSource> context) {
        return result.map(
                err -> {
                    context.getSource().sendError(err);
                    return 0;
                },
                msg -> {
                    context.getSource().sendFeedback(msg);
                    return 1;
                }
        );
    }
}
