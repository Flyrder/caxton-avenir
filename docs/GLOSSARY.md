# The Caxton code base glossary

## Vanilla concepts

In general, we use ([Modern]) Yarn names over official names. In the following entries, the Yarn name for the applicable
class is provided, followed by the official name if different.

[Modern]: github.com/RelativityMC/yarn

Text component
: Minecraft’s way of storing rich text.
: `Text` (`Component`)

Glyph provider
: Defines a source for glyphs.
: `Font` (`GlyphProvider`)

Font storage
: A list of glyph providers, in order of descending priority.
: `FontStorage` (`FontSet`)

Text renderer
: The class responsible for drawing text. Since 1.19.1, the Minecraft client holds two instances of this, one of which
validates advances for the chat input box.
: `TextRenderer` (`Font`)

Text handler
: Responsible for calculating layout-related properties of text such as width and line wrapping. Held by a text
renderer.
: `TextHandler` (`StringSplitter`)

Draw context
: A high-level wrapper class for drawing GUIs.
: `DrawContext` (`GuiGraphics`)

Baked glyph
: A glyph that can output a drawn version of itself at a certain location.
: `BakedGlyph`

## Caxton concepts

Caxton font
: Corresponds to a font file with an optional face index and settings related to rasterization.
: `CaxtonFont`

Font tech
: Either `msdf` or `raster`, dictating the method used to render text in a Caxton font.
: `FontTech`

Configured Caxton font
: Extends a Caxton font with layout-related properties (such as scaling and font features). Multiple configured Caxton
fonts can share the same Caxton font object.
: `ConfiguredCaxtonFont`

Caxton typeface
: Contains a configured Caxton font for each combination of bold and italic. This is the glyph provider added by Caxton.
: `CaxtonTypeface`

Caxton text
: A representation of text that contains layout information and is suitable for rendering by Caxton. Contains a series
of run groups.
: `CaxtonText`

Lay out
: To convert something that holds text data (such as `Text` or `String`) into `CaxtonText`.

Run group
: A series of style runs that can be shaped together.
: `RunGroup`

Style run
: A contiguous sequence of characters within a run group that share the same style.
: `Run`

Bidi run
: A contiguous sequence of characters within a run group that share the same directionality.

Shaping result
: The result of shaping a run of text.

Legacy
: Describing text or fonts implemented by the vanilla game as opposed to Caxton.

Caxton text renderer (CTR)
: Caxton’s extension to a text renderer.
: `CaxtonTextRenderer`

Caxton text handler (CTH)
: Caxton’s extension to a text handler.
: `CaxtonTextHandler`

Caxton atlas
: Used for packing glyph textures into a smaller number of textures on demand.
: Vanilla has its own atlas system; `CaxtonAtlas` is independent of it.
: `CaxtonAtlas`

Glyph cache
: Contains an atlas plus a mapping from glyph IDs to atlas locations for each Caxton font.
: From 1.21.6 onward, this also contains mappings from glyph IDs to baked glyphs.
: `CaxtonGlyphCache`

`caxton-impl`
: The native library implementing MSDF generation and text layout.

Batching hack
: A workaround for [MC-161933], which concerns text with translucent pixels being drawn in the wrong order. Caxton fixes
this by redirecting the `VertexConsumerProvider#immediate` call initializing
`BufferBuilderStorage#entityVertexConsumers` to its own implementation of `VertexConsumerProvider.Immediate` (called
`WorldRendererVertexConsumerProvider`, or WRVCP for short), which delays drawing all Caxton text layers until explicitly
told to do so.

[MC-161933]: https://bugs.mojang.com/browse/MC/issues/MC-161933