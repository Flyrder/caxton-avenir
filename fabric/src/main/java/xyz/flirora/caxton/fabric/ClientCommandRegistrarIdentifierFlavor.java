package xyz.flirora.caxton.fabric;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.datafixers.util.Either;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;
import xyz.flirora.caxton.command.ClientCommand;
import xyz.flirora.caxton.command.ClientCommandRegistrar;
import xyz.flirora.caxton.command.GenericArgumentBuilderFactory;

import java.util.function.BiFunction;

public record ClientCommandRegistrarIdentifierFlavor(
        CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess registryAccess) implements ClientCommandRegistrar {
    @Override
    public void registerClientCommand(GenericArgumentBuilderFactory builder, ClientCommand command) {
        dispatcher.register(builder.create(context -> command.run(context, new ClientCommandSourceIdentifierEdition(context.getSource())), this.registryAccess));
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
