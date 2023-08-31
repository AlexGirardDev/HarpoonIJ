package ca.alexgirard.harpoonIJ

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager

abstract class GoToHarpoonActionBase : AnAction() {
    abstract val index: Int
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val vf = HarpoonState.GetItem(index, project) ?: return
        val fileManager = FileEditorManager.getInstance(project)
        fileManager.openFile(vf, true)
    }
}
