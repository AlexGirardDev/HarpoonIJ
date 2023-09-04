package ca.alexgirard.harpoonIJ;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class SelectHarpoonItem extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        ShowHarpoon.SelectHarpoonItem();
    }
}
