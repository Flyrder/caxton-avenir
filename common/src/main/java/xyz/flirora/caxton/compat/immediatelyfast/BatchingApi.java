// (C) 2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.compat.immediatelyfast;

import xyz.flirora.caxton.PlatformHooks;

/**
 * An interface wrapping ImmediatelyFast’s API so that it can be used without a hard dependency on
 * that mod.
 */
public interface BatchingApi {
  void beginHudBatching();

  void endHudBatching();

  boolean isBatching();

  boolean isEnabled();

  static BatchingApi createImpl(PlatformHooks hooks) {
    //    if (hooks.isModAtLeastVersion("immediatelyfast", "1.2.0")) {
    //      CaxtonModClient.LOGGER.info("ImmediatelyFast ≥1.2.0 detected; enabling integration");
    //      return new ImmediatelyFastBatchingAccess();
    //    }
    //        CaxtonModClient.LOGGER.info(
    //                "Using dummy batching access because ImmediatelyFast is not present or is too
    // old");
    return new DummyBatchingAccess();
  }
}
