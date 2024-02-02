package ca.alexgirard.harpoonIJ;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public abstract class GoToHarpoonActionBase extends AnAction {

    public abstract  int getIndex();

    public GoToHarpoonActionBase() {
        super();
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        var project = e.getProject();

        if (project == null) return;
        VirtualFile vf = HarpoonState.GetItem(getIndex(), project);
        if (vf == null || !vf.isValid() )
            return;
        
        var fileManager = FileEditorManager.getInstance(project);
        fileManager.openFile(vf, true);
    }
}
