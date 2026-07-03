package com.tjcdeveloper.open2048.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tjcdeveloper.open2048.game.Direction
import com.tjcdeveloper.open2048.game.GameState
import com.tjcdeveloper.open2048.game.Tile
import com.tjcdeveloper.open2048.ui.theme.LocalOpenColors
import com.tjcdeveloper.open2048.ui.theme.tileColors
import kotlinx.coroutines.delay
import kotlin.math.abs

private const val SLIDE_MILLIS = 100
private const val POP_MILLIS = 150
private const val SWIPE_THRESHOLD_PX = 50f

/** Swipe detection for board moves. Apply to the board or any region surrounding it. */
fun Modifier.swipeInput(onSwipe: (Direction) -> Unit): Modifier = pointerInput(Unit) {
    var drag = Offset.Zero
    detectDragGestures(
        onDragStart = { drag = Offset.Zero },
        onDragEnd = { swipeDirection(drag)?.let(onSwipe) },
    ) { change, amount ->
        change.consume()
        drag += amount
    }
}

@Composable
fun GameBoard(
    game: GameState,
    boardSize: Dp,
    gap: Dp,
    cornerRadius: Dp,
    isGameOver: Boolean,
    showWin: Boolean,
    onTryAgain: () -> Unit,
    onKeepGoing: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalOpenColors.current
    val n = game.size
    val cell = (boardSize - gap * (n + 1)) / n

    Box(
        modifier = modifier
            .size(boardSize)
            .clip(RoundedCornerShape(cornerRadius))
            .background(colors.boardContainer),
    ) {
        for (row in 0 until n) {
            for (col in 0 until n) {
                Box(
                    modifier = Modifier
                        .offset(x = gap + (cell + gap) * col, y = gap + (cell + gap) * row)
                        .size(cell)
                        .clip(RoundedCornerShape(4.dp))
                        .background(colors.emptyCell),
                )
            }
        }

        // Consumed tiles first so merge survivors render above them.
        game.tiles.sortedBy { if (it.consumed) 0 else 1 }.forEach { tile ->
            key(tile.id) { TileView(tile = tile, cell = cell, gap = gap) }
        }

        if (isGameOver || showWin) {
            BoardOverlay(
                title = if (showWin) "You win!" else "Game over!",
                primaryLabel = if (showWin) "Keep going" else "Try again",
                onPrimary = if (showWin) onKeepGoing else onTryAgain,
                secondaryLabel = if (showWin) "New Game" else null,
                onSecondary = onTryAgain,
            )
        }
    }
}

private fun swipeDirection(drag: Offset): Direction? {
    val (dx, dy) = drag
    return when {
        abs(dx) < SWIPE_THRESHOLD_PX && abs(dy) < SWIPE_THRESHOLD_PX -> null
        abs(dx) >= abs(dy) && dx > 0 -> Direction.RIGHT
        abs(dx) >= abs(dy) -> Direction.LEFT
        dy > 0 -> Direction.DOWN
        else -> Direction.UP
    }
}

@Composable
private fun TileView(tile: Tile, cell: Dp, gap: Dp) {
    val colors = LocalOpenColors.current
    val x by animateDpAsState(
        targetValue = gap + (cell + gap) * tile.col,
        animationSpec = tween(SLIDE_MILLIS),
        label = "tileX",
    )
    val y by animateDpAsState(
        targetValue = gap + (cell + gap) * tile.row,
        animationSpec = tween(SLIDE_MILLIS),
        label = "tileY",
    )
    val scale = remember { Animatable(if (tile.justSpawned) 0f else 1f) }
    LaunchedEffect(tile.id, tile.value) {
        if (tile.justSpawned) {
            delay(SLIDE_MILLIS.toLong())
            scale.animateTo(1f, tween(POP_MILLIS))
        } else if (tile.justMerged) {
            delay(SLIDE_MILLIS.toLong())
            scale.animateTo(1.1f, tween(POP_MILLIS / 2))
            scale.animateTo(1f, tween(POP_MILLIS / 2))
        }
    }

    val (background, textColor) = tileColors(tile.value, colors.isDark)
    val digits = tile.value.toString().length
    val baseSize = when {
        digits <= 2 -> 32f
        digits == 3 -> 26f
        else -> 20f
    }
    val fontSize = (baseSize * (cell.value / 78f)).sp

    Box(
        modifier = Modifier
            .offset(x = x, y = y)
            .size(cell)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .clip(RoundedCornerShape(4.dp))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = tile.value.toString(),
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = textColor,
        )
    }
}

@Composable
private fun BoardOverlay(
    title: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String?,
    onSecondary: () -> Unit,
) {
    val colors = LocalOpenColors.current
    val scrim = if (colors.isDark) Color(0xBA000000) else Color(0xBAEEE4DA)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scrim),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = title,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.text,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton(
                    text = primaryLabel,
                    onClick = onPrimary,
                    modifier = Modifier.height(44.dp),
                )
                if (secondaryLabel != null) {
                    PrimaryButton(
                        text = secondaryLabel,
                        onClick = onSecondary,
                        modifier = Modifier.height(44.dp),
                    )
                }
            }
        }
    }
}
