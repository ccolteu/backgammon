package com.example.backgammon.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.backgammon.domain.Move
import com.example.backgammon.domain.Side
import com.example.backgammon.domain.BLACK_OFF
import com.example.backgammon.domain.WHITE_OFF
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay

class BoardAnchors {
  val points = mutableStateMapOf<Int, Offset>()

  fun put(point: Int, coordinates: LayoutCoordinates, preferBottom: Boolean) {
    val root = coordinates.positionInRoot()
    val size = coordinates.size
    val x = root.x + size.width / 2f
    val y = if (preferBottom) root.y + size.height - size.width * 0.48f else root.y + size.width * 0.48f
    points[point] = Offset(x, y)
  }

  fun putCenter(point: Int, coordinates: LayoutCoordinates) {
    val root = coordinates.positionInRoot()
    val size = coordinates.size
    points[point] = Offset(root.x + size.width / 2f, root.y + size.height / 2f)
  }

  fun putOffPile(point: Int, coordinates: LayoutCoordinates, count: Int, checkerPx: Float) {
    val root = coordinates.positionInRoot()
    val size = coordinates.size
    points[point] =
      Offset(
        root.x + size.width / 2f,
        trayPileTopCenterY(root.y, size.height.toFloat(), count, checkerPx),
      )
  }

  fun putOffEdges(coordinates: LayoutCoordinates) {
    val root = coordinates.positionInRoot()
    val size = coordinates.size
    points[WHITE_OFF] = Offset(root.x + size.width + 28f, root.y + size.height * 0.75f)
    points[BLACK_OFF] = Offset(root.x - 28f, root.y + size.height * 0.25f)
  }
}

@Composable
fun rememberBoardAnchors(): BoardAnchors = remember { BoardAnchors() }

@Composable
fun FlyingChecker(
  animId: Int,
  move: Move?,
  side: Side?,
  anchors: BoardAnchors,
  originInRoot: Offset,
  onSettled: () -> Unit,
) {
  if (move == null || side == null) return
  val progress = remember(animId) { Animatable(0f) }
  val density = LocalDensity.current

  LaunchedEffect(animId) {
    val from = anchors.points[move.from]
    val to = anchors.points[move.to]
    if (from == null || to == null) {
      delay(360)
      onSettled()
      return@LaunchedEffect
    }
    progress.snapTo(0f)
    progress.animateTo(1f, tween(durationMillis = 380, easing = FastOutSlowInEasing))
    onSettled()
  }

  val from = anchors.points[move.from]
  val to = anchors.points[move.to]
  if (from == null || to == null) return
  val t = progress.value
  val x = from.x + (to.x - from.x) * t - originInRoot.x
  val y = from.y + (to.y - from.y) * t - originInRoot.y - (sin(Math.PI.toFloat() * t) * 48f)
  val checker = LocalCheckerSize.current
  val sizePx = with(density) { checker.roundToPx() }
  Box(
    modifier =
      Modifier
        .offset { IntOffset((x - sizePx / 2f).roundToInt(), (y - sizePx / 2f).roundToInt()) }
        .size(checker),
  ) {
    CheckerDot(side = side, size = checker)
  }
}
