// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package ca.alexgirard.harpoonIJ;

import com.intellij.ui.JBIntSpinner;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Supports creating and managing a {@link JPanel} for the Settings Dialog.
 */
public class AppSettingsComponent {

    private final JPanel myMainPanel;
    private final JBIntSpinner dialogWidth = new JBIntSpinner(800,1,5000, 10);
    private final JBIntSpinner dialogHeight = new JBIntSpinner(400,1,5000,10);
    private final JBIntSpinner dialogFontSize = new JBIntSpinner(20,1,100);

    public AppSettingsComponent() {
        myMainPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("Popup width"), dialogWidth, 1, false)
                .addLabeledComponent(new JBLabel("Popup height"), dialogHeight, 1, false)
                .addLabeledComponent(new JBLabel("Popup font size"), dialogFontSize, 1, false)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }

    public JPanel getPanel() {
        return myMainPanel;
    }

    public JComponent getPreferredFocusedComponent() {
        return dialogWidth;
    }

    @NotNull
    public int getDialogWidth() {
        return dialogWidth.getNumber();
    }

    @NotNull
    public int getDialogHeight() {
        return dialogHeight.getNumber();
    }

    @NotNull
    public int getDialogFontSize() {
        return dialogFontSize.getNumber();
    }

    public void setDialogFontSize(int fontSize) {
        dialogFontSize.setNumber(fontSize);

    }
    public void setDialogWidth(int width) {
        dialogWidth.setNumber(width);
        
    }
    public void setDialogHeight(int height) {
        dialogHeight.setNumber(height);
        
    }

}
