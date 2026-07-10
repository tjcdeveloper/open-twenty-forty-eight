package com.tjcdeveloper.open2048

import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tjcdeveloper.open2048.data.GameRepository
import com.tjcdeveloper.open2048.data.ThemePreference
import com.tjcdeveloper.open2048.ui.DoubleBackToExit
import com.tjcdeveloper.open2048.ui.GameScreen
import com.tjcdeveloper.open2048.ui.GameViewModel
import com.tjcdeveloper.open2048.ui.SettingsScreen
import com.tjcdeveloper.open2048.ui.theme.LocalOpenColors
import com.tjcdeveloper.open2048.ui.theme.Open2048Theme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // One blocking read of the tiny preferences file so the window background and
        // first frames match the saved theme instead of flashing the light palette
        // while the async ViewModel load is still in flight.
        val initialTheme = GameRepository(applicationContext).loadThemeBlocking()
        window.setBackgroundDrawable(ColorDrawable(if (resolveDark(initialTheme)) 0xFF000000.toInt() else 0xFFFAF8EF.toInt()))
        setContent {
            val viewModel: GameViewModel = viewModel()
            val preference = if (viewModel.isLoaded) viewModel.theme else initialTheme
            val darkTheme = when (preference) {
                ThemePreference.LIGHT -> false
                ThemePreference.DARK -> true
                ThemePreference.SYSTEM -> isSystemInDarkTheme()
            }
            Open2048Theme(darkTheme = darkTheme) {
                WindowChrome(darkTheme)
                val widthSizeClass = calculateWindowSizeClass(this).widthSizeClass
                AppContent(viewModel = viewModel, widthSizeClass = widthSizeClass)
            }
        }
    }

    private fun resolveDark(preference: ThemePreference): Boolean = when (preference) {
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
        ThemePreference.SYSTEM ->
            resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

    @Composable
    private fun WindowChrome(darkTheme: Boolean) {
        val view = LocalView.current
        SideEffect {
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
            // configChanges handles uiMode and fold resizes without recreation, so the
            // drawable set in onCreate would go stale after a live theme switch and
            // flash the old colour in newly exposed areas during a fold/unfold resize.
            window.setBackgroundDrawable(ColorDrawable(if (darkTheme) 0xFF000000.toInt() else 0xFFFAF8EF.toInt()))
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
    val activity = LocalActivity.current
    val doubleBackToExit = remember { DoubleBackToExit() }
    var exitPromptCount by remember { mutableIntStateOf(0) }
    var exitPromptVisible by remember { mutableStateOf(false) }

    LaunchedEffect(exitPromptCount) {
        if (exitPromptCount == 0) return@LaunchedEffect
        exitPromptVisible = true
        delay(3_000L)
        exitPromptVisible = false
    }

    // Navigating to or from Settings disarms the exit window, so back presses that were
    // navigation can never count toward the double press that closes the app.
    LaunchedEffect(showSettings) {
        doubleBackToExit.reset()
        exitPromptVisible = false
    }

    // Registered outside the isLoaded gate so a back press during the brief load
    // window still requires the double press instead of exiting immediately. The
    // Settings BackHandler composes later, so it wins while Settings is open.
    BackHandler(enabled = !showSettings) {
        if (doubleBackToExit.onBackPressed(SystemClock.elapsedRealtime())) {
            activity?.finish()
            return@BackHandler
        }
        exitPromptCount++
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.page),
    ) {
        if (viewModel.isLoaded) {
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
        ExitPrompt(
            visible = exitPromptVisible && !showSettings,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
private fun ExitPrompt(visible: Boolean, modifier: Modifier = Modifier) {
    val colors = LocalOpenColors.current
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 150)),
        exit = fadeOut(animationSpec = tween(durationMillis = 500)),
        modifier = modifier,
    ) {
        Text(
            text = "Press back again to exit",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = colors.onPrimary,
            modifier = Modifier
                // Announce the warning to screen readers; without it TalkBack users
                // would get no feedback before the second press closes the app.
                .semantics { liveRegion = LiveRegionMode.Polite }
                .clip(RoundedCornerShape(24.dp))
                .background(colors.primaryButton)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        )
    }
}
