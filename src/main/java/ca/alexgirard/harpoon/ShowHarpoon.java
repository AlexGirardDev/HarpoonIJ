package ca.alexgirard.harpoon;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class ShowHarpoon extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {

        var stringBuilder = new StringBuilder();
        var fileStrings = HarpoonState.GetFiles(e.getProject());
        for (var vFile : fileStrings)
            stringBuilder.append(vFile == null ? "" : vFile.getCanonicalPath()).append("\n");
        var text = stringBuilder.toString().trim();

        var dialog = new HarpoonDialog(text);
        var response = dialog.showAndGet();
        if (response) {
            String newText = dialog.editorTextField.getText().trim();
            if (text.equals(newText)) return;

            String[] lines = newText.split("\n");
            var outputList = new ArrayList<String>();
            for (String line :lines) {
                outputList.add(line.trim());
            }
            HarpoonState.SetFiles(outputList);
        }
    }
}



