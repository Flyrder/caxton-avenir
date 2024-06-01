// (C) 2022-2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.layout.gui;

import java.util.List;
import xyz.flirora.caxton.layout.CaxtonText;
import xyz.flirora.caxton.layout.FcIndexConverter;

public interface BookEditScreenPageContentExt {
  List<CaxtonText> getCaxtonText();

  void setCaxtonText(List<CaxtonText> texts);

  FcIndexConverter getWarts();

  void setWarts(FcIndexConverter warts);
}
