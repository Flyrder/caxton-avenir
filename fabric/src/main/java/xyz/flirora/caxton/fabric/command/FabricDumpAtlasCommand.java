package xyz.flirora.caxton.fabric.command;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandRegistryAccess;
import xyz.flirora.caxton.command.CommonDumpAtlasCommand;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

@Environment(EnvType.CLIENT)
public class FabricDumpAtlasCommand {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(literal("caxtondumpatlas").executes(context -> {
            MinecraftClient client = context.getSource().getClient();

            return CommonDumpAtlasCommand.run(client).map(
                    err -> {
                        context.getSource().sendError(err);
                        return 0;
                    },
                    msg -> {
                        context.getSource().sendFeedback(msg);
                        return 1;
                    }
            );
        }));
    }
}
