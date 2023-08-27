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
public class DumpAtlasCommand {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(literal("caxtondumpatlas").executes(context -> {
            MinecraftClient client = context.getSource().getClient();
            File debugDir = new File(client.runDirectory, "debug");
            File outputDir = new File(debugDir, "dumped-atlas-" + Util.getFormattedCurrentTime() + "-client");
            outputDir.mkdirs();

            TextRenderer vanillaRenderer = context.getSource().getClient().textRenderer;
            CaxtonTextRenderer caxtonRenderer = ((HasCaxtonTextRenderer) vanillaRenderer).getCaxtonTextRenderer();
            CaxtonGlyphCache cache = caxtonRenderer.getCache();

            List<CaxtonAtlas.Page> pages = cache.getAtlas().getAllPages();
            for (int i = 0; i < pages.size(); i++) {
                CaxtonAtlas.Page page = pages.get(i);
                int numMipmaps = page.getNumMipmapLevels();
                GlStateManager._bindTexture(page.getGlId());
                for (int mipmap = 0; mipmap < numMipmaps; ++mipmap) {
                    String name = mipmap > 0 ?
                            "page" + i + "_lv" + mipmap + ".png" :
                            "page" + i + ".png";
                    File imageOut = new File(outputDir, name);
                    try (NativeImage image = new NativeImage(CaxtonAtlas.PAGE_SIZE >> mipmap, CaxtonAtlas.PAGE_SIZE >> mipmap, false)) {
                        image.loadFromTextureImage(mipmap, false);
                        image.writeTo(imageOut);
                    } catch (Exception exception) {
                        LOGGER.warn("Couldn’t save atlas page #" + i + ", level " + mipmap, exception);
                        context.getSource().sendError(Text.translatable("caxton.command.caxtondumpatlas.atlasFailed", i));
                    }
                }
            }

            Map<CaxtonFont, Int2LongMap> locations = cache.getLocations();
            File locationsOut = new File(outputDir, "locations.json");
            try (FileWriter writer = new FileWriter(locationsOut);
                 JsonWriter jsonWriter = GSON.newJsonWriter(writer)) {
                jsonWriter.beginObject();
                for (var fontEntry : locations.entrySet()) {
                    CaxtonFont font = fontEntry.getKey();
                    jsonWriter.name(font.getId().toString());
                    jsonWriter.beginObject();
                    for (var locationEntry : fontEntry.getValue().int2LongEntrySet()) {
                        int glyphId = locationEntry.getIntKey();
                        String glyphName = font.getGlyphName(glyphId);
                        jsonWriter.name(glyphName != null ? glyphName : "_glyph" + glyphId);
                        jsonWriter.beginObject();
                        long location = locationEntry.getLongValue();
                        jsonWriter.name("id").value(glyphId);
                        jsonWriter.name("x").value(CaxtonAtlas.getX(location));
                        jsonWriter.name("y").value(CaxtonAtlas.getY(location));
                        jsonWriter.name("width").value(CaxtonAtlas.getW(location));
                        jsonWriter.name("height").value(CaxtonAtlas.getH(location));
                        jsonWriter.name("page").value(CaxtonAtlas.getPage(location));
                        jsonWriter.endObject();
                    }
                    jsonWriter.endObject();
                }
                jsonWriter.endObject();
            } catch (Exception exception) {
                LOGGER.warn("Couldn’t save location data", exception);
                context.getSource().sendError(Text.translatable("caxton.command.caxtondumpatlas.locationDataFailed"));
            }

            context.getSource().sendFeedback(Text.translatable("caxton.command.caxtondumpatlas.success", outputDir));

            return 1;
        }));
    }
}
