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
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
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
private val SWIPE_THRESHOLD = 50.dp

/** Swipe detection for board moves. Apply to the board or any region surrounding it. */
fun Modifier.swipeInput(onSwipe: (Direction) -> Unit): Modifier = pointerInput(Unit) {
    // dp, not raw px: a pixel threshold would make swipes ~4x more hair-trigger on a
    // high-density screen than on a low-density one.
    val threshold = SWIPE_THRESHOLD.toPx()
    var drag = Offset.Zero
    detectDragGestures(
        onDragStart = { drag = Offset.Zero },
        onDragEnd = { swipeDirection(drag, threshold)?.let(onSwipe) },
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
    animations: TileAnimationRegistry,
    modifier: Modifier = Modifier,
) {
    val colors = LocalOpenColors.current
    val n = game.size
    val cell = (boardSize - gap * (n + 1)) / n

    // The board is positioned with layout-direction-aware offsets, but swipe deltas and
    // engine columns are absolute — under RTL the grid would mirror and horizontal
    // swipes would move tiles opposite to the finger. Pin the board itself to LTR
    // (a number grid has no reading direction).
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = modifier
                .size(boardSize)
                .clip(RoundedCornerShape(cornerRadius))
                .background(colors.boardContainer),
        ) {
            val overlayShown = isGameOver || showWin
            // The board under an overlay is inert; hide it from TalkBack so focus
            // cannot wander over dead tiles behind the scrim.
            val boardSemantics = if (overlayShown) Modifier.clearAndSetSemantics {} else Modifier
            Box(modifier = Modifier.fillMaxSize().then(boardSemantics)) {
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
                    key(tile.id) { TileView(tile = tile, cell = cell, gap = gap, animations = animations) }
                }
            }

            if (overlayShown) {
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
}

private fun swipeDirection(drag: Offset, threshold: Float): Direction? {
    val (dx, dy) = drag
    return when {
        abs(dx) < threshold && abs(dy) < threshold -> null
        abs(dx) >= abs(dy) && dx > 0 -> Direction.RIGHT
        abs(dx) >= abs(dy) -> Direction.LEFT
        dy > 0 -> Direction.DOWN
        else -> Direction.UP
    }
}

@Composable
private fun TileView(tile: Tile, cell: Dp, gap: Dp, animations: TileAnimationRegistry) {
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
    // Start hidden only for a spawn whose pop hasn't played yet — flags stay set on the
    // state until the next slide, so without the registry a recomposition from scratch
    // (fold layout swap, Settings round trip) would replay old animations.
    val startHidden = remember(tile.id) { tile.justSpawned && !animations.spawnPlayed(tile.id) }
    val scale = remember { Animatable(if (startHidden) 0f else 1f) }
    LaunchedEffect(tile.id, tile.value) {
        when {
            tile.justSpawned && animations.claimSpawn(tile.id) -> {
                delay(SLIDE_MILLIS.toLong())
                scale.animateTo(1f, tween(POP_MILLIS))
            }
            tile.justMerged && animations.claimMerge(tile.id, tile.value) -> {
                // The tile may merge while its spawn pop is still pending (a swipe within
                // SLIDE_MILLIS of spawning); restarting the effect would otherwise leave
                // it stuck at scale 0 until the pulse, blinking it out mid-board.
                if (scale.value < 1f) scale.snapTo(1f)
                delay(SLIDE_MILLIS.toLong())
                scale.animateTo(1.1f, tween(POP_MILLIS / 2))
                scale.animateTo(1f, tween(POP_MILLIS / 2))
            }
            scale.value != 1f -> scale.snapTo(1f)
        }
    }

    val (background, textColor) = tileColors(tile.value, colors.isDark)
    val digits = tile.value.toString().length
    val baseSize = when {
        digits <= 2 -> 32f
        digits == 3 -> 26f
        else -> 20f
    }
    // Sized from the cell in dp (not sp) so system font scaling cannot grow the
    // number past its fixed-size cell and clip it unreadably.
    val fontSize = with(LocalDensity.current) { (cell * (baseSize / 78f)).toSp() }
    val tileSemantics = if (tile.consumed) {
        // Consumed tiles exist only for the merge animation; hide them from TalkBack.
        Modifier.clearAndSetSemantics {}
    } else {
        Modifier.semantics(mergeDescendants = true) {
            contentDescription = "${tile.value}, row ${tile.row + 1}, column ${tile.col + 1}"
        }
    }

    Box(
        modifier = Modifier
            .offset(x = x, y = y)
            .size(cell)
            .then(tileSemantics)
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
                // End-of-game state must be announced, not just drawn over the board.
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton(
                    text = primaryLabel,
                    onClick = onPrimary,
                    modifier = Modifier.height(48.dp),
                )
                if (secondaryLabel != null) {
                    PrimaryButton(
                        text = secondaryLabel,
                        onClick = onSecondary,
                        modifier = Modifier.height(48.dp),
                    )
                }
            }
        }
    }
}
