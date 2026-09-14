package com.kinsync.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

// Larger-than-default sizes throughout, per spec.md NFR-6 (elder-facing UI: large text).
val KinSyncTypography = Typography(
    headlineMedium = TextStyle(fontSize = 30.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontSize = 26.sp, lineHeight = 32.sp),
    bodyLarge = TextStyle(fontSize = 20.sp, lineHeight = 28.sp),
    bodyMedium = TextStyle(fontSize = 18.sp, lineHeight = 24.sp),
    labelLarge = TextStyle(fontSize = 18.sp, lineHeight = 22.sp),
)
