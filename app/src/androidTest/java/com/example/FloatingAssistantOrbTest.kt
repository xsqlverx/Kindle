package com.example

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.model.AssistantOrbPlacement
import com.example.ui.components.FloatingAssistantOrb
import com.example.ui.components.LocalOrbScene
import com.example.ui.components.LocalReducedMotion
import com.example.ui.components.OrbScene
import com.example.ui.theme.LifeOsTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Runs on a cloud/device Android runner, not locally. */
@RunWith(AndroidJUnit4::class)
class FloatingAssistantOrbTest {
    @get:Rule val compose = createComposeRule()

    @Test fun tapOpensAssistantAndDragSnapsToTheOtherEdge() {
        var opened = 0
        var moved: AssistantOrbPlacement? = null
        compose.setContent {
            var position by remember { mutableStateOf(AssistantOrbPlacement(false, 0.5f)) }
            LifeOsTheme {
                CompositionLocalProvider(LocalOrbScene provides remember { OrbScene() }, LocalReducedMotion provides true) {
                    FloatingAssistantOrb(position, 120f, { position = it; moved = it }, { opened++ })
                }
            }
        }
        compose.onNodeWithTag("floating_assistant_orb").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, opened) }
        compose.onNodeWithTag("floating_assistant_orb").performTouchInput {
            down(center); moveBy(Offset(-1000f, 60f)); up()
        }
        compose.runOnIdle { assertNotNull(moved); assertTrue(moved!!.left); assertEquals(1, opened) }
    }
}
