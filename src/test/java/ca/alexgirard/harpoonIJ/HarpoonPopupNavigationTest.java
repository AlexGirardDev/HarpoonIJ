package ca.alexgirard.harpoonIJ;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.testFramework.TestActionEvent;

import java.util.List;

/**
 * Moving around inside the Harpoon popup and acting on the entry under the caret.
 * <p>
 * The popup is a modal dialog, so the tests drive the dialog directly rather than through
 * {@link ShowHarpoon}, which can only reach a popup that is actually on screen.
 */
public class HarpoonPopupNavigationTest extends HarpoonTestCase {

    private HarpoonDialog dialog;

    @Override
    protected void tearDown() throws Exception {
        try {
            if (dialog != null) dialog.disposeIfNeeded();
        } finally {
            super.tearDown();
        }
    }

    public void testThePopupStartsOnTheFirstEntry() {
        Editor editor = openPopup("one\ntwo\nthree");

        assertEquals(0, caretLine(editor));
    }

    public void testNextMovesToTheFollowingEntry() {
        Editor editor = openPopup("one\ntwo\nthree");

        dialog.Next();

        assertEquals(1, caretLine(editor));
    }

    public void testNextWalksTheWholeList() {
        Editor editor = openPopup("one\ntwo\nthree");

        dialog.Next();
        dialog.Next();

        assertEquals(2, caretLine(editor));
    }

    public void testNextStopsAtTheLastEntry() {
        Editor editor = openPopup("one\ntwo\nthree");

        for (int i = 0; i < 10; i++) dialog.Next();

        assertEquals("running off the bottom of the list must stay on the last entry",
                2, caretLine(editor));
    }

    public void testPreviousMovesToThePrecedingEntry() {
        Editor editor = openPopup("one\ntwo\nthree");
        dialog.Next();
        dialog.Next();

        dialog.Previous();

        assertEquals(1, caretLine(editor));
    }

    public void testPreviousStopsAtTheFirstEntry() {
        Editor editor = openPopup("one\ntwo\nthree");
        dialog.Next();

        for (int i = 0; i < 10; i++) dialog.Previous();

        assertEquals("running off the top of the list must stay on the first entry",
                0, caretLine(editor));
    }

    public void testPreviousOnTheFirstEntryStaysPut() {
        Editor editor = openPopup("one\ntwo\nthree");

        dialog.Previous();

        assertEquals(0, caretLine(editor));
    }

    public void testASinglePopupEntryHasNowhereToGo() {
        Editor editor = openPopup("only");

        dialog.Next();
        assertEquals(0, caretLine(editor));

        dialog.Previous();
        assertEquals(0, caretLine(editor));
    }

    public void testSelectingRecordsTheEntryUnderTheCaret() {
        openPopup("one\ntwo\nthree");
        dialog.Next();
        dialog.Next();

        dialog.Ok();

        assertEquals(2, dialog.SelectedIndex);
    }

    public void testSelectingWithoutMovingPicksTheFirstEntry() {
        openPopup("one\ntwo\nthree");

        dialog.Ok();

        assertEquals(0, dialog.SelectedIndex);
    }

    public void testSelectingAnEntryOpensTheFilePinnedToThatSlot() throws Exception {
        for (int slot = 0; slot < 3; slot++) {
            HarpoonState.SetItem(createFile("slot" + slot + ".txt"), slot, getProject());
        }

        ShowHarpoon.NavigateToIndex(getProject(), 1);

        assertEquals(List.of("slot1.txt"), openFileNames());
    }

    public void testSelectingAnEmptySlotOpensNothing() throws Exception {
        HarpoonState.SetItem(createFile("only.txt"), 0, getProject());

        ShowHarpoon.NavigateToIndex(getProject(), 2);

        assertEquals(List.of(), openFileNames());
    }

    public void testNextIsANoOpWhileNoPopupIsOpen() {
        invoke(new NextHarpoonItem());

        assertEquals(List.of(), openFileNames());
    }

    public void testPreviousIsANoOpWhileNoPopupIsOpen() {
        invoke(new PreviousHarpoonItem());

        assertEquals(List.of(), openFileNames());
    }

    public void testSelectIsANoOpWhileNoPopupIsOpen() throws Exception {
        HarpoonState.SetItem(createFile("a.txt"), 0, getProject());

        invoke(new SelectHarpoonItem());

        assertEquals("the popup keybindings must not fire when the popup is closed",
                List.of(), openFileNames());
    }

    private Editor openPopup(String text) {
        dialog = new HarpoonDialog(text);
        dialog.editorTextField.setDisposedWith(getTestRootDisposable());
        Editor editor = dialog.editorTextField.getEditor(true);
        assertNotNull("the popup should have a real editor to navigate", editor);
        return editor;
    }

    private static int caretLine(Editor editor) {
        return editor.getCaretModel().getLogicalPosition().line;
    }

    private void invoke(AnAction action) {
        action.actionPerformed(TestActionEvent.createTestEvent(
                action, SimpleDataContext.getProjectContext(getProject())));
    }
}
