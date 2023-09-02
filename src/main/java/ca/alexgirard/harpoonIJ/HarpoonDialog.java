package ca.alexgirard.harpoonIJ;

import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.EditorTextField;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.api.VimInjectorKt;
import com.maddyhome.idea.vim.newapi.IjVimEditorKt;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

public class HarpoonDialog extends DialogWrapper {

    public EditorTextField editorTextField;
    private final String text;
    private boolean forceNormalMode;
    private boolean normalModeForcedAlready = false;

    protected HarpoonDialog(String inputText) {
        super(true);
        AppSettingsState settings = AppSettingsState.getInstance();
        setSize(settings.dialogWidth, settings.dialogHeight);
        setTitle("Harpoon");
        if (PluginManager.isPluginInstalled(PluginId.getId("IdeaVIM"))) {
            forceNormalMode = settings.dialogForceVimNormalMode;
        }
        text = inputText;
        init();
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return editorTextField;
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        AppSettingsState appSettings = AppSettingsState.getInstance();
        editorTextField = new EditorTextField(text);
        editorTextField.setOneLineMode(false);
        editorTextField.addSettingsProvider(editor -> {
            editor.setFontSize(appSettings.dialogFontSize);
            editor.setInsertMode(true);
            var settings = editor.getSettings();
            settings.setLineNumbersShown(true);
        });


        if (forceNormalMode) {
            editorTextField.addFocusListener(new FocusListener() {
                public void focusGained(FocusEvent e) {
                    if (normalModeForcedAlready) return;
                    var editor = editorTextField.getEditor();
                    if (editor == null) return;
                    var context = VimInjectorKt.injector.getExecutionContextManager().onEditor(IjVimEditorKt.getVim(editor), null);
                    KeyHandler.getInstance().handleKey(
                            IjVimEditorKt.getVim(editor),
                            VimInjectorKt.injector.getParser().parseKeys("<ESC>").get(0),
                            context
                    );
                    normalModeForcedAlready = true;
                }

                public void focusLost(FocusEvent e) {
                }
            });
        }
        return editorTextField;
    }


    @Override
    protected JComponent createSouthPanel() {
        return null;
    }
}
