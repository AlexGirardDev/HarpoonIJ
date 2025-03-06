package ca.alexgirard.harpoonIJ;


import com.intellij.openapi.project.Project;

public class GotoPreviousHarpoonAction extends RelativeGotoHarpoonActionBase {

    @Override
    int computeNewIndex(int index, Project project) {
        int newIndex = index - 1;
        return Math.max(newIndex, 0);
    }
}
