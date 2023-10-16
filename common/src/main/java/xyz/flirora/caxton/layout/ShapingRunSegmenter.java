package xyz.flirora.caxton.layout;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UCharacterCategory;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.Bidi;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static xyz.flirora.caxton.layout.Interleaving.*;

public class ShapingRunSegmenter {
    private final String contents;
    private final Bidi bidi;
    private final List<RunGroup> runGroups;
    private final LayoutCache cache;
    private int charOffset = 0;
    private int currentBidiRun = 0;
    private int currentBidiStringIndex = 0;
    private final int totalBidiRuns;
    private final IntList bidiRunsList;

    public ShapingRunSegmenter(List<Run> runs, boolean rtl, LayoutCache cache) {
        contents = runs.stream().map(Run::text).collect(Collectors.joining());
        this.bidi = new Bidi(
                contents,
                rtl ? Bidi.DIRECTION_DEFAULT_RIGHT_TO_LEFT
                        : Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);
        this.runGroups = new ArrayList<>();
        this.totalBidiRuns = this.bidi.countRuns();
        this.cache = cache;
        this.bidiRunsList = new IntArrayList(6);

        List<Run> last = null;
        for (Run run : runs) {
            if (last == null) {
                last = new ArrayList<>();
            } else if (!areRunsCompatible(last.get(0), run)) {
                addRunGroup(last);
                last = new ArrayList<>();
            }
            last.add(run);
        }
        if (last != null) {
            addRunGroup(last);
        }
    }

    public String getContents() {
        return this.contents;
    }

    public Bidi getBidi() {
        return this.bidi;
    }

    public List<RunGroup> getRunGroupsInVisualOrder() {
        return reorderRunGroups(this.runGroups);
    }

    public boolean isRightToLeft() {
        return this.bidi.getParaLevel() % 2 != 0;
    }

    private void addRunGroup(List<Run> runs) {
        // We don’t use Bidi#getLogicalRun(int) because it’s O(n) and will make
        // the whole segmentation process O(n^2).
        int firstBidiRunInGroup = currentBidiRun;
        int firstBidiStringIndex = currentBidiStringIndex;
        for (Run run : runs) {
            currentBidiStringIndex += run.text().length();
        }
        // Advance to the bidi run for the end of the run group’s text
        while (currentBidiRun < totalBidiRuns && bidi.getRunStart(currentBidiRun) < currentBidiStringIndex) {
            ++currentBidiRun;
        }
//            if (totalBidiRuns > 1)
//                System.err.println("run group from " + firstBidiStringIndex + " to " + currentBidiStringIndex + ": firstBidiRunInGroup = " + firstBidiRunInGroup + ", firstBidiStringIndex = " + firstBidiStringIndex);
        if (currentBidiRun == totalBidiRuns || bidi.getRunStart(currentBidiRun) >= currentBidiStringIndex)
            --currentBidiRun;
        // Always compute bidi runs for this run group;
        // this information is useful for reshaping in legacy fonts.
        bidiRunsList.clear();
        for (int i = firstBidiRunInGroup; i <= currentBidiRun; ++i) {
//                if (totalBidiRuns > 1) {
//                    System.err.println("i = " + i);
//                    System.err.println("firstBidiStringIndex = " + firstBidiStringIndex);
//                    System.err.println("start = " + bidi.getRunStart(i));
//                    System.err.println("limit = " + bidi.getRunLimit(i));
//                    System.err.println("level = " + bidi.getRunLevel(i));
//                }
            int start = Math.max(
                    0,
                    bidi.getRunStart(i) - firstBidiStringIndex);
            int end = Math.min(
                    currentBidiStringIndex - firstBidiStringIndex,
                    bidi.getRunLimit(i) - firstBidiStringIndex);
            // Segment this bidi run by script.
            // See UAX #24 <https://www.unicode.org/reports/tr24/> for more info.
            if (end > start) {
                int level = bidi.getRunLevel(i);
                int c = contents.codePointAt(start);
                int script = UScript.getScript(c);
                int cur = start + Character.charCount(c);
                while (cur < end) {
                    int c2 = contents.codePointAt(cur);
                    int script2 = UScript.getScript(c2);
                    // Handle cases when one of the scripts is common or inherited.
                    // Check script extensions (UAX #24, §3) in this case.
                    // Some characters have an explicit script property but have multiple
                    // scripts in its extension.
                    if (isCommonOrInherited(script2) && includesScript(c2, script)) {
                        script2 = script;
                    } else if (isCommonOrInherited(script) && includesScript(c, script2)) {
                        script = script2;
                    }
                    // UAX #24, §5.2
                    if (script != script2 && !isCombiningMark(c)) {
                        // We need to start a new run
                        bidiRunsList.add(start);
                        bidiRunsList.add(cur);
                        bidiRunsList.add(level);
                        bidiRunsList.add(ScriptTags.USCRIPT_VALUES_TO_TAGS[script]);
                        start = cur;
                        script = script2;
                        c = c2;
                    }
                    cur += Character.charCount(c2);
                }
                bidiRunsList.add(start);
                bidiRunsList.add(end);
                bidiRunsList.add(level);
                bidiRunsList.add(ScriptTags.USCRIPT_VALUES_TO_TAGS[script]);
            }
        }
        int[] bidiRuns = reorderBidiRuns(bidiRunsList);
        int runLevel = bidi.getRunLevel(firstBidiRunInGroup);
//            System.out.println(group);
//            System.out.println(Arrays.toString(bidiRuns));
        RunGroup runGroup = new RunGroup(runs, runLevel, charOffset, bidiRuns, cache);
        this.runGroups.add(runGroup);

        charOffset += runGroup.getTotalLength();

        if (currentBidiRun < totalBidiRuns && currentBidiStringIndex >= bidi.getRunLimit(currentBidiRun))
            ++currentBidiRun;
    }

    private static boolean isCombiningMark(int c) {
        int cat = UCharacter.getType(c);
        return cat == UCharacter.COMBINING_SPACING_MARK || cat == UCharacter.NON_SPACING_MARK || cat == UCharacter.ENCLOSING_MARK;
    }

    private static boolean isCommonOrInherited(int script) {
        return script == UScript.COMMON || script == UScript.INHERITED;
    }

    private static boolean includesScript(int codePoint, int script) {
        return UScript.hasScript(codePoint, script) || UScript.hasScript(codePoint, UScript.COMMON) || UScript.hasScript(codePoint, UScript.INHERITED);
    }

    private static int[] reorderBidiRuns(IntList runs) {
        int nRuns = runs.size() / SHAPING_RUN_STRIDE;
        if (nRuns == 0) return runs.toIntArray();

        byte[] levels = new byte[nRuns];
        for (int i = 0; i < nRuns; ++i) {
            levels[i] = (byte) runs.getInt(SHAPING_RUN_STRIDE * i + SHAPING_RUN_OFF_LEVEL);
        }
        int[] indices = Bidi.reorderVisual(levels);

        int[] runsOut = new int[runs.size()];

        for (int i = 0; i < nRuns; ++i) {
            int j = indices[i];
            runsOut[SHAPING_RUN_STRIDE * i + SHAPING_RUN_OFF_START] = runs.getInt(SHAPING_RUN_STRIDE * j + SHAPING_RUN_OFF_START);
            runsOut[SHAPING_RUN_STRIDE * i + SHAPING_RUN_OFF_END] = runs.getInt(SHAPING_RUN_STRIDE * j + SHAPING_RUN_OFF_END);
            runsOut[SHAPING_RUN_STRIDE * i + SHAPING_RUN_OFF_LEVEL] = runs.getInt(SHAPING_RUN_STRIDE * j + SHAPING_RUN_OFF_LEVEL);
            runsOut[SHAPING_RUN_STRIDE * i + SHAPING_RUN_OFF_SCRIPT] = runs.getInt(SHAPING_RUN_STRIDE * j + SHAPING_RUN_OFF_SCRIPT);
        }

        return runsOut;
    }

    private static List<RunGroup> reorderRunGroups(List<RunGroup> runGroups) {
        int nRuns = runGroups.size();

        byte[] levels = new byte[nRuns];
        for (int i = 0; i < nRuns; ++i) {
            levels[i] = (byte) runGroups.get(i).getRunLevel();
        }
        RunGroup[] runGroupsArray = runGroups.toArray(new RunGroup[0]);
        Bidi.reorderVisually(levels, 0, runGroupsArray, 0, nRuns);

        return List.of(runGroupsArray);
    }

    private static boolean areRunsCompatible(Run a, Run b) {
        return a.font() == b.font();
    }
}
