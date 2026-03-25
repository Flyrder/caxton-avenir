// (C) 2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.command;

import net.minecraft.util.StringIdentifiable;

public class DebugShapeInfo {
  public String buffer;
  public String inferredLanguage, inferredScript, inferredDirection;

  public DebugShapeInfo() {}

  public DebugShapeInfo(
      String buffer, String inferredLanguage, String inferredScript, String inferredDirection) {
    this.buffer = buffer;
    this.inferredLanguage = inferredLanguage;
    this.inferredScript = inferredScript;
    this.inferredDirection = inferredDirection;
  }

  public enum Direction implements StringIdentifiable {
    INVALID("invalid"),
    LEFT_TO_RIGHT("ltr"),
    RIGHT_TO_LEFT("rtl"),
    TOP_TO_BOTTOM("ttb"),
    BOTTOM_TO_TOP("btt");

    /// pls don’t modify this uwu
    public static final Direction[] VALUES = Direction.values();

    private final String name;

    Direction(String name) {
      this.name = name;
    }

    @Override
    public String asString() {
      return this.name;
    }
  }
}
