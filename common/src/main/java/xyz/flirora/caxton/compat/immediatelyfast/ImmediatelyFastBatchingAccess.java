// (C) 2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.compat.immediatelyfast;

import net.raphimc.immediatelyfastapi.ApiAccess;
import net.raphimc.immediatelyfastapi.BatchingAccess;
import net.raphimc.immediatelyfastapi.ImmediatelyFastApi;

public class ImmediatelyFastBatchingAccess implements BatchingApi {
  private final BatchingAccess batching;

  public ImmediatelyFastBatchingAccess() {
    ApiAccess api = ImmediatelyFastApi.getApiImpl();
    this.batching = api.getBatching();
  }

  @Override
  public void beginHudBatching() {
    batching.beginHudBatching();
  }

  @Override
  public void endHudBatching() {
    batching.endHudBatching();
  }

  @Override
  public boolean isBatching() {
    return batching.isHudBatching();
  }

  @Override
  public boolean isEnabled() {
    return true;
  }
}
