package com.tjcdeveloper.open2048

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tjcdeveloper.open2048.data.ThemePreference
import com.tjcdeveloper.open2048.ui.GameScreen
import com.tjcdeveloper.open2048.ui.GameViewModel
import com.tjcdeveloper.open2048.ui.SettingsScreen
import com.tjcdeveloper.open2048.ui.theme.LocalOpenColors
import com.tjcdeveloper.open2048.ui.theme.Open2048Theme

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: GameViewModel = viewModel()
            val darkTheme = when (viewModel.theme) {
                ThemePreference.LIGHT -> false
                ThemePreference.DARK -> true
                ThemePreference.SYSTEM -> isSystemInDarkTheme()
            }
            Open2048Theme(darkTheme = darkTheme) {
                StatusBarStyle(darkTheme)
                val widthSizeClass = calculateWindowSizeClass(this).widthSizeClass
                AppContent(viewModel = viewModel, widthSizeClass = widthSizeClass)
            }
        }
    }

    @Composable
    private fun StatusBarStyle(darkTheme: Boolean) {
        val view = LocalView.current
        SideEffect {
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }
}

@Composable
private fun AppContent(
    viewModel: GameViewModel,
    widthSizeClass: androidx.compose.material3.windowsizeclass.WindowWidthSizeClass,
) {
    val colors = LocalOpenColors.current
    var showSettings by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.page),
    ) {
        if (!viewModel.isLoaded) return@Box
        Box(modifier = Modifier.safeDrawingPadding()) {
            if (showSettings) {
                BackHandler { showSettings = false }
                SettingsScreen(viewModel = viewModel, onBack = { showSettings = false })
            } else {
                GameScreen(
                    viewModel = viewModel,
                    widthSizeClass = widthSizeClass,
                    onOpenSettings = { showSettings = true },
                )
            }
        }
    }
}
