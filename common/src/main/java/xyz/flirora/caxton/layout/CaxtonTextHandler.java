// (C) 2022-2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.layout;

import static xyz.flirora.caxton.layout.Interleaving.*;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Bidi;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.FontManager;
import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.TextHandler;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;
import xyz.flirora.caxton.font.ConfiguredCaxtonFont;
import xyz.flirora.caxton.mixin.TextHandlerAccessor;

/**
 * Reimplements methods from {@link TextHandler}.
 *
 * <p>Most of the vanilla {@link TextHandler} methods do not work correctly for Caxton text. The
 * methods in this class handle both legacy and Caxton text correctly.
 *
 * <p>A few methods remain unsupported, as they cannot be made to work in the presence of complex
 * text layout. These are {@link TextHandler#trimToWidth(String, int, Style)}, {@link
 * TextHandler#trimToWidthBackwards(String, int, Style)}, and {@link TextHandler#limitString(String,
 * int, Style)}. Some others have a different interpretation in Caxton than in vanilla Minecraft.
 */
@Environment(EnvType.CLIENT)
public class CaxtonTextHandler {
  private static final FontStorage EMPTY_FONT_STORAGE =
      new FontStorage(null, FontManager.MISSING_STORAGE_ID);

  /**
   * A font storage accessor that always returns a missing font storage.
   *
   * <p>This is used to construct a reasonable default value for {@link TextHandler}s that are
   * constructed without an accompanying {@link net.minecraft.client.font.TextRenderer}. EMI does
   * this, for instance, to line-wrap strings using a width retriever that returns 1 for every
   * glyph.
   */
  public static final Function<Identifier, FontStorage> EMPTY_FONT_STORAGE_ACCESSOR =
      fontId -> EMPTY_FONT_STORAGE;

  private final LayoutCache cache = LayoutCache.getInstance();
  private final Function<Identifier, FontStorage> fontStorageAccessor;
  private final TextHandler vanillaHandler;

  /**
   * Constructs a new {@link CaxtonTextHandler}.
   *
   * @param fontStorageAccessor A function mapping font identifiers to {@link FontStorage} objects.
   *     This is usually acquired from a {@link net.minecraft.client.font.TextRenderer}.
   * @param vanillaHandler The parent vanilla {@link TextHandler}.
   */
  public CaxtonTextHandler(
      Function<Identifier, FontStorage> fontStorageAccessor, TextHandler vanillaHandler) {
    this.fontStorageAccessor = fontStorageAccessor;
    this.vanillaHandler = vanillaHandler;
    ((TextHandlerExt) this.vanillaHandler).setCaxtonTextHandler(this);
  }

  public CaxtonText layoutText(OrderedText text, boolean validateAdvance, boolean rtl) {
    return CaxtonText.from(text, this.fontStorageAccessor, validateAdvance, rtl, this.cache);
  }

  /**
   * @return The {@link LayoutCache} used by this handler.
   */
  public LayoutCache getCache() {
    return cache;
  }

  public float getWidth(int codePoint, Style style) {
    return ((TextHandlerAccessor) vanillaHandler).getWidthRetriever().getWidth(codePoint, style);
  }

  /**
   * Calculates the width of a {@link String}, assuming that formatting codes are applied.
   *
   * <p>This replaces {@link TextHandler#getWidth(String)}.
   */
  public float getWidth(@Nullable String text) {
    if (text == null) return 0.0f;

    CaxtonText runGroups =
        CaxtonText.fromFormatted(
            text,
            fontStorageAccessor,
            Style.EMPTY,
            false,
            Language.getInstance().isRightToLeft(),
            cache);
    return getWidth(runGroups);
  }

  /**
   * Calculates the width of a {@link StringVisitable}, assuming that formatting codes are applied.
   *
   * <p>This replaces {@link TextHandler#getWidth(StringVisitable)}.
   */
  public float getWidth(StringVisitable text) {
    CaxtonText runGroups =
        CaxtonText.fromFormatted(
            text,
            fontStorageAccessor,
            Style.EMPTY,
            false,
            Language.getInstance().isRightToLeft(),
            cache);
    return getWidth(runGroups);
  }

  /**
   * Calculates the width of an {@link OrderedText}.
   *
   * <p>This replaces {@link TextHandler#getWidth(OrderedText)}.
   */
  public float getWidth(OrderedText text) {
    CaxtonText runGroups =
        CaxtonText.from(
            text, fontStorageAccessor, false, Language.getInstance().isRightToLeft(), cache);
    return getWidth(runGroups);
  }

  /** Calculates the width of a {@link CaxtonText}. */
  public float getWidth(CaxtonText text) {
    float total = 0;
    for (RunGroup runGroup : text.runGroups()) {
      total += getWidth(runGroup);
    }
    return total;
  }

  private float getWidth(RunGroup runGroup) {
    float total = 0;
    if (runGroup.getFont() == null) {
      MutableFloat cumulWidth = new MutableFloat();
      runGroup.acceptRender(
          (index, style, codePoint) -> {
            cumulWidth.add(getWidth(codePoint, style));
            return true;
          });
      total += cumulWidth.floatValue();
    } else {
      float scale = runGroup.getFont().getScale();
      ShapingResult[] shapingResults = runGroup.getShapingResults();

      for (ShapingResult shapingResult : shapingResults) {
        total += shapingResult.totalWidth() * scale;
      }
    }
    return total;
  }

  /**
   * Computes the character index of the glyph at a given <var>x</var>-position.
   *
   * <p>This replaces {@link TextHandler#getTrimmedLength(String, int, Style)}.
   *
   * @param text The {@link String} to get the information from. Formatting codes are <em>not</em>
   *     applied.
   * @param maxWidth The <var>x</var>-position of the glyph requested.
   * @param style The style in which to interpret {@code text}.
   * @return The character index of the glyph at the <var>x</var>-position {@code maxWidth}.
   * @see CaxtonTextHandler#getCharIndexAtX(CaxtonText, float, int, boolean)
   */
  public int getCharIndexAtX(String text, int maxWidth, Style style) {
    CaxtonText runGroups =
        CaxtonText.fromForwards(
            text, fontStorageAccessor, style, false, Language.getInstance().isRightToLeft(), cache);
    return getCharIndexAtX(runGroups, maxWidth, -1);
  }

  /**
   * Computes the character index of the glyph at a given <var>x</var>-position.
   *
   * <p>This replaces {@link TextHandler#getLimitedStringLength(String, int, Style)}.
   *
   * @param text The {@link String} to get the information from. Formatting codes are applied.
   * @param maxWidth The <var>x</var>-position of the glyph requested.
   * @param style The style in which to interpret {@code text}.
   * @return The character index of the glyph at the <var>x</var>-position {@code maxWidth}.
   * @see CaxtonTextHandler#getCharIndexAtX(CaxtonText, float, int, boolean)
   */
  public int getCharIndexAtXFormatted(String text, int maxWidth, Style style) {
    CaxtonText runGroups =
        CaxtonText.fromFormatted(
            text, fontStorageAccessor, style, false, Language.getInstance().isRightToLeft(), cache);
    return getCharIndexAtX(runGroups, maxWidth, -1);
  }

  /**
   * Computes the character index of the glyph at a given <var>x</var>-position.
   *
   * @param text The {@link CaxtonText} to get the information from.
   * @param x The <var>x</var>-position of the glyph requested.
   * @param from The character index of the leftmost glyph that should be counted, or <code>-1
   *             </code> if all glyphs should be counted.
   * @return The character index of the glyph at the <var>x</var>-position {@code maxWidth} relative
   *     to the glyph with character index {@code from}, or to the leftmost glyph if {@code from ==
   *     -1}.
   * @see CaxtonTextHandler#getCharIndexAtX(CaxtonText, float, int, boolean)
   */
  public int getCharIndexAtX(CaxtonText text, float x, int from) {
    return getCharIndexAtX(text, x, from, false);
  }

  /**
   * Computes the character index of the glyph after a given <var>x</var>-position.
   *
   * @param text The {@link CaxtonText} to get the information from.
   * @param x The <var>x</var>-position of the glyph requested.
   * @param from The character index of the leftmost glyph that should be counted, or <code>-1
   *             </code> if all glyphs should be counted.
   * @return The character index of the glyph at the <var>x</var>-position {@code maxWidth} relative
   *     to the glyph with character index {@code from}, or to the leftmost glyph if {@code from ==
   *     -1}.
   * @see CaxtonTextHandler#getCharIndexAtX(CaxtonText, float, int, boolean)
   */
  public int getCharIndexAfterX(CaxtonText text, float x, int from) {
    return getCharIndexAtX(text, x, from, true);
  }

  /**
   * Computes the character index of the glyph at a given <var>x</var>-position.
   *
   * @param text The {@link CaxtonText} to get the information from.
   * @param x The <var>x</var>-position of the glyph requested.
   * @param from The character index of the leftmost glyph that should be counted, or <code>-1
   *              </code> if all glyphs should be counted.
   * @param after If true, then this gets the character index of the glyph to the right of the
   *     queried glyph.
   * @return The character index of the glyph at the <var>x</var>-position {@code maxWidth} relative
   *     to the glyph with character index {@code from}, or to the leftmost glyph if {@code from ==
   *     -1}.
   */
  // Gets the index of the first char that fails to fit in a width of x, starting from the char at
  // index `from`.
  public int getCharIndexAtX(CaxtonText text, float x, int from, boolean after) {
    Threshold threshold = new Threshold(from);
    for (RunGroup runGroup : text.runGroups()) {
      if (threshold.shouldSkip(runGroup)) {
        continue;
      }
      if (runGroup.getFont() == null) {
        MutableFloat cumulWidth = new MutableFloat(x);
        MutableInt theIndex = new MutableInt();
        boolean completed =
            runGroup.acceptRender(
                (index, style, codePoint) -> {
                  int index2 = index + runGroup.getCharOffset();
                  if (threshold.updateLegacy(index2)) {
                    return true;
                  }
                  float width = getWidth(codePoint, style);
                  if (cumulWidth.floatValue() < (after ? 0 : width)) {
                    theIndex.setValue(index2);
                    return false;
                  }
                  cumulWidth.subtract(width);
                  return true;
                });
        if (!completed) {
          return theIndex.intValue();
        }
        x = cumulWidth.floatValue();
      } else {
        float scale = runGroup.getFont().getScale();
        ShapingResult[] shapingResults = runGroup.getShapingResults();

        int runIndex = 0;
        for (ShapingResult shapingResult : shapingResults) {
          for (int i = 0; i < shapingResult.numGlyphs(); ++i) {
            if (threshold.updateCaxton(runGroup, runIndex, shapingResult, i)) {
              continue;
            }
            float width = scale * shapingResult.advanceX(i);
            if (x < (after ? 0 : width)) {
              int[] bidiRuns = runGroup.getBidiRuns();
              int start = bidiRuns[SHAPING_RUN_STRIDE * runIndex + SHAPING_RUN_OFF_START];
              return runGroup.getCharOffset() + start + shapingResult.clusterIndex(i);
            }
            x -= width;
          }
          ++runIndex;
        }
      }
    }
    return text.totalLength();
  }

  /**
   * Trims a string to be at most {@code width} wide.
   *
   * <p>This is intended to make a string fit in an allotted amount of space. To get a character
   * index associated with an <var>x</var>-offset, use {@link
   * CaxtonTextHandler#getCharIndexAtX(String, int, Style)} instead.
   *
   * <p>This replaces {@link TextHandler#trimToWidth(StringVisitable, int, Style)}.
   *
   * @param text The {@link StringVisitable} to get the information from. Formatting codes are
   *     applied.
   * @param width The maximum width of the trimmed string.
   * @param style The style in which to interpret {@code text}.
   * @return A string trimmed to be at most {@code width} units wide.
   */
  public StringVisitable trimToWidth(StringVisitable text, int width, Style style) {
    CaxtonText.Full caxtonText =
        CaxtonText.fromForwardsFull(text, fontStorageAccessor, style, false, false, cache);
    LineWrapper wrapper =
        new LineWrapper(
            caxtonText.text(),
            caxtonText.bidi(),
            ((TextHandlerAccessor) vanillaHandler).getWidthRetriever(),
            width,
            true);

    if (wrapper.isFinished()) {
      return text;
    }

    LineWrapper.Result line = wrapper.nextLine(fontStorageAccessor);
    return runsToStringVisitable(line.runs());
  }

  /**
   * Gets the style at a given <var>x</var>-offset.
   *
   * <p>This replaces {@link TextHandler#getStyleAt(StringVisitable, int)}.
   *
   * @param text The {@link StringVisitable} to get the information from. Formatting codes are
   *     applied.
   * @param x The <var>x</var>-offset to get the style from.
   * @return The style at the <var>x</var>-offset {@code x}.
   * @see CaxtonTextHandler#getStyleAt(CaxtonText, float, int)
   */
  public @Nullable Style getStyleAt(StringVisitable text, int x) {
    CaxtonText caxtonText =
        CaxtonText.fromFormatted(text, fontStorageAccessor, Style.EMPTY, false, false, cache);
    return getStyleAt(caxtonText, x, -1);
  }

  /**
   * Gets the style at a given <var>x</var>-offset.
   *
   * <p>This replaces {@link TextHandler#getStyleAt(OrderedText, int)}.
   *
   * @param text The {@link OrderedText} to get the information from.
   * @param x The <var>x</var>-offset to get the style from.
   * @return The style at the <var>x</var>-offset {@code x}.
   * @see CaxtonTextHandler#getStyleAt(CaxtonText, float, int)
   */
  public @Nullable Style getStyleAt(OrderedText text, int x) {
    CaxtonText caxtonText = CaxtonText.from(text, fontStorageAccessor, false, false, cache);
    return getStyleAt(caxtonText, x, -1);
  }

  /**
   * Gets the style at a given <var>x</var>-offset.
   *
   * @param text The {@link CaxtonText} to get the information from.
   * @param x The <var>x</var>-offset to get the style from.
   * @param from The character index of the leftmost glyph that should be counted, or <code>-1
   *             </code> if all glyphs should be counted.
   * @return The style at the <var>x</var>-offset {@code x} relative to the glyph with character
   *     index {@code from}, or to the leftmost glyph if {@code from == -1}.
   */
  public @Nullable Style getStyleAt(CaxtonText text, float x, int from) {
    MutableObject<Style> styleBox = new MutableObject<>();
    Threshold threshold = new Threshold(from);
    for (RunGroup runGroup : text.runGroups()) {
      if (threshold.shouldSkip(runGroup)) {
        continue;
      }
      if (runGroup.getFont() == null) {
        MutableFloat cumulWidth = new MutableFloat(x);
        boolean completed =
            runGroup.acceptRender(
                (index, style, codePoint) -> {
                  int index2 = index + runGroup.getCharOffset();
                  if (threshold.updateLegacy(index2)) {
                    return true;
                  }
                  float width = getWidth(codePoint, style);
                  if (cumulWidth.floatValue() < width) {
                    styleBox.setValue(style);
                    return false;
                  }
                  cumulWidth.subtract(width);
                  return true;
                });
        if (!completed) {
          return styleBox.getValue();
        }
        x = cumulWidth.floatValue();
      } else {
        float scale = runGroup.getFont().getScale();
        ShapingResult[] shapingResults = runGroup.getShapingResults();

        int runIndex = 0;
        for (ShapingResult shapingResult : shapingResults) {
          for (int i = 0; i < shapingResult.numGlyphs(); ++i) {
            if (threshold.updateCaxton(runGroup, runIndex, shapingResult, i)) {
              continue;
            }
            float width = scale * shapingResult.advanceX(i);
            if (x < width) {
              int[] bidiRuns = runGroup.getBidiRuns();
              int start = bidiRuns[SHAPING_RUN_STRIDE * runIndex + SHAPING_RUN_OFF_START];
              return runGroup.getStyleAt(start + shapingResult.clusterIndex(i));
            }
            x -= width;
          }
          ++runIndex;
        }
      }
    }
    return null;
  }

  /**
   * Given the index of a character in a piece of text, return its horizontal position.
   *
   * <p>If you are calculating the width of a substring of a string, then you should probably use
   * this method instead.
   *
   * @param text The {@link CaxtonText} to use.
   * @param textIndex The UTF-16 code unit index to get the position for.
   * @param direction If this is {@link DirectionSetting#AUTO}, then gets the position corresponding
   *     to the startward edge of the glyph, accounting for bidirectional text. Otherwise, always
   *     gets either the left (for {@link DirectionSetting#FORCE_LTR}) or the right (for {@link
   *     DirectionSetting#FORCE_RTL}) edge of the glyph.
   * @return An x-offset from the left edge of the text.
   */
  // TODO: can we avoid repeated traversal if we have something like this?:
  // getOffsetAtIndex(text, i2) - getOffsetAtIndex(text, i1)
  public float getOffsetAtIndex(CaxtonText text, int textIndex, DirectionSetting direction) {
    if (direction.treatAsRtl(text.rtl()) ? textIndex >= text.totalLength() : textIndex < 0) {
      return 0.0f;
    }
    float offset = 0.0f;
    for (RunGroup runGroup : text.runGroups()) {
      ConfiguredCaxtonFont font = runGroup.getFont();
      if (font == null) {
        MutableFloat mutableFloat = new MutableFloat(offset);
        boolean completed =
            runGroup.acceptRender(
                (index, style, codePoint, rtl) -> {
                  if (index + runGroup.getCharOffset() == textIndex) {
                    if (direction.treatAsRtl(rtl)) mutableFloat.add(getWidth(codePoint, style));
                    return false;
                  }
                  mutableFloat.add(getWidth(codePoint, style));
                  return true;
                });
        offset = mutableFloat.floatValue();
        if (!completed) return offset;
      } else {
        float scale = runGroup.getFont().getScale();
        ShapingResult[] shapingResults = runGroup.getShapingResults();
        int[] bidiRuns = runGroup.getBidiRuns();
        int advance = 0;

        int runIndex = 0;
        for (ShapingResult shapingResult : shapingResults) {
          int start = bidiRuns[SHAPING_RUN_STRIDE * runIndex + SHAPING_RUN_OFF_START];
          int level = bidiRuns[SHAPING_RUN_STRIDE * runIndex + SHAPING_RUN_OFF_LEVEL];
          for (int i = 0; i < shapingResult.numGlyphs(); ++i) {
            int r0 = runGroup.getCharOffset() + start + shapingResult.clusterIndex(i);
            int r1 = runGroup.getCharOffset() + start + shapingResult.clusterLimit(i);
            if (r0 <= textIndex && textIndex < r1) {
              float frac = ((float) (textIndex - r0)) / (r1 - r0);
              if (direction.treatAsRtl(level % 2 != 0)) { // RTL correction
                frac = 1 - frac;
              }
              return offset + scale * (advance + frac * shapingResult.advanceX(i));
            }
            advance += shapingResult.advanceX(i);
          }
          ++runIndex;
        }

        offset += advance * scale;
      }
    }

    return offset;
  }

  /**
   * Calculates the visual highlight ranges for a selection within a piece of text and passes them
   * into {@code callback}.
   *
   * @param text The {@link CaxtonText} to get the information from.
   * @param startIndex The startward index of the selection, in UTF-16 code units.
   * @param endIndex The endward index of the selection, in UTF-16 code units.
   * @param callback The {@link HighlightConsumer} to which the individual highlight ranges should
   *     be passed.
   * @throws IllegalArgumentException if {@code endIndex < startIndex}
   */
  public void getHighlightRanges(
      CaxtonText text, int startIndex, int endIndex, HighlightConsumer callback) {
    if (endIndex < startIndex)
      throw new IllegalArgumentException("startIndex must be less than or equal to endIndex");
    Highlighter highlighter = new Highlighter(startIndex, endIndex, callback);

    for (RunGroup runGroup : text.runGroups()) {
      ConfiguredCaxtonFont font = runGroup.getFont();
      if (font == null) {
        runGroup.acceptRender(
            (index, style, codePoint, rtl) -> {
              int index2 = index + runGroup.getCharOffset();
              highlighter.accept(index2, index2 + 1, rtl, getWidth(codePoint, style));
              return true;
            });
      } else {
        float scale = runGroup.getFont().getScale();
        ShapingResult[] shapingResults = runGroup.getShapingResults();
        int[] bidiRuns = runGroup.getBidiRuns();

        int runIndex = 0;
        for (ShapingResult shapingResult : shapingResults) {
          int start = bidiRuns[SHAPING_RUN_STRIDE * runIndex + SHAPING_RUN_OFF_START];
          boolean rtl = bidiRuns[SHAPING_RUN_STRIDE * runIndex + SHAPING_RUN_OFF_LEVEL] % 2 != 0;
          for (int i = 0; i < shapingResult.numGlyphs(); ++i) {
            int r0 = runGroup.getCharOffset() + start + shapingResult.clusterIndex(i);
            int r1 = runGroup.getCharOffset() + start + shapingResult.clusterLimit(i);
            highlighter.accept(r0, r1, rtl, scale * shapingResult.advanceX(i));
          }
          ++runIndex;
        }
      }
    }

    highlighter.finish();
  }

  /**
   * Line-wraps a piece of text, passing the results to {@code consumer}.
   *
   * <p>This replaces {@link TextHandler#wrapLines(String, int, Style, boolean,
   * TextHandler.LineWrappingConsumer)}.
   *
   * @param text The {@link String} to line-wrap. Formatting codes are applied.
   * @param maxWidth The maximum width of the lines.
   * @param style The style in which to interpret {@code text}.
   * @param retainTrailingWordSplit Whether to return trailing word splits.
   * @param consumer The {@link net.minecraft.client.font.TextHandler.LineWrappingConsumer} for
   *     accepting the output lines.
   */
  public void wrapLines(
      String text,
      int maxWidth,
      Style style,
      boolean retainTrailingWordSplit,
      TextHandler.LineWrappingConsumer consumer) {
    wrapLines(text, maxWidth, style, retainTrailingWordSplit, new FcIndexConverter(), consumer);
  }

  /**
   * Line-wraps a piece of text, passing the results to {@code consumer}.
   *
   * @param text The {@link String} to line-wrap. Formatting codes are applied.
   * @param maxWidth The maximum width of the lines.
   * @param style The style in which to interpret {@code text}.
   * @param retainTrailingWordSplit Whether to return trailing word splits.
   * @param formattingCodeStarts An empty {@link FcIndexConverter} into which index conversion
   *     results will be written.
   * @param consumer The {@link net.minecraft.client.font.TextHandler.LineWrappingConsumer} for
   *     accepting the output lines.
   */
  public void wrapLines(
      String text,
      int maxWidth,
      Style style,
      boolean retainTrailingWordSplit,
      FcIndexConverter formattingCodeStarts,
      TextHandler.LineWrappingConsumer consumer) {
    // Apparently, vanilla uses TextVisitFactory.visitFormatted for this.
    CaxtonText.Full caxtonText =
        CaxtonText.fromFormattedFull(
            text, fontStorageAccessor, style, false, false, cache, formattingCodeStarts);
    wrapLines(
        caxtonText.text(),
        caxtonText.bidi(),
        maxWidth,
        consumer,
        formattingCodeStarts,
        retainTrailingWordSplit);
  }

  /**
   * Line-wraps a piece of text, passing the results to {@code consumer}.
   *
   * @param text The {@link String} to line-wrap. Formatting codes are applied.
   * @param maxWidth The maximum width of the lines.
   * @param style The style in which to interpret {@code text}.
   * @param retainTrailingWordSplit Whether to return trailing word splits.
   * @param formattingCodeStarts An empty {@link FcIndexConverter} into which index conversion
   *     results will be written.
   * @param consumer The {@link IndexedLineWrappingConsumer} for accepting the output lines.
   */
  public void wrapLines(
      String text,
      int maxWidth,
      Style style,
      boolean retainTrailingWordSplit,
      FcIndexConverter formattingCodeStarts,
      IndexedLineWrappingConsumer consumer) {
    // Apparently, vanilla uses TextVisitFactory.visitFormatted for this.
    CaxtonText.Full caxtonText =
        CaxtonText.fromFormattedFull(
            text, fontStorageAccessor, style, false, false, cache, formattingCodeStarts);
    wrapLines(
        caxtonText.text(),
        caxtonText.bidi(),
        maxWidth,
        consumer,
        formattingCodeStarts,
        retainTrailingWordSplit);
  }

  /**
   * Line-wraps a piece of text, passing the results to {@code consumer}.
   *
   * @param text The {@link CaxtonText} to line-wrap. Formatting codes are applied.
   * @param bidi The {@link Bidi} containing bidi data for {@code text}.
   * @param maxWidth The maximum width of the lines.
   * @param lineConsumer The {@link net.minecraft.client.font.TextHandler.LineWrappingConsumer} for
   *     accepting the output lines.
   * @param formattingCodeStarts An {@link FcIndexConverter} containing index conversion data for
   *     {@code text}.
   * @param retainTrailingWordSplit Whether to return trailing word splits.
   */
  public void wrapLines(
      CaxtonText text,
      Bidi bidi,
      int maxWidth,
      TextHandler.LineWrappingConsumer lineConsumer,
      FcIndexConverter formattingCodeStarts,
      boolean retainTrailingWordSplit) {
    wrapLines(
        text,
        bidi,
        maxWidth,
        IndexedLineWrappingConsumer.from(lineConsumer),
        formattingCodeStarts,
        retainTrailingWordSplit);
  }

  /**
   * Line-wraps a piece of text, passing the results to {@code consumer}.
   *
   * @param text The {@link CaxtonText} to line-wrap. Formatting codes are applied.
   * @param bidi The {@link Bidi} containing bidi data for {@code text}.
   * @param maxWidth The maximum width of the lines.
   * @param lineConsumer The {@link IndexedLineWrappingConsumer} for accepting the output lines.
   * @param formattingCodeStarts An {@link FcIndexConverter} containing index conversion data for
   *     {@code text}.
   * @param retainTrailingWordSplit Whether to return trailing word splits.
   */
  public void wrapLines(
      CaxtonText text,
      Bidi bidi,
      int maxWidth,
      IndexedLineWrappingConsumer lineConsumer,
      FcIndexConverter formattingCodeStarts,
      boolean retainTrailingWordSplit) {
    // lineConsumer: (visual line, is continuation)
    //        System.err.println(formattingCodeStarts);
    LineWrapper wrapper =
        new LineWrapper(
            text,
            bidi,
            ((TextHandlerAccessor) vanillaHandler).getWidthRetriever(),
            maxWidth,
            false);
    String contents = wrapper.getContents();
    if (wrapper.isFinished()) {
      // Ensure that at least one line is output
      lineConsumer.accept(
          Style.EMPTY, 0, text.totalLength() + 2 * formattingCodeStarts.valueOfMaxKey(), false);
    }
    while (!wrapper.isFinished()) {
      boolean rtl = wrapper.isCurrentlyRtl();
      int start = wrapper.getCurrentLineStart();
      wrapper.goToNextLine();
      int end = wrapper.getCurrentLineStart();
      if (!retainTrailingWordSplit) {
        if (end > start) {
          char trailing = contents.charAt(end - 1);
          if (wrapper.isFinished() ? trailing == '\n' : UCharacter.isWhitespace(trailing)) {
            --end;
          }
        }
      }
      RunGroup rg = wrapper.getRunGroupAt(start);
      //            System.err.println(start + " .. " + end);
      lineConsumer.accept(
          rg.getStyleAt(start - rg.getCharOffset()),
          formattingCodeStarts.formatlessToFormatful(start),
          formattingCodeStarts.formatlessToFormatful(end),
          rtl);
    }
  }

  /**
   * Line-wraps a piece of text, passing the results to {@code consumer}.
   *
   * <p>This replaces {@link TextHandler#wrapLines(StringVisitable, int, Style, BiConsumer)}.
   *
   * @param text The {@link StringVisitable} to line-wrap. Formatting codes are applied.
   * @param maxWidth The maximum width of the lines.
   * @param style The style in which to interpret {@code text}.
   * @param lineConsumer The {@code BiConsumer<StringVisitable, Boolean>} accepting the output
   *     lines: {@code (line, continuation)}.
   */
  public void wrapLines(
      StringVisitable text,
      int maxWidth,
      Style style,
      BiConsumer<StringVisitable, Boolean> lineConsumer) {
    // Apparently, vanilla uses TextVisitFactory.visitFormatted for this.
    CaxtonText.Full caxtonText =
        CaxtonText.fromFormattedFull(text, fontStorageAccessor, style, false, false, cache);
    wrapLines(caxtonText.text(), caxtonText.bidi(), maxWidth, lineConsumer);
  }

  /**
   * Line-wraps a piece of text, passing the results to {@code consumer}.
   *
   * @param text The {@link CaxtonText} to line-wrap.
   * @param bidi The {@link Bidi} containing bidi data for {@code text}.
   * @param maxWidth The maximum width of the lines.
   * @param lineConsumer The {@code BiConsumer<StringVisitable, Boolean>} accepting the output
   *     lines: {@code (line, continuation)}.
   */
  public void wrapLines(
      CaxtonText text, Bidi bidi, int maxWidth, BiConsumer<StringVisitable, Boolean> lineConsumer) {
    wrapLines(text, bidi, maxWidth, DirectionalLineWrappingConsumer.from(lineConsumer));
  }

  /**
   * Line-wraps a piece of text, passing the results to {@code consumer}.
   *
   * @param text The {@link CaxtonText} to line-wrap.
   * @param bidi The {@link Bidi} containing bidi data for {@code text}.
   * @param maxWidth The maximum width of the lines.
   * @param lineConsumer The {@link DirectionalLineWrappingConsumer} accepting the output lines.
   */
  public void wrapLines(
      CaxtonText text, Bidi bidi, int maxWidth, DirectionalLineWrappingConsumer lineConsumer) {
    // lineConsumer: (visual line, is continuation)
    LineWrapper wrapper =
        new LineWrapper(
            text,
            bidi,
            ((TextHandlerAccessor) vanillaHandler).getWidthRetriever(),
            maxWidth,
            false);
    while (!wrapper.isFinished()) {
      boolean continuation = wrapper.isContinuation();
      LineWrapper.Result line = wrapper.nextLine(fontStorageAccessor);
      lineConsumer.accept(runsToStringVisitable(line.runs()), continuation, line.rtl());
    }
  }

  private StringVisitable runsToStringVisitable(List<Run> runs) {
    return new StringVisitable() {
      @Override
      public <T> Optional<T> visit(Visitor<T> visitor) {
        for (Run run : runs) {
          var result = visitor.accept(run.text());
          if (result.isPresent()) return result;
        }
        return Optional.empty();
      }

      @Override
      public <T> Optional<T> visit(StyledVisitor<T> styledVisitor, Style style) {
        for (Run run : runs) {
          var result = styledVisitor.accept(run.style().withParent(style), run.text());
          if (result.isPresent()) return result;
        }
        return Optional.empty();
      }
    };
  }

  /** Clears the layout caches. */
  public void clearCaches() {
    cache.clear();
  }

  /**
   * A callback receiving information from {@link CaxtonTextHandler#getHighlightRanges(CaxtonText,
   * int, int, HighlightConsumer)}.
   */
  @FunctionalInterface
  public interface HighlightConsumer {
    /**
     * Accepts a visual highlight range.
     *
     * @param left The <var>x</var>-coordinate of the leftmost edge of the range.
     * @param right The <var>x</var>-coordinate of the rightmost edge of the range.
     */
    void accept(float left, float right);
  }

  /**
   * A callback accepting information about wrapped lines, along with their directionality.
   *
   * <p>This is an extension of {@link net.minecraft.client.font.TextHandler.LineWrappingConsumer}.
   */
  @FunctionalInterface
  public interface IndexedLineWrappingConsumer {
    static IndexedLineWrappingConsumer from(TextHandler.LineWrappingConsumer callback) {
      return (style, start, end, rtl) -> callback.accept(style, start, end);
    }

    /**
     * Accepts a line.
     *
     * @param style The style at the beginning of the line.
     * @param start The character index of the start of the line.
     * @param end The character index of the end of the line.
     * @param rtl Whether this line should be laid out right-to-left, as opposed to left-to-right.
     */
    void accept(Style style, int start, int end, boolean rtl);
  }

  /**
   * A callback accepting information about wrapped lines, along with their directionality.
   *
   * <p>This is an extension of {@code BiConsumer<{@link StringVisitable}, Boolean>}.
   */
  @FunctionalInterface
  public interface DirectionalLineWrappingConsumer {
    static DirectionalLineWrappingConsumer from(BiConsumer<StringVisitable, Boolean> callback) {
      return (line, continuation, rtl) -> callback.accept(line, continuation);
    }

    /**
     * Accepts a line.
     *
     * @param line The {@link StringVisitable} describing the line.
     * @param continuation Whether this line is a continuation line.
     * @param rtl Whether this line should be laid out right-to-left, as opposed to left-to-right.
     */
    void accept(StringVisitable line, boolean continuation, boolean rtl);
  }

  private static class Highlighter {
    private final int startIndex, endIndex;
    private final HighlightConsumer callback;
    private boolean wasRtl = false;
    private float offset = 0.0f;
    private float leftBound = Float.NaN;

    private Highlighter(int startIndex, int endIndex, HighlightConsumer callback) {
      this.startIndex = startIndex;
      this.endIndex = endIndex;
      this.callback = callback;
      //            System.out.printf("Highlighter(%d, %d)\n", startIndex, endIndex);
    }

    public void accept(int r0, int r1, boolean rtl, float glyphWidth) {
      //            System.out.printf("accept(%d, %d, %b, %f) with leftBound = %f, offset = %f\n",
      // r0, r1, rtl, glyphWidth, leftBound, offset);
      if (r1 <= r0) {
        return;
      }
      if (!Float.isNaN(leftBound)) {
        float rightBound = Float.NaN;
        if (rtl != wasRtl) {
          rightBound = offset;
        } else if (rtl && r1 >= startIndex && startIndex > r0) {
          float frac = ((float) startIndex - r1) / (r0 - r1);
          rightBound = offset + frac * glyphWidth;
        } else if (!rtl && r0 <= endIndex && endIndex < r1) {
          float frac = ((float) endIndex - r0) / (r1 - r0);
          rightBound = offset + frac * glyphWidth;
        } else if (r1 < startIndex || endIndex < r0) {
          rightBound = offset;
        }
        if (!Float.isNaN(rightBound)) {
          callback.accept(leftBound, rightBound);
          leftBound = Float.NaN;
        }
      }
      if (Float.isNaN(leftBound)) {
        if (rtl && r1 >= endIndex && endIndex > r0) {
          float frac = ((float) endIndex - r1) / (r0 - r1);
          leftBound = offset + frac * glyphWidth;
        } else if (!rtl && r0 <= startIndex && startIndex < r1) {
          float frac = ((float) startIndex - r0) / (r1 - r0);
          leftBound = offset + frac * glyphWidth;
        } else if (startIndex <= r0 && r1 <= endIndex) {
          leftBound = offset;
        }
      }
      offset += glyphWidth;
      wasRtl = rtl;
    }

    public void finish() {
      if (!Float.isNaN(leftBound)) {
        callback.accept(leftBound, offset);
        leftBound = Float.NaN;
      }
    }
  }
}
