package com.kinsync.android.collector

/** Local, on-device only signal types (FR-2.1). Never transmitted off-device (NFR-1). */
enum class UnlockEventType {
    SCREEN_ON,
    SCREEN_OFF,
    USER_PRESENT,
}
