package ca.alexgirard.harpoonIJ;

import com.intellij.openapi.editor.Editor;
import com.intellij.testFramework.EditorTestUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.api.VimEditor;
import com.maddyhome.idea.vim.api.VimInjectorKt;
import com.maddyhome.idea.vim.command.MappingMode;
import com.maddyhome.idea.vim.key.KeySource;
import com.maddyhome.idea.vim.key.MappingOwner;
import com.maddyhome.idea.vim.newapi.IjVimEditorKt;
import com.maddyhome.idea.vim.state.mode.Mode;

import javax.swing.KeyStroke;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/**
 * The popup itself, driven the way the IDE drives it.
 * <p>
 * These tests assert what <em>keys do</em> in the popup, not what mode IdeaVim reports for it. That
 * distinction is the whole point of this class. {@code VimEditor.mode} reads one application-global
 * value, so it answers {@code NORMAL} for an editor IdeaVim is not driving at all - which is
 * exactly the state a popup built over an in-memory document is in. An earlier version of this
 * suite asserted that global and passed for two years while the popup was unusable, because the
 * value it read was identical in the broken and the working case.
 * <p>
 * So: {@link #testIdeaVimDrivesThePopupsEditor()} is the tripwire for IdeaVim silently detaching,
 * and the typing tests are the behaviour a user actually sees.
 * <p>
 * One thing these cannot cover headlessly: Escape closing the dialog. That depends on Swing's
 * {@code WHEN_IN_FOCUSED_WINDOW} binding on a real, focused, mapped window, which a light fixture
 * cannot produce. When IdeaVim is attached and the popup is in normal mode, Escape correctly falls
 * through to the dialog's cancel action; verifying that end to end needs a UI test.
 */
public class HarpoonPopupNormalModeTest extends BasePlatformTestCase {

    private HarpoonDialog dialog;
    private boolean originalEnterRemap;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        originalEnterRemap = AppSettingsState.getInstance().enterRemap;
        forgetHarpoonMappings();
        resetVimMode();
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            AppSettingsState.getInstance().enterRemap = originalEnterRemap;
            forgetHarpoonMappings();
            resetVimMode();
            if (dialog != null) dialog.disposeIfNeeded();
        } finally {
            super.tearDown();
        }
    }

    /**
     * The Vim mode is application-global, so a test that leaves it in insert mode changes what the
     * next test's first keystroke means. Reset it so these run in any order.
     */
    private static void resetVimMode() {
        VimInjectorKt.injector.getVimState().reset();
    }

    public void testIdeaVimDrivesThePopupsEditor() {
        Editor editor = openPopup("one\ntwo\nthree");

        assertTrue("IdeaVim must be handling keys in the popup's editor. If it is not, every "
                        + "assertion about the popup's mode is meaningless, because the mode is "
                        + "application-global and reads NORMAL either way.",
                IdeaVimIntegration.isAttachedTo(editor));
    }

    public void testTypingALetterNavigatesInsteadOfInsertingIt() {
        Editor editor = focusedPopup("one\ntwo\nthree");

        EditorTestUtil.performTypingAction(editor, 'j');

        assertEquals("j is a motion in the popup, not text to insert",
                "one\ntwo\nthree", editor.getDocument().getText());
        assertEquals("j must move down one entry", 1, caretLine(editor));
    }

    public void testTypingWalksBackUpTheList() {
        Editor editor = focusedPopup("one\ntwo\nthree");

        EditorTestUtil.performTypingAction(editor, 'j');
        EditorTestUtil.performTypingAction(editor, 'j');
        EditorTestUtil.performTypingAction(editor, 'k');

        assertEquals("one\ntwo\nthree", editor.getDocument().getText());
        assertEquals(1, caretLine(editor));
    }

    public void testTheEditingCommandsStillWork() {
        Editor editor = focusedPopup("one\ntwo\nthree");

        // dd on the first entry: the popup is an editable list, so this has to keep working.
        EditorTestUtil.performTypingAction(editor, 'd');
        EditorTestUtil.performTypingAction(editor, 'd');

        assertEquals("two\nthree", editor.getDocument().getText());
    }

    public void testThePopupOpensOnTheFirstEntry() {
        Editor editor = focusedPopup("one\ntwo\nthree");

        assertEquals("the caret must land on the first entry, so <cr> jumps to harpoon slot 1",
                0, caretLine(editor));
    }

    public void testThePopupStillOpensOnTheFirstEntryWithSingleEntryLists() {
        Editor editor = focusedPopup("only");

        assertEquals(0, caretLine(editor));
    }

    public void testThePopupKeepsNavigatingOnLaterFocusEvents() {
        Editor editor = focusedPopup("one\ntwo\nthree");

        focusPopup(editor);
        EditorTestUtil.performTypingAction(editor, 'j');

        assertEquals("one\ntwo\nthree", editor.getDocument().getText());
        assertEquals(1, caretLine(editor));
    }

    public void testThePopupsTextSurvivesDisposal() {
        openPopup("one\ntwo\nthree");

        dialog.disposeIfNeeded();

        assertEquals("ShowHarpoon reads the edited list back after the dialog closes, which is "
                        + "after the popup's backing file has been deleted",
                "one\ntwo\nthree", dialog.getListText());
    }

    public void testOpeningThePopupMapsEnterToTheSelectAction() {
        AppSettingsState.getInstance().enterRemap = true;

        openPopup("one\ntwo");

        var enter = VimInjectorKt.injector.getParser().parseKeys("<cr>");
        var mapping = VimInjectorKt.injector.getKeyGroup().getKeyMapping(MappingMode.NORMAL).get(enter);
        assertNotNull("<cr> should select the entry under the caret", mapping);
        assertEquals(":action SelectHarpoonItem<CR>", mapping.getPresentableString());
    }

    public void testTheEnterMappingIsSkippedWhenTheUserTurnedItOff() {
        AppSettingsState.getInstance().enterRemap = false;

        openPopup("one\ntwo");

        var enter = VimInjectorKt.injector.getParser().parseKeys("<cr>");
        assertNull("the setting exists so <cr> can be left to the user's own mapping",
                VimInjectorKt.injector.getKeyGroup().getKeyMapping(MappingMode.NORMAL).get(enter));
    }

    /** A popup that has been opened and focused, i.e. the state the user first sees. */
    private Editor focusedPopup(String text) {
        Editor editor = openPopup(text);
        // Start from insert mode so the test fails if focusing the popup stops leaving it.
        enterInsertMode(editor);
        focusPopup(editor);
        return editor;
    }

    private Editor openPopup(String text) {
        dialog = new HarpoonDialog(text);
        dialog.editorTextField.setDisposedWith(getTestRootDisposable());
        Editor editor = dialog.editorTextField.getEditor(true);
        assertNotNull("the popup should have a real editor to navigate", editor);
        return editor;
    }

    private void enterInsertMode(Editor editor) {
        VimEditor vim = IjVimEditorKt.getVim(editor);
        var context = VimInjectorKt.injector.getExecutionContextManager().getEditorExecutionContext(vim);
        KeyHandler.getInstance().handleKey(vim, KeyStroke.getKeyStroke('i'), KeySource.TYPED,
                context, KeyHandler.getInstance().getKeyHandlerState());
        assertTrue(vim.getMode() instanceof Mode.INSERT);
    }

    /** Delivers the focus event the platform sends when the popup opens. */
    private void focusPopup(Editor editor) {
        var content = editor.getContentComponent();
        var event = new FocusEvent(content, FocusEvent.FOCUS_GAINED);
        for (FocusListener listener : content.getFocusListeners()) {
            listener.focusGained(event);
        }
    }

    private static int caretLine(Editor editor) {
        return editor.getCaretModel().getLogicalPosition().line;
    }

    private static void forgetHarpoonMappings() {
        IdeaVimIntegration.forgetEnterRemap();
        VimInjectorKt.injector.getKeyGroup().removeKeyMapping(MappingOwner.Plugin.Companion.get("HarpoonIj"));
    }
}
