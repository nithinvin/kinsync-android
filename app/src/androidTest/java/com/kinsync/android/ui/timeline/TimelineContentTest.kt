package com.kinsync.android.ui.timeline

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kinsync.android.R
import com.kinsync.android.activityrecognition.CoarseActivity
import com.kinsync.android.timeline.ActivityPeriod
import com.kinsync.android.timeline.DayTimeline
import com.kinsync.android.timeline.MovementBurst
import com.kinsync.android.timeline.PhoneSession
import com.kinsync.android.usage.AppUsageTotal
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class TimelineContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun text(id: Int, vararg args: Any) = composeRule.activity.getString(id, *args)

    private val minute = 60_000L
    private val day = LocalDate.of(2026, 10, 9)
    private val dayStart = day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    private fun at(hour: Int, minuteOfHour: Int) = dayStart + (hour * 60 + minuteOfHour) * minute

    private val typicalDay = DayTimeline(
        dayStartEpochMillis = dayStart,
        dayEndEpochMillis = dayStart + 24 * 60 * minute,
        countUntilEpochMillis = at(10, 0),
        sessions = listOf(
            PhoneSession(
                startEpochMillis = at(7, 12),
                endEpochMillis = at(7, 30),
                firstUnlockEpochMillis = at(7, 12),
                isOpen = false,
                apps = listOf(
                    AppUsageTotal("com.whatsapp", 10 * minute),
                    AppUsageTotal("com.android.chrome", 5 * minute),
                    AppUsageTotal("com.kindle", 2 * minute),
                    AppUsageTotal("com.android.camera", 1 * minute),
                ),
            ),
            PhoneSession(
                startEpochMillis = at(8, 40),
                endEpochMillis = at(8, 40),
                firstUnlockEpochMillis = null,
                isOpen = false,
                apps = emptyList(),
            ),
        ),
        activityPeriods = listOf(
            ActivityPeriod(CoarseActivity.WALKING, at(8, 58), at(9, 10), isOpen = false),
            ActivityPeriod(CoarseActivity.STILL, at(9, 10), at(10, 0), isOpen = true),
        ),
        movements = listOf(MovementBurst(at(8, 59), at(9, 5), count = 4)),
    )

    private fun state(timeline: DayTimeline = typicalDay) = TimelineUiState(
        day = day,
        isToday = true,
        timeline = timeline,
        appLabels = mapOf(
            "com.whatsapp" to "WhatsApp",
            "com.android.chrome" to "Chrome",
            "com.kindle" to "Kindle",
            "com.android.camera" to "Camera",
        ),
    )

    private var previousClicks = 0
    private var nextClicks = 0

    private fun show(state: TimelineUiState?, isToday: Boolean = true) {
        composeRule.setContent {
            TimelineContent(
                day = day,
                isToday = isToday,
                state = state,
                onPreviousDay = { previousClicks++ },
                onNextDay = { nextClicks++ },
                zoneId = ZoneOffset.UTC,
            )
        }
    }

    @Test
    fun typicalDay_listsSessionsActivityAndMovement() {
        show(state())

        composeRule.onNodeWithText(text(R.string.timeline_phone_unlocked)).assertIsDisplayed()
        composeRule.onNodeWithText("WhatsApp 10 min, Chrome 5 min, Kindle 2 min and 1 more app").assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.timeline_phone_screen_on)).assertIsDisplayed()
        // "Walking" is also in the band's legend, so check the walk's line by its length.
        composeRule.onNodeWithText("(12 min)", substring = true).assertIsDisplayed()
        // The band takes part of the screen, so the last lines may need scrolling to.
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(text(R.string.timeline_moved)))
        composeRule.onNodeWithText(text(R.string.timeline_moved)).assertIsDisplayed()
        composeRule.onNodeWithText("4 times", substring = true).assertIsDisplayed()
    }

    @Test
    fun activityGoingOnToday_saysStillGoing() {
        show(state())

        composeRule.onNode(hasScrollAction())
            .performScrollToNode(hasText(text(R.string.timeline_still_going, "50 min")))
        composeRule.onNodeWithText(text(R.string.timeline_still_going, "50 min")).assertIsDisplayed()
    }

    @Test
    fun today_cannotGoToTheNextDayButCanGoBack() {
        show(state())

        composeRule.onNodeWithText(text(R.string.timeline_next_day)).assertIsNotEnabled()
        composeRule.onNodeWithText(text(R.string.timeline_previous_day)).performClick()

        assertEquals(1, previousClicks)
    }

    @Test
    fun earlierDay_canGoToTheNextDay() {
        show(state(), isToday = false)

        composeRule.onNodeWithText(text(R.string.timeline_next_day)).assertIsEnabled().performClick()

        assertEquals(1, nextClicks)
    }

    @Test
    fun emptyDay_saysNothingWasRecorded() {
        show(state(typicalDay.copy(sessions = emptyList(), activityPeriods = emptyList(), movements = emptyList())))

        composeRule.onNodeWithText(text(R.string.timeline_empty)).assertIsDisplayed()
    }

    @Test
    fun stillLoading_showsLoading() {
        show(state = null)

        composeRule.onNodeWithText(text(R.string.timeline_loading)).assertIsDisplayed()
    }
}
