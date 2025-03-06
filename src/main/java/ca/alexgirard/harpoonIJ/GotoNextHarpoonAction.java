package ca.alexgirard.harpoonIJ;


import com.intellij.openapi.project.Project;

public class GotoNextHarpoonAction extends RelativeGotoHarpoonActionBase {

    @Override
    int computeNewIndex(int index, Project project) {
        int newIndex = index + 1;
        int indexOfLastFileInList = HarpoonState.GetFiles(project).size() - 1;
        return Math.min(newIndex, indexOfLastFileInList);
    }
}
