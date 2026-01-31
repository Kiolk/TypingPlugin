package com.github.kiolk.typingplugin.actions

import com.github.kiolk.typingplugin.ui.TypingDialog
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

class StartTypingAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return

        // Check for selection
        val selectionModel = editor.selectionModel
        val textToType =
            if (selectionModel.hasSelection()) {
                selectionModel.selectedText ?: editor.document.text
            } else {
                editor.document.text
            }

        // Clean up the selected text:
        // If we select a middle block, we might want to trim initial common indentation
        val cleanedText = textToType.trimIndent()

        val typingDialog = TypingDialog(project, cleanedText)
        typingDialog.show()
    }

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabledAndVisible = editor != null

        // Optional: Update text based on selection
        if (editor?.selectionModel?.hasSelection() == true) {
            e.presentation.text = "Type Selected Area"
        } else {
            e.presentation.text = "Type This Class"
        }
    }
}
