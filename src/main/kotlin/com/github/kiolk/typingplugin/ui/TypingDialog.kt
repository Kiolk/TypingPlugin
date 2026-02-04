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
import java.awt.Dimension
import java.awt.Font
import java.awt.GraphicsEnvironment
import java.awt.Point
import java.awt.Rectangle
import java.awt.Toolkit
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextPane
import javax.swing.SwingUtilities
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants
import kotlin.math.max
import kotlin.math.min

class TypingDialog(private val project: Project, private val sourceCode: String) : DialogWrapper(project) {
    private var currentIndex = 0
    private var errorCount = 0
    private var startTime: Long = 0
    private val textPane = JTextPane()
    private val skippedIndices = mutableSetOf<Int>()
    private val log = logger<TypingDialog>()
    private val centerPanel = JPanel(BorderLayout())
    private val typingService = TypingService.getInstance(project)

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
        log.info("TypingDialog initialized with source code length: ${sourceCode.length}")
    }

    override fun getDimensionServiceKey(): String? = null // Disable saving dimension to allow auto-resize based on content

    override fun createCenterPanel(): JComponent {
        val dragListener =
            object : MouseAdapter() {
                private var initialScreenClick: Point? = null
                private var initialWindowLocation: Point? = null

                override fun mousePressed(e: MouseEvent) {
                    if (GraphicsEnvironment.isHeadless()) return
                    initialScreenClick = e.locationOnScreen
                    initialWindowLocation = SwingUtilities.getWindowAncestor(centerPanel)?.location
                }

                override fun mouseDragged(e: MouseEvent) {
                    if (GraphicsEnvironment.isHeadless()) return
                    val window = SwingUtilities.getWindowAncestor(centerPanel)
                    if (window != null && initialScreenClick != null && initialWindowLocation != null) {
                        val deltaX = e.locationOnScreen.x - initialScreenClick!!.x
                        val deltaY = e.locationOnScreen.y - initialScreenClick!!.y
                        window.setLocation(initialWindowLocation!!.x + deltaX, initialWindowLocation!!.y + deltaY)
                    }
                }
            }

        centerPanel.addMouseListener(dragListener)
        centerPanel.addMouseMotionListener(dragListener)

        textPane.apply {
            text = sourceCode
            isEditable = false
            background = EditorColorsManager.getInstance().globalScheme.defaultBackground

            // Load saved font size or use default
            val savedSize = typingService.getFontSize()
            val baseFont = EditorColorsManager.getInstance().globalScheme.getFont(EditorFontType.PLAIN)
            font = Font(baseFont.name, baseFont.style, savedSize)

            StyleConstants.setBackground(ghostAttributes, background)
            StyleConstants.setBackground(correctAttributes, background)
            StyleConstants.setBackground(wrongAttributes, background)

            // Initial setup
            textPane.styledDocument.setCharacterAttributes(0, sourceCode.length, ghostAttributes, true)
            updateCursor()

            addKeyListener(
                object : KeyAdapter() {
                    override fun keyTyped(e: KeyEvent) {
                        log.info("Key typed: '${e.keyChar}' (code: ${e.keyChar.code})")
                        if (e.keyChar.code < 32 || e.keyChar.code == 127) return
                        if (startTime == 0L) {
                            startTime = System.currentTimeMillis()
                            log.info("Session started at $startTime")
                        }
                        handleTyping(e.keyChar)
                    }

                    override fun keyPressed(e: KeyEvent) {
                        log.debug("Key pressed: code=${e.keyCode}")
                        when (e.keyCode) {
                            KeyEvent.VK_BACK_SPACE, KeyEvent.VK_DELETE, KeyEvent.VK_CLEAR -> {
                                handleBackspace()
                            }
                            KeyEvent.VK_ENTER -> {
                                if (startTime == 0L) {
                                    startTime = System.currentTimeMillis()
                                    log.info("Session started at $startTime (via Enter)")
                                }
                                handleTyping('\n')
                            }
                        }
                    }
                },
            )

            addMouseWheelListener { e ->
                if (e.isControlDown) {
                    handleZoom(e)
                } else {
                    // Pass to parent if not zooming
                    parent?.dispatchEvent(e)
                }
            }

            addMouseListener(dragListener)
            addMouseMotionListener(dragListener)
        }

        val scrollPane = JBScrollPane(textPane)
        scrollPane.addMouseListener(dragListener)
        scrollPane.addMouseMotionListener(dragListener)
        centerPanel.add(scrollPane, BorderLayout.CENTER)

        updateWindowSize()

        return centerPanel
    }

    private fun handleZoom(e: MouseWheelEvent) {
        val currentFont = textPane.font
        val newSize = if (e.wheelRotation < 0) currentFont.size + 1 else max(8, currentFont.size - 1)

        if (newSize != currentFont.size) {
            textPane.font = Font(currentFont.name, currentFont.style, newSize)

            // Persist the new font size
            typingService.setFontSize(newSize)

            val window = SwingUtilities.getWindowAncestor(centerPanel)
            if (window != null) {
                val oldSize = window.size
                val oldLocation = window.location

                updateWindowSize()

                val newPreferredSize = centerPanel.preferredSize
                val decorationWidth = oldSize.width - centerPanel.width
                val decorationHeight = oldSize.height - centerPanel.height

                val newWidth = newPreferredSize.width + decorationWidth
                val newHeight = newPreferredSize.height + decorationHeight

                window.setSize(newWidth, newHeight)
                window.setLocation(
                    oldLocation.x - (newWidth - oldSize.width) / 2,
                    oldLocation.y - (newHeight - oldSize.height) / 2,
                )

                centerPanel.revalidate()
                window.validate()
                window.repaint()

                // Ensure cursor is still visible and has padding after zoom
                updateCursor()
            }
        }
    }

    private fun updateWindowSize() {
        val metrics = textPane.getFontMetrics(textPane.font)
        val lines = sourceCode.lines()
        val maxLineWidth = lines.maxOfOrNull { metrics.stringWidth(it) } ?: 0
        val totalHeight = lines.size * metrics.height

        val screenSize =
            if (GraphicsEnvironment.isHeadless()) {
                Dimension(800, 600)
            } else {
                Toolkit.getDefaultToolkit().screenSize
            }
        val maxAvailableWidth = (screenSize.width * 0.9).toInt()
        val maxAvailableHeight = (screenSize.height * 0.9).toInt()

        val preferredWidth = min(max(maxLineWidth + 60, 400), maxAvailableWidth)
        val preferredHeight = min(max(totalHeight + 60, 200), maxAvailableHeight)

        centerPanel.preferredSize = Dimension(preferredWidth, preferredHeight)
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
            log.debug("Typing error at index $currentIndex: expected '$targetChar', got '$charTyped'. Total errors: $errorCount")
            val errorStyle = SimpleAttributeSet(wrongAttributes)
            StyleConstants.setBackground(errorStyle, Color.LIGHT_GRAY)
            textPane.styledDocument.setCharacterAttributes(currentIndex, 1, errorStyle, true)
        }

        if (currentIndex >= sourceCode.length) {
            log.info("Typing finished. Recording statistics.")
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

            // Auto-scroll to keep cursor visible with padding of at least 3 characters
            try {
                val rect = textPane.modelToView(currentIndex)
                if (rect != null) {
                    val metrics = textPane.getFontMetrics(textPane.font)
                    // Calculate padding based on 3 widest characters ('W') and line height
                    val horizontalPadding = metrics.stringWidth("WWW")
                    val verticalPadding = metrics.height

                    val paddedRect =
                        Rectangle(
                            rect.x - horizontalPadding,
                            rect.y - verticalPadding,
                            rect.width + 2 * horizontalPadding,
                            rect.height + 2 * verticalPadding,
                        )
                    textPane.scrollRectToVisible(paddedRect)
                }
            } catch (e: Exception) {
                log.warn("Could not scroll to cursor", e)
            }
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
        log.info(
            "Session Result: Attempt #${latest.attemptNumber}, WPM: ${"%.1f".format(
                latest.wpm,
            )}, EPM: ${"%.1f".format(latest.errorsPerMinute)}, Accuracy: ${"%.1f".format(latest.accuracy)}%",
        )

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
