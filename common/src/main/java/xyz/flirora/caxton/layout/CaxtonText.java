// (C) 2022-2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.layout;

import com.google.common.collect.ImmutableList;
import com.ibm.icu.text.Bidi;
import java.util.List;
import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.FontStorage;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

/**
 * Represents text laid out by Caxton.
 *
 * @param runGroups the list of run groups in this text
 * @param totalLength the total number of UTF-16 code units in this text
 * @param rtl true if this text was treated as right-to-left
 */
@Environment(EnvType.CLIENT)
public record CaxtonText(List<RunGroup> runGroups, int totalLength, boolean rtl, String contents) {
  /** The empty text. */
  public static final CaxtonText EMPTY = new CaxtonText(ImmutableList.of(), 0, false, "");

  private CaxtonText(List<RunGroup> runGroups, boolean rtl, String contents) {
    this(runGroups, runGroups.stream().mapToInt(RunGroup::getTotalLength).sum(), rtl, contents);
  }

  /**
   * Lays out an {@link OrderedText} into a {@link CaxtonText}.
   *
   * @param text The {@link OrderedText} to lay out.
   * @param fonts A function mapping font identifiers to {@link FontStorage}s.
   * @param validateAdvance Whether to validate the advances of glyphs.
   * @param rtl Whether the text should be assumed to be right-to-left as opposed to left-to-right.
   * @param cache The {@link LayoutCache} to use for caching the results.
   * @return A {@link CaxtonText} describing the layout of {@code text}.
   */
  @NotNull public static CaxtonText from(
      OrderedText text,
      Function<Identifier, FontStorage> fonts,
      boolean validateAdvance,
      boolean rtl,
      LayoutCache cache) {
    return fromFull(text, fonts, validateAdvance, rtl, cache).text;
  }

  /**
   * Similar to {@link CaxtonText#from(OrderedText, Function, boolean, boolean, LayoutCache)} but
   * also returns the bidi information.
   *
   * @param text The {@link OrderedText} to lay out.
   * @param fonts A function mapping font identifiers to {@link FontStorage}s.
   * @param validateAdvance Whether to validate the advances of glyphs.
   * @param rtl Whether the text should be assumed to be right-to-left as opposed to left-to-right.
   * @param cache The {@link LayoutCache} to use for caching the results.
   * @return A {@link CaxtonText.Full} describing the layout of {@code text}.
   */
  @NotNull public static Full fromFull(
      OrderedText text,
      Function<Identifier, FontStorage> fonts,
      boolean validateAdvance,
      boolean rtl,
      LayoutCache cache) {
    List<Run> runs = Run.splitIntoRuns(text, fonts, validateAdvance);
    return fromRuns(runs, rtl, cache);
  }

  /**
   * Lays out a {@link StringVisitable} into a {@link CaxtonText}, applying the formatting codes
   * within.
   *
   * @param text The {@link StringVisitable} to lay out.
   * @param fonts A function mapping font identifiers to {@link FontStorage}s.
   * @param style The style to use for this text.
   * @param validateAdvance Whether to validate the advances of glyphs.
   * @param rtl Whether the text should be assumed to be right-to-left as opposed to left-to-right.
   * @param cache The {@link LayoutCache} to use for caching the results.
   * @return A {@link CaxtonText} describing the layout of {@code text}.
   */
  @NotNull public static CaxtonText fromFormatted(
      StringVisitable text,
      Function<Identifier, FontStorage> fonts,
      Style style,
      boolean validateAdvance,
      boolean rtl,
      LayoutCache cache) {
    return fromFormattedFull(text, fonts, style, validateAdvance, rtl, cache).text;
  }

  /**
   * Similar to {@link CaxtonText#fromFormatted(StringVisitable, Function, Style, boolean, boolean,
   * LayoutCache)} but also returns the bidi information.
   *
   * @param text The {@link StringVisitable} to lay out.
   * @param fonts A function mapping font identifiers to {@link FontStorage}s.
   * @param style The style to use for this text.
   * @param validateAdvance Whether to validate the advances of glyphs.
   * @param rtl Whether the text should be assumed to be right-to-left as opposed to left-to-right.
   * @param cache The {@link LayoutCache} to use for caching the results.
   * @return A {@link CaxtonText.Full} describing the layout of {@code text}.
   */
  @NotNull public static Full fromFormattedFull(
      StringVisitable text,
      Function<Identifier, FontStorage> fonts,
      Style style,
      boolean validateAdvance,
      boolean rtl,
      LayoutCache cache) {
    List<Run> runs = Run.splitIntoRunsFormatted(text, fonts, style, validateAdvance);
    return fromRuns(runs, rtl, cache);
  }

  /**
   * Lays out a {@link StringVisitable} into a {@link CaxtonText} <em>without</em> applying the
   * formatting codes within.
   *
   * @param text The {@link StringVisitable} to lay out.
   * @param fonts A function mapping font identifiers to {@link FontStorage}s.
   * @param style The style to use for this text.
   * @param validateAdvance Whether to validate the advances of glyphs.
   * @param rtl Whether the text should be assumed to be right-to-left as opposed to left-to-right.
   * @param cache The {@link LayoutCache} to use for caching the results.
   * @return A {@link CaxtonText} describing the layout of {@code text}.
   */
  @NotNull public static CaxtonText fromForwards(
      StringVisitable text,
      Function<Identifier, FontStorage> fonts,
      Style style,
      boolean validateAdvance,
      boolean rtl,
      LayoutCache cache) {
    return fromForwardsFull(text, fonts, style, validateAdvance, rtl, cache).text;
  }

  /**
   * Similar to {@link CaxtonText#fromForwards(StringVisitable, Function, Style, boolean, boolean,
   * LayoutCache)} but also returns the bidi information.
   *
   * @param text The {@link StringVisitable} to lay out.
   * @param fonts A function mapping font identifiers to {@link FontStorage}s.
   * @param style The style to use for this text.
   * @param validateAdvance Whether to validate the advances of glyphs.
   * @param rtl Whether the text should be assumed to be right-to-left as opposed to left-to-right.
   * @param cache The {@link LayoutCache} to use for caching the results.
   * @return A {@link CaxtonText.Full} describing the layout of {@code text}.
   */
  @NotNull public static Full fromForwardsFull(
      StringVisitable text,
      Function<Identifier, FontStorage> fonts,
      Style style,
      boolean validateAdvance,
      boolean rtl,
      LayoutCache cache) {
    List<Run> runs = Run.splitIntoRunsForwards(text, fonts, style, validateAdvance);
    return fromRuns(runs, rtl, cache);
  }

  /**
   * Lays out a {@link String} into a {@link CaxtonText}, applying the formatting codes within.
   *
   * @param text The {@link String} to lay out.
   * @param fonts A function mapping font identifiers to {@link FontStorage}s.
   * @param style The style to use for this text.
   * @param validateAdvance Whether to validate the advances of glyphs.
   * @param rtl Whether the text should be assumed to be right-to-left as opposed to left-to-right.
   * @param cache The {@link LayoutCache} to use for caching the results.
   * @return A {@link CaxtonText} describing the layout of {@code text}.
   */
  @NotNull public static CaxtonText fromFormatted(
      String text,
      Function<Identifier, FontStorage> fonts,
      Style style,
      boolean validateAdvance,
      boolean rtl,
      LayoutCache cache) {
    return fromFormattedFull(text, fonts, style, validateAdvance, rtl, cache).text;
  }

  /**
   * Similar to {@link CaxtonText#fromFormatted(String, Function, Style, boolean, boolean,
   * LayoutCache)} but also returns the bidi information.
   *
   * @param text The {@link String} to lay out.
   * @param fonts A function mapping font identifiers to {@link FontStorage}s.
   * @param style The style to use for this text.
   * @param validateAdvance Whether to validate the advances of glyphs.
   * @param rtl Whether the text should be assumed to be right-to-left as opposed to left-to-right.
   * @param cache The {@link LayoutCache} to use for caching the results.
   * @return A {@link CaxtonText.Full} describing the layout of {@code text}.
   */
  @NotNull public static Full fromFormattedFull(
      String text,
      Function<Identifier, FontStorage> fonts,
      Style style,
      boolean validateAdvance,
      boolean rtl,
      LayoutCache cache) {
    List<Run> runs = Run.splitIntoRunsFormatted(text, fonts, style, validateAdvance);
    return fromRuns(runs, rtl, cache);
  }

  /**
   * Lays out a {@link String} into a {@link CaxtonText}, applying the formatting codes within.
   *
   * @param text The {@link String} to lay out.
   * @param fonts A function mapping font identifiers to {@link FontStorage}s.
   * @param style The style to use for this text.
   * @param baseStyle The style to use when a <code>§r</code> code is encountered.
   * @param validateAdvance Whether to validate the advances of glyphs.
   * @param rtl Whether the text should be assumed to be right-to-left as opposed to left-to-right.
   * @param cache The {@link LayoutCache} to use for caching the results.
   * @return A {@link CaxtonText} describing the layout of {@code text}.
   */
  @NotNull public static CaxtonText fromFormatted(
      String text,
      Function<Identifier, FontStorage> fonts,
      Style style,
      Style baseStyle,
      boolean validateAdvance,
      boolean rtl,
      LayoutCache cache) {
    return fromFormattedFull(text, fonts, style, baseStyle, validateAdvance, rtl, cache).text;
  }

  /**
   * Similar to {@link CaxtonText#fromFormatted(String, Function, Style, boolean, boolean,
   * LayoutCache)} but also returns the bidi information.
   *
   * @param text The {@link String} to lay out.
   * @param fonts A function mapping font identifiers to {@link FontStorage}s.
   * @param style The style to use for this text.
   * @param baseStyle The style to use when a <code>§r</code> code is encountered.
   * @param validateAdvance Whether to validate the advances of glyphs.
   * @param rtl Whether the text should be assumed to be right-to-left as opposed to left-to-right.
   * @param cache The {@link LayoutCache} to use for caching the results.
   * @return A {@link CaxtonText.Full} describing the layout of {@code text}.
   */
  @NotNull public static Full fromFormattedFull(
      String text,
      Function<Identifier, FontStorage> fonts,
      Style style,
      Style baseStyle,
      boolean validateAdvance,
      boolean rtl,
      LayoutCache cache) {
    List<Run> runs = Run.splitIntoRunsFormatted(text, fonts, style, baseStyle, validateAdvance);
    return fromRuns(runs, rtl, cache);
  }

  /**
   * Lays out a {@link String} into a {@link CaxtonText}, applying the formatting codes within.
   *
   * @param text The {@link String} to lay out.
   * @param fonts A function mapping font identifiers to {@link FontStorage}s.
   * @param style The style to use for this text.
   * @param validateAdvance Whether to validate the advances of glyphs.
   * @param rtl Whether the text should be assumed to be right-to-left as opposed to left-to-right.
   * @param cache The {@link LayoutCache} to use for caching the results.
   * @param formattingCodeStarts The {@link FcIndexConverter} to which data should be written.
   * @return A {@link CaxtonText} describing the layout of {@code text}.
   */
  @NotNull public static CaxtonText fromFormatted(
      String text,
      Function<Identifier, FontStorage> fonts,
      Style style,
      boolean validateAdvance,
      boolean rtl,
      LayoutCache cache,
      FcIndexConverter formattingCodeStarts) {
    return fromFormattedFull(text, fonts, style, validateAdvance, rtl, cache, formattingCodeStarts)
        .text;
  }

  /**
   * Similar to {@link CaxtonText#fromFormatted(String, Function, Style, boolean, boolean,
   * LayoutCache, FcIndexConverter)} but also returns the bidi information.
   *
   * @param text The {@link String} to lay out.
   * @param fonts A function mapping font identifiers to {@link FontStorage}s.
   * @param style The style to use for this text.
   * @param validateAdvance Whether to validate the advances of glyphs.
   * @param rtl Whether the text should be assumed to be right-to-left as opposed to left-to-right.
   * @param cache The {@link LayoutCache} to use for caching the results.
   * @param formattingCodeStarts The {@link FcIndexConverter} to which data should be written.
   * @return A {@link CaxtonText.Full} describing the layout of {@code text}.
   */
  @NotNull public static Full fromFormattedFull(
      String text,
      Function<Identifier, FontStorage> fonts,
      Style style,
      boolean validateAdvance,
      boolean rtl,
      LayoutCache cache,
      FcIndexConverter formattingCodeStarts) {
    List<Run> runs =
        Run.splitIntoRunsFormatted(text, fonts, style, validateAdvance, formattingCodeStarts);
    return fromRuns(runs, rtl, cache);
  }

  /**
   * Lays out a {@link String} into a {@link CaxtonText} <em>without</em> applying the formatting
   * codes within.
   *
   * @param text The {@link String} to lay out.
   * @param fonts A function mapping font identifiers to {@link FontStorage}s.
   * @param style The style to use for this text.
   * @param validateAdvance Whether to validate the advances of glyphs.
   * @param rtl Whether the text should be assumed to be right-to-left as opposed to left-to-right.
   * @param cache The {@link LayoutCache} to use for caching the results.
   * @return A {@link CaxtonText} describing the layout of {@code text}.
   */
  @NotNull public static CaxtonText fromForwards(
      String text,
      Function<Identifier, FontStorage> fonts,
      Style style,
      boolean validateAdvance,
      boolean rtl,
      LayoutCache cache) {
    return fromForwardsFull(text, fonts, style, validateAdvance, rtl, cache).text;
  }

  /**
   * Similar to {@link CaxtonText#fromForwards(String, Function, Style, boolean, boolean,
   * LayoutCache)} but also returns the bidi information.
   *
   * @param text The {@link String} to lay out.
   * @param fonts A function mapping font identifiers to {@link FontStorage}s.
   * @param style The style to use for this text.
   * @param validateAdvance Whether to validate the advances of glyphs.
   * @param rtl Whether the text should be assumed to be right-to-left as opposed to left-to-right.
   * @param cache The {@link LayoutCache} to use for caching the results.
   * @return A {@link CaxtonText.Full} describing the layout of {@code text}.
   */
  @NotNull public static Full fromForwardsFull(
      String text,
      Function<Identifier, FontStorage> fonts,
      Style style,
      boolean validateAdvance,
      boolean rtl,
      LayoutCache cache) {
    List<Run> runs = Run.splitIntoRunsForwards(text, fonts, style, validateAdvance);
    return fromRuns(runs, rtl, cache);
  }

  @NotNull public static Full fromRuns(List<Run> runs, boolean rtl, LayoutCache cache) {
    return cache
        .getReorderCache()
        .get(
            new LayoutCache.FromRunsInput(runs, rtl),
            key -> computeFromRuns(key.runs(), key.rtl(), cache));
  }

  @NotNull private static Full computeFromRuns(List<Run> runs, boolean rtl, LayoutCache cache) {
    ShapingRunSegmenter segmenter = new ShapingRunSegmenter(runs, rtl, cache);
    return new Full(
        new CaxtonText(
            segmenter.getRunGroupsInVisualOrder(),
            segmenter.isRightToLeft(),
            segmenter.getContents()),
        segmenter.getBidi());
  }

  /**
   * Represents a {@link CaxtonText} along with associated bidi info.
   *
   * <p>The bidi info is used for line wrapping.
   */
  public record Full(CaxtonText text, Bidi bidi) {}
}
