package com.github.kiolk.typingplugin.ui

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Test
import java.awt.Color
import java.awt.event.KeyEvent
import javax.swing.JTextPane
import javax.swing.text.StyleConstants

class TypingDialogTest : BasePlatformTestCase() {
    private lateinit var dialog: TypingDialog
    private lateinit var textPane: JTextPane
    private val sourceCode = "public class Test {}"

    override fun setUp() {
        super.setUp()
        // TypingDialog needs to be initialized on EDT potentially,
        // but for unit testing logic we might get away with it or use invokeAndWait.
        dialog = TypingDialog(project, sourceCode)
        textPane = dialog.getPreferredFocusedComponent() as JTextPane
    }

    @Test
    fun testInitialState() {
        assertEquals(sourceCode, textPane.text)
        val attrs = textPane.styledDocument.getCharacterElement(0).attributes
        // Cursor should be at 0 (background LIGHT_GRAY)
        assertEquals(Color.LIGHT_GRAY, StyleConstants.getBackground(attrs))
    }

    @Test
    fun testTypingCorrectCharacter() {
        simulateType('p')

        // Character 'p' (index 0) should now be GREEN
        val attrs0 = textPane.styledDocument.getCharacterElement(0).attributes
        assertEquals(Color.GREEN, StyleConstants.getForeground(attrs0))

        // Cursor should move to index 1
        val attrs1 = textPane.styledDocument.getCharacterElement(1).attributes
        assertEquals(Color.LIGHT_GRAY, StyleConstants.getBackground(attrs1))
    }

    @Test
    fun testTypingWrongCharacter() {
        simulateType('x')

        // Character at index 0 should be RED
        val attrs = textPane.styledDocument.getCharacterElement(0).attributes
        assertEquals(Color.RED, StyleConstants.getForeground(attrs))
        assertTrue(StyleConstants.isUnderline(attrs))
    }

    @Test
    fun testSkipLeadingWhitespaceAfterNewline() {
        val codeWithNewline = "a\n    b"
        val dialog = TypingDialog(project, codeWithNewline)
        val pane = dialog.getPreferredFocusedComponent() as JTextPane

        // Type 'a'
        val keyEventA = KeyEvent(pane, KeyEvent.KEY_TYPED, System.currentTimeMillis(), 0, KeyEvent.VK_UNDEFINED, 'a')
        pane.keyListeners.forEach { it.keyTyped(keyEventA) }

        // Type Enter
        val keyEventEnter = KeyEvent(pane, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ENTER, KeyEvent.CHAR_UNDEFINED)
        pane.keyListeners.forEach { it.keyPressed(keyEventEnter) }

        // After Enter, it should skip 4 spaces and land on 'b'
        // Index 0: 'a' (Correct)
        // Index 1: '\n' (Correct)
        // Index 2,3,4,5: ' ' (Skipped - Correct)
        // Index 6: 'b' (Cursor)

        val attrsB = pane.styledDocument.getCharacterElement(6).attributes
        assertEquals(Color.LIGHT_GRAY, StyleConstants.getBackground(attrsB))

        val attrsSpace = pane.styledDocument.getCharacterElement(2).attributes
        assertEquals(Color.GREEN, StyleConstants.getForeground(attrsSpace))
    }

    @Test
    fun testFormatTime() {
        assertEquals("0.0s", TypingDialog.formatTime(0.0))
        assertEquals("30.0s", TypingDialog.formatTime(30.0))
        assertEquals("59.9s", TypingDialog.formatTime(59.9))
        assertEquals("1m 0.0s", TypingDialog.formatTime(60.0))
        assertEquals("1m 5.4s", TypingDialog.formatTime(65.4))
        assertEquals("2m 5.4s", TypingDialog.formatTime(125.4))
        assertEquals("10m 0.0s", TypingDialog.formatTime(600.0))
    }

    private fun simulateType(char: Char) {
        val keyEvent = KeyEvent(textPane, KeyEvent.KEY_TYPED, System.currentTimeMillis(), 0, KeyEvent.VK_UNDEFINED, char)
        textPane.keyListeners.forEach { it.keyTyped(keyEvent) }
    }

    private fun simulateKeyPress(keyCode: Int) {
        val keyEvent = KeyEvent(textPane, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, keyCode, KeyEvent.CHAR_UNDEFINED)
        textPane.keyListeners.forEach { it.keyPressed(keyEvent) }
    }
}
