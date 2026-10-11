// (C) 2022-2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.dll;

import java.util.List;
import xyz.flirora.caxton.CaxtonModClient;

public class RustPlatform {
  public static List<String> candidatesForCurrent() {
    String target = CaxtonModClient.CONFIG.rustTarget;
    if (target != null) {
      return List.of(target);
    }

    String osName = System.getProperty("os.name");
    String osArch = System.getProperty("os.arch");

    String osArch3 =
        switch (osArch) {
          case "x86_64", "amd64" -> "x86_64";
          case "x86", "i686" -> "i686";
          case "aarch64" -> "aarch64";
          default ->
              throw new UnsupportedPlatformException(
                  "Cannot determine Rust platform name: unrecognized architecture name " + osArch);
        };

    if (osName.startsWith("Windows")) {
      if ("aarch64".equals(osArch3)) {
        return List.of(
            "aarch64-pc-windows-gnullvm", "aarch64-pc-windows-msvc", "aarch64-pc-windows-gnu");
      } else {
        return List.of(osArch3 + "-pc-windows-gnu", osArch3 + "-pc-windows-msvc");
      }
    } else if (osName.startsWith("Linux")) {
      return List.of(osArch3 + "-unknown-linux-gnu", osArch3 + "-unknown-linux-musl");
    } else if (osName.startsWith("FreeBSD")) {
      return List.of(osArch3 + "-unknown-freebsd");
    } else if (osName.startsWith("Mac OS X") || osName.startsWith("Darwin")) {
      return List.of(osArch3 + "-apple-darwin");
    } else {
      throw new UnsupportedPlatformException(
          "Cannot determine Rust platform name: unrecognized OS name " + osName);
    }
  }

  public static String forCurrent() {
    return candidatesForCurrent().get(0);
  }
}
