// (C) 2022-2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.layout;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

/**
 * A map from integers to integers that can only be traversed forward.
 *
 * <p>Any retrieval of a value using {@link ForwardTraversedMap#inf(int)} or {@link
 * ForwardTraversedMap#infp(int, int)} must be with a key that is greater than or equal to the last
 * key used for retrieval. If this condition can be held, then <var>k</var> retrievals to a map with
 * <var>n</var> entries can be done in a total of <var>O</var>(<var>n</var> + <var>k</var>) time.
 *
 * <p>There also exist {@link ForwardTraversedMap#infSlow(int, boolean)} and {@link
 * ForwardTraversedMap#infpSlow(int, int, boolean)}, which do not have this requirement but take
 * <var>O</var>(log(<var>n</var>)) time per invocation.
 *
 * <p>This class is represented using an array of key–value pairs, which are assumed to be in
 * ascending order by key. Some methods, such as {@link ForwardTraversedMap#arginfSlow(int)} return
 * an index into this array. (Technically, it uses an array of integers with keys and values
 * interleaved with each other in order to reduce the number of heap allocations.)
 *
 * <p>This class uses an internal field for the index of the entry that was last accessed. This
 * field is used by {@link ForwardTraversedMap#inf(int)} and {@link ForwardTraversedMap#infp(int,
 * int)} to allow linear-time forward traversal. Consequently, these methods must be called with
 * keys in nondescending order. This field is also set by {@link ForwardTraversedMap#infSlow(int,
 * boolean)} and {@link ForwardTraversedMap#infpSlow(int, int, boolean)} when {@code save} is true.
 */
public class ForwardTraversedMap {
  /**
   * The entries of this map.
   *
   * <p>This is an array of key–value pairs, where the <code>k</code>th key is at index <code>2 * k
   * </code> and the <code>k</code>th value is at index <code>2 * k + 1</code>.
   */
  protected final IntList entries = new IntArrayList();

  /**
   * The index of the entry that was last accessed by {@link ForwardTraversedMap#inf(int)} and
   * {@link ForwardTraversedMap#infp(int, int)}, or by {@link ForwardTraversedMap#infSlow(int,
   * boolean)} and {@link ForwardTraversedMap#infpSlow(int, int, boolean)} when {@code save} is
   * true.
   */
  protected int lastAccessedIndex = 0;

  /** Constructs an empty {@link ForwardTraversedMap}. */
  public ForwardTraversedMap() {}

  /**
   * Resets the state of lookups for this map, so that it acts if {@link
   * ForwardTraversedMap#inf(int)} and {@link ForwardTraversedMap#infp(int, int)} had never been
   * called.
   */
  public void reset() {
    lastAccessedIndex = 0;
  }

  /**
   * Gets the value associated with the greatest key in this map that is less than {@code key}.
   *
   * @param key The key; must be greater than or equal to the value of {@code key} passed into the
   *     previous invocation of this method, and must be greater than or equal to the first key
   *     added to this map.
   * @return The value associated with the greatest key in this map that is strictly less than
   *     {@code key}.
   */
  public int inf(int key) {
    if (key < entries.getInt(2 * lastAccessedIndex)) {
      throw new IllegalArgumentException("detected inf with descending key");
    }
    while (lastAccessedIndex < size() - 1 && entries.getInt(2 * lastAccessedIndex + 2) < key) {
      ++lastAccessedIndex;
    }
    return entries.getInt(2 * lastAccessedIndex + 1);
  }

  /**
   * This method requires the entries in the map to be monotonic in terms of the sum of the key and
   * value as well. It acts similarly to {@link ForwardTraversedMap#inf(int)} but treats each key in
   * the map as if it had {@code factor} times the corresponding value added to it.
   *
   * @param key The key; must be greater than or equal to the value of {@code key} passed into the
   *     previous invocation of this method, and must be greater than or equal to the first key
   *     added to this map.
   * @return The value associated with the last entry in this map such that {@code k + factor * v}
   *     is strictly less than {@code key}.
   */
  public int infp(int key, int factor) {
    if (key
        < entries.getInt(2 * lastAccessedIndex)
            + factor * entries.getInt(2 * lastAccessedIndex + 1)) {
      throw new IllegalArgumentException("detected inf with descending key");
    }
    while (lastAccessedIndex < size() - 1
        && entries.getInt(2 * lastAccessedIndex + 2)
                + factor * entries.getInt(2 * lastAccessedIndex + 3)
            < key) {
      ++lastAccessedIndex;
    }
    return entries.getInt(2 * lastAccessedIndex + 1);
  }

  /**
   * @return The internal last-accessed index value.
   */
  public int getLastResultIndex() {
    return lastAccessedIndex;
  }

  /**
   * @return The key associated with the last-accessed index value.
   *     <p>This is not necessarily the value of {@code key} to the last call to {@link
   *     ForwardTraversedMap#inf(int)} or {@link ForwardTraversedMap#infp(int, int)}.
   */
  public int getLastResultKey() {
    return entries.getInt(2 * lastAccessedIndex);
  }

  /**
   * @param offset The offset to add to the last-accessed index.
   * @return The key associated with an offset from the last-accessed index.
   *     <p>This is not necessarily the value of {@code key} to the last call to {@link
   *     ForwardTraversedMap#inf(int)} or {@link ForwardTraversedMap#infp(int, int)}.
   */
  public int getLastResultKey(int offset) {
    return entries.getInt(2 * (lastAccessedIndex + offset));
  }

  /**
   * @param offset The offset to add to the last-accessed index.
   * @return The value associated with an offset from the last-accessed index.
   */
  public int getLastResultValue(int offset) {
    return entries.getInt(2 * (lastAccessedIndex + offset) + 1);
  }

  /**
   * @return The value of the last entry of this map (i.e. with the greatest key).
   */
  public int valueOfMaxKey() {
    return entries.getInt(entries.size() - 1);
  }

  protected int arginfSlow(int key) {
    int lower = 0, upper = size();
    while (upper > lower + 1) {
      int mid = (lower + upper) >>> 1;
      int midVal = entries.getInt(2 * mid);
      if (midVal >= key) {
        upper = mid;
      } else {
        lower = mid;
      }
    }
    return lower;
  }

  protected int arginfpSlow(int key, int factor) {
    int lower = 0, upper = size();
    while (upper > lower + 1) {
      int mid = (lower + upper) >>> 1;
      int midVal = entries.getInt(2 * mid) + factor * entries.getInt(2 * mid + 1);
      if (midVal >= key) {
        upper = mid;
      } else {
        lower = mid;
      }
    }
    return lower;
  }

  /**
   * Acts like {@link ForwardTraversedMap#inf(int)} without the assumption that {@code key} is
   * greater than the last value of {@code key} passed into this method.
   *
   * @param key The key; must be greater than or equal to the first key added to this map.
   * @param save If true, then sets the last value of {@code key} passed into {@link
   *     ForwardTraversedMap#inf(int)}.
   * @return The value associated with the greatest key in this map that is strictly less than
   *     {@code key}.
   */
  public int infSlow(int key, boolean save) {
    int index = arginfSlow(key);
    if (save) lastAccessedIndex = index;
    return entries.getInt(2 * index + 1);
  }

  /**
   * Acts like {@link ForwardTraversedMap#infp(int, int)} without the assumption that {@code key} is
   * greater than the last value of {@code key} passed into this method.
   *
   * @param key The key; must be greater than or equal to the first key added to this map.
   * @param save If true, then sets the last value of {@code key} passed into {@link
   *     ForwardTraversedMap#infp(int, int)}.
   * @return The value associated with the last entry in this map such that {@code k + factor * v}
   *     is strictly less than {@code key}.
   */
  public int infpSlow(int key, int factor, boolean save) {
    int index = arginfpSlow(key, factor);
    if (save) lastAccessedIndex = index;
    return entries.getInt(2 * index + 1);
  }

  /**
   * Adds a key–value pair to the map. This method must be called with key values in nondescending
   * order. If the key is the same as the key of the last pair added with this method, then this
   * invocation replaces the old value.
   *
   * @param key The key for this map. Must be greater than or equal to the value of {@code key}
   *     passed into the previous invocation of this method.
   * @param value The value to associate with this key.
   */
  public void put(int key, int value) {
    int size = size();
    if (size > 0 && entries.getInt(2 * size - 2) == key) {
      entries.set(2 * size - 1, value);
    } else {
      entries.add(key);
      entries.add(value);
    }
  }

  /**
   * @return The number of entries in this map.
   */
  public int size() {
    return entries.size() / 2;
  }

  public String toString() {
    StringBuilder builder = new StringBuilder("{");
    for (int i = 0; i < size(); ++i) {
      if (i != 0) builder.append(", ");
      if (i == lastAccessedIndex) builder.append('(');
      builder.append(entries.getInt(2 * i));
      builder.append("=>");
      builder.append(entries.getInt(2 * i + 1));
      if (i == lastAccessedIndex) builder.append(')');
    }
    builder.append('}');
    return builder.toString();
  }
}
