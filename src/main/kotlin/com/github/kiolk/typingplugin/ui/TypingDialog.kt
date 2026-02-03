package com.github.kiolk.typingplugin.ui

import com.github.kiolk.typingplugin.service.TypingService
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Point
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextPane
import javax.swing.SwingUtilities
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants

class TypingDialog(private val project: Project, private val sourceCode: String) : DialogWrapper(project) {
    private var currentIndex = 0
    private var errorCount = 0
    private var startTime: Long = 0
    private val textPane = JTextPane()
    private val skippedIndices = mutableSetOf<Int>()
    private val LOG = logger<TypingDialog>()

    // Styles
    private val ghostAttributes =
        SimpleAttributeSet().apply {
            StyleConstants.setForeground(this, Color.GRAY)
        }
    private val correctAttributes =
        SimpleAttributeSet().apply {
            StyleConstants.setForeground(this, Color.GREEN)
        }
    private val wrongAttributes =
        SimpleAttributeSet().apply {
            StyleConstants.setForeground(this, Color.RED)
            StyleConstants.setUnderline(this, true)
        }
    private val cursorAttributes =
        SimpleAttributeSet().apply {
            StyleConstants.setBackground(this, Color.LIGHT_GRAY)
            StyleConstants.setForeground(this, Color.BLACK)
        }

    init {
        title = "Typing Training"
        isModal = false
        init()
        LOG.info("TypingDialog initialized with source code length: ${sourceCode.length}")
    }

    override fun getDimensionServiceKey(): String? = "com.github.kiolk.typingplugin.ui.TypingDialog"

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())

        val dragListener =
            object : MouseAdapter() {
                private var initialScreenClick: Point? = null
                private var initialWindowLocation: Point? = null

                override fun mousePressed(e: MouseEvent) {
                    initialScreenClick = e.locationOnScreen
                    initialWindowLocation = SwingUtilities.getWindowAncestor(panel)?.location
                }

                override fun mouseDragged(e: MouseEvent) {
                    val window = SwingUtilities.getWindowAncestor(panel)
                    if (window != null && initialScreenClick != null && initialWindowLocation != null) {
                        val deltaX = e.locationOnScreen.x - initialScreenClick!!.x
                        val deltaY = e.locationOnScreen.y - initialScreenClick!!.y
                        window.setLocation(initialWindowLocation!!.x + deltaX, initialWindowLocation!!.y + deltaY)
                    }
                }
            }

        panel.addMouseListener(dragListener)
        panel.addMouseMotionListener(dragListener)

        textPane.apply {
            text = sourceCode
            isEditable = false
            background = EditorColorsManager.getInstance().globalScheme.defaultBackground
            font = EditorColorsManager.getInstance().globalScheme.getFont(EditorFontType.PLAIN)

            StyleConstants.setBackground(ghostAttributes, background)
            StyleConstants.setBackground(correctAttributes, background)
            StyleConstants.setBackground(wrongAttributes, background)

            // Initial setup
            textPane.styledDocument.setCharacterAttributes(0, sourceCode.length, ghostAttributes, true)
            updateCursor()

            addKeyListener(
                object : KeyAdapter() {
                    override fun keyTyped(e: KeyEvent) {
                        LOG.info("Key typed: '${e.keyChar}' (code: ${e.keyChar.code})")
                        if (e.keyChar.code < 32 || e.keyChar.code == 127) return
                        if (startTime == 0L) {
                            startTime = System.currentTimeMillis()
                            LOG.info("Session started at $startTime")
                        }
                        handleTyping(e.keyChar)
                    }

                    override fun keyPressed(e: KeyEvent) {
                        LOG.debug("Key pressed: code=${e.keyCode}")
                        when (e.keyCode) {
                            KeyEvent.VK_BACK_SPACE, KeyEvent.VK_DELETE, KeyEvent.VK_CLEAR -> {
                                handleBackspace()
                            }
                            KeyEvent.VK_ENTER -> {
                                if (startTime == 0L) {
                                    startTime = System.currentTimeMillis()
                                    LOG.info("Session started at $startTime (via Enter)")
                                }
                                handleTyping('\n')
                            }
                        }
                    }
                },
            )
            addMouseListener(dragListener)
            addMouseMotionListener(dragListener)
        }

        val scrollPane = JBScrollPane(textPane)
        scrollPane.addMouseListener(dragListener)
        scrollPane.addMouseMotionListener(dragListener)
        panel.add(scrollPane, BorderLayout.CENTER)
        panel.preferredSize = java.awt.Dimension(800, 600)

        return panel
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return textPane
    }

    private fun handleTyping(charTyped: Char) {
        if (currentIndex >= sourceCode.length) return

        val targetChar = sourceCode[currentIndex]
        val isNewlineMatch = (targetChar == '\n' || targetChar == '\r') && (charTyped == '\n' || charTyped == '\r')

        if (charTyped == targetChar || isNewlineMatch) {
            textPane.styledDocument.setCharacterAttributes(currentIndex, 1, correctAttributes, true)

            if (targetChar == '\r' && currentIndex + 1 < sourceCode.length && sourceCode[currentIndex + 1] == '\n') {
                currentIndex += 2
            } else {
                currentIndex++
            }

            if (isNewlineMatch) {
                skipLeadingWhitespace()
            }
            updateCursor()
        } else {
            errorCount++
            LOG.debug("Typing error at index $currentIndex: expected '$targetChar', got '$charTyped'. Total errors: $errorCount")
            val errorStyle = SimpleAttributeSet(wrongAttributes)
            StyleConstants.setBackground(errorStyle, Color.LIGHT_GRAY)
            textPane.styledDocument.setCharacterAttributes(currentIndex, 1, errorStyle, true)
        }

        if (currentIndex >= sourceCode.length) {
            LOG.info("Typing finished. Recording statistics.")
            recordAndShowStatistics()
            close(OK_EXIT_CODE)
        }
    }

    private fun handleBackspace() {
        val attrs = textPane.styledDocument.getCharacterElement(currentIndex).attributes
        val isWrong = StyleConstants.getForeground(attrs) == Color.RED

        if (isWrong) {
            resetToGhost(currentIndex)
        } else if (currentIndex > 0) {
            resetToGhost(currentIndex)
            currentIndex--

            while (currentIndex >= 0 && (skippedIndices.contains(currentIndex) || isPartOfNewline(currentIndex))) {
                resetToGhost(currentIndex)
                skippedIndices.remove(currentIndex)
                if (currentIndex == 0) break
                currentIndex--
            }
            resetToGhost(currentIndex)
        }
        updateCursor()
    }

    private fun isPartOfNewline(index: Int): Boolean = sourceCode[index] == '\n' || sourceCode[index] == '\r'

    private fun resetToGhost(index: Int) {
        if (index >= 0 && index < sourceCode.length) {
            textPane.styledDocument.setCharacterAttributes(index, 1, ghostAttributes, true)
        }
    }

    private fun skipLeadingWhitespace() {
        while (currentIndex < sourceCode.length && (sourceCode[currentIndex] == ' ' || sourceCode[currentIndex] == '\t')) {
            skippedIndices.add(currentIndex)
            textPane.styledDocument.setCharacterAttributes(currentIndex, 1, correctAttributes, true)
            currentIndex++
        }
    }

    private fun updateCursor() {
        if (currentIndex < sourceCode.length) {
            val attrs = textPane.styledDocument.getCharacterElement(currentIndex).attributes
            val isWrong = StyleConstants.getForeground(attrs) == Color.RED

            val style =
                if (isWrong) {
                    val s = SimpleAttributeSet(wrongAttributes)
                    StyleConstants.setBackground(s, Color.LIGHT_GRAY)
                    s
                } else {
                    cursorAttributes
                }

            textPane.styledDocument.setCharacterAttributes(currentIndex, 1, style, true)
            textPane.caretPosition = currentIndex
        }
    }

    private fun recordAndShowStatistics() {
        val endTime = System.currentTimeMillis()
        val totalTimeSeconds = if (startTime != 0L) (endTime - startTime) / 1000.0 else 0.0
        val totalTimeMinutes = totalTimeSeconds / 60.0
        val totalChars = sourceCode.length
        val wpm = if (totalTimeMinutes > 0) (totalChars / 5.0) / totalTimeMinutes else 0.0
        val accuracy =
            if (totalChars + errorCount > 0) {
                (totalChars.toDouble() / (totalChars + errorCount)) * 100
            } else {
                0.0
            }
        val epm = if (totalTimeMinutes > 0) errorCount / totalTimeMinutes else 0.0

        val service = TypingService.getInstance(project)
        service.addResult(wpm, epm, accuracy)
        
        val results = service.getResults()
        val latest = results.last()
        LOG.info("Session Result: Attempt #${latest.attemptNumber}, WPM: ${"%.1f".format(latest.wpm)}, EPM: ${"%.1f".format(latest.errorsPerMinute)}, Accuracy: ${"%.1f".format(latest.accuracy)}%")

        val timeFormatted = formatTime(totalTimeSeconds)
        val statsMessage = "Typing Finished!\n\nTime: $timeFormatted\nWPM: ${"%.1f".format(
            wpm,
        )}\nAccuracy: ${"%.1f".format(accuracy)}%\nErrors: $errorCount"

        StatisticsDialog(project, results, statsMessage).show()
    }

    companion object {
        fun formatTime(totalTimeSeconds: Double): String {
            if (totalTimeSeconds >= 60) {
                val minutes = totalTimeSeconds.toInt() / 60
                val seconds = totalTimeSeconds % 60
                return "${minutes}m ${"%.1f".format(seconds)}s"
            }
            return "${"%.1f".format(totalTimeSeconds)}s"
        }
    }
}
