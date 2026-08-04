package ca.alexgirard.harpoonIJ;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.testFramework.TestActionEvent;

import java.util.List;

/** "Goto Harpoon File N": the hotkeys that jump straight to a pinned slot. */
public class GoToHarpoonActionTest extends HarpoonTestCase {

    private static final List<GoToHarpoonActionBase> GOTO_ACTIONS = List.of(
            new GotoHarpoon1Action(),
            new GotoHarpoon2Action(),
            new GotoHarpoon3Action(),
            new GotoHarpoon4Action(),
            new GotoHarpoon5Action());

    public void testTheFiveActionsTargetTheFiveSlotsInOrder() {
        for (int slot = 0; slot < GOTO_ACTIONS.size(); slot++) {
            assertEquals("Goto Harpoon File " + (slot + 1) + " must target slot " + slot,
                    slot, GOTO_ACTIONS.get(slot).getIndex());
        }
    }

    public void testGoingToASlotOpensTheFilePinnedThere() throws Exception {
        for (int slot = 0; slot < 5; slot++) {
            HarpoonState.SetItem(createFile("slot" + slot + ".txt"), slot, getProject());
        }

        invoke(GOTO_ACTIONS.get(2));

        assertEquals(List.of("slot2.txt"), openFileNames());
    }

    public void testEachSlotOpensItsOwnFile() throws Exception {
        for (int slot = 0; slot < 5; slot++) {
            HarpoonState.SetItem(createFile("slot" + slot + ".txt"), slot, getProject());
        }

        for (int slot = 0; slot < 5; slot++) {
            closeAllEditors();

            invoke(GOTO_ACTIONS.get(slot));

            assertEquals(List.of("slot" + slot + ".txt"), openFileNames());
        }
    }

    public void testGoingToAnEmptySlotOpensNothing() throws Exception {
        HarpoonState.SetItem(createFile("only.txt"), 0, getProject());

        invoke(GOTO_ACTIONS.get(3));

        assertEquals(List.of(), openFileNames());
    }

    public void testGoingToASlotOnAnEmptyListOpensNothing() {
        invoke(GOTO_ACTIONS.get(0));

        assertEquals(List.of(), openFileNames());
    }

    public void testGoingToAFileThatWasDeletedOpensNothing() throws Exception {
        var gone = createFile("gone.txt");
        HarpoonState.SetItem(gone, 0, getProject());
        deleteFile(gone);

        invoke(GOTO_ACTIONS.get(0));

        assertEquals("a pinned file that no longer exists must be skipped, not opened",
                List.of(), openFileNames());
    }

    public void testGoingToASlotDoesNothingWithoutAProject() throws Exception {
        HarpoonState.SetItem(createFile("a.txt"), 0, getProject());

        GOTO_ACTIONS.get(0).actionPerformed(
                TestActionEvent.createTestEvent(GOTO_ACTIONS.get(0), DataContext.EMPTY_CONTEXT));

        assertEquals(List.of(), openFileNames());
    }

    public void testGoingToASlotLeavesTheHarpoonListAlone() throws Exception {
        var a = createFile("a.txt");
        var b = createFile("b.txt");
        HarpoonState.SetItem(a, 0, getProject());
        HarpoonState.SetItem(b, 1, getProject());

        invoke(GOTO_ACTIONS.get(1));

        assertEquals(List.of("a.txt", "b.txt"), currentSlotNames());
    }

    private void invoke(AnAction action) {
        DataContext context = SimpleDataContext.getProjectContext(getProject());
        action.actionPerformed(TestActionEvent.createTestEvent(action, context));
    }
}
