# Caxton Development Rules

## Native Toolchain on Windows AArch64
- When compiling native components on Windows ARM64, use the `stable-aarch64-pc-windows-gnullvm` toolchain paired with LLVM-MinGW (`aarch64-w64-mingw32-clang`).
- For runtime and packaging compatibility, both `aarch64-pc-windows-gnullvm` and `aarch64-pc-windows-gnu` target names must be accommodated.

## Gradle Task Lifecycle
- Any task writing hashes or processing generated artifacts must encapsulate its file operations inside `doLast { ... }` so they execute during the execution phase, not the configuration phase.

## HMCL Launcher Version Isolation
- When installing or testing mod JARs with HMCL version isolation enabled, target the specific version subfolder at `.minecraft/versions/<ProfileName>/mods/`, and preserve existing JARs as `.orig` backups before overwriting.
