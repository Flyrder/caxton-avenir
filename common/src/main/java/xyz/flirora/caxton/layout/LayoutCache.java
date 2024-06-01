// (C) 2022-2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.layout;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.flirora.caxton.font.ConfiguredCaxtonFont;

import java.time.Duration;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * A cache for expensive layout computations.
 * <p>
 * Currently, this is a singleton class.
 */
@Environment(EnvType.CLIENT)
public class LayoutCache {
    private static final LayoutCache INSTANCE = new LayoutCache();
    private final Map<ConfiguredCaxtonFont, Cache<ShapedString, ShapingResult>> shapingCaches = new IdentityHashMap<>();
    private final Cache<FromRunsInput, CaxtonText.Full> reorderCache;

    private LayoutCache() {
        reorderCache = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterAccess(Duration.ofMinutes(1))
                .build();
    }

    /**
     * @return The only instance of {@link LayoutCache}.
     */
    public static LayoutCache getInstance() {
        return INSTANCE;
    }

    public Map<ConfiguredCaxtonFont, Cache<ShapedString, ShapingResult>> getShapingCaches() {
        return shapingCaches;
    }

    /**
     * Gets the shaping cache for a Caxton font.
     * <p>
     * This holds shaping results for strings.
     *
     * @param font The {@link ConfiguredCaxtonFont} for which to get the shaping cache.
     * @return The shaping cache for {@code font}.
     */
    public Cache<ShapedString, ShapingResult> getShapingCacheFor(ConfiguredCaxtonFont font) {
        return shapingCaches.computeIfAbsent(
                font,
                f -> Caffeine.newBuilder()
                        .maximumSize(10_000)
                        .expireAfterAccess(Duration.ofMinutes(1))
                        .build());
    }

    /**
     * Gets the reorder cache.
     * <p>
     * This is responsible for caching the results of laying out lists of runs into {@link CaxtonText.Full} objects.
     *
     * @return The reorder cache.
     */
    public Cache<FromRunsInput, CaxtonText.Full> getReorderCache() {
        return reorderCache;
    }

    /**
     * Clears this cache.
     */
    public void clear() {
        shapingCaches.clear();
        reorderCache.invalidateAll();
    }

    /**
     * Contains a list of runs, along with their overall directionality.
     *
     * @param runs The list of runs computed from some text.
     * @param rtl  Whether this text should be laid out right-to-left, as opposed to left-to-right.
     */
    public record FromRunsInput(List<Run> runs, boolean rtl) {
    }
}
