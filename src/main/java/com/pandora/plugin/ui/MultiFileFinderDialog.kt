/*
 * Copyright 2019 Pandora Media, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * See accompanying LICENSE file or you may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pandora.plugin.ui

import com.intellij.openapi.ui.InputValidator
import com.intellij.openapi.ui.Messages
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import java.awt.BorderLayout
import javax.swing.Icon
import javax.swing.JCheckBox
import javax.swing.JPanel
import javax.swing.JTextArea

class MultiFileFinderDialog(message: String,
                            @Nls(capitalization = Nls.Capitalization.Title) title: String,
                            checkboxText: String,
                            checked: Boolean,
                            checkboxEnabled: Boolean,
                            icon: Icon?,
                            initialValue: String?,
                            validator: InputValidator? = null
) : Messages.InputDialog(message, title, icon, initialValue, validator) {
    private lateinit var checkBox: JCheckBox

    val isChecked: Boolean?
        get() = checkBox.isSelected

    init {
        checkBox.text = checkboxText
        checkBox.isSelected = checked
        checkBox.isEnabled = checkboxEnabled
    }

    override fun createTextFieldComponent() = JTextArea(7, 50)

    override fun createMessagePanel(): JPanel {
        val messagePanel = JPanel(BorderLayout())
        if (myMessage != null) {
            val textComponent = createTextComponent()
            messagePanel.add(textComponent, BorderLayout.NORTH)
        }

        myField = createTextFieldComponent()
        messagePanel.add(myField, BorderLayout.CENTER)

        checkBox = JCheckBox()
        messagePanel.add(checkBox, BorderLayout.SOUTH)

        return messagePanel
    }

    companion object {
        fun showInputDialogWithCheckBox(message: String,
                                        @Nls(capitalization = Nls.Capitalization.Title) title: String,
                                        checkboxText: String,
                                        checked: Boolean,
                                        checkboxEnabled: Boolean,
                                        icon: Icon?,
                                        @NonNls initialValue: String,
                                        validator: InputValidator?): Pair<String, Boolean> {
            val dialog = MultiFileFinderDialog(
                    message = message,
                    title = title,
                    checkboxText = checkboxText,
                    checked = checked,
                    checkboxEnabled = checkboxEnabled,
                    icon = icon,
                    initialValue = initialValue,
                    validator = validator)
            dialog.show()
            return Pair(if (dialog.isOK) dialog.inputString ?: "" else "", dialog.isChecked == true)
        }
    }
}