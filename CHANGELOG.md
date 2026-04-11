## 1.0.0-alpha.3

Adopt official enum extension methods on Fabric.

* On Fabric, we used to use [Fabric ASM](https://github.com/Chocohead/Fabric-ASM) to extend the `FontType` (Mojmap:
  `GlyphProviderType`) enum. Since the recently released version 0.17.1, Mixin
  supports [enum extensions](https://github.com/FabricMC/Mixin/pull/199), which removes the need for Fabric ASM. This
  also makes the source code for the Fabric version completely free of Mojmap. Note that **Caxton now requires Fabric
  Loader 0.19.1 or later.**
* On Forge, we originally used JavaScript core mods to extend the `FontType` enum. When NeoForge planned to remove
  support for core mods, this was replaced with a mixin that adds the field manually. NeoForge has
  a [built-in way to extend certain enums](https://docs.neoforged.net/docs/advanced/extensibleenums), which we plan to
  use eventually, but we’re waiting on Neo Loom being able to remap enum extension files.
