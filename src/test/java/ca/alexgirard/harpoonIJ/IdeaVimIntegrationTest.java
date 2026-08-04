package ca.alexgirard.harpoonIJ;

import com.intellij.openapi.editor.Editor;
import com.intellij.testFramework.EditorTestUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.ui.EditorTextField;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.api.VimEditor;
import com.maddyhome.idea.vim.api.VimInjectorKt;
import com.maddyhome.idea.vim.command.MappingMode;
import com.maddyhome.idea.vim.key.KeySource;
import com.maddyhome.idea.vim.key.MappingInfo;
import com.maddyhome.idea.vim.key.MappingOwner;
import com.maddyhome.idea.vim.newapi.IjVimEditorKt;
import com.maddyhome.idea.vim.state.mode.Mode;

import javax.swing.KeyStroke;

/**
 * The IdeaVim integration, which is the part of this plugin that keeps breaking when IdeaVim
 * refactors its internals.
 * <p>
 * The mode-related cases assert what keys <em>do</em> rather than what mode IdeaVim reports.
 * {@code VimEditor.mode} is one application-global value, so asserting on it says nothing about any
 * particular editor: it reads {@code NORMAL} just as happily for an editor IdeaVim has not attached
 * to at all. Asserting on it is what let the popup ship unusable.
 * <p>
 * The {@code <cr>} cases do assert what IdeaVim reports, but there the reported thing - the mapping
 * registry - is real per-mapping state this plugin writes, not a global that something else set.
 */
public class IdeaVimIntegrationTest extends BasePlatformTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        forgetHarpoonMappings();
        resetVimMode();
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            forgetHarpoonMappings();
            resetVimMode();
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

    public void testIdeaVimIsActiveInTheTestIde() {
        assertTrue("the rest of this class is meaningless without IdeaVim on the classpath",
                IdeaVimIntegration.isAvailable());
    }

    public void testForcingNormalModeMakesTypingNavigateRatherThanInsert() {
        Editor editor = editorInInsertMode();

        IdeaVimIntegration.forceNormalMode(editor);
        EditorTestUtil.performTypingAction(editor, 'j');

        assertEquals("j must be a motion afterwards, not text", "one\ntwo\nthree\n",
                editor.getDocument().getText());
        assertEquals(1, editor.getCaretModel().getLogicalPosition().line);
    }

    public void testForcingNormalModeReportsThatItActed() {
        Editor editor = editorInInsertMode();

        assertTrue(IdeaVimIntegration.forceNormalMode(editor));
    }

    public void testForcingNormalModeIsSafeToRepeat() {
        Editor editor = editorInInsertMode();

        IdeaVimIntegration.forceNormalMode(editor);
        IdeaVimIntegration.forceNormalMode(editor);
        EditorTestUtil.performTypingAction(editor, 'j');

        assertEquals("one\ntwo\nthree\n", editor.getDocument().getText());
    }

    public void testIdeaVimDrivesAFileBackedEditor() {
        assertTrue(IdeaVimIntegration.isAttachedTo(editorInInsertMode()));
    }

    public void testIdeaVimDoesNotDriveAnInMemoryEditor() {
        assertFalse("IdeaVim only attaches to editors backed by a real file. This is the whole "
                        + "reason the Harpoon popup has to be backed by one.",
                IdeaVimIntegration.isAttachedTo(inMemoryEditor()));
    }

    public void testForcingNormalModeRefusesAnEditorIdeaVimIsNotDriving() {
        assertFalse("forcing normal mode on an unattached editor would change the global Vim mode "
                        + "of the editor behind the dialog while doing nothing for the popup",
                IdeaVimIntegration.forceNormalMode(inMemoryEditor()));
    }

    public void testEnterIsMappedToTheSelectHarpoonItemAction() {
        IdeaVimIntegration.remapEnterToSelectHarpoonItem();

        assertEquals(":action SelectHarpoonItem<CR>", presentableEnterMapping(MappingMode.NORMAL));
    }

    public void testEnterIsMappedInNormalVisualAndOperatorPendingModes() {
        IdeaVimIntegration.remapEnterToSelectHarpoonItem();

        assertEquals(":action SelectHarpoonItem<CR>", presentableEnterMapping(MappingMode.NORMAL));
        assertEquals(":action SelectHarpoonItem<CR>", presentableEnterMapping(MappingMode.VISUAL));
        assertEquals(":action SelectHarpoonItem<CR>", presentableEnterMapping(MappingMode.OP_PENDING));
    }

    public void testEnterIsLeftAloneInInsertMode() {
        IdeaVimIntegration.remapEnterToSelectHarpoonItem();

        assertNull("remapping enter while inserting text would break typing",
                enterMapping(MappingMode.INSERT));
    }

    public void testTheEnterMappingIsRegisteredAgainstHarpoon() {
        IdeaVimIntegration.remapEnterToSelectHarpoonItem();

        MappingInfo mapping = enterMapping(MappingMode.NORMAL);
        assertNotNull(mapping);
        assertEquals("IdeaVim needs to be able to attribute the mapping back to this plugin",
                harpoonMappingOwner(), mapping.getOwner());
    }

    public void testRemappingEnterTwiceLeavesOneMapping() {
        IdeaVimIntegration.remapEnterToSelectHarpoonItem();
        IdeaVimIntegration.forgetEnterRemap();
        IdeaVimIntegration.remapEnterToSelectHarpoonItem();

        assertEquals(":action SelectHarpoonItem<CR>", presentableEnterMapping(MappingMode.NORMAL));
    }

    public void testEnterIsNotMappedBeforeTheDialogAsksForIt() {
        assertNull(enterMapping(MappingMode.NORMAL));
    }

    /** An editor over a plain in-memory document, i.e. what the popup used to be built over. */
    private Editor inMemoryEditor() {
        EditorTextField field = new EditorTextField("one\ntwo\nthree\n");
        field.setDisposedWith(getTestRootDisposable());
        Editor editor = field.getEditor(true);
        assertNotNull(editor);
        return editor;
    }

    private Editor editorInInsertMode() {
        myFixture.configureByText("harpoon-list.txt", "one\ntwo\nthree\n");
        Editor editor = myFixture.getEditor();
        VimEditor vim = vim(editor);
        var context = VimInjectorKt.injector.getExecutionContextManager().getEditorExecutionContext(vim);
        // Enter insert mode the way a user would, by typing `i`.
        KeyHandler.getInstance().handleKey(vim, KeyStroke.getKeyStroke('i'), KeySource.TYPED,
                context, KeyHandler.getInstance().getKeyHandlerState());
        assertTrue("fixture should have put the editor in insert mode",
                vim.getMode() instanceof Mode.INSERT);
        return editor;
    }

    private static VimEditor vim(Editor editor) {
        return IjVimEditorKt.getVim(editor);
    }

    private static MappingInfo enterMapping(MappingMode mode) {
        var enter = VimInjectorKt.injector.getParser().parseKeys("<cr>");
        return VimInjectorKt.injector.getKeyGroup().getKeyMapping(mode).get(enter);
    }

    private static String presentableEnterMapping(MappingMode mode) {
        MappingInfo mapping = enterMapping(mode);
        assertNotNull("<cr> is not mapped in " + mode, mapping);
        return mapping.getPresentableString();
    }

    private static MappingOwner harpoonMappingOwner() {
        return MappingOwner.Plugin.Companion.get("HarpoonIj");
    }

    private static void forgetHarpoonMappings() {
        IdeaVimIntegration.forgetEnterRemap();
        VimInjectorKt.injector.getKeyGroup().removeKeyMapping(harpoonMappingOwner());
    }
}
