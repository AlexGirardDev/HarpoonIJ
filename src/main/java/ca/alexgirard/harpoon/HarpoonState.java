package ca.alexgirard.harpoon;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class HarpoonState {
    private static List<VirtualFile> Files;
    private static List<String> FileStrings;
    private static final String ListPersistanceKey = "HarpoonJumpList";

    public static void SetItem(VirtualFile file, int index, Project project) {
        if (Files == null) {
            FillLists(project);
        }
        if (index < 0) {
            Files.add(file);
            FileStrings.add(file.getPath());
            return;
        }
        if (index >= Files.size()) {
            for (int i = FileStrings.size();  index >= i; i++) {
                Files.add(null);
                FileStrings.add(null);
            }
        }
        Files.set(index, file);
        var filePath = file == null ?null: file.getPath();
        FileStrings.set(index, filePath);

        var propsComp = PropertiesComponent.getInstance(project);
        propsComp.setList(ListPersistanceKey, FileStrings);

    }

    public static VirtualFile GetItem(int index, Project project) {
        if (Files == null) {
            FillLists(project);
        }
        if (index < Files.size()) {
            return Files.get(index);
        }
        return null;

    }
    public static void FillLists(Project project)
    {
        var propsComp = PropertiesComponent.getInstance(project);
        var list = propsComp.getList(ListPersistanceKey);
        list = (list == null) ? new ArrayList<>() : list;
        var LFS = LocalFileSystem.getInstance();
        Files = list.stream().map(LFS::findFileByPath).collect(Collectors.toList());
        FileStrings = list;
    }

    public static List<String> GetStrings(Project project) {
        if (Files == null) {
            FillLists(project);
        }
        return FileStrings;

    }
    public static List<VirtualFile> GetFiles(Project project) {
        if (Files == null) {
            FillLists(project);
        }
        return Files;

    }
}
