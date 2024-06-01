// (C) 2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.layout.gui;

import com.ibm.icu.text.BreakIterator;
import java.lang.ref.WeakReference;
import java.util.Objects;

/** Replacement for methods in {@link net.minecraft.util.Util}. */
public class CaxtonUtil {
  private static final ThreadLocal<BreakIterator> characterBreakIterator =
      ThreadLocal.withInitial(() -> BreakIterator.getCharacterInstance());
  private static final ThreadLocal<WeakReference<String>> lastQueriedString =
      ThreadLocal.withInitial(() -> null);

  public static int moveCursor(String string, int cursor, int delta) {
    // TODO: try to handle bidi text (assuming delta measures changes in visual direction)
    Objects.requireNonNull(string);
    BreakIterator bi = characterBreakIterator.get();
    WeakReference<String> lqs = lastQueriedString.get();
    // comparing lqs.get() by reference is intentional
    if (lqs == null || lqs.get() != string) {
      bi.setText(string);
      lastQueriedString.set(new WeakReference<>(string));
    }
    if (delta > 0) {
      bi.following(cursor);
      bi.next(delta - 1);
    } else if (delta < 0) {
      bi.preceding(cursor);
      bi.next(delta + 1);
    }
    return bi.current();
  }
}
