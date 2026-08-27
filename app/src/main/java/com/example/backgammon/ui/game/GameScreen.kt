package com.example.backgammon.ui.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import com.example.backgammon.domain.BLACK_BAR
import com.example.backgammon.domain.BLACK_OFF
import com.example.backgammon.domain.GameState
import com.example.backgammon.domain.PlayPhase
import com.example.backgammon.domain.Side
import com.example.backgammon.domain.WHITE_BAR
import com.example.backgammon.domain.WHITE_OFF
import com.example.backgammon.engine.AiLevel
import com.example.backgammon.theme.BackgammonTheme
import com.example.backgammon.theme.Highlight

@Composable
fun GameScreen(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val viewModel: BackgammonViewModel = viewModel(factory = BackgammonViewModel.factory(context))
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  CompositionLocalProvider(LocalHudChrome provides state.boardStyle.chrome) {
    GameTable(state = state, viewModel = viewModel, modifier = modifier)
  }
}

@Composable
private fun GameTable(state: GameUiState, viewModel: BackgammonViewModel, modifier: Modifier) {
  LaunchedEffect(state.phase) {
    if (state.phase == PlayPhase.NO_MOVES) {
      delay(1_200)
      viewModel.onNoMovesSettled()
    }
  }
  Box(modifier = modifier.fillMaxSize().background(state.boardStyle.backdrop)) {
    Image(
      painter = painterResource(state.boardStyle.cloth),
      contentDescription = null,
      modifier = Modifier.fillMaxSize(),
      contentScale = ContentScale.Crop,
    )
    val anchors = rememberBoardAnchors()
    var origin by remember { mutableStateOf(Offset.Zero) }
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val hudGutter = 112.dp
      val playWidth = (maxWidth - hudGutter).coerceAtLeast(0.dp)
      val boardHeight = minOf(maxHeight, playWidth / BOARD_ASPECT)
      val boardWidth = boardHeight * BOARD_ASPECT
      val layout = boardLayout(boardWidth.value, boardHeight.value)
      Box(modifier = Modifier.fillMaxSize().padding(end = hudGutter)) {
        BoardFrame(
          style = state.boardStyle,
          modifier =
            Modifier.align(Alignment.Center)
              .width(boardWidth)
              .height(boardHeight)
              .shadow(14.dp, RectangleShape, clip = false),
        ) {
        BoxWithConstraints(Modifier.fillMaxSize().onGloballyPositioned { origin = it.positionInRoot() }) {
          CompositionLocalProvider(LocalCheckerSize provides layout.checker.dp) {
            BackgammonBoard(
              layout = layout,
              board = state.board,
              selected = state.selected,
              legalTargets = state.legalTargets,
              onPointClick = viewModel::onPointClicked,
              enabled = state.boardInteractive,
              anchors = anchors,
              dice = {
                DiceTray(
                  rollA = state.rollA,
                  rollB = state.rollB,
                  usedA = state.usedA,
                  usedB = state.usedB,
                  phase = state.phase,
                  enabled = state.diceInteractive,
                  onTap = viewModel::onDiceTapped,
                  onRollSettled = viewModel::onRollSettled,
                  dieSize = (layout.frame * 1.14f * 0.63f).dp,
                  showHint = false,
                )
              },
            )
            FlyingChecker(
              animId = state.animId,
              move = state.animatingMove,
              side = state.animatingSide,
              board = state.board,
              anchors = anchors,
              originInRoot = origin,
              onSettled = viewModel::onMoveSettled,
            )
          }
        }
        }
      }
      Column(
        modifier =
          Modifier.align(Alignment.CenterEnd)
            .fillMaxHeight()
            .width(hudGutter)
            .offset(x = (-10).dp)
            .padding(top = 20.dp, bottom = 20.dp, end = 12.dp, start = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Column(
          modifier = Modifier.width(IntrinsicSize.Max),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          BoardStyleMenu(selected = state.boardStyle, onPick = viewModel::setBoardStyle)
          AiLevelMenu(level = state.aiLevel, onPick = viewModel::setAiLevel)
          HudButton(onClick = viewModel::requestNewGame, label = "New game")
        }
        Spacer(Modifier.weight(1f))
        if (state.statusText.isNotEmpty()) {
          Text(
            text = state.statusText,
            style = hudStatusStyle,
            color = state.boardStyle.chrome.status,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }
    }
  }
  if (state.askConfirmNewGame) {
    TableDialog(
      title = "Start a new game?",
      text = "This will erase the game in progress.",
      confirmLabel = "New game",
      onConfirm = viewModel::confirmNewGame,
      dismissLabel = "Cancel",
      onDismiss = viewModel::dismissNewGameConfirm,
      onDismissRequest = viewModel::dismissNewGameConfirm,
    )
  }
  if (state.board.winner != null && !state.askResume && !state.askConfirmNewGame) {
    TableDialog(
      title = if (state.board.winner == Side.WHITE) "You won!" else "CPU wins",
      text =
        if (state.board.winner == Side.WHITE) "All of your checkers are off the board."
        else "The computer bore off all its checkers.",
      confirmLabel = "New game",
      onConfirm = viewModel::confirmNewGame,
    )
  }
  if (state.askResume) {
    TableDialog(
      title = "Resume game?",
      text = "A game was in progress. Do you want to resume it or start a new one?",
      confirmLabel = "Resume",
      onConfirm = viewModel::resumeSavedGame,
      dismissLabel = "New game",
      onDismiss = viewModel::startNewGameFromPrompt,
    )
  }
}

private val hudLabelStyle =
  TextStyle(
    fontFamily = FontFamily.Serif,
    fontWeight = FontWeight.SemiBold,
    fontSize = 12.sp,
    letterSpacing = 0.6.sp,
    lineHeight = 14.sp,
  )

private val hudStatusStyle =
  TextStyle(
    fontFamily = FontFamily.Serif,
    fontWeight = FontWeight.Medium,
    fontSize = 13.sp,
    letterSpacing = 0.3.sp,
    lineHeight = 16.sp,
  )

private val hudButtonShape = RoundedCornerShape(4.dp)

private val LocalHudChrome = staticCompositionLocalOf { BoardStyle.ORIGINAL.chrome }

private val hudDialogTitleStyle =
  TextStyle(
    fontFamily = FontFamily.Serif,
    fontWeight = FontWeight.SemiBold,
    fontSize = 16.sp,
    letterSpacing = 0.4.sp,
    lineHeight = 20.sp,
  )

@Composable
private fun TableDialog(
  title: String,
  text: String,
  confirmLabel: String,
  onConfirm: () -> Unit,
  dismissLabel: String? = null,
  onDismiss: (() -> Unit)? = null,
  onDismissRequest: () -> Unit = {},
) {
  val chrome = LocalHudChrome.current
  AlertDialog(
    onDismissRequest = onDismissRequest,
    modifier = Modifier.border(BorderStroke(1.dp, chrome.border), hudButtonShape),
    shape = hudButtonShape,
    containerColor = chrome.fill,
    titleContentColor = chrome.onFill,
    textContentColor = chrome.onFill,
    tonalElevation = 0.dp,
    title = {
      Text(text = title, style = hudDialogTitleStyle, color = chrome.onFill, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    },
    text = {
      Text(text = text, style = hudStatusStyle, color = chrome.onFill, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    },
    confirmButton = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (dismissLabel != null && onDismiss != null) {
          HudTextAction(label = dismissLabel, onClick = onDismiss)
        }
        HudTextAction(label = confirmLabel, onClick = onConfirm, selected = true)
      }
    },
  )
}

@Composable
private fun HudButton(onClick: () -> Unit, label: String, modifier: Modifier = Modifier) {
  val chrome = LocalHudChrome.current
  Button(
    onClick = onClick,
    modifier = modifier.fillMaxWidth().defaultMinSize(minWidth = 0.dp, minHeight = 0.dp).height(34.dp).shadow(4.dp, hudButtonShape),
    shape = hudButtonShape,
    border = BorderStroke(1.dp, chrome.border),
    colors =
      ButtonDefaults.buttonColors(
        containerColor = chrome.fill,
        contentColor = chrome.onFill,
        disabledContainerColor = chrome.fill,
        disabledContentColor = chrome.onFill.copy(alpha = 0.5f),
      ),
    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
  ) {
    Text(text = label, style = hudLabelStyle, maxLines = 1, textAlign = TextAlign.Center)
  }
}

@Composable
private fun HudTextAction(
  label: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  selected: Boolean = false,
  enabled: Boolean = true,
) {
  val chrome = LocalHudChrome.current
  Text(
    text = label,
    style = hudLabelStyle.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold, fontSize = 13.sp),
    color = if (selected) chrome.accent else chrome.onFill,
    textAlign = TextAlign.Center,
    maxLines = 1,
    modifier =
      modifier
        .clickable(enabled = enabled, onClick = onClick)
        .padding(horizontal = 8.dp, vertical = 8.dp),
  )
}

@Composable
private fun ChoiceDialog(onDismissRequest: () -> Unit, content: @Composable () -> Unit) {
  val chrome = LocalHudChrome.current
  Dialog(onDismissRequest = onDismissRequest) {
    Column(
      modifier =
        Modifier
          .widthIn(min = 168.dp)
          .border(BorderStroke(1.dp, chrome.border), hudButtonShape)
          .background(chrome.fill, hudButtonShape)
          .padding(vertical = 8.dp, horizontal = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      content()
    }
  }
}

@Composable
private fun HudChoiceMenu(anchorLabel: String, content: @Composable (close: () -> Unit) -> Unit) {
  var open by remember { mutableStateOf(false) }
  HudButton(onClick = { open = true }, label = anchorLabel)
  if (open) {
    ChoiceDialog(onDismissRequest = { open = false }) { content { open = false } }
  }
}

@Composable
private fun AiLevelMenu(level: AiLevel, onPick: (AiLevel) -> Unit) {
  HudChoiceMenu(anchorLabel = level.label) { close ->
    AiLevel.entries.forEach { option ->
      HudTextAction(
        label = option.label,
        selected = option == level,
        onClick = {
          onPick(option)
          close()
        },
        modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}

@Composable
private fun BoardStyleMenu(selected: BoardStyle, onPick: (BoardStyle) -> Unit) {
  HudChoiceMenu(anchorLabel = "Board") { close ->
    BoardStyle.entries.forEach { option ->
      HudTextAction(
        label = option.label,
        selected = option == selected,
        onClick = {
          onPick(option)
          close()
        },
        modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}

@Composable
private fun BoardFrame(style: BoardStyle, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
  Box(modifier = modifier) {
    Image(
      painter = painterResource(style.drawable),
      contentDescription = style.label,
      modifier = Modifier.fillMaxSize(),
      contentScale = ContentScale.FillBounds,
    )
    content()
  }
}

@Composable
private fun BackgammonBoard(
  board: GameState,
  selected: Int?,
  legalTargets: Set<Int>,
  onPointClick: (Int) -> Unit,
  enabled: Boolean,
  anchors: BoardAnchors,
  layout: BoardLayout,
  dice: @Composable () -> Unit,
) {
  val top = layout.topFrame.dp
  val bottom = layout.bottomFrame.dp
  Row(modifier = Modifier.fillMaxSize()) {
    Spacer(Modifier.width(layout.leftFrame.dp))
    Column(modifier = Modifier.width(layout.leftPlay.dp).padding(top = top, bottom = bottom).fillMaxHeight()) {
      PointRow(
        points = (13..18).toList(),
        board,
        selected,
        legalTargets,
        onPointClick,
        enabled,
        pointUp = false,
        anchors = anchors,
        modifier = Modifier.weight(1f),
      )
      PointRow(
        points = (12 downTo 7).toList(),
        board,
        selected,
        legalTargets,
        onPointClick,
        enabled,
        pointUp = true,
        anchors = anchors,
        modifier = Modifier.weight(1f),
      )
    }
    Bar(
      board = board,
      selected = selected,
      legalTargets = legalTargets,
      onPointClick = onPointClick,
      enabled = enabled,
      anchors = anchors,
      modifier = Modifier.fillMaxHeight().padding(top = top, bottom = bottom).width(layout.barWidth.dp).zIndex(1f),
    )
    Box(modifier = Modifier.width(layout.rightPlay.dp).padding(top = top, bottom = bottom).fillMaxHeight()) {
      Column(Modifier.fillMaxSize()) {
        PointRow(
          points = (19..24).toList(),
          board,
          selected,
          legalTargets,
          onPointClick,
          enabled,
          pointUp = false,
          anchors = anchors,
          modifier = Modifier.weight(1f),
        )
        PointRow(
          points = (6 downTo 1).toList(),
          board,
          selected,
          legalTargets,
          onPointClick,
          enabled,
          pointUp = true,
          anchors = anchors,
          modifier = Modifier.weight(1f),
        )
      }
      Box(Modifier.align(Alignment.Center).offset(y = (-layout.frame * 0.04f).dp)) { dice() }
    }
    OffRack(
      whiteOff = board.whiteOff,
      blackOff = board.blackOff,
      layout = layout,
      anchors = anchors,
      modifier = Modifier.fillMaxHeight().width(layout.trayWidth.dp),
    )
  }
}

@Composable
private fun OffRack(
  whiteOff: Int,
  blackOff: Int,
  layout: BoardLayout,
  anchors: BoardAnchors,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier) {
    Spacer(Modifier.height(layout.wellTop.dp))
    OffSlot(
      side = Side.BLACK,
      count = trayStackCount(blackOff),
      modifier =
        Modifier.height(layout.blackWellHeight.dp)
          .fillMaxWidth()
          .onGloballyPositioned { anchors.putOffWell(BLACK_OFF, it) },
    )
    Spacer(Modifier.height(layout.wellSplit.dp))
    OffSlot(
      side = Side.WHITE,
      count = trayStackCount(whiteOff),
      modifier =
        Modifier.height(layout.whiteWellHeight.dp)
          .fillMaxWidth()
          .onGloballyPositioned { anchors.putOffWell(WHITE_OFF, it) },
    )
    Spacer(Modifier.height(layout.wellBottomPad.dp))
  }
}

@Composable
private fun OffSlot(side: Side, count: Int, modifier: Modifier = Modifier) {
  val checker = LocalCheckerSize.current
  val n = trayStackCount(count)
  BoxWithConstraints(
    modifier = modifier,
    contentAlignment = if (side == Side.BLACK) Alignment.TopCenter else Alignment.BottomCenter,
  ) {
    if (n == 0) return@BoxWithConstraints
    val thickness = trayEdgeThickness(checker.value)
    val step = trayStackStep(n, maxHeight.value, checker.value)
    val overlap = (thickness - step).coerceAtLeast(0f)
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy((-overlap).dp),
    ) {
      repeat(n) { CheckerEdge(side = side, diameter = checker) }
    }
  }
}

@Composable
private fun PointRow(
  points: List<Int>,
  board: GameState,
  selected: Int?,
  legalTargets: Set<Int>,
  onPointClick: (Int) -> Unit,
  enabled: Boolean,
  pointUp: Boolean,
  anchors: BoardAnchors,
  modifier: Modifier = Modifier,
) {
  Row(modifier = modifier.fillMaxWidth()) {
    points.forEachIndexed { index, point ->
      PointColumn(
        point = point,
        board = board,
        selected = selected == point,
        target = point in legalTargets,
        pointUp = pointUp,
        onClick = { onPointClick(point) },
        enabled = enabled,
        anchors = anchors,
        modifier = Modifier.weight(1f).fillMaxHeight(),
      )
    }
  }
}

@Composable
private fun PointColumn(
  point: Int,
  board: GameState,
  selected: Boolean,
  target: Boolean,
  pointUp: Boolean,
  onClick: () -> Unit,
  enabled: Boolean,
  anchors: BoardAnchors,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier =
      modifier
        .clickable(enabled = enabled, onClick = onClick)
        .then(if (selected || target) Modifier.background(Highlight) else Modifier)
        .onGloballyPositioned { anchors.putPoint(point, it, pointUp = pointUp) },
  ) {
    Column(
      modifier = Modifier.fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = if (pointUp) Arrangement.Bottom else Arrangement.Top,
    ) {
      CheckerStack(owner = board.ownerAt(point), count = board.countAt(point), fromTop = !pointUp)
    }
  }
}

@Composable
private fun CheckerStack(owner: Side?, count: Int, fromTop: Boolean) {
  if (owner == null || count == 0) {
    Spacer(Modifier.height(4.dp))
    return
  }
  val shown = stackShown(count)
  val badge = stackBadge(count)
  val size = LocalCheckerSize.current
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy((-size * STACK_OVERLAP)),
  ) {
    repeat(shown) { i ->
      val outer = if (fromTop) i == 0 else i == shown - 1
      CheckerDot(owner, size = size, badge = if (outer) badge else null)
    }
  }
}

@Composable
private fun Bar(
  board: GameState,
  selected: Int?,
  legalTargets: Set<Int>,
  onPointClick: (Int) -> Unit,
  enabled: Boolean,
  anchors: BoardAnchors,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(
      modifier =
        Modifier.weight(1f)
          .fillMaxWidth()
          .clickable(enabled = enabled) { onPointClick(BLACK_BAR) }
          .onGloballyPositioned { anchors.putBar(BLACK_BAR, it) }
          .then(if (selected == BLACK_BAR || BLACK_BAR in legalTargets) Modifier.background(Highlight) else Modifier),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      BarStack(Side.BLACK, board.blackBar)
    }
    Column(
      modifier =
        Modifier.weight(1f)
          .fillMaxWidth()
          .clickable(enabled = enabled) { onPointClick(WHITE_BAR) }
          .onGloballyPositioned { anchors.putBar(WHITE_BAR, it) }
          .then(if (selected == WHITE_BAR || WHITE_BAR in legalTargets) Modifier.background(Highlight) else Modifier),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      BarStack(Side.WHITE, board.whiteBar)
    }
  }
}

@Composable
private fun BarStack(side: Side, count: Int) {
  if (count <= 0) return
  val size = LocalCheckerSize.current
  val shown = stackShown(count)
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy((-size * STACK_OVERLAP)),
  ) {
    repeat(shown) { i ->
      CheckerDot(side, size = size, badge = if (i == shown - 1) stackBadge(count) else null)
    }
  }
}

@Preview(showBackground = true, widthDp = 800, heightDp = 400)
@Composable
private fun GameScreenPreview() {
  BackgammonTheme { GameScreen() }
}
