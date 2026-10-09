package com.kinsync.android.ui.common

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

private const val TICK_MILLIS = 60_000L

/** The current time now and then once a minute, so something in progress keeps growing on screen. */
fun minuteTicks(clock: () -> Long): Flow<Long> = flow {
    while (true) {
        emit(clock())
        delay(TICK_MILLIS)
    }
}
