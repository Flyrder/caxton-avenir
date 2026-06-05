// (C) 2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.slf4j.Logger;
import xyz.flirora.caxton.font.CaxtonFont;
import xyz.flirora.caxton.render.CaxtonAtlas;
import xyz.flirora.caxton.render.CaxtonGlyphCache;
import xyz.flirora.caxton.render.CaxtonTextRenderer;
import xyz.flirora.caxton.render.HasCaxtonTextRenderer;

public class DumpAtlasCommand {
  private static final Logger LOGGER = LogUtils.getLogger();
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

  private static final DynamicCommandExceptionType ATLAS_FAILED =
      new DynamicCommandExceptionType(
          i -> Text.translatable("caxton.command.caxtondumpatlas.atlasFailed", i));
  private static final SimpleCommandExceptionType LOCATION_DATA_FAILED =
      new SimpleCommandExceptionType(
          Text.translatable("caxton.command.caxtondumpatlas.locationDataFailed"));

  public static int run(CommandContext<?> context, ClientCommandSource source)
      throws CommandSyntaxException {
    MinecraftClient client = source.getClient();
    File debugDir = new File(client.runDirectory, "debug");
    File outputDir =
        new File(debugDir, "dumped-atlas-" + Util.getFormattedCurrentTime() + "-client");
    outputDir.mkdirs();

    TextRenderer vanillaRenderer = client.textRenderer;
    CaxtonTextRenderer caxtonRenderer =
        ((HasCaxtonTextRenderer) vanillaRenderer).getCaxtonTextRenderer();
    CaxtonGlyphCache cache = caxtonRenderer.getCache();

    List<CaxtonAtlas.Page> pages = cache.getAtlas().getAllPages();
    Path outputPath = outputDir.toPath();
    for (int i = 0; i < pages.size(); i++) {
      CaxtonAtlas.Page page = pages.get(i);
      int numMipmaps = page.getNumMipmapLevels();
      String name = "page" + i;
      TextureUtil.writeAsPNG(outputPath, name, page.getTexture(), numMipmaps, color -> color);
    }

    Map<CaxtonFont, CaxtonGlyphCache.Font> locations = cache.getLocations();
    File locationsOut = new File(outputDir, "locations.json");
    try (FileWriter writer = new FileWriter(locationsOut);
        JsonWriter jsonWriter = GSON.newJsonWriter(writer)) {
      jsonWriter.beginObject();
      for (var fontEntry : locations.entrySet()) {
        CaxtonFont font = fontEntry.getKey();
        jsonWriter.name(font.getId().toString());
        jsonWriter.beginObject();
        for (var locationEntry : fontEntry.getValue().getEntry().int2LongEntrySet()) {
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
      throw LOCATION_DATA_FAILED.create();
    }

    source.sendFeedback(
        () -> Text.translatable("caxton.command.caxtondumpatlas.success", outputDir));
    return 1;
  }
}
