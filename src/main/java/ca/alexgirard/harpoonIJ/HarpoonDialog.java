package ca.alexgirard.harpoonIJ;

import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.EditorTextField;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

public class HarpoonDialog extends DialogWrapper {

    public EditorTextField editorTextField;
    private final String text;
    public int SelectedIndex = -1;
    private boolean normalModeForcedAlready = false;

    public void Next() {

        var editor = editorTextField.getEditor();
        if (editor != null) {
            editor.getCaretModel().moveCaretRelatively(0, 1, false, false, false);
        }
    }

    public void Previous() {

        var editor = editorTextField.getEditor();
        if (editor != null) {
            editor.getCaretModel().moveCaretRelatively(0, -1, false, false, false);
        }
    }

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
        if (settings.enterRemap && IdeaVimIntegration.isAvailable()) {
            IdeaVimIntegration.remapEnterToSelectHarpoonItem();
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
            editor.getCaretModel().moveToLogicalPosition(new LogicalPosition(0, 0));
            var settings = editor.getSettings();
            settings.setLineNumbersShown(true);
        });


        editorTextField.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {
                if (normalModeForcedAlready) return;
                var editor = editorTextField.getEditor();
                if (editor == null) return;
                if (!IdeaVimIntegration.forceNormalMode(editor)) return;
                normalModeForcedAlready = true;
                editor.getCaretModel().moveCaretRelatively(0, 1, false, false, false);
            }

            public void focusLost(FocusEvent e) {
            }
        });
        return editorTextField;
    }


    @Override
    protected JComponent createSouthPanel() {
        return null;
    }
}
