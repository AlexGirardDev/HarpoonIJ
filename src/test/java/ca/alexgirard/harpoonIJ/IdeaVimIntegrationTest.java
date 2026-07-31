package ca.alexgirard.harpoonIJ;

import com.intellij.openapi.editor.Editor;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
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
 * Everything here asserts an outcome IdeaVim itself reports - the mode the editor ends up in, and
 * the mapping IdeaVim has registered for {@code <cr>} - rather than which calls got us there. A
 * rewrite of IdeaVim's key handling should leave these tests passing as long as the behaviour a
 * user sees is unchanged.
 */
public class IdeaVimIntegrationTest extends BasePlatformTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        forgetHarpoonMappings();
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            forgetHarpoonMappings();
        } finally {
            super.tearDown();
        }
    }

    public void testIdeaVimIsActiveInTheTestIde() {
        assertTrue("the rest of this class is meaningless without IdeaVim on the classpath",
                IdeaVimIntegration.isAvailable());
    }

    public void testForcingNormalModeTakesTheEditorOutOfInsertMode() {
        Editor editor = editorInInsertMode();

        IdeaVimIntegration.forceNormalMode(editor);

        assertTrue("the popup must open in normal mode, not insert mode",
                vim(editor).getMode() instanceof Mode.NORMAL);
    }

    public void testForcingNormalModeReportsThatItActed() {
        Editor editor = editorInInsertMode();

        assertTrue(IdeaVimIntegration.forceNormalMode(editor));
    }

    public void testForcingNormalModeIsSafeToRepeat() {
        Editor editor = editorInInsertMode();

        IdeaVimIntegration.forceNormalMode(editor);
        IdeaVimIntegration.forceNormalMode(editor);

        assertTrue(vim(editor).getMode() instanceof Mode.NORMAL);
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
