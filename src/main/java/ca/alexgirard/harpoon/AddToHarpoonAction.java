package ca.alexgirard.harpoon;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class AddToHarpoonAction extends AnAction {


    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {

        var numOfFiles = HarpoonState.GetFiles(e.getProject()).size();
        var insertIndex = numOfFiles;
        VirtualFile vFile = e.getData(PlatformDataKeys.VIRTUAL_FILE);
        for (int i = 0; i <numOfFiles ; i++) {
            if (HarpoonState.GetItem(i, e.getProject()) == null) {
                insertIndex = i;
                break;
            }
        }
        HarpoonState.SetItem(vFile,insertIndex,e.getProject());
    }
}
