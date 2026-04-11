## 1.0.0-alpha.3

Adopt official enum extension methods on Fabric and NeoForge.

* On Fabric, we used to use [Fabric ASM](https://github.com/Chocohead/Fabric-ASM) to extend the `FontType` (Mojmap:
  `GlyphProviderType`) enum. Since the recently released version 0.17.1, Mixin
  supports [enum extensions](https://github.com/FabricMC/Mixin/pull/199), which removes the need for Fabric ASM. Note
  that **Caxton now requires Fabric Loader 0.19.1 or later.**
