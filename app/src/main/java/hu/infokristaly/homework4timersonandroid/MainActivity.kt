package hu.infokristaly.homework4timersonandroid

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import hu.infokristaly.homework4timersonandroid.ui.LocalizedApp
import hu.infokristaly.homework4timersonandroid.ui.TimerMainScreen
import hu.infokristaly.homework4timersonandroid.ui.theme.Homework4TimersOnAndroidTheme
import hu.infokristaly.homework4timersonandroid.viewmodel.TimerViewModel

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestNotificationPermissionIfNeeded()

        setContent {
            val timerViewModel: TimerViewModel = viewModel()
            val appLanguage by timerViewModel.appLanguage.collectAsState()

            LocalizedApp(languageCode = appLanguage) {
                Homework4TimersOnAndroidTheme {
                    TimerMainScreen(viewModel = timerViewModel)
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
