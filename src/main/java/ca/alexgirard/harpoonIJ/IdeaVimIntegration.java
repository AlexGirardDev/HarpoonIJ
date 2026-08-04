package ca.alexgirard.harpoonIJ;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.api.VimInjectorKt;
import com.maddyhome.idea.vim.command.MappingMode;
import com.maddyhome.idea.vim.helper.EditorHelperRt;
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

    private static final Logger LOG = Logger.getInstance(IdeaVimIntegration.class);

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
     * Whether IdeaVim has installed its key handling in {@code editor}.
     * <p>
     * This is the question that actually matters, and it is not the same as asking what mode
     * IdeaVim reports. {@code VimEditor.mode} reads a single application-global value, so it
     * answers {@code NORMAL} for an editor IdeaVim is not driving at all - which is exactly what
     * happens to a popup built over an in-memory document. In that state nothing here has any
     * effect on the popup, no matter what the mode says.
     * <p>
     * Reads IdeaVim's own {@code isIdeaVimDisabledHere}, which is the predicate its key dispatch
     * consults. Like everything else in this class that is an IdeaVim internal, so it is one of the
     * things to re-check when {@code ideaVimVersion} moves.
     */
    public static boolean isAttachedTo(Editor editor) {
        if (!isAvailable()) return false;
        try {
            return !EditorHelperRt.isIdeaVimDisabledHere(editor);
        } catch (LinkageError movedOrRenamed) {
            LOG.warn("IdeaVim's isIdeaVimDisabledHere is no longer where HarpoonIJ expects it; "
                    + "assuming the popup is Vim-driven", movedOrRenamed);
            return true;
        }
    }

    /**
     * Sends the editor an escape, so the Harpoon popup ends up in normal mode the way the NeoVim
     * original does rather than in insert mode.
     * <p>
     * Refuses to act on an editor IdeaVim is not driving. The mode it would change is global, so
     * sending escape there would silently reach through the dialog and change the mode of the file
     * editor behind it, while doing nothing for the popup.
     * <p>
     * The insert-mode guard reads IdeaVim's {@code VimEditor.insertMode}, which on the IntelliJ
     * side is the editor's insert-versus-overwrite typing flag and not the Vim mode, so it is true
     * for an ordinary editor. Sending escape to an editor that is already in normal mode is
     * harmless.
     *
     * @return {@code true} if the escape was sent, {@code false} if it was not - IdeaVim missing,
     * IdeaVim not driving this editor, or the editor reporting it was not in insert mode.
     */
    public static boolean forceNormalMode(Editor editor) {
        if (!isAvailable()) return false;
        if (!isAttachedTo(editor)) {
            LOG.warn("IdeaVim is installed but is not driving the Harpoon popup's editor, so the "
                    + "popup cannot be put into normal mode and Vim keys will not work in it. "
                    + "IdeaVim only drives editors backed by a real file; see "
                    + "HarpoonDialog.createPopupEditor.");
            return false;
        }
        var vim = IjVimEditorKt.getVim(editor);
        if (!vim.getInsertMode()) {
            LOG.warn("IdeaVim reported the Harpoon popup's editor was not in insert mode, so it "
                    + "was left alone and the popup may open in the wrong mode.");
            return false;
        }
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
