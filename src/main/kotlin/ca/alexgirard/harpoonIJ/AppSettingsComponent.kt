// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package ca.alexgirard.harpoonIJ

import com.intellij.ui.JBIntSpinner
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Supports creating and managing a [JPanel] for the Settings Dialog.
 */
class AppSettingsComponent {
    val panel: JPanel
    private val dialogWidth = JBIntSpinner(800, 1, 5000, 10)
    private val dialogHeight = JBIntSpinner(400, 1, 5000, 10)
    private val dialogFontSize = JBIntSpinner(20, 1, 100)

    init {
        panel = FormBuilder.createFormBuilder()
                .addLabeledComponent(JBLabel("Popup width"), dialogWidth, 1, false)
                .addLabeledComponent(JBLabel("Popup height"), dialogHeight, 1, false)
                .addLabeledComponent(JBLabel("Popup font size"), dialogFontSize, 1, false)
                .addComponentFillVertically(JPanel(), 0)
                .panel
    }

    val preferredFocusedComponent: JComponent
        get() = dialogWidth

    fun getDialogWidth(): Int {
        return dialogWidth.number
    }

    fun getDialogHeight(): Int {
        return dialogHeight.number
    }

    fun getDialogFontSize(): Int {
        return dialogFontSize.number
    }

    fun setDialogFontSize(fontSize: Int) {
        dialogFontSize.number = fontSize
    }

    fun setDialogWidth(width: Int) {
        dialogWidth.number = width
    }

    fun setDialogHeight(height: Int) {
        dialogHeight.number = height
    }
}
