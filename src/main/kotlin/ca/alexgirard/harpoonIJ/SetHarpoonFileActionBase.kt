package ca.alexgirard.harpoonIJ

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys

abstract class SetHarpoonFileActionBase : AnAction() {
    abstract val index: Int
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project?: return;
        val vFile = e.getData(PlatformDataKeys.VIRTUAL_FILE)
        HarpoonState.setItem(vFile, index, project)
    }
}
