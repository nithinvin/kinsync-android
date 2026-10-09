package com.kinsync.android.ui.debug

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kinsync.android.R
import com.kinsync.android.activityrecognition.ActivityTransitionRecord
import com.kinsync.android.activityrecognition.CoarseActivity
import com.kinsync.android.activityrecognition.CurrentActivityStatus
import com.kinsync.android.activityrecognition.TransitionKind
import com.kinsync.android.movement.LastMovedStatus
import com.kinsync.android.ui.common.AppUsageRow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DebugEventListContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun text(id: Int) = composeRule.activity.getString(id)

    private fun show(
        isUsageAccessGranted: Boolean = true,
        appUsage: List<AppUsageRow> = emptyList(),
        lastMoved: LastMovedStatus = LastMovedStatus.NotYet,
        activityStatus: CurrentActivityStatus = CurrentActivityStatus.NotYet,
        recentActivity: List<ActivityTransitionRecord> = emptyList(),
        onAllowActivityRecognition: () -> Unit = {},
    ) {
        composeRule.setContent {
            DebugEventListContent(
                health = null,
                isUsageAccessGranted = isUsageAccessGranted,
                appUsage = appUsage,
                lastMoved = lastMoved,
                activityStatus = activityStatus,
                recentActivity = recentActivity,
                events = emptyList(),
                onAllowActivityRecognition = onAllowActivityRecognition,
                onRevokeConsent = {},
            )
        }
    }

    @Test
    fun appUsageRows_showNameAndTime() {
        show(
            isUsageAccessGranted = true,
            appUsage = listOf(AppUsageRow("com.example.maps", "Maps", 65 * 60_000L)),
        )

        composeRule.onNodeWithText("Maps").assertIsDisplayed()
        composeRule.onNodeWithText("1 h 05 min").assertIsDisplayed()
    }

    @Test
    fun noUsageYet_showsEmptyMessage() {
        show(isUsageAccessGranted = true, appUsage = emptyList())

        composeRule.onNodeWithText(text(R.string.debug_app_usage_empty)).assertIsDisplayed()
    }

    @Test
    fun usageAccessOff_saysSoInsteadOfRows() {
        show(
            isUsageAccessGranted = false,
            appUsage = listOf(AppUsageRow("com.example.maps", "Maps", 60_000L)),
        )

        composeRule.onNodeWithText(text(R.string.debug_app_usage_no_access)).assertIsDisplayed()
        composeRule.onNodeWithText("Maps").assertDoesNotExist()
    }

    @Test
    fun noMotionSensor_saysLastMovedIsNotAvailable() {
        show(lastMoved = LastMovedStatus.NotAvailable)

        composeRule.onNodeWithText(text(R.string.debug_last_moved_not_available)).assertIsDisplayed()
    }

    @Test
    fun noMovementYet_saysSo() {
        show(lastMoved = LastMovedStatus.NotYet)

        composeRule.onNodeWithText(text(R.string.debug_last_moved_not_yet)).assertIsDisplayed()
    }

    @Test
    fun activityPermissionOff_offersToAllowIt() {
        var allowClicks = 0
        show(activityStatus = CurrentActivityStatus.NoPermission, onAllowActivityRecognition = { allowClicks++ })

        composeRule.onNodeWithText(text(R.string.debug_activity_no_permission)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.debug_activity_allow)).performClick()

        assertEquals(1, allowClicks)
    }

    @Test
    fun noActivityYet_saysSo() {
        show(activityStatus = CurrentActivityStatus.NotYet)

        composeRule.onNodeWithText(text(R.string.debug_activity_not_yet)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.debug_activity_allow)).assertDoesNotExist()
    }

    @Test
    fun recentTransitions_areListedInPlainWords() {
        val walking = composeRule.activity.getString(R.string.activity_walking)
        show(
            activityStatus = CurrentActivityStatus.Doing(CoarseActivity.WALKING, 2_000L),
            recentActivity = listOf(
                ActivityTransitionRecord(id = 2, activity = CoarseActivity.WALKING, kind = TransitionKind.ENTER, timestampEpochMillis = 2_000L),
                ActivityTransitionRecord(id = 1, activity = CoarseActivity.WALKING, kind = TransitionKind.EXIT, timestampEpochMillis = 1_000L),
            ),
        )

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.debug_activity_started, walking))
            .assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.debug_activity_ended, walking))
            .assertIsDisplayed()
    }
}
