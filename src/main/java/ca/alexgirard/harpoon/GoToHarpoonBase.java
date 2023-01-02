package ca.alexgirard.harpoon;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public abstract class GoToHarpoonBase extends AnAction {

    public abstract  int getIndex();

    public GoToHarpoonBase() {
        super();
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        var project = e.getProject();
        if (project == null) return;
        VirtualFile vf = HarpoonState.GetItem(getIndex(), project);
        if (vf == null)
            return;
        var fileManager = FileEditorManager.getInstance(project);
        fileManager.openFile(vf, true);
    }
}
