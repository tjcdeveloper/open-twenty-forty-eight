package com.tjcdeveloper.open2048.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tjcdeveloper.open2048.game.Direction
import com.tjcdeveloper.open2048.ui.theme.LocalOpenColors
import com.tjcdeveloper.open2048.ui.theme.MoreVertIcon

private val COMPACT_BOARD_MAX = 362.dp
private val EXPANDED_BOARD_MAX = 508.dp

private data class BoardSpec(val maxBoard: Dp, val gap: Dp, val cornerRadius: Dp)

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    widthSizeClass: WindowWidthSizeClass,
    onOpenSettings: () -> Unit,
) {
    var confirmNewGame by rememberSaveable { mutableStateOf(false) }
    var pendingGridSize by rememberSaveable { mutableStateOf<Int?>(null) }
    val requestNewGame = {
        if (viewModel.hasProgressToLose) confirmNewGame = true else viewModel.newGame()
    }
    val requestGridSize: (Int) -> Unit = { size ->
        when {
            size == viewModel.gridSize -> Unit
            viewModel.hasProgressToLose -> pendingGridSize = size
            else -> viewModel.setGridSize(size)
        }
    }

    // movableContentOf keeps the board's composition (tile animation state) alive when
    // fold/unfold swaps between the two layouts; recreating it would replay the last
    // spawn/merge animations on every posture change.
    val boardArea = remember(viewModel) {
        movableContentOf { spec: BoardSpec, modifier: Modifier ->
            BoardArea(viewModel, spec, modifier)
        }
    }

    if (widthSizeClass == WindowWidthSizeClass.Compact) {
        CompactGameLayout(viewModel, requestNewGame, onOpenSettings, boardArea)
    } else {
        ExpandedGameLayout(viewModel, requestNewGame, requestGridSize, onOpenSettings, boardArea)
    }

    if (confirmNewGame) {
        ConfirmNewGameDialog(
            onConfirm = {
                confirmNewGame = false
                viewModel.newGame()
            },
            onDismiss = { confirmNewGame = false },
        )
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
private fun CompactGameLayout(
    viewModel: GameViewModel,
    onNewGame: () -> Unit,
    onOpenSettings: () -> Unit,
    boardArea: @Composable (BoardSpec, Modifier) -> Unit,
) {
    val colors = LocalOpenColors.current
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppLogo(size = 42.dp, fontSize = 13.sp)
            Spacer(Modifier.width(10.dp))
            Wordmark(nameFontSize = 18.sp, twoLines = false)
            Spacer(Modifier.weight(1f))
            OverflowMenuButton(onOpenSettings)
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ScoreCard("SCORE", viewModel.game.score, 20.sp, 7.dp, Modifier.weight(1f), announceChanges = true)
            ScoreCard("BEST", viewModel.bestScore, 20.sp, 7.dp, Modifier.weight(1f))
        }

        Spacer(Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PrimaryButton("New Game", onNewGame, Modifier.height(48.dp))
            Spacer(Modifier.weight(1f))
            UndoRedoButton(
                mirrored = false,
                enabled = viewModel.canUndo,
                onClick = viewModel::undo,
                modifier = Modifier.width(62.dp).height(57.dp),
            )
            UndoRedoButton(
                mirrored = true,
                enabled = viewModel.canRedo,
                onClick = viewModel::redo,
                modifier = Modifier.width(62.dp).height(57.dp),
            )
        }

        boardArea(
            BoardSpec(maxBoard = COMPACT_BOARD_MAX, gap = 10.dp, cornerRadius = 8.dp),
            Modifier.weight(1f).fillMaxWidth(),
        )

        Text(
            text = "Free & open source. No ads, no purchases.",
            fontSize = 13.sp,
            color = colors.secondary,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 8.dp),
        )
    }
}

@Composable
private fun ExpandedGameLayout(
    viewModel: GameViewModel,
    onNewGame: () -> Unit,
    onSelectGridSize: (Int) -> Unit,
    onOpenSettings: () -> Unit,
    boardArea: @Composable (BoardSpec, Modifier) -> Unit,
) {
    val colors = LocalOpenColors.current
    Row(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            boardArea(
                BoardSpec(maxBoard = EXPANDED_BOARD_MAX, gap = 12.dp, cornerRadius = 10.dp),
                Modifier.weight(1f).fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Free & open source. No ads, no purchases.",
                fontSize = 13.sp,
                color = colors.secondary,
            )
        }

        Column(
            modifier = Modifier
                .width(240.dp)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppLogo(size = 48.dp, fontSize = 14.sp)
                Spacer(Modifier.width(10.dp))
                Wordmark(nameFontSize = 19.sp, twoLines = true)
                Spacer(Modifier.weight(1f))
                OverflowMenuButton(onOpenSettings)
            }
            ScoreCard("SCORE", viewModel.game.score, 28.sp, 12.dp, Modifier.fillMaxWidth(), announceChanges = true)
            ScoreCard("BEST", viewModel.bestScore, 28.sp, 12.dp, Modifier.fillMaxWidth())
            PrimaryButton("New Game", onNewGame, Modifier.fillMaxWidth().height(48.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                UndoRedoPair(
                    canUndo = viewModel.canUndo,
                    canRedo = viewModel.canRedo,
                    onUndo = viewModel::undo,
                    onRedo = viewModel::redo,
                    height = 62.dp,
                )
            }
            Spacer(Modifier.height(4.dp))
            SectionLabel("GRID SIZE")
            GridSizeChips(
                selected = viewModel.gridSize,
                onSelect = onSelectGridSize,
                chipHeight = 48.dp,
                onCard = false,
            )
        }
    }
}

@Composable
private fun BoardArea(
    viewModel: GameViewModel,
    spec: BoardSpec,
    modifier: Modifier = Modifier,
) {
    val overlayShown = viewModel.isGameOver || viewModel.showWinOverlay
    // Swipe is the only pointer input, so expose the four moves as accessibility
    // actions — without them the game cannot be played with TalkBack. They return
    // the real outcome so an ineffective move is distinguishable, and disappear
    // while an overlay makes the board inert.
    val moveActions = if (overlayShown) {
        emptyList()
    } else {
        Direction.entries.map { direction ->
            CustomAccessibilityAction("Move ${direction.name.lowercase()}") {
                viewModel.move(direction)
            }
        }
    }
    BoxWithConstraints(
        modifier = modifier
            .swipeInput(onSwipe = { viewModel.move(it) })
            .semantics {
                contentDescription = "${viewModel.gridSize} by ${viewModel.gridSize} game board"
                customActions = moveActions
            },
        contentAlignment = Alignment.Center,
    ) {
        val boardSize = minOf(maxWidth, maxHeight, spec.maxBoard)
        GameBoard(
            game = viewModel.game,
            boardSize = boardSize,
            gap = spec.gap,
            cornerRadius = spec.cornerRadius,
            isGameOver = viewModel.isGameOver,
            showWin = viewModel.showWinOverlay,
            onTryAgain = viewModel::newGame,
            onKeepGoing = viewModel::acknowledgeWin,
        )
    }
}

@Composable
private fun OverflowMenuButton(onClick: () -> Unit) {
    val colors = LocalOpenColors.current
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = MoreVertIcon,
            contentDescription = "Settings",
            tint = colors.text,
            modifier = Modifier.size(20.dp),
        )
    }
}
