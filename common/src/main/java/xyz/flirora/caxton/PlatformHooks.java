// (C) 2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton;

public interface PlatformHooks {
    boolean isModLoaded(String modId);

    boolean isModAtLeastVersion(String modId, String required);

    boolean isDevelopmentEnvironment();
}
