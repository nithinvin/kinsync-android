package com.kinsync.android.ui.timeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kinsync.android.R
import com.kinsync.android.activityrecognition.CoarseActivity
import com.kinsync.android.timeline.ActivityPeriod
import com.kinsync.android.timeline.DayTimeline
import com.kinsync.android.timeline.MovementBurst
import com.kinsync.android.timeline.PhoneSession
import com.kinsync.android.timeline.TimelineEntry
import com.kinsync.android.ui.common.usageDurationText
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Apps listed under one phone session; the rest are summed up as "and N more". */
private const val APPS_PER_SESSION = 3

/** "My day": everything recorded on one day, on a 24-hour timeline (FR-2.5). */
@Composable
fun TimelineScreen(viewModel: TimelineViewModel) {
    val day by viewModel.day.collectAsState()
    val state by viewModel.state.collectAsState()

    TimelineContent(
        day = day,
        isToday = day == viewModel.today(),
        // Until the picked day is read, show it as loading rather than the previous day.
        state = state?.takeIf { it.day == day },
        onPreviousDay = viewModel::showPreviousDay,
        onNextDay = viewModel::showNextDay,
    )
}

/** Stateless screen content, so it can be shown in tests without a database. */
@Composable
fun TimelineContent(
    day: LocalDate,
    isToday: Boolean,
    state: TimelineUiState?,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    zoneId: ZoneId = ZoneId.systemDefault(),
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(stringResource(R.string.timeline_title), style = MaterialTheme.typography.headlineMedium)
        DayPicker(day, isToday, onPreviousDay, onNextDay)
        Spacer(Modifier.height(8.dp))
        when {
            state == null -> Text(stringResource(R.string.timeline_loading))
            state.timeline.isEmpty -> {
                DayBand(state.timeline, isToday)
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.timeline_empty), style = MaterialTheme.typography.bodyLarge)
            }
            else -> {
                DayBand(state.timeline, isToday)
                Spacer(Modifier.height(12.dp))
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(state.timeline.entries) { entry ->
                        TimelineRow(entry, state, isToday, zoneId)
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun DayPicker(day: LocalDate, isToday: Boolean, onPreviousDay: () -> Unit, onNextDay: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onPreviousDay) {
            Text(stringResource(R.string.timeline_previous_day))
        }
        Text(
            if (isToday) {
                stringResource(R.string.timeline_today, dateText(day))
            } else {
                dateText(day)
            },
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onNextDay, enabled = !isToday) {
            Text(stringResource(R.string.timeline_next_day))
        }
    }
}

/**
 * The whole day from midnight to midnight, one lane per signal. Only a picture: the list below
 * it says the same in words, so screen readers get a short description instead.
 */
@Composable
private fun DayBand(timeline: DayTimeline, isToday: Boolean) {
    val colors = MaterialTheme.colorScheme
    val unlockedColor = colors.primary
    val screenOnlyColor = colors.primary.copy(alpha = 0.35f)
    val movedColor = colors.tertiary
    // The card is surfaceVariant, so the empty lane needs a different colour to show.
    val trackColor = colors.surface
    val nowColor = colors.error
    val activityColors = CoarseActivity.entries.associateWith { activityColor(it) }
    val description = stringResource(R.string.timeline_band_description)
    val nowEpochMillis = if (isToday) timeline.countUntilEpochMillis else null

    Card(modifier = Modifier.fillMaxWidth().semantics { contentDescription = description }) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            BandLane(stringResource(R.string.timeline_lane_phone), timeline, trackColor, nowEpochMillis, nowColor) {
                timeline.sessions.forEach { session ->
                    drawSpan(
                        timeline,
                        session.startEpochMillis,
                        session.endEpochMillis,
                        if (session.isUnlocked) unlockedColor else screenOnlyColor,
                    )
                }
            }
            BandLane(stringResource(R.string.timeline_lane_activity), timeline, trackColor, nowEpochMillis, nowColor) {
                timeline.activityPeriods.forEach { period ->
                    drawSpan(timeline, period.startEpochMillis, period.endEpochMillis, activityColors.getValue(period.activity))
                }
            }
            BandLane(stringResource(R.string.timeline_lane_moved), timeline, trackColor, nowEpochMillis, nowColor) {
                timeline.movements.forEach { burst ->
                    drawSpan(timeline, burst.firstEpochMillis, burst.lastEpochMillis, movedColor)
                }
            }
            HourLabels()
            ActivityLegend()
        }
    }
}

@Composable
private fun BandLane(
    label: String,
    timeline: DayTimeline,
    trackColor: Color,
    nowEpochMillis: Long?,
    nowColor: Color,
    drawMarks: DrawScope.() -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(LANE_LABEL_WIDTH))
        Canvas(modifier = Modifier.weight(1f).height(18.dp)) {
            drawRoundRect(trackColor, cornerRadius = CornerRadius(4.dp.toPx()))
            drawMarks()
            if (nowEpochMillis != null) {
                val x = xFor(timeline, nowEpochMillis)
                drawLine(nowColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 2.dp.toPx())
            }
        }
    }
}

/** Where a time falls across the lane, from 0 (midnight) to the lane's width. */
private fun DrawScope.xFor(timeline: DayTimeline, epochMillis: Long): Float {
    val dayLength = (timeline.dayEndEpochMillis - timeline.dayStartEpochMillis).toFloat()
    val fraction = (epochMillis - timeline.dayStartEpochMillis) / dayLength
    return fraction.coerceIn(0f, 1f) * size.width
}

/** A bar from [startEpochMillis] to [endEpochMillis], at least 2 dp wide so short events still show. */
private fun DrawScope.drawSpan(timeline: DayTimeline, startEpochMillis: Long, endEpochMillis: Long, color: Color) {
    val start = xFor(timeline, startEpochMillis)
    val width = maxOf(xFor(timeline, endEpochMillis) - start, 2.dp.toPx())
    drawRect(color, topLeft = Offset(start, 0f), size = Size(width, size.height))
}

@Composable
private fun HourLabels() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.width(LANE_LABEL_WIDTH))
        BAND_HOUR_MARKS.forEach { hour ->
            Text(
                LocalTime.of(hour, 0).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ActivityLegend() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Spacer(Modifier.width(LANE_LABEL_WIDTH - 12.dp))
        CoarseActivity.entries.forEach { activity ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                val color = activityColor(activity)
                Canvas(modifier = Modifier.size(10.dp)) { drawRect(color) }
                Spacer(Modifier.width(4.dp))
                Text(activityLabel(activity), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private val LANE_LABEL_WIDTH = 72.dp

/** The band is split into four quarters of six hours, each labelled with its first hour. */
private val BAND_HOUR_MARKS = listOf(0, 6, 12, 18)

@Composable
private fun activityColor(activity: CoarseActivity): Color = when (activity) {
    CoarseActivity.STILL -> MaterialTheme.colorScheme.outline
    CoarseActivity.WALKING -> MaterialTheme.colorScheme.tertiary
    CoarseActivity.IN_VEHICLE -> MaterialTheme.colorScheme.secondary
}

@Composable
private fun activityLabel(activity: CoarseActivity): String = when (activity) {
    CoarseActivity.STILL -> stringResource(R.string.summary_activity_still)
    CoarseActivity.WALKING -> stringResource(R.string.summary_activity_walking)
    CoarseActivity.IN_VEHICLE -> stringResource(R.string.summary_activity_in_vehicle)
}

/** One line of the list: the time on the left, what happened on the right. */
@Composable
private fun TimelineRow(entry: TimelineEntry, state: TimelineUiState, isToday: Boolean, zoneId: ZoneId) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            timeText(entry.startEpochMillis, zoneId),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.width(TIME_COLUMN_WIDTH),
        )
        Box(modifier = Modifier.weight(1f)) {
            when (entry) {
                is TimelineEntry.Phone -> PhoneSessionText(entry.session, state.appLabels, isToday, zoneId)
                is TimelineEntry.Activity -> ActivityPeriodText(entry.period, isToday, zoneId)
                is TimelineEntry.Moved -> MovementText(entry.burst, zoneId)
            }
        }
    }
}

private val TIME_COLUMN_WIDTH = 100.dp

@Composable
private fun PhoneSessionText(session: PhoneSession, appLabels: Map<String, String>, isToday: Boolean, zoneId: ZoneId) {
    Column {
        Text(
            stringResource(if (session.isUnlocked) R.string.timeline_phone_unlocked else R.string.timeline_phone_screen_on),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(untilText(session.endEpochMillis, session.durationMillis, session.isOpen && isToday, zoneId))
        if (session.apps.isNotEmpty()) {
            // map is inline, so stringResource can be called inside it; joinToString's lambda is not.
            val shown = session.apps.take(APPS_PER_SESSION).map { app ->
                stringResource(
                    R.string.timeline_app_time,
                    appLabels[app.packageName] ?: app.packageName,
                    usageDurationText(app.totalMillis),
                )
            }.joinToString(separator = ", ")
            val hidden = session.apps.size - APPS_PER_SESSION
            Text(
                if (hidden > 0) {
                    pluralStringResource(R.plurals.timeline_apps_and_more, hidden, shown, hidden)
                } else {
                    shown
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun ActivityPeriodText(period: ActivityPeriod, isToday: Boolean, zoneId: ZoneId) {
    Column {
        Text(activityLabel(period.activity), style = MaterialTheme.typography.bodyLarge)
        Text(untilText(period.endEpochMillis, period.durationMillis, period.isOpen && isToday, zoneId))
    }
}

@Composable
private fun MovementText(burst: MovementBurst, zoneId: ZoneId) {
    Column {
        Text(stringResource(R.string.timeline_moved), style = MaterialTheme.typography.bodyLarge)
        if (burst.count > 1) {
            Text(
                pluralStringResource(
                    R.plurals.timeline_moved_times,
                    burst.count,
                    burst.count,
                    timeText(burst.lastEpochMillis, zoneId),
                ),
            )
        }
    }
}

/** "until 07:30 (18 min)", or "still going (18 min)" for something in progress today. */
@Composable
private fun untilText(endEpochMillis: Long, durationMillis: Long, isGoingOn: Boolean, zoneId: ZoneId): String =
    if (isGoingOn) {
        stringResource(R.string.timeline_still_going, usageDurationText(durationMillis))
    } else {
        stringResource(R.string.timeline_until, timeText(endEpochMillis, zoneId), usageDurationText(durationMillis))
    }

private fun timeText(epochMillis: Long, zoneId: ZoneId): String =
    Instant.ofEpochMilli(epochMillis).atZone(zoneId).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

private fun dateText(day: LocalDate): String = day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))
