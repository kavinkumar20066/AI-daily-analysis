package com.dailyworktracker

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.dailyworktracker.ui.navigation.AppNavigation
import com.dailyworktracker.ui.theme.DailyWorkTrackerTheme
import com.dailyworktracker.ui.viewmodel.ExcelViewModel

/**
 * Single Activity — hosts the full Compose navigation graph.
 * Owns the SAF file picker launcher (must be registered in Activity, not Composable).
 */
class MainActivity : ComponentActivity() {

    val excelViewModel: ExcelViewModel by viewModels()

    /**
     * SAF file picker for .xlsx files.
     * Registered here so it can persist across configuration changes.
     */
    val excelFilePicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { excelViewModel.onFilePicked(it) }
    }

    /**
     * Launcher for the POST_NOTIFICATIONS runtime permission (Android 13+).
     * The callback delivers true if the user granted the permission, false otherwise.
     * Callers hook into this via [notificationPermissionCallback].
     */
    private var notificationPermissionCallback: ((Boolean) -> Unit)? = null

    val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationPermissionCallback?.invoke(granted)
        notificationPermissionCallback = null
    }

    /**
     * Requests the POST_NOTIFICATIONS permission on Android 13+.
     * On older API levels [onResult] is called immediately with `true` (no permission needed).
     *
     * @param onResult called with `true` if the permission is (or was already) granted,
     *                 `false` if the user denied it.
     */
    fun requestNotificationPermission(onResult: (Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionCallback = onResult
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            // API < 33 — permission not required
            onResult(true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            DailyWorkTrackerTheme {
                AppNavigation()
            }
        }
    }

    /** Called from any screen to launch the SAF picker for Excel files. */
    fun launchExcelPicker() {
        excelFilePicker.launch(
            arrayOf(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/vnd.ms-excel",
                "*/*" // fallback for file managers that don't report MIME type correctly
            )
        )
    }
}
