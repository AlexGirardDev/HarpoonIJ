package ca.alexgirard.harpoonIJ

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.vfs.VirtualFile

class AddToHarpoonAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return;
        val files = HarpoonState.files(project)
        val numOfFiles = files.size
        var insertIndex = numOfFiles
        val vFile = e.getData(PlatformDataKeys.VIRTUAL_FILE)
        if (files.stream().anyMatch { x: VirtualFile? -> x != null && x.path == vFile?.path } || vFile == null) return
        for (i in 0 until numOfFiles) {
            if (HarpoonState.GetItem(i, project) == null) {
                insertIndex = i
                break
            }
        }
        HarpoonState.setItem(vFile, insertIndex, project)
    }
}
