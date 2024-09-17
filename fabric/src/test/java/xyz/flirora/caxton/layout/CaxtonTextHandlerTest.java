// (C) 2024 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.layout;

import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.client.font.TextHandler;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class CaxtonTextHandlerTest {
  @BeforeAll
  static void beforeAll() {
    SharedConstants.createGameVersion();
    Bootstrap.initialize();
  }

  @Test
  void testWrapLinesTakingDirectionalLineWrappingConsumer() {
    TextHandler textHandler =
        new TextHandler(
            ((codePoint, style) -> {
              if (style.isItalic()) {
                return 0.0f;
              }
              if (style.isBold()) {
                return 12.0f;
              }
              return 4.0f;
            }));
    Text text =
        Text.literal("[")
            .append(Text.literal("H").formatted(Formatting.BOLD))
            .append(Text.literal("exPattern(EAST)").formatted(Formatting.ITALIC))
            .append(Text.literal("]").formatted(Formatting.RESET));
    var lines = textHandler.wrapLines(text, 1, Style.EMPTY);
    // We just want to make sure this terminates
    System.err.println(lines);
  }

  private ArrayList<Pair<Integer, Integer>> getWrapIndices(
      TextHandler textHandler, String string, int width, boolean retainTraillingWordSplit) {
    ArrayList<Pair<Integer, Integer>> bounds = new ArrayList<>();
    textHandler.wrapLines(
        string,
        width,
        Style.EMPTY,
        retainTraillingWordSplit,
        (style, start, end) -> bounds.add(Pair.of(start, end)));
    return bounds;
  }

  @Test
  void testWrapLinesWithTrailingWhitespace() {
    TextHandler textHandler = new TextHandler((codePoint, style) -> 1.0f);
    Assertions.assertEquals(
        List.of(Pair.of(0, 3), Pair.of(4, 7)), getWrapIndices(textHandler, "aaa bbb", 5, false));
    Assertions.assertEquals(
        List.of(Pair.of(0, 4), Pair.of(5, 8)), getWrapIndices(textHandler, "aaa  bbb", 5, false));
    Assertions.assertEquals(List.of(Pair.of(0, 2)), getWrapIndices(textHandler, "a ", 5, false));
    Assertions.assertEquals(List.of(Pair.of(0, 3)), getWrapIndices(textHandler, "a  ", 5, false));
    Assertions.assertEquals(List.of(Pair.of(0, 1)), getWrapIndices(textHandler, "a\n", 5, false));

    Assertions.assertEquals(
        List.of(Pair.of(0, 4), Pair.of(4, 7)), getWrapIndices(textHandler, "aaa bbb", 5, true));
    Assertions.assertEquals(
        List.of(Pair.of(0, 5), Pair.of(5, 8)), getWrapIndices(textHandler, "aaa  bbb", 5, true));
    Assertions.assertEquals(List.of(Pair.of(0, 2)), getWrapIndices(textHandler, "a ", 5, true));
    Assertions.assertEquals(List.of(Pair.of(0, 3)), getWrapIndices(textHandler, "a  ", 5, true));
    Assertions.assertEquals(List.of(Pair.of(0, 2)), getWrapIndices(textHandler, "a\n", 5, true));
  }
}
