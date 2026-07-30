package ca.alexgirard.harpoonIJ;

import com.intellij.openapi.editor.Editor;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.api.VimEditor;
import com.maddyhome.idea.vim.api.VimInjectorKt;
import com.maddyhome.idea.vim.command.MappingMode;
import com.maddyhome.idea.vim.key.MappingOwner;
import com.maddyhome.idea.vim.newapi.IjVimEditorKt;
import com.maddyhome.idea.vim.state.mode.Mode;

import javax.swing.KeyStroke;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/**
 * The popup itself, driven the way the IDE drives it.
 * <p>
 * The dialog only forces normal mode once its text field receives focus, so these tests dispatch a
 * real focus event to the editor's content component - the same event the platform delivers when
 * the popup opens - and then assert what IdeaVim reports. That keeps the assertion on the outcome
 * a user sees while still covering the wiring between the dialog and
 * {@link IdeaVimIntegration}.
 */
public class HarpoonPopupNormalModeTest extends BasePlatformTestCase {

    private HarpoonDialog dialog;
    private boolean originalEnterRemap;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        originalEnterRemap = AppSettingsState.getInstance().enterRemap;
        forgetHarpoonMappings();
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            AppSettingsState.getInstance().enterRemap = originalEnterRemap;
            forgetHarpoonMappings();
            if (dialog != null) dialog.disposeIfNeeded();
        } finally {
            super.tearDown();
        }
    }

    public void testThePopupEndsUpInNormalModeWhenItGetsFocus() {
        Editor editor = openPopup("one\ntwo\nthree");
        enterInsertMode(editor);

        focusPopup(editor);

        assertTrue("the popup must be in normal mode once it is focused, so hjkl navigates",
                vim(editor).getMode() instanceof Mode.NORMAL);
    }

    public void testThePopupOpensOnTheFirstEntry() {
        Editor editor = openPopup("one\ntwo\nthree");
        enterInsertMode(editor);

        focusPopup(editor);

        assertEquals("the caret must land on the first entry, so <cr> jumps to harpoon slot 1",
                0, editor.getCaretModel().getLogicalPosition().line);
    }

    public void testThePopupStillOpensOnTheFirstEntryWithSingleEntryLists() {
        Editor editor = openPopup("only");
        enterInsertMode(editor);

        focusPopup(editor);

        assertEquals(0, editor.getCaretModel().getLogicalPosition().line);
    }

    public void testThePopupStaysInNormalModeOnLaterFocusEvents() {
        Editor editor = openPopup("one\ntwo\nthree");
        enterInsertMode(editor);
        focusPopup(editor);

        focusPopup(editor);

        assertTrue(vim(editor).getMode() instanceof Mode.NORMAL);
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

    private Editor openPopup(String text) {
        dialog = new HarpoonDialog(text);
        dialog.editorTextField.setDisposedWith(getTestRootDisposable());
        Editor editor = dialog.editorTextField.getEditor(true);
        assertNotNull("the popup should have a real editor to navigate", editor);
        return editor;
    }

    private void enterInsertMode(Editor editor) {
        VimEditor vim = vim(editor);
        var context = VimInjectorKt.injector.getExecutionContextManager().getEditorExecutionContext(vim);
        KeyHandler.getInstance().handleKey(vim, KeyStroke.getKeyStroke('i'), context,
                KeyHandler.getInstance().getKeyHandlerState());
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

    private static VimEditor vim(Editor editor) {
        return IjVimEditorKt.getVim(editor);
    }

    private static void forgetHarpoonMappings() {
        IdeaVimIntegration.forgetEnterRemap();
        VimInjectorKt.injector.getKeyGroup().removeKeyMapping(MappingOwner.Plugin.Companion.get("HarpoonIj"));
    }
}
