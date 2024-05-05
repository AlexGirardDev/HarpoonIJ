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

    private HarpoonIjSettingsComponent harpoonSettings;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "HarpoonIJ Settings";
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return harpoonSettings.getPreferredFocusedComponent();
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        harpoonSettings = new HarpoonIjSettingsComponent();
        return harpoonSettings.getPanel();
    }

    @Override
    public boolean isModified() {
        AppSettingsState settings = AppSettingsState.getInstance();
        boolean modified = harpoonSettings.getDialogHeight() != settings.dialogHeight;
        //TODO cleanup in kotlin rewrite
        modified |= harpoonSettings.getDialogWidth() != settings.dialogWidth;
        modified |= harpoonSettings.getDialogFontSize() != settings.dialogFontSize;
        modified |= harpoonSettings.getRemapEnter() != settings.enterRemap;
        return modified;
    }

    @Override
    public void apply() {
        AppSettingsState settings = AppSettingsState.getInstance();
        settings.dialogWidth = harpoonSettings.getDialogWidth();
        settings.dialogHeight = harpoonSettings.getDialogHeight();
        settings.dialogFontSize = harpoonSettings.getDialogFontSize();
        settings.enterRemap = harpoonSettings.getRemapEnter();
    }

    @Override
    public void reset() {
        AppSettingsState settings = AppSettingsState.getInstance();
        harpoonSettings.setDialogWidth(settings.dialogWidth);
        harpoonSettings.setDialogHeight(settings.dialogHeight);
        harpoonSettings.setDialogFontSize(settings.dialogFontSize);
        harpoonSettings.setRemapEnter(settings.enterRemap);
    }

    @Override
    public void disposeUIResources() {
        harpoonSettings = null;
    }

}
