package ca.alexgirard.harpoonIJ;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class ShowHarpoon extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {

        var stringBuilder = new StringBuilder();
        var fileStrings = HarpoonState.GetFiles(e.getProject());
        var project = e.getProject();
        var projectPath = project == null ? "" : project.getBasePath();
        projectPath = projectPath == null ? "" : projectPath;

        for (var vFile : fileStrings) {
            var path = vFile == null ? "" : vFile.getCanonicalPath();
            path = path == null ? "" : path;
            stringBuilder.append(path.replace(projectPath, "...")).append("\n");
        }


        var text = stringBuilder.toString().trim();

        var dialog = new HarpoonDialog(text);
        dialog.showAndGet();
        String newText = dialog.editorTextField.getText().trim().replace("...", projectPath);
        if (text.equals(newText)) return;

        String[] lines = newText.split("\n");
        var outputList = new ArrayList<String>();
        for (String line : lines) {
            outputList.add(line.trim());
        }
        HarpoonState.SetFiles(outputList, e.getProject());
    }
}



