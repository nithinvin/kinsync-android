package com.kinsync.android.timeline

/** One or more movements close together, shown as a single line on the timeline. */
data class MovementBurst(
    val firstEpochMillis: Long,
    val lastEpochMillis: Long,
    val count: Int,
)

/**
 * Groups the times the phone moved (FR-2.7). The sensor can fire many times during one walk;
 * listing each one would hide everything else on the timeline.
 */
object MovementBursts {

    /** Movements less than this far apart belong to the same burst. */
    const val MAX_GAP_MILLIS = 10 * 60_000L

    /** Bursts inside [fromEpochMillis, toEpochMillis), oldest first. */
    fun within(timestamps: List<Long>, fromEpochMillis: Long, toEpochMillis: Long): List<MovementBurst> {
        val bursts = mutableListOf<MovementBurst>()
        val ordered = timestamps.filter { it in fromEpochMillis until toEpochMillis }.sorted()
        for (time in ordered) {
            val last = bursts.lastOrNull()
            if (last != null && time - last.lastEpochMillis < MAX_GAP_MILLIS) {
                bursts[bursts.lastIndex] = last.copy(lastEpochMillis = time, count = last.count + 1)
            } else {
                bursts += MovementBurst(firstEpochMillis = time, lastEpochMillis = time, count = 1)
            }
        }
        return bursts
    }
}
