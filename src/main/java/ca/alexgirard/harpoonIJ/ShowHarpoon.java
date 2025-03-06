package ca.alexgirard.harpoonIJ;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class ShowHarpoon extends AnAction {

    private static HarpoonDialog dialog;

    public static void NextHarpoonItem() {
        if (dialog == null || !dialog.isShowing())
            return;
        dialog.Next();
    }
    public static void PreviousHarpoonItem() {
        if (dialog == null || !dialog.isShowing())
            return;
        dialog.Previous();
    }
    public static void SelectHarpoonItem() {
        if (dialog == null || !dialog.isShowing())
            return;
        dialog.Ok();
    }

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
        dialog = new HarpoonDialog(text);
        var result = dialog.showAndGet();
        if (text.equals(dialog.editorTextField.getText().trim())) {
            if(result) {
                NavigateToFile(project);
            }
            return;
        }
        String newText = dialog.editorTextField.getText().trim().replace("...", projectPath);

        String[] lines = newText.split("\n");
        var outputList = new ArrayList<String>();
        for (String line : lines) {
            outputList.add(line.trim());
        }
        HarpoonState.SetFiles(outputList, e.getProject());
        if (result) {
            NavigateToFile(project);
        }
    }
    private void NavigateToFile(Project project){
        if (project == null) return;
        VirtualFile vf = HarpoonState.GetItem(dialog.SelectedIndex, project);
        if (vf == null)
            return;
        HarpoonState.setSelectedIndex(project, dialog.SelectedIndex);
        var fileManager = FileEditorManager.getInstance(project);
        fileManager.openFile(vf, true);
    }
        
}



