// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package ca.alexgirard.harpoonIJ

import com.intellij.openapi.options.Configurable
import org.jetbrains.annotations.Nls
import javax.swing.JComponent

/**
 * Provides controller functionality for application settings.
 */
class AppSettingsConfigurable : Configurable {
    private lateinit var mySettingsComponent: AppSettingsComponent;

    // A default constructor with no arguments is required because this implementation
    // is registered as an applicationConfigurable EP
    override fun getDisplayName(): @Nls(capitalization = Nls.Capitalization.Title) String? {
        return "HarpoonIJ Settings"
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return mySettingsComponent.preferredFocusedComponent
    }

    override fun createComponent(): JComponent {
        mySettingsComponent = AppSettingsComponent()
        return mySettingsComponent.panel;

    }

    override fun isModified(): Boolean {
        val settings: AppSettingsState = AppSettingsState.Companion.instance
        var modified = mySettingsComponent.getDialogHeight() != settings.dialogHeight
        modified = modified or (mySettingsComponent.getDialogWidth() != settings.dialogWidth)
        modified = modified or (mySettingsComponent.getDialogFontSize() != settings.dialogFontSize)
        return modified
    }

    override fun apply() {
        val settings: AppSettingsState = AppSettingsState.Companion.instance;
        settings.dialogWidth = mySettingsComponent.getDialogWidth();
        settings.dialogHeight = mySettingsComponent.getDialogHeight();
        settings.dialogFontSize = mySettingsComponent.getDialogFontSize()
    }

    override fun reset() {
        val settings: AppSettingsState = AppSettingsState.Companion.instance
        mySettingsComponent.setDialogWidth(settings.dialogWidth)
        mySettingsComponent.setDialogHeight(settings.dialogHeight)
        mySettingsComponent.setDialogFontSize(settings.dialogFontSize)
    }

    override fun disposeUIResources() {
    }
}
