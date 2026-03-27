## 1.0.0-alpha.1

Initial experimental build using
the [Foreign Function & Memory](https://docs.oracle.com/en/java/javase/25/core/foreign-function-and-memory-api.html) (
FFM) API.

Caxton previously used the [Java Native Interface](https://en.wikipedia.org/wiki/Java_Native_Interface) (JNI) to
interact with the native `caxton-impl` library, which handles text shaping and glyph rasterization (both conventional
and MSDF). Java 22 added the Foreign Function & Memory (FFM) API as an alternative to JNI. Since Minecraft 26.1 requires
Java 25, it is now possible to migrate to FFM.

Since this is a major change, this version might have bugs or performance issues.
Please report them to our issue tracker!
