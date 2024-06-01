// (C) 2022-2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.logging.LogUtils;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.FontType;
import net.minecraft.client.font.TextHandler;
import net.minecraft.client.font.TextRenderer;
import org.slf4j.Logger;
import xyz.flirora.caxton.compat.immediatelyfast.BatchingApi;
import xyz.flirora.caxton.dll.LibraryLoading;

@Environment(EnvType.CLIENT)
public class CaxtonModClient {
  public static final String MOD_ID = "caxton";
  public static final List<String> BUILTIN_PACKS = ImmutableList.of("inter", "opensans");
  public static final Logger LOGGER = LogUtils.getLogger();
  public static final boolean APRIL_FOOLS = isAprilFools();
  private static final Set<String> CALLSITE_BLACKLIST =
      ImmutableSet.of(TextHandler.class.getName(), TextRenderer.class.getName());
  private static final ConcurrentMap<StackTraceElement, StackTraceElement>
      PREVIOUS_BROKEN_METHOD_CALLERS = new ConcurrentHashMap<>();
  private static PlatformHooks platformHooks;
  private static BatchingApi batchingApi;

  private static FontType getCaxtonFontType() {
    for (FontType variant : FontType.class.getEnumConstants()) {
      if (variant.name().equals("CAXTON")) {
        return variant;
      }
    }
    throw new EnumConstantNotPresentException(FontType.class, "Could not find CAXTON font type");
  }

  /** The {@link FontType} for Caxton fonts. */
  public static final FontType CAXTON_FONT_TYPE = getCaxtonFontType();

  /**
   * Whether to force the batching hack in {@link
   * xyz.flirora.caxton.mixin.BufferBuilderStorageMixin} to be disabled.
   *
   * <p>This is set when Iris is loaded.
   */
  public static boolean FORCE_DISABLE_BATCHING_HACK = false;

  /**
   * The current global config for the mod.
   *
   * <p>This is set during initialization.
   */
  public static CaxtonConfig CONFIG = null;

  private static boolean isAprilFools() {
    Calendar calendar = Calendar.getInstance(Locale.ENGLISH);
    return calendar.get(Calendar.MONTH) == Calendar.APRIL
        && calendar.get(Calendar.DAY_OF_MONTH) == 1;
  }

  // This is meant to be called from a mixin method injecting into a Minecraft method.
  public static void onBrokenMethod() {
    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    for (int i = 4; i < stackTrace.length; ++i) {
      String className = stackTrace[i].getClassName();
      if (CALLSITE_BLACKLIST.contains(className)) continue;
      if (PREVIOUS_BROKEN_METHOD_CALLERS.put(stackTrace[i], stackTrace[i]) != null) {
        return;
      }
      break;
    }

    LOGGER.warn(
        "Use of {}.{} detected.", stackTrace[3].getClassName(), stackTrace[3].getMethodName());
    for (int i = 3; i < stackTrace.length; ++i) {
      LOGGER.warn("    at " + stackTrace[i].toString());
    }
    LOGGER.warn("Do not use this method; its API is fundamentally broken.");
    if (CONFIG.fatalOnBrokenMethodCall) {
      throw new UnsupportedOperationException();
    }
  }

  public static PlatformHooks getPlatformHooks() {
    return CaxtonModClient.platformHooks;
  }

  public static BatchingApi getBatchingApi() {
    return CaxtonModClient.batchingApi;
  }

  public static void init(PlatformHooks platformHooks) {
    CaxtonModClient.platformHooks = platformHooks;
    CaxtonModClient.batchingApi = BatchingApi.createImpl(platformHooks);
    FORCE_DISABLE_BATCHING_HACK = platformHooks.isModLoaded("iris");

    LOGGER.info(APRIL_FOOLS ? "Ufhed Gaadn ghap Muzsar!" : "Initializing mod...");

    CONFIG = CaxtonConfig.readFromFile();

    LibraryLoading.loadNativeLibrary(LOGGER);
  }
}
