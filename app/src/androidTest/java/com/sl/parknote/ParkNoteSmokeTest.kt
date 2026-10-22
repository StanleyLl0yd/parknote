package com.sl.parknote

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Smoke check: the manual garage flow works without requesting location. */
@RunWith(AndroidJUnit4::class)
class ParkNoteSmokeTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun opensGarageEditorWithoutLocationPermission() {
        val underground = rule.activity.getString(R.string.underground)
        rule.waitUntil(timeoutMillis = 15_000) {
            rule.onAllNodesWithText(underground).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText(underground).performClick()
        rule.onNodeWithText(rule.activity.getString(R.string.floor)).assertExists()
    }

    @Test fun parkingCanBeSavedRestoredAfterActivityRecreationAndFinished() {
        val underground = rule.activity.getString(R.string.underground)
        val savedPlace = rule.activity.getString(R.string.current_place)
        rule.waitUntil(timeoutMillis = 15_000) {
            rule.onAllNodesWithText(underground).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText(underground).performClick()
        // The second editable field is the floor; no GPS or network is required.
        rule.onAllNodes(hasSetTextAction())[1].performTextInput("-2")
        rule.onNodeWithText(rule.activity.getString(R.string.save)).performClick()
        rule.waitUntil(timeoutMillis = 15_000) {
            rule.onAllNodesWithText(savedPlace).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("-2").assertExists()
        rule.activityRule.scenario.recreate()
        rule.waitUntil(timeoutMillis = 15_000) {
            rule.onAllNodesWithText(savedPlace).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("-2").assertExists()
        rule.onNodeWithText(rule.activity.getString(R.string.finish)).performClick()
        rule.onNodeWithText(rule.activity.getString(R.string.delete)).performClick()
        rule.waitUntil(timeoutMillis = 15_000) {
            rule.onAllNodesWithText(underground).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
