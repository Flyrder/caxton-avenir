package xyz.flirora.caxton.forge;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.datafixers.util.Either;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import xyz.flirora.caxton.command.CommonDumpAtlasCommand;

public class CaxtonCommandsResourceLocationFlavor {
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

    public static void register(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(LiteralArgumentBuilder.<ServerCommandSource>literal("caxtondumpatlas").executes(context -> {
            MinecraftClient client = MinecraftClient.getInstance();

            return reportResult(CommonDumpAtlasCommand.run(client), context);
        }));
    }
}
