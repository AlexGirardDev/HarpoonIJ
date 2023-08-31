package ca.alexgirard.harpoonIJ

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.util.stream.Collectors

object HarpoonState {
    private val FilesMap: MutableMap<String, MutableList<VirtualFile?>> = HashMap()
    private val FileStringsMap: MutableMap<String, MutableList<String?>> = HashMap()
    private const val ListPersistenceKey = "HarpoonJumpList"
    fun setItem(file: VirtualFile?, index: Int, project: Project) {
        FillLists(project)
        val Files = FilesMap.getOrDefault(project.name, ArrayList())
        val FileStrings = FileStringsMap.getOrDefault(project.name, ArrayList())
        if (index < 0) {
            Files.add(file)
            FileStrings.add(file!!.path)
            return
        }
        if (index >= Files.size) {
            var i = FileStrings.size
            while (index >= i) {
                Files.add(null)
                FileStrings.add("")
                i++
            }
        }
        Files[index] = file
        val filePath = file?.path
        FileStrings[index] = filePath
        val propsComp = PropertiesComponent.getInstance(project)
        propsComp.setList(ListPersistenceKey, FileStrings)
        FilesMap[project.name] = Files
        FileStringsMap[project.name] = FileStrings
    }

    fun GetItem(index: Int, project: Project): VirtualFile? {
        FillLists(project)
        val Files: List<VirtualFile?> = FilesMap.getOrDefault(project.name, ArrayList())
        return if (index < Files.size) {
            Files[index]
        } else null
    }

    private fun FillLists(project: Project) {
        if (FilesMap.containsKey(project.name) && FileStringsMap.containsKey(project.name)) return
        var Files: MutableList<VirtualFile?>
        var FileStrings: MutableList<String?>? = FileStringsMap.getOrDefault(project.name, ArrayList())
        val propsComp = PropertiesComponent.getInstance(project)
        var list = propsComp.getList(ListPersistenceKey)
        list = list ?: ArrayList()
        val LFS = LocalFileSystem.getInstance()
        Files = list.stream().map { path: String? -> LFS.findFileByPath(path!!) }.collect(Collectors.toList())
        FileStrings = list
        FilesMap[project.name] = Files
        FileStringsMap[project.name] = FileStrings
    }

    @JvmStatic
    fun files(project: Project): List<VirtualFile?> {
        FillLists(project)
        return FilesMap.getOrDefault(project.name, ArrayList())
    }

    private fun clearLists(project: Project) {
        FillLists(project)
        if (FilesMap.containsKey(project.name)) FilesMap[project.name]!!.clear()
        if (FileStringsMap.containsKey(project.name)) FileStringsMap[project.name]!!.clear()
    }

    fun setFiles(list: List<String?>, project: Project) {
        clearLists(project)
        val LFS = LocalFileSystem.getInstance()
        for (i in list.indices) {
            val s = list[i]
            if (s.isNullOrBlank()) setItem(null, i, project) else setItem(LFS.findFileByPath(s), i, project)
        }
    }
}
