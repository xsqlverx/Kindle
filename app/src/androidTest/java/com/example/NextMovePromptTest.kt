package com.example

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.model.DayMove
import com.example.ui.components.LocalReducedMotion
import com.example.ui.screens.workspace.NextMovePrompt
import com.example.ui.theme.LifeOsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Cloud/device interaction coverage; not executed locally. */
@RunWith(AndroidJUnit4::class)
class NextMovePromptTest {
    @get:Rule val compose = createComposeRule()

    @Test fun expandOpensTheEditorAction() {
        var result = ""
        compose.setContent {
            LifeOsTheme { CompositionLocalProvider(LocalReducedMotion provides true) {
                NextMovePrompt(DayMove("Read my notes"), { result = "expand" }, { result = "dismiss" })
            } }
        }
        compose.onNodeWithTag("next_move_prompt").assertIsDisplayed()
        compose.onNodeWithTag("next_move_expand").performClick()
        compose.runOnIdle { assertEquals("expand", result) }
    }

    @Test fun dismissKeepsTheSeparateDismissAction() {
        var result = ""
        compose.setContent {
            LifeOsTheme { CompositionLocalProvider(LocalReducedMotion provides true) {
                NextMovePrompt(DayMove("Read my notes"), { result = "expand" }, { result = "dismiss" })
            } }
        }
        compose.onNodeWithTag("next_move_dismiss").performClick()
        compose.runOnIdle { assertEquals("dismiss", result) }
    }
}
