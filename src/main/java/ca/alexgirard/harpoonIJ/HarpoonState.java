package ca.alexgirard.harpoonIJ;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.*;
import java.util.stream.Collectors;

public class HarpoonState {
    private static final Map<String, List<VirtualFile>> FilesMap = new HashMap<>();
    private static final Map<String, Integer> LastHarpoonIndexMap = new HashMap<>();
    private static final String ListPersistenceKey = "HarpoonJumpList";

    public static void SetItem(VirtualFile file, int index, Project project) {
        FillLists(project);

        var Files = FilesMap.getOrDefault(project.getName(), new ArrayList<>());

        if (index >= Files.size()) {
            for (int i = Files.size(); index >= i; i++) {
                Files.add(null);
            }
        }
        Files.set(index, file);
        LastHarpoonIndexMap.put(project.getName(), index);

        var propsComp = PropertiesComponent.getInstance(project);
        var stringLists = Files.stream().filter(virtualFile -> virtualFile != null && virtualFile.isValid()).map(VirtualFile::getPath).toList();
        propsComp.setList(ListPersistenceKey, stringLists);
        FilesMap.put(project.getName(), Files);
    }

    public static VirtualFile GetItem(int index, Project project) {
        FillLists(project);
        var Files = FilesMap.getOrDefault(project.getName(), new ArrayList<>());
        if (index < Files.size()) {
            LastHarpoonIndexMap.put(project.getName(), index);
            return Files.get(index);
        }
        return null;

    }

    private static void FillLists(Project project) {
        if (FilesMap.containsKey(project.getName()))//&& FileStringsMap.containsKey(project.getName()))
            return;
        var files = FilesMap.getOrDefault(project.getName(), new ArrayList<>());
        var propsComp = PropertiesComponent.getInstance(project);
        var list = propsComp.getList(ListPersistenceKey);
        list = (list == null) ? new ArrayList<>() : list;
        var LFS = LocalFileSystem.getInstance();
        files = list.stream().map(LFS::findFileByPath).collect(Collectors.toList());
        FilesMap.put(project.getName(), files);
    }

    public static List<VirtualFile> GetFiles(Project project) {
        FillLists(project);
        return FilesMap.getOrDefault(project.getName(), new ArrayList<>());

    }

    public static OptionalInt getFileIndex(Project project, VirtualFile file) {
        List<VirtualFile> virtualFiles = FilesMap.getOrDefault(project.getName(), List.of());
        int indexOfFile = virtualFiles.indexOf(file);
        if(indexOfFile == -1) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(indexOfFile);
    }

    private static void ClearLists(Project project) {
        FillLists(project);
        if (FilesMap.containsKey(project.getName()))
            FilesMap.get(project.getName()).clear();
    }

    public static void SetFiles(List<String> list, Project project) {
        ClearLists(project);
        var LFS = LocalFileSystem.getInstance();
        var index = 0;

        for (String s : list) {
            if (s == null || s.isBlank())
                continue;
            var vf = LFS.findFileByPath(s);
            if (vf != null) {
                SetItem(vf, index, project);
                index += 1;
            }
        }
    }

    public static void setSelectedIndex(Project project, int selectedIndex) {
        LastHarpoonIndexMap.put(project.getName(), selectedIndex);
    }

    public static int getLastSelectedIndex(Project project) {
        return Optional.ofNullable(LastHarpoonIndexMap.get(project.getName())).orElse(-1);

    }
}

