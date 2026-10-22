package com.sl.parknote

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
}
