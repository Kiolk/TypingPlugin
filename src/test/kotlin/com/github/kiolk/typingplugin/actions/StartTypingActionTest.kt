package com.github.kiolk.typingplugin.actions

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.testFramework.TestActionEvent
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Test

class StartTypingActionTest : BasePlatformTestCase() {
    @Test
    fun testActionIsDisabledWhenNoEditor() {
        val action = StartTypingAction()
        val event =
            TestActionEvent.createTestEvent(action) { dataId ->
                if (CommonDataKeys.EDITOR.name == dataId) null else null
            }

        action.update(event)

        assertFalse(event.presentation.isEnabledAndVisible)
    }

    @Test
    fun testActionIsEnabledWhenEditorIsPresent() {
        myFixture.configureByText("Test.java", "public class Test {}")
        val action = StartTypingAction()
        val event =
            TestActionEvent.createTestEvent(action) { dataId ->
                when (dataId) {
                    CommonDataKeys.EDITOR.name -> myFixture.editor
                    CommonDataKeys.PROJECT.name -> project
                    else -> null
                }
            }

        action.update(event)

        assertTrue(event.presentation.isEnabledAndVisible)
        assertEquals("Type This Class", event.presentation.text)
    }

    @Test
    fun testActionTextChangesWhenTextIsSelected() {
        myFixture.configureByText("Test.java", "public class <selection>Test</selection> {}")
        val action = StartTypingAction()
        val event =
            TestActionEvent.createTestEvent(action) { dataId ->
                when (dataId) {
                    CommonDataKeys.EDITOR.name -> myFixture.editor
                    CommonDataKeys.PROJECT.name -> project
                    else -> null
                }
            }

        action.update(event)

        assertTrue(event.presentation.isEnabledAndVisible)
        assertEquals("Type Selected Area", event.presentation.text)
    }

    @Test
    fun testGetCleanedTextReturnsFullTextWhenNoSelection() {
        val content = "public class Test {}"
        myFixture.configureByText("Test.java", content)
        val action = StartTypingAction()

        val cleanedText = action.getCleanedText(myFixture.editor)

        assertEquals(content, cleanedText)
    }

    @Test
    fun testGetCleanedTextReturnsTrimmedSelection() {
        // We align the selection start with the indentation we want to preserve/trim consistently.
        val content =
            """
            public class Test {
                public void main() {
            <selection>        System.out.println("Hello");
                    System.out.println("World");</selection>
                }
            }
            """.trimIndent()

        myFixture.configureByText("Test.java", content)
        val action = StartTypingAction()

        val cleanedText = action.getCleanedText(myFixture.editor)

        val expected =
            """
            System.out.println("Hello");
            System.out.println("World");
            """.trimIndent()
        assertEquals(expected, cleanedText)
    }
}
