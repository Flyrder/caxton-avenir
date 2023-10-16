package xyz.flirora.caxton.layout;

import com.ibm.icu.text.Bidi;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
            int j = i - firstBidiRunInGroup;
            bidiRunsList.add(Math.max(
                    0,
                    bidi.getRunStart(i) - firstBidiStringIndex));
            bidiRunsList.add(Math.min(
                    currentBidiStringIndex - firstBidiStringIndex,
                    bidi.getRunLimit(i) - firstBidiStringIndex));
            bidiRunsList.add(bidi.getRunLevel(i));
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

    private static int[] reorderBidiRuns(IntList runs) {
        int nRuns = runs.size() / 3;
        if (nRuns == 0) return runs.toIntArray();

        byte[] levels = new byte[nRuns];
        for (int i = 0; i < nRuns; ++i) {
            levels[i] = (byte) runs.getInt(3 * i + 2);
        }
        int[] indices = Bidi.reorderVisual(levels);

        int[] runsOut = new int[runs.size()];

        for (int i = 0; i < nRuns; ++i) {
            int j = indices[i];
            runsOut[3 * i] = runs.getInt(3 * j);
            runsOut[3 * i + 1] = runs.getInt(3 * j + 1);
            runsOut[3 * i + 2] = runs.getInt(3 * j + 2);
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
