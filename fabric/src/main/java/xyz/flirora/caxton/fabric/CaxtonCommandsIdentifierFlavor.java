package xyz.flirora.caxton.fabric;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.datafixers.util.Either;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;
import xyz.flirora.caxton.command.CommonDumpAtlasCommand;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

@Environment(EnvType.CLIENT)
public class CaxtonCommandsIdentifierFlavor {
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

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(literal("caxtondumpatlas").executes(context -> {
            MinecraftClient client = context.getSource().getClient();

            return reportResult(CommonDumpAtlasCommand.run(client), context);
        }));
    }
}
