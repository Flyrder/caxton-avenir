package xyz.flirora.caxton.fabric.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.slf4j.Logger;
import xyz.flirora.caxton.command.CommonDumpAtlasCommand;
import xyz.flirora.caxton.font.CaxtonFont;
import xyz.flirora.caxton.render.CaxtonAtlas;
import xyz.flirora.caxton.render.CaxtonGlyphCache;
import xyz.flirora.caxton.render.CaxtonTextRenderer;
import xyz.flirora.caxton.render.HasCaxtonTextRenderer;

import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.Map;

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
