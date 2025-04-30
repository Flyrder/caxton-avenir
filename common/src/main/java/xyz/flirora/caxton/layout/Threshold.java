// (C) 2022-2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.layout;

import static xyz.flirora.caxton.layout.Interleaving.*;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/** A class used to help skip over glyphs before a given character index. */
@Environment(EnvType.CLIENT)
public class Threshold {
  private int threshold;

  /**
   * Constructs a new {@link Threshold}.
   *
   * @param threshold The character index of the leftmost glyph to handle, or <code>-1</code> if all
   *     glyphs should be handled.
   */
  public Threshold(int threshold) {
    this.threshold = threshold;
  }

  /** Constructs a new {@link Threshold} that does not skip over glyphs. */
  public Threshold() {
    this(-1);
  }

  public int getValue() {
    return threshold;
  }

  /**
   * Checks whether a given run group can be skipped entirely because all of its glyphs would be
   * skipped.
   *
   * @return Whether {@code runGroup} can be skipped.
   */
  public boolean shouldSkip(RunGroup runGroup) {
    return threshold >= 0 && !runGroup.containsIndex(threshold);
  }

  /**
   * Updates the state of the threshold from a legacy glyph.
   *
   * @param index The character index of the glyph.
   * @return Whether this glyph should be skipped.
   */
  public boolean updateLegacy(int index) {
    if (threshold >= 0 && threshold != index) {
      return true;
    }
    threshold = -1;
    return false;
  }

  /**
   * Updates the state of the threshold from a Caxton glyph.
   *
   * @param runGroup The {@link RunGroup} in which the glyph belongs.
   * @param bri The index of the bidi run within {@code runGroup}.
   * @param shapingResult The {@link ShapingResult} resulting from shaping the {@code bri}th bidi
   *     run in {@code runGroup}.
   * @param sri The index of the glyph within {@code shapingResult}.
   */
  public boolean updateCaxton(RunGroup runGroup, int bri, ShapingResult shapingResult, int sri) {
    int[] bidiRuns = runGroup.getBidiRuns();
    int start = bidiRuns[SHAPING_RUN_STRIDE * bri + SHAPING_RUN_OFF_START];
    int r0 = runGroup.getCharOffset() + start + shapingResult.clusterIndex(sri);
    int r1 = runGroup.getCharOffset() + start + shapingResult.clusterLimit(sri);
    if (threshold >= 0 && !(r0 <= threshold && threshold < r1)) {
      return true;
    }
    threshold = -1;
    return false;
  }
}
