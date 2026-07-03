package com.tjcdeveloper.open2048.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tjcdeveloper.open2048.ui.theme.LocalOpenColors
import com.tjcdeveloper.open2048.ui.theme.ReplyIcon
import java.util.Locale

fun formatScore(value: Int): String = String.format(Locale.US, "%,d", value)

@Composable
fun AppLogo(size: Dp, fontSize: TextUnit) {
    val colors = LocalOpenColors.current
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(6.dp))
            .background(colors.logo),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "2048",
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold,
            color = colors.onPrimary,
        )
    }
}

@Composable
fun Wordmark(nameFontSize: TextUnit, twoLines: Boolean) {
    val colors = LocalOpenColors.current
    Column {
        Text(
            text = "OPEN",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = colors.secondary,
        )
        Text(
            text = if (twoLines) "Twenty\nForty-Eight" else "Twenty Forty-Eight",
            fontSize = nameFontSize,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = nameFontSize * 1.1f,
            color = colors.text,
        )
    }
}

@Composable
fun ScoreCard(label: String, value: Int, valueFontSize: TextUnit, verticalPadding: Dp, modifier: Modifier = Modifier) {
    val colors = LocalOpenColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(colors.scoreCard)
            .padding(vertical = verticalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = colors.scoreLabel,
        )
        Text(
            text = formatScore(value),
            fontSize = valueFontSize,
            fontWeight = FontWeight.Bold,
            color = colors.scoreValue,
        )
    }
}

@Composable
fun UndoRedoButton(
    mirrored: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalOpenColors.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (enabled) colors.primaryButton else colors.disabledButton)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = ReplyIcon,
            contentDescription = if (mirrored) "Redo" else "Undo",
            tint = if (enabled) colors.onPrimary else colors.disabledIcon,
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer { scaleX = if (mirrored) -1f else 1f },
        )
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalOpenColors.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(colors.primaryButton)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = colors.onPrimary,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
    }
}

@Composable
fun GridSizeChips(
    selected: Int,
    onSelect: (Int) -> Unit,
    chipHeight: Dp,
    onCard: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalOpenColors.current
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(4, 5, 6).forEach { size ->
            val isSelected = size == selected
            val background = when {
                isSelected -> colors.primaryButton
                onCard -> colors.chipUnselectedOnCard
                else -> colors.chipUnselectedOnPage
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(chipHeight)
                    .clip(RoundedCornerShape(6.dp))
                    .background(background)
                    .clickable { onSelect(size) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "$size×$size",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) colors.onPrimary else colors.chipUnselectedText,
                )
            }
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = LocalOpenColors.current.secondary,
    )
}

/** Fills a row with undo + redo buttons sized for the given height. */
@Composable
fun RowScope.UndoRedoPair(
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    height: Dp,
) {
    UndoRedoButton(
        mirrored = false,
        enabled = canUndo,
        onClick = onUndo,
        modifier = Modifier
            .weight(1f)
            .height(height),
    )
    UndoRedoButton(
        mirrored = true,
        enabled = canRedo,
        onClick = onRedo,
        modifier = Modifier
            .weight(1f)
            .height(height),
    )
}
