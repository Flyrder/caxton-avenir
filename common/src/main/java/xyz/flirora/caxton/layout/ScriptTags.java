// (C) 2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.layout;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UScript;

public class ScriptTags {
    public static final int[] USCRIPT_VALUES_TO_TAGS;

    static {
        int len = UCharacter.getIntPropertyMaxValue(UProperty.SCRIPT) + 1;
        USCRIPT_VALUES_TO_TAGS = new int[len];
        for (int i = 0; i < len; ++i) {
            String tag = UScript.getShortName(i);
            int tag32 = ((int) tag.charAt(0)) << 24 |
                    ((int) tag.charAt(1)) << 16 |
                    ((int) tag.charAt(2)) << 8 |
                    ((int) tag.charAt(3));
            USCRIPT_VALUES_TO_TAGS[i] = tag32;
        }
    }
}
