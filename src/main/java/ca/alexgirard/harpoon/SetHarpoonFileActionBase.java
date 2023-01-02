package ca.alexgirard.harpoon;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public abstract class SetHarpoonFileActionBase extends AnAction {

    public abstract int getIndex();

    public SetHarpoonFileActionBase() {
        super();
    }
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        VirtualFile vFile = e.getData(PlatformDataKeys.VIRTUAL_FILE);
        HarpoonState.SetItem(vFile,getIndex(),e.getProject());
    }
}
