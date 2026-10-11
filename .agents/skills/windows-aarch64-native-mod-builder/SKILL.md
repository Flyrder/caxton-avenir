---
name: windows-aarch64-native-mod-builder
description: >-
  Use this skill when building or cross-compiling native Minecraft mods and Rust JNI/FFM libraries on Windows AArch64 (ARM64).
---

# Windows AArch64 Native Mod Builder

## Procedure
1. Verify LLVM-MinGW is present via WinGet:
   `winget install --id=MartinStorsjo.LLVM-MinGW.UCRT -e`
2. Configure Rust default toolchain:
   `rustup default stable-aarch64-pc-windows-gnullvm`
3. Verify linker and target headers:
   `aarch64-w64-mingw32-clang --version`
4. Compile native crate:
   `cargo build --release --target aarch64-pc-windows-gnullvm`
5. Verify PE architecture:
   `llvm-readobj --file-headers <path-to-dll>` (look for IMAGE_FILE_MACHINE_ARM64).
