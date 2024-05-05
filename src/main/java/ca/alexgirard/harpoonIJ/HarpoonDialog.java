package ca.alexgirard.harpoonIJ;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.EditorTextField;
import com.maddyhome.idea.vim.api.VimInjectorKt;
import com.maddyhome.idea.vim.command.MappingMode;
import com.maddyhome.idea.vim.key.MappingOwner;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class HarpoonDialog extends DialogWrapper {

    public EditorTextField editorTextField;
    private final String text;
    public int SelectedIndex = -1;
    private static boolean enterRemapped = false;

    public void Ok() {

        var editor = editorTextField.getEditor();
        if (editor != null) {
            SelectedIndex = editor.getCaretModel().getLogicalPosition().line;
        }
        doOKAction();
    }

    protected HarpoonDialog(String inputText) {
        super(true);
        AppSettingsState settings = AppSettingsState.getInstance();
        setSize(settings.dialogWidth, settings.dialogHeight);
        setTitle("Harpoon");
        var vimPlugin = PluginManagerCore.getPlugin(PluginId.getId("IdeaVIM"));
        var vimEnabled = vimPlugin != null && vimPlugin.isEnabled();
        if (vimEnabled) {
            setEnterRemap(settings);
        }
        text = inputText;
        init();
    }

    //TODO move this to app startup or something 
    private void setEnterRemap(AppSettingsState settings) {
        if (enterRemapped || !settings.enterRemap) return;
        // i had 2 <cr> here before and i can't remember why
        var keys = VimInjectorKt.injector.getParser().parseKeys(":action SelectHarpoonItem<cr>");
        var keyGroup = VimInjectorKt.injector.getKeyGroup();
        keyGroup.putKeyMapping(MappingMode.NVO,
                VimInjectorKt.injector.getParser().parseKeys("<cr>")
                , MappingOwner.Plugin.Companion.get("HarpoonIj"), keys, false);
        enterRemapped = true;
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
            editor.getCaretModel().moveToLogicalPosition(new LogicalPosition(0, 0));
            var settings = editor.getSettings();
            settings.setLineNumbersShown(true);
        });


        return editorTextField;
    }


    @Override
    protected JComponent createSouthPanel() {
        return null;
    }
}
