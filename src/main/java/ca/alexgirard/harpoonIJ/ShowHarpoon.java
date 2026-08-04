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
        // Read through the dialog: it holds on to the text past dispose, which is when the popup's
        // backing file goes away.
        String editedText = dialog.getListText().trim();
        if (text.equals(editedText)) {
            if(result) {
                NavigateToFile(project);
            }
            return;
        }
        String newText = editedText.replace("...", projectPath);

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
        NavigateToIndex(project, dialog.SelectedIndex);
    }

    /**
     * Opens the file pinned to {@code index}. Package-private so that what the popup does with the
     * entry the user selected can be tested without showing a modal dialog.
     */
    static void NavigateToIndex(Project project, int index){
        if (project == null) return;
        VirtualFile vf = HarpoonState.GetItem(index, project);
        if (vf == null)
            return;
        var fileManager = FileEditorManager.getInstance(project);
        fileManager.openFile(vf, true);
    }
        
}



