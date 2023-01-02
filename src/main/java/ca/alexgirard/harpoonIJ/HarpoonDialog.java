package ca.alexgirard.harpoonIJ;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.EditorTextField;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class HarpoonDialog extends DialogWrapper {

    public EditorTextField editorTextField;
    private final String text;

    protected HarpoonDialog(String inputText) {
        super(true);
        setSize(800,400);
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
        editorTextField = new EditorTextField(text);
        editorTextField.setOneLineMode(false);
        editorTextField.addSettingsProvider(editor -> {
            editor.setFontSize(18);
            editor.setInsertMode(true);
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
