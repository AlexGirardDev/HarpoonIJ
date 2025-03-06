package ca.alexgirard.harpoonIJ;


import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.OptionalInt;


public abstract class RelativeGotoHarpoonActionBase extends AnAction {

    abstract int computeNewIndex(int index, Project project);

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        var project = e.getProject();

        if (project == null) return;

        var fileManager = FileEditorManager.getInstance(project);

        FileEditor selectedEditor = fileManager.getSelectedEditor();
        if (selectedEditor == null) return;
        VirtualFile currentFile = selectedEditor.getFile();
        OptionalInt fileIndexOfCurrentFile = HarpoonState.getFileIndex(project, currentFile);

        fileIndexOfCurrentFile.ifPresentOrElse(
                index -> gotoFileAtIndex(computeNewIndex(index, project), project),
                () -> {
                    int lastSelectedIndex = HarpoonState.getLastSelectedIndex(project);
                    if(lastSelectedIndex == -1) {
                        return;
                    }
                    gotoFileAtIndex(computeNewIndex(lastSelectedIndex, project), project);
                }
        );
    }

  private void gotoFileAtIndex(int index, Project project) {
        var fileManager = FileEditorManager.getInstance(project);
        VirtualFile newFile = HarpoonState.GetItem(index, project);
        if (newFile == null) return;
        fileManager.openFile(newFile, true);
    }
}
