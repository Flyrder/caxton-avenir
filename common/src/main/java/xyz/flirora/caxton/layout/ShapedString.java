package xyz.flirora.caxton.layout;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * A string, along with directionality information.
 *
 * @param text The text to be shaped.
 * @param rtl  Whether this text should be laid out right-to-left, as opposed to left-to-right.
 */
@Environment(EnvType.CLIENT)
public record ShapedString(String text, boolean rtl) {
}
