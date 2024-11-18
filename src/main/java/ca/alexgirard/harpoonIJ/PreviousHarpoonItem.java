package ca.alexgirard.harpoonIJ;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class PreviousHarpoonItem extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        ShowHarpoon.PreviousHarpoonItem();
    }
}
