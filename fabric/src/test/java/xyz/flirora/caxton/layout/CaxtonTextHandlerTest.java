// (C) 2024 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.layout;

import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.client.font.TextHandler;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
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
}
