// (C) 2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.dll;

/** Used to signal errors from the caxton-impl crate. */
public class CaxtonNativeException extends RuntimeException {
  private final String originalMessage;

  public CaxtonNativeException(String s) {
    super(formatMessage(s));
    this.originalMessage = s;
  }

  public String getOriginalMessage() {
    return this.originalMessage;
  }

  private static String formatMessage(String s) {
    if (!s.contains("\n")) return s;
    return s.replace("\n", "\n | ") + "\n |";
  }
}
