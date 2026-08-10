package com.dailyworktracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
