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

#### Mod compatibility

Caxton has been confirmed to work with [Sodium] and [ImmediatelyFast].

If [Exordium]’s sign buffering feature is enabled, then [text on signs will look pixelated]. If this happens, then
disable sign buffering for Exordium.

The following mods are known to handle text layout incorrectly. They should not crash the game, but GUI elements from
these mods might have unexpected behavior and might cause warnings to be printed about unsupported text handling
methods.

* [IBE Editor] uses a custom text field widget
  that [clones most of vanilla Minecraft’s rendering code](https://github.com/Franckyi/IBE-Editor/issues/73).
* [Roughly Enough Items]
  uses [unsupported methods in some of its widgets](https://github.com/shedaniel/RoughlyEnoughItems/search?q=plainSubstrByWidth).

[Sodium]: https://modrinth.com/mod/sodium

[ImmediatelyFast]: https://modrinth.com/mod/immediatelyfast

[Exordium]: https://modrinth.com/mod/exordium

[text on signs will look pixelated]: https://github.com/tr7zw/Exordium/issues/48

[IBE Editor]: https://modrinth.com/mod/ibe-editor

[Roughly Enough Items]: https://modrinth.com/mod/roughly-enough-items

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
  "scale_factor": 1.0,
  // The shadow offset, as a multiple of the memefont pixel size.
  "shadow_offset": 1.0,
  // A list of OpenType feature tags. See below for the syntax:
  // https://docs.rs/rustybuzz/0.6.0/rustybuzz/struct.Feature.html#method.from_str
  "features": [],
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
  // This can be set to a high value because the atlas is an MTSDF.
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
  // If this is null, then Caxton will try to determine this automatically, but you can override this in case it guesses incorrectly.
  "invert": null,
  // This option is used to set variation axis coordinates in variable fonts.
  // Each element has the following format:
  // { "axis": <axis type>, "value": <axis value> }
  "variations": [],
  // The index of the font face to use in a font collection.
  // Leave this as 0 if you’re not sure.
  "face_index": 0,
  // Specifies whether to use the MSDF-based rendering method ("msdf") or
  // to use glyph bitmaps ("raster").
  // "msdf" is recommended for most fonts, but "raster" is more suitable for
  // pixel fonts.
  // Neither method is currently compatible with Iris Shaders, but support for
  // "raster" when a shader is present is planned.
  "tech": "msdf",
  // Whether to interpolate texels in the bitmap (NOT YET IMPLEMENTED).
  // Only has an effect on raster tech.
  "blur": false,
  // The maximum mipmap level (0 – 4).
  // This is pointless to set if you’re using the MSDF rendering tech.
  // However, it can be useful for non-pixel fonts when you’re using the raster
  // rendering tech.
  "max_mipmap": 0
}
```

### Global configuration

The following options are available in `config/caxton.json`:

```json5
{
  // One of the Rust platform names (https://doc.rust-lang.org/nightly/rustc/platform-support.html) corresponding to your platform.
  // If this is null, then Caxton will determine the correct value for your platform, so it is recommended to change this only when necessary.
  "rustTarget": null,
  // Use a different method of drawing the level text on the experience bar:
  // For Caxton fonts, this draws the text with an outline shader, instead of
  // drawing four offset copies in the outline color followed by the main text
  // in the fill color. This acts as a workaround for a misrendering issue when
  // ImmediatelyFast is present (https://gitlab.com/Kyarei/caxton/-/issues/31).
  // Non-Caxton fonts are not affected.
  "tweakExpText": true,
  // Some text handling methods are inherently broken and unsupported by Caxton. Caxton will log a warning whenever these methods are called. If this option is set to true, then Caxton will throw an exception instead.
  // This option is primarily useful for debugging. If in doubt, set this to false.
  "fatalOnBrokenMethodCall": false,
  // Track information about when a Caxton font object has a reference added or
  // removed for debugging purposes.
  // If in doubt, set this to false.
  "debugRefcountChanges": false
}
```

[William Caxton]: https://en.wikipedia.org/wiki/William_Caxton

[MSDF]: https://github.com/Chlumsky/msdfgen

[Inter]: https://github.com/rsms/inter

[Open Sans]: https://github.com/googlefonts/opensans

## Comparison with other mods

### BetterFonts / TrueType Font Replacement

Originally created by [thvortex] through 1.4.4, updated to 1.4.7 by bechill, then to 1.5.2 by [The_MiningCrafter], then
by [secretdataz] to 1.6.x and 1.7.x, then by [cubex2] from 1.8.9 to 1.12.2. Then updated to 1.13 again by secretdataz.

This mod uses Java AWT’s text layout functionality for laying out text. For rendering the glyphs, it rasterizes them
into bitmaps. The resolution is quite limited. Unlike the other mods listed below, however, it implements bold and
italic styles, as well as complex scripts, properly.

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
* [msdfgen] (Chlumsky, MIT) and the [msdfgen-rs] bindings (Kayo Phoenix, MIT)
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
