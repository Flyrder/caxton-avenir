# Caxton

**Caxton**, named after [William Caxton], is a Minecraft mod that adds TrueType and OpenType font support.

Available on [Modrinth](https://modrinth.com/mod/caxton)!

## Features

* Crisp text at any size thanks to [MSDF] technology
* Real bold and italic fonts
* Complex text rendering
* Does not use AWT

### Current limitations

* **Arabic shaping in legacy fonts is not currently supported.** Making this work in the presence of styling and proper
  bidirectional text handling is complicated as we cannot use ICU4J’s API for this. If you want Arabic text to render
  properly, then you’ll have to use a font with Arabic support under Caxton.
* Generating MTSDFs from fonts is expensive. For this reason, Caxton will parallelize this process and cache the results
  after it is first done.
* Many UI elements, both in Minecraft and in mods, make incorrect assumptions about text rendering. Making them aware of
  bidirectional text – let alone matters such as ligatures – will be a major undertaking, and patches in this area
  are welcome.
    * GUI elements have been patched to account for this, but the displayed text is aligned to the left regardless
      of its base direction.
* Font hinting will probably never be supported.

#### Mod and resource pack compatibility

Caxton has been confirmed to work with [Sodium].

For [ImmediatelyFast], make sure that:

* either Caxton’s `tweakExpText` option or IF’s `hud_batching` option is disabled, and
* either Caxton’s `reuseTextRendererDrawer` option or IF’s `fast_text_lookup` option is disabled, and
* Caxton’s `sortTextRenderLayers` option is disabled.

Caxton is not fully compatible with [Iris Shaders]. Text outside the world (e.g. in GUIs or HUDs) is rendered normally,
but the default MSDF-based font rendering technology uses [custom core shaders, which Iris does not support].
As a result, text rendered in world using such fonts will not render at all when a shader is active.

Caxton is not compatible with [Emojiful], which replaces Minecraft’s default text renderer with its own implementation
and does not delegate to the original text renderer’s methods.

Caxton 0.5.0 and later are incompatible with [MemoryLeakFix] version 1.1.1 and earlier; update the latter mod to 1.1.2 or later.

If [Exordium]’s sign buffering feature is enabled, then [text on signs will look pixelated]. If this happens, then
disable sign buffering for Exordium.

The following mods are known to handle text layout incorrectly. They should not crash the game, but GUI elements from
these mods might have unexpected behavior and might cause warnings to be printed about unsupported text handling
methods.

* [IBE Editor] uses a custom text field widget
  that [clones most of vanilla Minecraft’s rendering code](https://github.com/Franckyi/IBE-Editor/issues/73).
* [Roughly Enough Items]
  uses [unsupported methods in some of its widgets](https://github.com/shedaniel/RoughlyEnoughItems/search?q=plainSubstrByWidth).

Resource packs that modify core shaders related to text, such as the “Dark UI” and “Transparent UI” packs from [Vanilla Tweaks], will require modification to work with Caxton fonts.

[Sodium]: https://modrinth.com/mod/sodium

[ImmediatelyFast]: https://modrinth.com/mod/immediatelyfast

[Iris Shaders]: https://github.com/IrisShaders/Iris

[custom core shaders, which Iris does not support]: https://github.com/IrisShaders/Iris/blob/1.18.2/docs/development/compatibility/core-shaders.md

[MemoryLeakFix]: https://modrinth.com/mod/memoryleakfix

[Exordium]: https://modrinth.com/mod/exordium

[text on signs will look pixelated]: https://github.com/tr7zw/Exordium/issues/48

[IBE Editor]: https://modrinth.com/mod/ibe-editor

[Roughly Enough Items]: https://modrinth.com/mod/rei

[Emojiful]: https://github.com/InnovativeOnlineIndustries/Emojiful

[Vanilla Tweaks]: https://vanillatweaks.net

## OS support

Caxton uses a native library to assist with text shaping and MSDF generation. The pre-built copy of the mod bundles
versions of this library for x86_64 Windows and Linux platforms. If you are playing on a different platform, then you
will have to build a copy of the mod yourself.

If the mod still does not recognize your platform, then change the `rustTarget` setting in `config/caxton.json`
to one of the [Rust platform names](https://doc.rust-lang.org/nightly/rustc/platform-support.html) corresponding to your
platform and report an issue here.

Note that as there is not a single Mac in my house, I cannot build binaries for macOS due to licensing issues.

## How to use Caxton

Caxton currently comes with two built-in resource packs for fonts. The first font included is [Inter], while the second
is [Open Sans].

If neither of these suit your needs, you can use your own font using a resource pack. **Before distributing a resource
pack that contains a font file, read the font’s license to make sure that you have the right to distribute that font.**

### Adding fonts via resource packs

Caxton adds a font provider of type `caxton`, which supports the keys `regular`, `bold`, `italic`, and `bold_italic`.
Each of these can be set to an identifier, where `<namespace>:<path>` resolves
to the font file `assets/<namespace>/textures/font/<path>`. To specify other options, use an object where the
key `file` specifies the path:

```json5
{
  // The only required element.
  "file": "<namespace>:<path>",
  // The factor by which to scale the font from the default size.
  // If this is 1.0, then the font is scaled so that the ascent is scaled to 7
  // pixels of the default bitmap font.
  // Added in Caxton 0.3.0.
  "scale_factor": 1.0,
  // The shadow offset, as a multiple of the memefont pixel size.
  "shadow_offset": 1.0,
  // The amount to shift the rendered text by in the X and Y axes, in memefont pixels.
  // Added in Caxton 0.4.4.
  "shift": [0.0, 0.0],
  // A list of OpenType feature tags. See below for the syntax:
  // https://docs.rs/rustybuzz/0.6.0/rustybuzz/struct.Feature.html#method.from_str
  "features": [],
  // Only has an effect on raster tech. Interpolation is always enabled for
  // MSDF fonts.
  // If true, texels in the glyph bitmap will be interpolated.
  // Added in Caxton 0.4.0.
  "blur": false
}
```

If an object exists at the key `caxton_providers` in the font JSON file, then it will be used in place of `providers` if
Caxton is installed. This can be used to load a Caxton font while having a fallback if Caxton is not installed.
If `caxton_providers` is not specified, then `providers` will be used instead.

You can also add the file `assets/<namespace>/textures/font/<path>.json`, which contains settings for
rasterizing the font:

```json5
{
  // Specifies the actual path of the font file, as it would appear in the Caxton
  // font provider.
  // This should generally be omitted, but can be useful if you are using a
  // variable font.
  "path": "<path of font file>",
  // All of these options are optional and will default to the provided values.
  // The number of font units corresponding to each pixel in the texture atlas.
  // This can be set to a high value if using the "msdf" font rendering tech.
  // If you’re using "raster", then you should set this to a lower value.
  "shrinkage": 32.0,
  // The number of pixels to leave around the glyph bounding box on each side.
  // This should be larger than `range` and will default to `range` if left blank.
  "margin": 4,
  // The width of the range around the glyph between the minimum and maximum
  // representable signed distances. This is a positive integer no greater
  // than 255.
  // This also determines the width of the border drawn for glowing sign text.
  "range": 4,
  // Whether to invert the signed distance field (true or false).
  // If this is null, then Caxton will try to determine this automatically,
  // but you can override this in case it guesses incorrectly.
  "invert": null,
  // This option is used to set variation axis coordinates in variable fonts.
  // Each element has the following format:
  // { "axis": <axis type>, "value": <axis value> }
  "variations": [],
  // The index of the font face to use in a font collection.
  // Leave this as 0 if you’re not sure.
  // Added in Caxton 0.3.0.
  "face_index": 0,
  // Specifies whether to use the MSDF-based rendering method ("msdf") or
  // to use glyph bitmaps ("raster" – EXPERIMENTAL).
  // "msdf" is recommended for most fonts, but "raster" is more suitable for
  // pixel fonts.
  // In addition, only "raster" is fully compatible with Iris Shaders – text in
  // MSDF fonts will not show up inside the world if a shader is loaded.
  // Added in Caxton 0.4.0.
  "tech": "msdf",
  // The maximum mipmap level (0 – 4).
  // This is pointless to set if you’re using the MSDF rendering tech.
  // However, it can be useful for non-pixel fonts when you’re using the raster
  // rendering tech.
  // Added in Caxton 0.4.0.
  "max_mipmap": 0
}
```

### Global configuration

The following options are available in `config/caxton.json`:

```json5
{
  // One of the Rust platform names (https://doc.rust-lang.org/nightly/rustc/platform-support.html)
  // corresponding to your platform.
  // If this is null, then Caxton will determine the correct value for your
  // platform, so it is recommended to change this only when necessary.
  // Added in Caxton 0.2.1.
  "rustTarget": null,
  // Use a different method of drawing the level text on the experience bar:
  // For Caxton fonts, this draws the text with an outline shader, instead of
  // drawing four offset copies in the outline color followed by the main text
  // in the fill color.
  // This option primarily serves to make the experience level text look better
  // in outline fonts. It used to act as a workaround for a misrendering issue
  // with ImmediatelyFast but poses issues on newer versions of that mod.
  // See:
  // * https://gitlab.com/Kyarei/caxton/-/issues/31
  // * https://github.com/RaphiMC/ImmediatelyFast/issues/49
  // Non-Caxton fonts are not affected.
  // Added in Caxton 0.4.0.
  "tweakExpText": true,
  // Sort primitives on Caxton’s text render layers from back to front.
  // Disabling this setting may theoretically result in misrendering of text;
  // however, ImmediatelyFast expects this setting to be disabled, and
  // the developer of ImmediatelyFast has not received any reports of rendering
  // issues when this functionality is disabled.
  // If you experience rendering issues where some text seems to be drawn in
  // the wrong z-order, then try enabling this option (and disabling HUD batching
  // in ImmediatelyFast).
  // See: https://github.com/RaphiMC/ImmediatelyFast/issues/49
  // Added in Caxton 0.4.0.
  "sortTextRenderLayers": false,
  // When drawing text in non-Caxton fonts, reuse a single `TextRenderer.Drawer`
  // instance. This avoids having to create a new object for each legacy run
  // group but can result in a crash with ImmediatelyFast.
  // See: https://gitlab.com/Kyarei/caxton/-/issues/50
  // Added in Caxton 0.4.7.
  "reuseTextRendererDrawer": true,
  // Some text handling methods are inherently broken and unsupported by Caxton.
  // Caxton will log a warning whenever these methods are called. If this option
  // is set to true, then Caxton will throw an exception instead.
  // This option is primarily useful for debugging. If in doubt, set this to false.
  // Added in Caxton 0.2.1.
  "fatalOnBrokenMethodCall": false,
  // Track information about when a Caxton font object has a reference added or
  // removed for debugging purposes.
  // If in doubt, set this to false.
  // Added in Caxton 0.3.0.
  "debugRefcountChanges": false
}
```

[William Caxton]: https://en.wikipedia.org/wiki/William_Caxton

[MSDF]: https://github.com/Chlumsky/msdfgen

[Inter]: https://github.com/rsms/inter

[Open Sans]: https://github.com/googlefonts/opensans

## Building from source

If you want to build Caxton from source, then you’ll need to install the [Rust toolchain] and [Clang] in addition to Gradle.

By default, the native library will be built for the host platform only. To build it for additional platforms, specify the additional targets by their [target triples] (separated by commas) in the
`xyz.flirora.caxton.additionalTargets` property. For instance, if you want to build the library for x86_64 Windows, then
you can invoke

```sh
gradle build -Dxyz.flirora.caxton.additionalTargets=x86_64-pc-windows-gnu
```

If you get an error about unknown targets, then modify the `cargoCrossBuildTasks` variable in `caxton-impl/build.gradle`.

[Rust toolchain]: https://www.rust-lang.org/tools/install
[Clang]: https://rust-lang.github.io/rust-bindgen/requirements.html
[cross]: https://github.com/cross-rs/cross/
[target triples]: https://doc.rust-lang.org/nightly/rustc/platform-support.html

## Comparison with other mods

### BetterFonts / TrueType Font Replacement

Originally created by [thvortex] through 1.4.4, updated to 1.4.7 by bechill, then to 1.5.2 by [The_MiningCrafter], then
by [secretdataz] to 1.6.x and 1.7.x, then by [cubex2] from 1.8.9 to 1.12.2. Then updated to 1.13 again by secretdataz.

This mod uses Java AWT’s text layout functionality for laying out text. For rendering the glyphs, it rasterizes them
into bitmaps. The resolution is quite limited. Unlike many of the other mods listed below, however, it implements bold
and italic styles, as well as complex scripts, properly.

[thvortex]: https://github.com/thvortex/BetterFonts

[The_MiningCrafter]: https://www.minecraftforum.net/forums/mapping-and-modding-java-edition/minecraft-mods/1287298-1-5-1-betterfonts-make-your-minecraft-chat-text

[secretdataz]: https://github.com/secretdataz/BetterFonts

[cubex2]: https://github.com/cubex2/BetterFonts

### Smooth Font

Created by [bre2el] for versions from 1.7 to 1.12. This mod also improves rendering of text at different scales and
implements some optimizations for text rendering.

As for how it works, who the `RenderType` knows? This mod is ARR.

[bre2el]: https://www.curseforge.com/minecraft/mc-mods/smooth-font

### ThaiFixes

Created by [lion328] for Forge on versions up to 1.12.2 and for Rift on 1.13, and updated through 1.18.2 on Fabric
by [secretdataz][secretdataz2].

This mod implements its own shaping routines for Thai specifically. Thus, it is not useful for other languages that
require complex rendering.

[lion328]: https://github.com/lion328/thaifixes

[secretdataz2]: https://github.com/secretdataz/ThaiFixes-Fabric

### Modern UI

Created by [BloCamLimb] for versions 1.15 to 1.19 on Forge.

From the screenshots, it seems that this mod supports complex text rendering and true bold and italic styles. It also
fixes many issues with vanilla text layout such as [MC-117311].

Judging by the code, Modern UI has a surprisingly sophisticated layout algorithm. I haven’t had much time to look at it,
though.

However, this mod fails to render text with crisp borders. It also uses AWT for performing text layout.

[BloCamLimb]: https://github.com/BloCamLimb/ModernUI

[MC-117311]: https://bugs.mojang.com/browse/MC-117311

### Minecraft 1.13 and later

Since 1.13, Minecraft supports TrueType and OpenType fonts. However, this implementation is not fundamentally different
from those of bitmap fonts – the game converts the glyphs into bitmaps and lays out text naïvely. In addition, it
handles glyph metrics incorrectly, causing TTF text to appear off-kilter.

## Credits

Caxton would not have been possible without the following projects:

* [Fabric] for Minecraft
* The [Rust] programming language
* [Gradle Cargo Wrapper] (Arc-blroth, Apache-2.0)
* [RustyBuzz] (RazrFalcon, MIT)
* [ttf-parser] (RazrFalcon, MIT/Apache-2.0)
* [fdsm] (me, MIT)
    * Versions 0.4.11 and earlier used [msdfgen] (Chlumsky, MIT) and the [msdfgen-rs] bindings (Kayo Phoenix, MIT), both of which have subsequently been invaluable in the development of fdsm.
    * Versions 0.2.3 and earlier used the [msdf-rs] bindings (Penple, MIT) instead.
* [ab-glyph-rasterizer] (alexheretic, Apache-2.0)
* [JNI bindings for Rust] (MIT/Apache-2.0)
* [flate2] (MIT/Apache-2.0)
* [Rayon] (MIT/Apache-2.0)
* [Cross] (MIT/Apache-2.0)
* [Fabric-ASM] (Chocohead, MPL-2.0)
* [MixinExtras] (LlamaLad7, MIT)
* [Caffeine] (Ben Manes, Apache-2.0)
* [Inter] (Rasmus Andersson, OFL-1.1)
* [Open Sans] (OFL-1.1)

[Fabric]: https://fabricmc.net/

[Rust]: https://www.rust-lang.org/

[Gradle Cargo Wrapper]: https://github.com/Arc-blroth/gradle-cargo-wrapper

[RustyBuzz]: https://github.com/RazrFalcon/rustybuzz

[ttf-parser]: https://github.com/RazrFalcon/ttf-parser

[fdsm]: https://gitlab.com/Kyarei/fdsm

[msdfgen]: https://github.com/Chlumsky/msdfgen

[msdf-rs]: https://github.com/Penple/msdf-rs

[msdfgen-rs]: https://github.com/katyo/msdfgen-rs

[ab-glyph-rasterizer]: https://github.com/alexheretic/ab-glyph

[JNI bindings for Rust]: https://github.com/jni-rs/jni-rs

[flate2]: https://github.com/rust-lang/flate2-rs

[Rayon]: https://github.com/rayon-rs/rayon

[Cross]: https://github.com/cross-rs/cross/

[Fabric-ASM]: https://github.com/Chocohead/Fabric-ASM

[MixinExtras]: https://github.com/LlamaLad7/MixinExtras

[Caffeine]: https://github.com/ben-manes/caffeine

The following people are also credited for translations:

* IcedDog for the Chinese translation
* JustFoxxo for the Polish translation

