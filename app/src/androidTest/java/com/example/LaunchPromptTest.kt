package com.example

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith

/** Run only in an isolated test installation: this exercises real Activity launch/dismissal. */
@RunWith(AndroidJUnit4::class)
class LaunchPromptTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun freshLauncherEntryShowsAgainAfterDismissAndActivityCloseButNotRecreation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        fun launcher() = Intent(context, MainActivity::class.java).setAction(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        fun waitForBanner() = compose.waitUntil(15_000) {
            compose.onAllNodesWithTag("next_move_prompt").fetchSemanticsNodes().isNotEmpty()
        }
        val first = ActivityScenario.launch<MainActivity>(launcher())
        try {
            waitForBanner()
            compose.onNodeWithTag("next_move_dismiss").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("next_move_prompt").fetchSemanticsNodes().isEmpty() }
            first.recreate()
            compose.waitForIdle()
            assertTrue(compose.onAllNodesWithTag("next_move_prompt").fetchSemanticsNodes().isEmpty())
        } finally { first.close() }
        val second = ActivityScenario.launch<MainActivity>(launcher())
        try {
            waitForBanner()
            compose.onNodeWithTag("next_move_prompt").assertIsDisplayed()
        } finally { second.close() }
    }
}
