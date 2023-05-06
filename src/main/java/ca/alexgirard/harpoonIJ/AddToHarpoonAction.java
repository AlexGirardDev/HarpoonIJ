package ca.alexgirard.harpoonIJ;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class AddToHarpoonAction extends AnAction {


    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {

        var files = HarpoonState.GetFiles(e.getProject());
        var numOfFiles = files.size();
        var insertIndex = numOfFiles;
        VirtualFile vFile = e.getData(PlatformDataKeys.VIRTUAL_FILE);
        if(files.stream().anyMatch(x->x!= null && x.getPath().equals(vFile != null ? vFile.getPath() : null)))
            return;
        for (int i = 0; i <numOfFiles ; i++) {
            if (HarpoonState.GetItem(i, e.getProject()) == null) {
                insertIndex = i;
                break;
            }
        }
        HarpoonState.SetItem(vFile,insertIndex,e.getProject());
    }
}
