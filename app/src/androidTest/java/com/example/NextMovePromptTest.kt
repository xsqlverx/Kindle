package com.example

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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

    @Test fun bannerDoesNotTimeOutOrBlockTheRestOfTheScreen() {
        var taps = 0
        compose.mainClock.autoAdvance = false
        compose.setContent {
            LifeOsTheme { CompositionLocalProvider(LocalReducedMotion provides true) {
                Box(Modifier.fillMaxSize()) {
                    Button(onClick = { taps++ }, modifier = Modifier.align(Alignment.BottomCenter).testTag("background_action")) { Text("Background") }
                    NextMovePrompt(DayMove("Read my notes"), {}, {})
                }
            } }
        }
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(12_000)
        compose.onNodeWithTag("next_move_prompt").assertIsDisplayed()
        compose.onNodeWithTag("background_action").performClick()
        compose.runOnIdle { assertEquals(1, taps) }
        compose.onNodeWithTag("next_move_prompt").assertIsDisplayed()
        compose.mainClock.autoAdvance = true
    }
}
