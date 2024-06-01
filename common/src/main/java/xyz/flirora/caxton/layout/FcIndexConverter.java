// (C) 2022-2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.layout;

/**
 * Converts between <i>formatless</i> and <i>formatful</i> indices.
 *
 * <p>In some places in Minecraft, substrings consisting of a section sign (§) followed by another
 * character are interpreted specially as a <i>format code</i>, which tells the game to change the
 * style for subsequent characters. Caxton distinguishes between string indices that count these
 * format codes and those that do not.
 *
 * <p>For example, the string <code>ab§1cd</code> contains four non-format characters and one format
 * code. If we wanted to describe the position between the <code>c</code> and the <code>d</code>,
 * then the formatful index would be 5 (counting the characters <code>ab§1c</code>), while the
 * formatless index would be 3 (counting <code>ab</code> and <code>c</code>, but not the format code
 * <code>§1</code>).
 *
 * <p>Note that the formatless and formatful indices differ by each other by 2 times the number of
 * format codes that have been encountered before the respective position.
 *
 * <p>The information for converting between the two types of indices is represented as a {@link
 * ForwardTraversedMap} in which the keys are formatless indices at which the format codes are
 * encountered and the values are the number of format codes encountered so far.
 */
public class FcIndexConverter extends ForwardTraversedMap {
  public int formatlessToFormatful(int index) {
    return index + 2 * this.inf(index);
  }

  public int formatlessToFormatful(int index, boolean save) {
    return index + 2 * this.infSlow(index, save);
  }

  public int formatfulToFormatless(int index) {
    int val = this.infp(index, 2);
    if (getLastResultIndex() < size() - 1) {
      int nextKey = getLastResultKey(1);
      int nextValue = getLastResultValue(1);
      int increment = nextValue - val;
      int offset = (nextKey + 2 * nextValue) - index;
      if (offset < 2 * increment) {
        return nextKey;
      }
    }
    return index - 2 * val;
  }

  public int formatfulToFormatless(int index, boolean save) {
    int k = this.arginfpSlow(index, 2);
    int val = this.entries.getInt(2 * k + 1);
    if (k < size() - 1) {
      int nextKey = this.entries.getInt(2 * k + 2);
      int nextValue = this.entries.getInt(2 * k + 3);
      int increment = nextValue - val;
      int offset = (nextKey + 2 * nextValue) - index;
      if (offset < 2 * increment) {
        return nextKey;
      }
    }
    if (save) this.lastAccessedIndex = k;
    return index - 2 * val;
  }
}
