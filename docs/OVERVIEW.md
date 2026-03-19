# Overview

## Font loading

Caxton adds a new glyph provider, `CaxtonTypeface`. A new glyph provider type is added for this class, through Fabric
ASM on Fabric and a mixin on NeoForge.

Loading a Caxton font requires computing either MSDFs or rasterized textures, depending on the font tech. This is
handled by the `caxton-impl` native library; results of this process are cached in the `caxton_cache` directory to speed
up subsequent loads.

## Text layout and shaping

`CaxtonText` is the starting point for further text-related operations. An instance can be constructed from various
text-related objects. This is partially cached by `LayoutCache`: there is a cache from `FromRunsInput` to `CaxtonText`,
but the process of splitting text into runs is not cached.

Laying out Caxton text also requires shaping the text. This is handled by `caxton-impl`, and results are cached by
`LayoutCache`.

## Text rendering

Caxton adds a mixin to `TextRenderer`, adding a `CaxtonTextRenderer` field and modifying `TextRenderer` methods to
redirect to their analogous `CaxtonTextRenderer` implementations.

## GUI

Caxton modifies several GUI widgets to be compatible with complex text layout:

* `AbstractSignEditScreen`
* `EditBox` / `EditBoxWidget`
* `TextFieldWidget`

The in-game HUD is also modified to draw the XP text using a dedicated outlined text-drawing method.
