package ca.alexgirard.harpoonIJ;

import java.util.ArrayList;
import java.util.List;

/**
 * The Harpoon list itself: which file ends up in which slot, and what survives a restart.
 * <p>
 * Slots are addressed here the way the actions do, zero-based, so slot {@code 0} is the file
 * reachable through "Goto Harpoon File 1".
 */
public class HarpoonStateTest extends HarpoonTestCase {

    public void testStartsEmpty() {
        assertEquals(List.of(), currentSlotNames());
        assertNull(HarpoonState.GetItem(0, getProject()));
    }

    public void testSetItemPutsTheFileInTheRequestedSlot() throws Exception {
        var a = createFile("a.txt");

        HarpoonState.SetItem(a, 2, getProject());

        assertEquals(a, HarpoonState.GetItem(2, getProject()));
        assertEquals(List.of("-", "-", "a.txt"), currentSlotNames());
    }

    public void testEachOfTheFiveSlotsHoldsItsOwnFile() throws Exception {
        for (int slot = 0; slot < 5; slot++) {
            HarpoonState.SetItem(createFile("file" + slot + ".txt"), slot, getProject());
        }

        assertEquals(List.of("file0.txt", "file1.txt", "file2.txt", "file3.txt", "file4.txt"),
                currentSlotNames());
        for (int slot = 0; slot < 5; slot++) {
            assertEquals("file" + slot + ".txt", HarpoonState.GetItem(slot, getProject()).getName());
        }
    }

    public void testSetItemReplacesWhateverWasInThatSlot() throws Exception {
        var first = createFile("first.txt");
        var second = createFile("second.txt");
        HarpoonState.SetItem(first, 1, getProject());

        HarpoonState.SetItem(second, 1, getProject());

        assertEquals(second, HarpoonState.GetItem(1, getProject()));
        assertEquals("replacing a slot must not grow the list", 2, HarpoonState.GetFiles(getProject()).size());
    }

    public void testSettingALaterSlotLeavesTheEarlierOnesEmpty() throws Exception {
        var a = createFile("a.txt");

        HarpoonState.SetItem(a, 4, getProject());

        assertEquals(List.of("-", "-", "-", "-", "a.txt"), currentSlotNames());
        assertNull(HarpoonState.GetItem(0, getProject()));
        assertNull(HarpoonState.GetItem(3, getProject()));
    }

    public void testGetItemBeyondTheEndOfTheListReturnsNull() throws Exception {
        HarpoonState.SetItem(createFile("a.txt"), 0, getProject());

        assertNull(HarpoonState.GetItem(1, getProject()));
        assertNull(HarpoonState.GetItem(99, getProject()));
    }

    public void testSettingASlotToNullEmptiesItWithoutMovingTheOthers() throws Exception {
        var a = createFile("a.txt");
        var b = createFile("b.txt");
        HarpoonState.SetItem(a, 0, getProject());
        HarpoonState.SetItem(b, 1, getProject());

        HarpoonState.SetItem(null, 0, getProject());

        assertNull(HarpoonState.GetItem(0, getProject()));
        assertEquals("b must stay in the slot the user put it in", b, HarpoonState.GetItem(1, getProject()));
        assertEquals(List.of("-", "b.txt"), currentSlotNames());
    }

    public void testTheSameFileCanBePinnedToTwoSlots() throws Exception {
        var a = createFile("a.txt");

        HarpoonState.SetItem(a, 0, getProject());
        HarpoonState.SetItem(a, 1, getProject());

        assertEquals("setting a slot explicitly is a deliberate placement, not a de-duplicated add",
                List.of("a.txt", "a.txt"), currentSlotNames());
    }

    public void testTheListIsNotCappedAtTheFiveHotkeySlots() throws Exception {
        var a = createFile("a.txt");

        HarpoonState.SetItem(a, 7, getProject());

        assertEquals(8, HarpoonState.GetFiles(getProject()).size());
        assertEquals("entries past slot 5 are reachable from the popup even without a hotkey",
                a, HarpoonState.GetItem(7, getProject()));
    }

    public void testSetFilesReplacesTheWholeListInTheGivenOrder() throws Exception {
        var a = createFile("a.txt");
        var b = createFile("b.txt");
        var c = createFile("c.txt");
        HarpoonState.SetItem(a, 0, getProject());
        HarpoonState.SetItem(b, 1, getProject());

        HarpoonState.SetFiles(List.of(c.getPath(), b.getPath(), a.getPath()), getProject());

        assertEquals(List.of("c.txt", "b.txt", "a.txt"), currentSlotNames());
    }

    public void testSetFilesIgnoresBlankLines() throws Exception {
        var a = createFile("a.txt");
        var b = createFile("b.txt");

        HarpoonState.SetFiles(List.of(a.getPath(), "", "   ", b.getPath()), getProject());

        assertEquals(List.of("a.txt", "b.txt"), currentSlotNames());
    }

    public void testSetFilesIgnoresPathsThatDoNotResolve() throws Exception {
        var a = createFile("a.txt");
        var b = createFile("b.txt");

        HarpoonState.SetFiles(List.of(a.getPath(), "/no/such/file.txt", b.getPath()), getProject());

        assertEquals("a path that no longer resolves is dropped rather than left as an empty slot",
                List.of("a.txt", "b.txt"), currentSlotNames());
    }

    public void testSetFilesAcceptsMoreThanFiveEntries() throws Exception {
        var paths = new ArrayList<String>();
        for (int i = 0; i < 7; i++) {
            paths.add(createFile("f" + i + ".txt").getPath());
        }

        HarpoonState.SetFiles(paths, getProject());

        assertEquals(7, HarpoonState.GetFiles(getProject()).size());
        assertEquals("f6.txt", HarpoonState.GetItem(6, getProject()).getName());
    }

    public void testAFilledListSurvivesAReload() throws Exception {
        var a = createFile("a.txt");
        var b = createFile("b.txt");
        var c = createFile("c.txt");
        HarpoonState.SetItem(a, 0, getProject());
        HarpoonState.SetItem(b, 1, getProject());
        HarpoonState.SetItem(c, 2, getProject());

        reload();

        assertEquals(List.of("a.txt", "b.txt", "c.txt"), currentSlotNames());
        assertEquals(c, HarpoonState.GetItem(2, getProject()));
    }

    public void testSetFilesSurvivesAReload() throws Exception {
        var a = createFile("a.txt");
        var b = createFile("b.txt");
        HarpoonState.SetFiles(List.of(b.getPath(), a.getPath()), getProject());

        reload();

        assertEquals(List.of("b.txt", "a.txt"), currentSlotNames());
    }

    public void testTheListIsPersistedUnderAStableKeyAsAbsolutePaths() throws Exception {
        var a = createFile("a.txt");

        HarpoonState.SetItem(a, 0, getProject());

        assertEquals("the persisted key is part of the on-disk contract",
                List.of(a.getPath()), persistedPaths());
    }

    public void testAFileDeletedFromDiskComesBackAsAnEmptySlot() throws Exception {
        var a = createFile("a.txt");
        var b = createFile("b.txt");
        HarpoonState.SetItem(a, 0, getProject());
        HarpoonState.SetItem(b, 1, getProject());

        deleteFile(a);
        reload();

        assertEquals("a deleted file must not pull the later entries up a slot",
                List.of("-", "b.txt"), currentSlotNames());
        assertNull(HarpoonState.GetItem(0, getProject()));
        assertEquals(b, HarpoonState.GetItem(1, getProject()));
    }
}
