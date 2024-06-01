// (C) 2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.compat.immediatelyfast;

public class DummyBatchingAccess implements BatchingApi {
  @Override
  public void beginHudBatching() {}

  @Override
  public void endHudBatching() {}

  @Override
  public boolean isBatching() {
    return false;
  }

  @Override
  public boolean isEnabled() {
    return false;
  }
}
