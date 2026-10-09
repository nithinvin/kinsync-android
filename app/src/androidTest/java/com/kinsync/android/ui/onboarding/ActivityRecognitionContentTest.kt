package com.kinsync.android.ui.onboarding

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kinsync.android.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActivityRecognitionContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun text(id: Int) = composeRule.activity.getString(id)

    private var allowed = 0
    private var settingsOpened = 0
    private var continued = 0

    private fun show(isGranted: Boolean, wasDenied: Boolean) {
        composeRule.setContent {
            ActivityRecognitionContent(
                isGranted = isGranted,
                wasDenied = wasDenied,
                onAllow = { allowed++ },
                onOpenSettings = { settingsOpened++ },
                onContinue = { continued++ },
            )
        }
    }

    @Test
    fun notAskedYet_allowAsksForThePermission() {
        show(isGranted = false, wasDenied = false)

        composeRule.onNodeWithText(text(R.string.activity_permission_body)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.activity_permission_allow)).performClick()

        assertEquals(1, allowed)
        assertEquals(0, continued)
    }

    @Test
    fun notAskedYet_notNowContinuesWithoutThePermission() {
        show(isGranted = false, wasDenied = false)

        composeRule.onNodeWithText(text(R.string.activity_permission_not_now)).performClick()

        assertEquals(0, allowed)
        assertEquals(1, continued)
    }

    @Test
    fun granted_showsThanksAndContinues() {
        show(isGranted = true, wasDenied = false)

        composeRule.onNodeWithText(text(R.string.activity_permission_granted)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.activity_permission_allow)).assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.activity_permission_continue)).performClick()

        assertEquals(1, continued)
    }

    @Test
    fun denied_offersSettingsAndStillContinues() {
        show(isGranted = false, wasDenied = true)

        composeRule.onNodeWithText(text(R.string.activity_permission_denied)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.activity_permission_open_settings)).performClick()
        composeRule.onNodeWithText(text(R.string.activity_permission_continue)).performClick()

        assertEquals(1, settingsOpened)
        assertEquals(1, continued)
    }
}
