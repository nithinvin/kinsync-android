package com.kinsync.android.ui.onboarding

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kinsync.android.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConsentScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun text(id: Int) = composeRule.activity.getString(id)

    @Test
    fun firstConsent_showsTitleAndEverySignal() {
        composeRule.setContent { ConsentScreen(isReconsent = false, onAgree = {}, onDecline = {}) }

        composeRule.onNodeWithText(text(R.string.consent_title)).assertIsDisplayed()
        listOf(
            R.string.consent_signal_unlocks,
            R.string.consent_signal_app_usage,
            R.string.consent_signal_last_moved,
            R.string.consent_signal_activity,
        ).forEach { signal ->
            composeRule.onNodeWithText(text(signal)).performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun reconsent_showsUpdatedTitleAndIntro() {
        composeRule.setContent { ConsentScreen(isReconsent = true, onAgree = {}, onDecline = {}) }

        composeRule.onNodeWithText(text(R.string.consent_title_updated)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.consent_updated_intro)).assertIsDisplayed()
    }

    @Test
    fun agree_callsOnAgreeOnly() {
        var agreed = 0
        var declined = 0
        composeRule.setContent {
            ConsentScreen(isReconsent = false, onAgree = { agreed++ }, onDecline = { declined++ })
        }

        composeRule.onNodeWithText(text(R.string.consent_agree)).performScrollTo().performClick()

        assertEquals(1, agreed)
        assertEquals(0, declined)
    }

    @Test
    fun decline_callsOnDeclineAndShowsMonitoringOff() {
        var declined = 0
        composeRule.setContent {
            ConsentScreen(isReconsent = true, onAgree = {}, onDecline = { declined++ })
        }

        composeRule.onNodeWithText(text(R.string.consent_decline)).performScrollTo().performClick()

        assertEquals(1, declined)
        composeRule.onNodeWithText(text(R.string.consent_declined_message)).performScrollTo().assertIsDisplayed()
    }
}
