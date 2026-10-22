package com.sl.parknote

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.*
import com.sl.parknote.data.ParkingRepository
import kotlinx.coroutines.runBlocking
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

    @Test fun cancellingAnEditPreservesPreviouslySavedParking() {
        val underground = rule.activity.getString(R.string.underground)
        val title = rule.activity.getString(R.string.current_place)
        rule.waitUntil(timeoutMillis = 15_000) {
            rule.onAllNodesWithText(underground).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText(underground).performClick()
        rule.onAllNodes(hasSetTextAction())[1].performTextInput("-2")
        val save = rule.onNodeWithText(rule.activity.getString(R.string.save))
        rule.onNode(hasScrollAction()).performTouchInput { swipeUp() }
        save.assertIsDisplayed()
        save.performClick()
        rule.waitUntil(timeoutMillis = 15_000) {
            rule.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("-2").assertExists()

        rule.onNodeWithText(rule.activity.getString(R.string.edit)).performClick()
        rule.onAllNodes(hasSetTextAction())[1].performTextReplacement("-4")
        val cancel = rule.onNodeWithText(rule.activity.getString(R.string.cancel))
        rule.onNode(hasScrollAction()).performTouchInput { swipeUp() }
        cancel.assertIsDisplayed()
        cancel.performClick()
        rule.waitUntil(timeoutMillis = 15_000) {
            rule.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("-2").assertExists()
        rule.onNodeWithText("-4").assertDoesNotExist()
        rule.activityRule.scenario.recreate()
        rule.waitUntil(timeoutMillis = 15_000) {
            rule.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("-2").assertExists()
        rule.onNodeWithText(rule.activity.getString(R.string.finish)).performClick()
        rule.onNodeWithText(rule.activity.getString(R.string.delete)).performClick()
        rule.waitUntil(timeoutMillis = 15_000) {
            rule.onAllNodesWithText(underground).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test fun savedParkingCanBeEditedRecreatedAndFinished() {
        val underground = rule.activity.getString(R.string.underground)
        val title = rule.activity.getString(R.string.current_place)
        rule.waitUntil(timeoutMillis = 15_000) {
            rule.onAllNodesWithText(underground).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText(underground).performClick()
        rule.onAllNodes(hasSetTextAction())[1].performTextInput("-1")
        val save = rule.onNodeWithText(rule.activity.getString(R.string.save))
        rule.onNode(hasScrollAction()).performTouchInput { swipeUp() }
        save.assertIsDisplayed()
        save.performClick()
        rule.waitUntil(timeoutMillis = 15_000) {
            rule.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("-1").assertExists()
        val originalId = runBlocking { ParkingRepository(rule.activity.applicationContext).load()?.id }
        org.junit.Assert.assertNotNull(originalId)

        rule.onNodeWithText(rule.activity.getString(R.string.edit)).performClick()
        rule.onAllNodes(hasSetTextAction())[1].performTextReplacement("-3")
        val saveEdited = rule.onNodeWithText(rule.activity.getString(R.string.save))
        rule.onNode(hasScrollAction()).performTouchInput { swipeUp() }
        saveEdited.assertIsDisplayed()
        saveEdited.performClick()
        rule.waitUntil(timeoutMillis = 15_000) {
            rule.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("-3").assertExists()
        val editedId = runBlocking { ParkingRepository(rule.activity.applicationContext).load()?.id }
        org.junit.Assert.assertEquals(originalId, editedId)
        rule.activityRule.scenario.recreate()
        rule.waitUntil(timeoutMillis = 15_000) {
            rule.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("-3").assertExists()
        rule.onNodeWithText(rule.activity.getString(R.string.finish)).performClick()
        rule.onNodeWithText(rule.activity.getString(R.string.delete)).performClick()
        rule.waitUntil(timeoutMillis = 15_000) {
            rule.onAllNodesWithText(underground).fetchSemanticsNodes().isNotEmpty()
        }
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
        rule.onNodeWithText("-2").assertExists()
        val save = rule.onNodeWithText(rule.activity.getString(R.string.save))
        save.assertIsEnabled()
        // On small screens the save button starts below the scroll viewport.
        // Scroll the actual form, then confirm a physical tap target is visible.
        rule.onNode(hasScrollAction()).performTouchInput { swipeUp() }
        save.assertIsDisplayed()
        save.performClick()
        try {
            rule.waitUntil(timeoutMillis = 15_000) {
                rule.onAllNodesWithText(savedPlace).fetchSemanticsNodes().isNotEmpty()
            }
        } catch (failure: Throwable) {
            val stored = runCatching {
                runBlocking { ParkingRepository(rule.activity.applicationContext).load() }
            }
            throw AssertionError("Parking save timeout; persisted=$stored; semantics:\\n${rule.onRoot().printToString()}", failure)
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
