package com.kinsync.android.ui.summary

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kinsync.android.R
import com.kinsync.android.activityrecognition.CoarseActivity
import com.kinsync.android.movement.LastMovedStatus
import com.kinsync.android.summary.DailySummary
import com.kinsync.android.summary.UnlockSummary
import com.kinsync.android.ui.common.AppUsageRow
import com.kinsync.android.usage.AppUsageTotal
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class SummaryContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun text(id: Int, vararg args: Any) = composeRule.activity.getString(id, *args)

    private val today = LocalDate.of(2026, 10, 9)

    private val typicalDay = SummaryUiState(
        summary = DailySummary(
            unlocks = UnlockSummary(firstUnlockEpochMillis = null, unlockCount = 34),
            screenTimeMillis = (2 * 60 + 15) * 60_000L,
            topApps = listOf(AppUsageTotal("com.whatsapp", 50 * 60_000L)),
            activityMillis = mapOf(CoarseActivity.STILL to 65 * 60_000L, CoarseActivity.WALKING to 12 * 60_000L),
        ),
        topApps = listOf(AppUsageRow("com.whatsapp", "WhatsApp", 50 * 60_000L)),
    )

    private var allowClicks = 0
    private var detailsClicks = 0

    private fun show(
        state: SummaryUiState? = typicalDay,
        lastMoved: LastMovedStatus = LastMovedStatus.NotYet,
        isUsageAccessGranted: Boolean = true,
        isActivityRecognitionGranted: Boolean = true,
    ) {
        composeRule.setContent {
            SummaryContent(
                today = today,
                state = state,
                lastMoved = lastMoved,
                isUsageAccessGranted = isUsageAccessGranted,
                isActivityRecognitionGranted = isActivityRecognitionGranted,
                onAllowActivityRecognition = { allowClicks++ },
                onOpenDetails = { detailsClicks++ },
                zoneId = ZoneOffset.UTC,
            )
        }
    }

    @Test
    fun typicalDay_showsCountsScreenTimeAndTopApp() {
        show()

        composeRule.onNodeWithText("34").assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.summary_not_yet)).assertIsDisplayed()
        composeRule.onNodeWithText("2 h 15 min").assertIsDisplayed()
        composeRule.onNodeWithText("WhatsApp").assertIsDisplayed()
        composeRule.onNodeWithText("50 min").assertIsDisplayed()
    }

    @Test
    fun activity_showsTimePerActivityIncludingNone() {
        show()

        composeRule.onNodeWithText("1 h 05 min").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("12 min").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.summary_activity_in_vehicle)).performScrollTo().assertIsDisplayed()
        // No time in a vehicle today: "none", not "under 1 min".
        composeRule.onNodeWithText(text(R.string.summary_activity_none)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun activityPermissionOff_offersToAllowIt() {
        show(isActivityRecognitionGranted = false)

        composeRule.onNodeWithText(text(R.string.debug_activity_allow)).performScrollTo().performClick()

        assertEquals(1, allowClicks)
    }

    @Test
    fun lastMovedToday_showsOnlyTheTime() {
        // 2026-10-09 08:59 UTC.
        show(lastMoved = LastMovedStatus.MovedAt(1_791_536_340_000L))

        // Localized short time: "8:59 AM" or "08:59" depending on the locale.
        composeRule.onNodeWithText("8:59", substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun usageAccessOff_saysSoInsteadOfApps() {
        show(isUsageAccessGranted = false)

        composeRule.onNodeWithText(text(R.string.debug_app_usage_no_access)).assertIsDisplayed()
        composeRule.onNodeWithText("WhatsApp").assertDoesNotExist()
    }

    @Test
    fun stillLoading_showsLoadingAndTheDetailsButton() {
        show(state = null)

        composeRule.onNodeWithText(text(R.string.summary_loading)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.summary_open_details)).performClick()

        assertEquals(1, detailsClicks)
    }
}
