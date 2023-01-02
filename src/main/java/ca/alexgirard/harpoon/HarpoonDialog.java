package ca.alexgirard.harpoon;

import com.intellij.json.JsonFileType;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileTypes.FileTypeFactory;
import com.intellij.openapi.fileTypes.impl.AbstractFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.EditorSettingsProvider;
import com.intellij.ui.EditorTextField;
import org.jetbrains.annotations.Nullable;

import javax.sound.midi.SysexMessage;
import javax.swing.*;

public class HarpoonDialog extends DialogWrapper {

    public static EditorTextField editorTextField;
    private String text ;
    private Project project ;

    protected HarpoonDialog(String inputText, Project project) {
        super(true);
        setSize(400,400);
        text = inputText;
        project = project;
        init();
    }
    protected HarpoonDialog() {
        super(true);
        init();
    }

    @Override
    public JComponent getPreferredFocusedComponent(){

        return editorTextField;
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        if(editorTextField == null)
            editorTextField = new EditorTextField(text,project, JsonFileType.INSTANCE );

        editorTextField.setOneLineMode(false);
        editorTextField.setText(text);
        editorTextField.addSettingsProvider(EditorSettingsProvider.NO_WHITESPACE);
        editorTextField.addSettingsProvider( editor -> editor.getSettings().setLineNumbersShown(true));
        editorTextField.setCaretPosition(0);

        editorTextField.setSize(500,500);

        return editorTextField;
    }

}
