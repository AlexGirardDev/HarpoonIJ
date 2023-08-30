package ca.alexgirard.harpoonIJ;

import com.intellij.ide.DataManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.EditorTextField;
import com.maddyhome.idea.vim.command.OperatorArguments;
import com.maddyhome.idea.vim.command.VimStateMachine;
import com.maddyhome.idea.vim.helper.ModeHelper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

public class HarpoonDialog extends DialogWrapper {

    public EditorTextField editorTextField;
    private final String text;

    protected HarpoonDialog(String inputText) {
        super(true);
        AppSettingsState settings = AppSettingsState.getInstance();
        setSize(settings.dialogWidth, settings.dialogHeight);
        setTitle("Harpoon");
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

        editorTextField.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {
                if (forcedNormalMode) return;
                var editor = editorTextField.getEditor();
                if (editor == null) return;
                ModeHelper.exitInsertMode(editorTextField.getEditor(),
                        DataManager.getInstance().getDataContext(e.getComponent()),
                        new OperatorArguments(false, 1, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE));
                editorTextField.setCaretPosition(0);
                forcedNormalMode = true;
            }

            public void focusLost(FocusEvent e) {
            }
        });
        return editorTextField;
    }
    private boolean forcedNormalMode = false;

    @Override
    protected JComponent createSouthPanel() {
        return null;
    }
}
