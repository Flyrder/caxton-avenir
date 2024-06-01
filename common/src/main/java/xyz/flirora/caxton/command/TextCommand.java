// (C) 2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import xyz.flirora.caxton.font.CaxtonFont;
import xyz.flirora.caxton.font.ConfiguredCaxtonFont;
import xyz.flirora.caxton.layout.*;

import static xyz.flirora.caxton.layout.Interleaving.*;

public class TextCommand {
    private static final Text RTL = Text.translatable("caxton.command.caxtontext.rtl");
    private static final Text LTR = Text.translatable("caxton.command.caxtontext.ltr");
    private static final Text IND1 = Text.literal("└ ").formatted(Formatting.YELLOW);
    private static final Text IND2 = Text.literal(" └ ").formatted(Formatting.YELLOW);
    private static final Text IND3 = Text.literal("  └ ").formatted(Formatting.YELLOW);

    public static int run(CommandContext<?> context, ClientCommandSource source) throws CommandSyntaxException {
        CaxtonTextHandler cth = ((TextHandlerExt) source.getClient().textRenderer.getTextHandler()).getCaxtonTextHandler();
        Text text = context.getArgument("text", Text.class);
        // TODO: make validateAdvance and rtl configurable
        CaxtonText caxtonText = cth.layoutText(text.asOrderedText(), false, false);
        dump(source, caxtonText);
        return 1;
    }

    private static void dump(ClientCommandSource source, CaxtonText text) {
        source.sendFeedback(() -> Text.translatable(
                "caxton.command.caxtontext.summary",
                escape(text.contents()),
                text.totalLength(),
                text.rtl() ? RTL : LTR));
        for (RunGroup runGroup : text.runGroups()) {
            dump(source, runGroup);
        }
    }

    private static void dump(ClientCommandSource source, RunGroup runGroup) {
        String joinedStr = runGroup.getJoinedStr();
        source.sendFeedback(() ->
                IND1.copy().append(
                        Text.translatable(
                                "caxton.command.caxtontext.runGroup.summary",
                                escape(joinedStr),
                                runGroup.getCharOffset(),
                                runGroup.getRunLevel()
                        ).formatted(Formatting.AQUA)
                ));
        // Dump style runs
        for (Run styleRun : runGroup.getStyleRuns()) {
            if (styleRun.font() == null) {
                source.sendFeedback(() ->
                        IND2.copy().append(
                                Text.translatable(
                                        "caxton.command.caxtontext.styleRun.summary.noFont",
                                        escape(styleRun.text()),
                                        styleRun.style().toString()
                                ).formatted(Formatting.GOLD)
                        ));
            } else {
                // TODO: more elaborate info about configured font
                source.sendFeedback(() ->
                        IND2.copy().append(
                                Text.translatable(
                                        "caxton.command.caxtontext.styleRun.summary.font",
                                        escape(styleRun.text()),
                                        styleRun.style(),
                                        styleRun.font().font().getId().toString()
                                ).formatted(Formatting.GOLD)
                        ));
            }
        }
        int[] bidiRuns = runGroup.getBidiRuns();
        int n = bidiRuns.length / SHAPING_RUN_STRIDE;
        ShapingResult[] shapingResults = runGroup.getShapingResults();
        for (int i = 0; i < n; ++i) {
            int start = bidiRuns[SHAPING_RUN_STRIDE * i + SHAPING_RUN_OFF_START];
            int end = bidiRuns[SHAPING_RUN_STRIDE * i + SHAPING_RUN_OFF_END];
            int level = bidiRuns[SHAPING_RUN_STRIDE * i + SHAPING_RUN_OFF_LEVEL];
            int scriptInt = bidiRuns[SHAPING_RUN_STRIDE * i + SHAPING_RUN_OFF_SCRIPT];
            String script = new String(new char[]{
                    (char) ((scriptInt >>> 24) & 0xFF),
                    (char) ((scriptInt >>> 16) & 0xFF),
                    (char) ((scriptInt >>> 8) & 0xFF),
                    (char) (scriptInt & 0xFF),
            });
            source.sendFeedback(() ->
                    IND2.copy().append(
                            Text.translatable(
                                    "caxton.command.caxtontext.bidiRun.summary",
                                    escape(joinedStr.substring(start, end)),
                                    start, end,
                                    level,
                                    script
                            ).formatted(Formatting.GREEN)
                    ));
            if (shapingResults != null && runGroup.getFont() != null) {
                dump(source, shapingResults[i], runGroup.getFont());
            }
        }
    }

    private static void dump(ClientCommandSource source, ShapingResult shapingResult, ConfiguredCaxtonFont configuredFont) {
        CaxtonFont font = configuredFont.font();
        StringBuilder builder = new StringBuilder();
        builder.append('[');
        shapingResult.serializeGlyphs(builder, font);
        builder.append(']');
        source.sendFeedback(() ->
                IND3.copy().append(Text.translatable(
                        "caxton.command.caxtontext.shapingResult.summary",
                        builder.toString(),
                        shapingResult.totalWidth()
                ).formatted(Formatting.LIGHT_PURPLE)));
    }

    private static String escape(String s) {
        return NbtString.escape(s);
    }
}
