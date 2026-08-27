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
import com.example.backgammon.domain.BLACK_BAR
import com.example.backgammon.domain.BLACK_OFF
import com.example.backgammon.domain.GameState
import com.example.backgammon.domain.Move
import com.example.backgammon.domain.Side
import com.example.backgammon.domain.WHITE_BAR
import com.example.backgammon.domain.WHITE_OFF
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay

enum class AnchorKind {
  PointBottom,
  PointTop,
  BarBlack,
  BarWhite,
  OffWhite,
  OffBlack,
}

data class AnchorSlot(val origin: Offset, val width: Float, val height: Float, val kind: AnchorKind)

class BoardAnchors {
  val slots = mutableStateMapOf<Int, AnchorSlot>()

  fun putPoint(point: Int, coordinates: LayoutCoordinates, pointUp: Boolean) {
    val root = coordinates.positionInRoot()
    val size = coordinates.size
    slots[point] =
      AnchorSlot(
        origin = Offset(root.x, root.y),
        width = size.width.toFloat(),
        height = size.height.toFloat(),
        kind = if (pointUp) AnchorKind.PointBottom else AnchorKind.PointTop,
      )
  }

  fun putBar(point: Int, coordinates: LayoutCoordinates) {
    val root = coordinates.positionInRoot()
    val size = coordinates.size
    slots[point] =
      AnchorSlot(
        origin = Offset(root.x, root.y),
        width = size.width.toFloat(),
        height = size.height.toFloat(),
        kind = if (point == BLACK_BAR) AnchorKind.BarBlack else AnchorKind.BarWhite,
      )
  }

  fun putOffWell(point: Int, coordinates: LayoutCoordinates) {
    val root = coordinates.positionInRoot()
    val size = coordinates.size
    slots[point] =
      AnchorSlot(
        origin = Offset(root.x, root.y),
        width = size.width.toFloat(),
        height = size.height.toFloat(),
        kind = if (point == BLACK_OFF) AnchorKind.OffBlack else AnchorKind.OffWhite,
      )
  }
}

fun occupancy(board: GameState, point: Int): Int =
  when (point) {
    WHITE_BAR -> board.whiteBar
    BLACK_BAR -> board.blackBar
    WHITE_OFF -> board.whiteOff
    BLACK_OFF -> board.blackOff
    else -> board.countAt(point)
  }

/** Visual board has already lifted the mover, so depart from the old top (remaining + 1). */
fun departShown(board: GameState, from: Int): Int {
  val remaining = occupancy(board, from)
  return if (from == WHITE_OFF || from == BLACK_OFF) trayStackCount(remaining + 1) else stackShown(remaining + 1)
}

/** Land on the new top of the destination. A hit replaces the blot instead of stacking on it. */
fun arriveShown(board: GameState, move: Move): Int {
  val current = occupancy(board, move.to)
  return when {
    move.to == WHITE_OFF || move.to == BLACK_OFF -> trayStackCount(current + 1)
    move.hit -> 1
    else -> stackShown(current + 1)
  }
}

fun stackTopCenter(slot: AnchorSlot, shown: Int, checkerPx: Float): Offset {
  val x = slot.origin.x + slot.width / 2f
  val y =
    when (slot.kind) {
      AnchorKind.PointBottom -> pointStackTopCenterY(slot.origin.y, slot.height, shown, checkerPx, pointUp = true)
      AnchorKind.PointTop -> pointStackTopCenterY(slot.origin.y, slot.height, shown, checkerPx, pointUp = false)
      AnchorKind.BarBlack -> barStackTopCenterY(slot.origin.y, slot.height, shown, checkerPx, black = true)
      AnchorKind.BarWhite -> barStackTopCenterY(slot.origin.y, slot.height, shown, checkerPx, black = false)
      AnchorKind.OffBlack ->
        trayPileTopCenterY(slot.origin.y, slot.height, shown, checkerPx, fromTop = true)
      AnchorKind.OffWhite ->
        trayPileTopCenterY(slot.origin.y, slot.height, shown, checkerPx, fromTop = false)
    }
  return Offset(x, y)
}

@Composable
fun rememberBoardAnchors(): BoardAnchors = remember { BoardAnchors() }

@Composable
fun FlyingChecker(
  animId: Int,
  move: Move?,
  side: Side?,
  board: GameState,
  anchors: BoardAnchors,
  originInRoot: Offset,
  onSettled: () -> Unit,
) {
  if (move == null || side == null) return
  val progress = remember(animId) { Animatable(0f) }
  val density = LocalDensity.current
  val checker = LocalCheckerSize.current
  val checkerPx = with(density) { checker.toPx() }

  val fromSlot = anchors.slots[move.from]
  val toSlot = anchors.slots[move.to]
  val from = fromSlot?.let { stackTopCenter(it, departShown(board, move.from), checkerPx) }
  val to = toSlot?.let { stackTopCenter(it, arriveShown(board, move), checkerPx) }

  LaunchedEffect(animId) {
    var start: Offset? = null
    var end: Offset? = null
    var tries = 0
    while (tries < 20 && (start == null || end == null)) {
      val fromSlot = anchors.slots[move.from]
      val toSlot = anchors.slots[move.to]
      if (fromSlot != null && toSlot != null) {
        start = stackTopCenter(fromSlot, departShown(board, move.from), checkerPx)
        end = stackTopCenter(toSlot, arriveShown(board, move), checkerPx)
        break
      }
      delay(16)
      tries++
    }
    if (start == null || end == null) {
      delay(360)
      onSettled()
      return@LaunchedEffect
    }
    progress.snapTo(0f)
    progress.animateTo(1f, tween(durationMillis = 380, easing = FastOutSlowInEasing))
    onSettled()
  }

  if (from == null || to == null) return
  val t = progress.value
  val x = from.x + (to.x - from.x) * t - originInRoot.x
  val y = from.y + (to.y - from.y) * t - originInRoot.y - (sin(Math.PI.toFloat() * t) * 48f)
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
