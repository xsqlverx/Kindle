package com.example

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.model.Destination
import com.example.ui.components.LocalReducedMotion
import com.example.ui.components.WorkspaceDock
import com.example.ui.theme.LifeOsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Device/cloud regression: searchable unpinned destinations and close/reopen remain usable. */
@RunWith(AndroidJUnit4::class)
class WorkspaceDockTest {
    @get:Rule val compose = createComposeRule()

    @Test fun searchAndNavigateToUnpinnedSpace() {
        var selected: Destination? = null
        compose.setContent {
            var expanded by remember { mutableStateOf(false) }
            LifeOsTheme {
                CompositionLocalProvider(LocalReducedMotion provides true) {
                    WorkspaceDock(Destination.defaults, Destination.TODAY, expanded, { expanded = it }, { selected = it })
                }
            }
        }
        compose.onNodeWithTag("centre_launcher").performClick()
        compose.onNodeWithTag("launcher_search").performTextInput("Review")
        compose.onNodeWithTag("launcher_library").performScrollToNode(hasTestTag("launcher_review"))
        compose.onNodeWithTag("launcher_review").performClick()
        compose.runOnIdle { assertEquals(Destination.REVIEW, selected) }
        compose.onNodeWithTag("centre_launcher").assertIsDisplayed().performClick()
        compose.onNodeWithTag("launcher_search").assertExists().performTextClearance()
        compose.onNodeWithContentDescription("Close launcher").performClick()
        compose.onNodeWithTag("centre_launcher").assertIsDisplayed()
    }
}
