package com.kinsync.android.ui.debug

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kinsync.android.R
import com.kinsync.android.movement.LastMovedStatus
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
    ) {
        composeRule.setContent {
            DebugEventListContent(
                health = null,
                isUsageAccessGranted = isUsageAccessGranted,
                appUsage = appUsage,
                lastMoved = lastMoved,
                events = emptyList(),
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
}
