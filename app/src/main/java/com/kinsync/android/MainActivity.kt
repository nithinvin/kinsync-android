package com.kinsync.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.kinsync.android.ui.KinSyncApp
import com.kinsync.android.ui.theme.KinSyncTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Android 15 (targetSdk 35) always draws apps edge to edge. Opt in on older versions too,
        // so every Android version behaves the same, and keep content clear of the system bars.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as KinSyncApplication).container

        setContent {
            KinSyncTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    // The background still fills behind the status and navigation bars, but no
                    // text or button is placed under them.
                    Box(modifier = Modifier.safeDrawingPadding()) {
                        KinSyncApp(container = container)
                    }
                }
            }
        }
    }
}
