package ca.alexgirard.harpoon;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.stream.Collectors;

public class ShowHarpoon extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {

        var stringBuilder = new StringBuilder();
        var fileStrings = HarpoonState.GetFiles(e.getProject());
        for (int i = 0; i < fileStrings.size(); i++) {
            var file = fileStrings.get(i);
            if (file == null) continue;
            var string = file.getCanonicalPath();
            if (string.isBlank()) continue;
            stringBuilder
//                    .append(i).append(" - ")
                    .append(string).append(System.lineSeparator());
        }
        var text = stringBuilder.toString().trim();

        var response = new HarpoonDialog(text,e.getProject()).showAndGet();
//        var popupBuilder = JBPopupFactory.getInstance().createComponentPopupBuilder(GetPanel(e), null);
//        JBPopup popup = popupBuilder.setCancelOnWindowDeactivation(false).createPopup();
//        popup.showInFocusCenter();

    }
//    private EditorTextField textArea = null;
//
//    private JComponent GetPanel(@NotNull AnActionEvent e){
//        if(textArea == null)
//        {
//
//            textArea = new EditorTextField();
//            textArea.setPreferredSize(new Dimension(500, 300));
//            textArea.setFont(JBFont.h1().asBold());
//        }
//        var stringBuilder = new StringBuilder();
//        var fileStrings = HarpoonState.GetFiles(e.getProject());
//        for (int i = 0; i <fileStrings.size() ; i++) {
//            stringBuilder.append(i).append(" - ").append(fileStrings.get(i).getPresentableName()).append(System.lineSeparator());
//        }
//        textArea.setText(stringBuilder.toString());
//        return textArea;
//
//    }
}



