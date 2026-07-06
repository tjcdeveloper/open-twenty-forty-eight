package com.tjcdeveloper.open2048.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tjcdeveloper.open2048.BuildConfig
import com.tjcdeveloper.open2048.data.ThemePreference
import com.tjcdeveloper.open2048.ui.theme.ArrowBackIcon
import com.tjcdeveloper.open2048.ui.theme.LocalOpenColors

private const val GITHUB_URL = "https://github.com/tjcdeveloper/open-twenty-forty-eight"

@Composable
fun SettingsScreen(viewModel: GameViewModel, onBack: () -> Unit) {
    val colors = LocalOpenColors.current
    var pendingGridSize by rememberSaveable { mutableStateOf<Int?>(null) }
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .align(Alignment.TopCenter)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(role = Role.Button, onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = ArrowBackIcon,
                        contentDescription = "Back",
                        tint = colors.text,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Settings",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.text,
                )
            }

            SettingsSection("APPEARANCE") {
                ThemeSegmentedControl(
                    selected = viewModel.theme,
                    onSelect = viewModel::updateTheme,
                )
            }

            SettingsSection("GAMEPLAY") {
                Text(
                    text = "Grid size",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.text,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Starts a new game when changed",
                    fontSize = 13.sp,
                    color = colors.secondary,
                )
                Spacer(Modifier.height(12.dp))
                GridSizeChips(
                    selected = viewModel.gridSize,
                    onSelect = { size ->
                        when {
                            size == viewModel.gridSize -> Unit
                            viewModel.hasProgressToLose -> pendingGridSize = size
                            else -> viewModel.setGridSize(size)
                        }
                    },
                    chipHeight = 48.dp,
                    onCard = true,
                )
            }

            SettingsSection("ABOUT") {
                val uriHandler = LocalUriHandler.current
                Text(
                    text = "Open Twenty Forty-Eight · v${BuildConfig.VERSION_NAME}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.text,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Free and open source. No ads, no purchases, no tracking.",
                    fontSize = 13.sp,
                    color = colors.secondary,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "View source on GitHub →",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.link,
                    modifier = Modifier.clickable(role = Role.Button) {
                        // No browser available (e.g. disabled by policy) must not crash.
                        runCatching { uriHandler.openUri(GITHUB_URL) }
                    },
                )
            }
        }
    }

    pendingGridSize?.let { size ->
        ConfirmNewGameDialog(
            onConfirm = {
                pendingGridSize = null
                viewModel.setGridSize(size)
            },
            onDismiss = { pendingGridSize = null },
        )
    }
}

@Composable
private fun SettingsSection(label: String, content: @Composable () -> Unit) {
    val colors = LocalOpenColors.current
    Column {
        SectionLabel(label)
        Spacer(Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(colors.settingsCard)
                .padding(16.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun ThemeSegmentedControl(selected: ThemePreference, onSelect: (ThemePreference) -> Unit) {
    val colors = LocalOpenColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.controlTrack)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf(
            ThemePreference.LIGHT to "Light",
            ThemePreference.DARK to "Dark",
            ThemePreference.SYSTEM to "System",
        ).forEach { (preference, label) ->
            val isSelected = preference == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) colors.primaryButton else colors.controlTrack)
                    .selectable(selected = isSelected, role = Role.RadioButton) { onSelect(preference) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) colors.onPrimary else colors.chipUnselectedText,
                )
            }
        }
    }
}
