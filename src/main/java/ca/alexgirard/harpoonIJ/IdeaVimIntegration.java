package ca.alexgirard.harpoonIJ;

import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.api.VimInjectorKt;
import com.maddyhome.idea.vim.command.MappingMode;
import com.maddyhome.idea.vim.key.KeySource;
import com.maddyhome.idea.vim.key.MappingOwner;
import com.maddyhome.idea.vim.newapi.IjVimEditorKt;
import org.jetbrains.annotations.TestOnly;

import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;

/**
 * Every call HarpoonIJ makes into IdeaVim lives here.
 * <p>
 * IdeaVim is an optional dependency and its internals move between releases, so this is the one
 * place that has to be re-checked when either version is bumped. Keeping it out of the Swing
 * plumbing also means the behaviour can be tested against a real editor without building a dialog.
 * <p>
 * Without the IdeaVim plugin installed its classes are not on the classpath at all, so every
 * method here that reaches into IdeaVim checks {@link #isAvailable()} first and does nothing when
 * IdeaVim is absent. Callers may therefore call them unconditionally.
 */
public final class IdeaVimIntegration {

    /** IdeaVim's entry point; used only as a probe for whether IdeaVim is loaded. */
    private static final String VIM_PLUGIN_CLASS = "com.maddyhome.idea.vim.VimPlugin";

    /** Identifies HarpoonIJ as the owner of the key mappings it registers with IdeaVim. */
    private static final String MAPPING_OWNER = "HarpoonIj";

    private static final String ENTER = "<cr>";
    private static final String SELECT_HARPOON_ITEM = ":action SelectHarpoonItem<cr>";

    private static boolean enterRemapped = false;

    private IdeaVimIntegration() {
    }

    /**
     * Whether the IdeaVim plugin is installed and enabled, i.e. whether its API can be called.
     * <p>
     * Asked of the classloader rather than of the plugin manager: {@code plugin.xml} declares
     * IdeaVim as an optional dependency, so its classes are visible here exactly when IdeaVim is
     * loaded, which is the question the callers actually need answered. The plugin manager's
     * equivalents ({@code PluginManagerCore.getPlugin}, {@code PluginManager.findEnabledPlugin})
     * are all marked internal and fail plugin verification.
     */
    public static boolean isAvailable() {
        try {
            Class.forName(VIM_PLUGIN_CLASS, false, IdeaVimIntegration.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError notLoaded) {
            return false;
        }
    }

    /**
     * Maps {@code <cr>} to the {@code SelectHarpoonItem} action so that pressing enter in the
     * Harpoon popup jumps to the file under the caret. Registered once per IDE session, and a
     * no-op when IdeaVim is not installed.
     */
    public static void remapEnterToSelectHarpoonItem() {
        if (enterRemapped || !isAvailable()) return;
        var parser = VimInjectorKt.injector.getParser();
        VimInjectorKt.injector.getKeyGroup().putKeyMapping(
                MappingMode.NVO,
                parser.parseKeys(ENTER),
                MappingOwner.Plugin.Companion.get(MAPPING_OWNER),
                parser.parseKeys(SELECT_HARPOON_ITEM),
                false);
        enterRemapped = true;
    }

    /**
     * Sends the editor an escape, so the Harpoon popup ends up in normal mode the way the NeoVim
     * original does rather than in insert mode.
     * <p>
     * The guard reads IdeaVim's {@code VimEditor.insertMode}, which on the IntelliJ side is the
     * editor's insert-versus-overwrite typing flag and not the Vim mode, so it is true for an
     * ordinary editor. Sending escape to an editor that is already in normal mode is harmless.
     *
     * @return {@code true} if the escape was sent, {@code false} if IdeaVim is not installed or
     * the editor reported that it was not in insert mode and was left alone.
     */
    public static boolean forceNormalMode(Editor editor) {
        if (!isAvailable()) return false;
        var vim = IjVimEditorKt.getVim(editor);
        if (!vim.getInsertMode()) return false;
        var context = VimInjectorKt.injector.getExecutionContextManager().getEditorExecutionContext(vim);
        // Same as if the user had pressed escape. KeySource.TYPED is what the shorter, now
        // scheduled-for-removal, overload of handleKey passed on our behalf.
        KeyHandler.getInstance().handleKey(
                vim,
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                KeySource.TYPED,
                context,
                KeyHandler.getInstance().getKeyHandlerState());
        return true;
    }

    /**
     * Forgets that {@code <cr>} was already remapped. Tests need this because IdeaVim resets its
     * key mappings between test cases while this class' latch would otherwise survive.
     */
    @TestOnly
    static void forgetEnterRemap() {
        enterRemapped = false;
    }
}
