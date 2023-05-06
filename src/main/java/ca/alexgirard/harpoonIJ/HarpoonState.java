package ca.alexgirard.harpoonIJ;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HarpoonState {
    private static final Map<String, List<VirtualFile>> FilesMap = new HashMap<>();
    private static final Map<String, List<String>> FileStringsMap = new HashMap<>();
    private static final String ListPersistenceKey = "HarpoonJumpList";

    public static void SetItem(VirtualFile file, int index, Project project) {

        FillLists(project);

        var Files = FilesMap.getOrDefault(project.getName(), new ArrayList<>());
        var FileStrings = FileStringsMap.getOrDefault(project.getName(), new ArrayList<>());
        if (index < 0) {
            Files.add(file);
            FileStrings.add(file.getPath());
            return;
        }
        if (index >= Files.size()) {
            for (int i = FileStrings.size(); index >= i; i++) { 
                Files.add(null);
                FileStrings.add("");
            }
        }
        Files.set(index, file);
        var filePath = file == null ? null : file.getPath();
        FileStrings.set(index, filePath);

        var propsComp = PropertiesComponent.getInstance(project);
        propsComp.setList(ListPersistenceKey, FileStrings);
        FilesMap.put(project.getName(), Files);
        FileStringsMap.put(project.getName(), FileStrings);

    }

    public static VirtualFile GetItem(int index, Project project) {
        FillLists(project);
        var Files = FilesMap.getOrDefault(project.getName(), new ArrayList<>());
        if (index < Files.size()) {
            return Files.get(index);
        }
        return null;

    }

    private static void FillLists(Project project) {
        if (FilesMap.containsKey(project.getName()) && FileStringsMap.containsKey(project.getName()))
            return;
        var Files = FilesMap.getOrDefault(project.getName(), new ArrayList<>());
        var FileStrings = FileStringsMap.getOrDefault(project.getName(), new ArrayList<>());
        var propsComp = PropertiesComponent.getInstance(project);
        var list = propsComp.getList(ListPersistenceKey);
        list = (list == null) ? new ArrayList<>() : list;
        var LFS = LocalFileSystem.getInstance();
        Files = list.stream().map(LFS::findFileByPath).collect(Collectors.toList());
        FileStrings = list;
        FilesMap.put(project.getName(), Files);
        FileStringsMap.put(project.getName(), FileStrings);
    }

    public static List<VirtualFile> GetFiles(Project project) {
        FillLists(project);
        return FilesMap.getOrDefault(project.getName(), new ArrayList<>());

    }

    private static void ClearLists(Project project) {
        FillLists(project);
        if (FilesMap.containsKey(project.getName()))
            FilesMap.get(project.getName()).clear();

        if (FileStringsMap.containsKey(project.getName()))
            FileStringsMap.get(project.getName()).clear();
    }

    public static void SetFiles(List<String> list, Project project) {
        ClearLists(project);
        var LFS = LocalFileSystem.getInstance();
        for (int i = 0; i < list.size(); i++) {
            var s = list.get(i);
            if (s == null || s.isBlank())
                SetItem(null, i, project);
            else
                SetItem(LFS.findFileByPath(s), i, project);
        }
    }
}
