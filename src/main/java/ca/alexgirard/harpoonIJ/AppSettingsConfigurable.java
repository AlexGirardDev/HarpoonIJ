// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package ca.alexgirard.harpoonIJ;

import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Provides controller functionality for application settings.
 */
public class AppSettingsConfigurable implements Configurable {

    private AppSettingsComponent mySettingsComponent;

    // A default constructor with no arguments is required because this implementation
    // is registered as an applicationConfigurable EP

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "HarpoonIJ Settings";
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return mySettingsComponent.getPreferredFocusedComponent();
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        mySettingsComponent = new AppSettingsComponent();
        return mySettingsComponent.getPanel();
    }

    @Override
    public boolean isModified() {
        AppSettingsState settings = AppSettingsState.getInstance();
        boolean modified = mySettingsComponent.getDialogHeight() != settings.dialogHeight;
        modified |= mySettingsComponent.getDialogWidth() != settings.dialogWidth;
        modified |= mySettingsComponent.getDialogFontSize() != settings.dialogFontSize;
        modified |= mySettingsComponent.getForceNormalMode() != settings.dialogForceVimNormalMode;
        return modified;
    }

    @Override
    public void apply() {
        AppSettingsState settings = AppSettingsState.getInstance();
        settings.dialogWidth = mySettingsComponent.getDialogWidth();
        settings.dialogHeight = mySettingsComponent.getDialogHeight();
        settings.dialogFontSize = mySettingsComponent.getDialogFontSize();
        settings.dialogForceVimNormalMode = mySettingsComponent.getForceNormalMode();
    }

    @Override
    public void reset() {
        AppSettingsState settings = AppSettingsState.getInstance();
        mySettingsComponent.setDialogWidth(settings.dialogWidth);
        mySettingsComponent.setDialogHeight(settings.dialogHeight);
        mySettingsComponent.setDialogFontSize(settings.dialogFontSize);
        mySettingsComponent.setForceVimNormalMode(settings.dialogForceVimNormalMode);
    }

    @Override
    public void disposeUIResources() {
        mySettingsComponent = null;
    }

}
