package ca.alexgirard.harpoonIJ

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class ShowHarpoon : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return;
        val stringBuilder = StringBuilder()
        val fileStrings = HarpoonState.files(project)
        var projectPath = project.basePath
        projectPath = projectPath ?: ""
        for (vFile in fileStrings) {
            var path = if (vFile == null) "" else vFile.canonicalPath
            path = path ?: ""
            stringBuilder.append(path.replace(projectPath, "...")).append("\n")
        }
        val text = stringBuilder.toString().trim { it <= ' ' }
        val dialog = HarpoonDialog(text)
        dialog.showAndGet()
        val newText = dialog.editorTextField!!.getText().trim { it <= ' ' }.replace("...", projectPath)
        if (text == newText) return
        val lines = newText.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val outputList = ArrayList<String?>()
        for (line in lines) {
            outputList.add(line.trim { it <= ' ' })
        }
        HarpoonState.setFiles(outputList, project)
    }
}
