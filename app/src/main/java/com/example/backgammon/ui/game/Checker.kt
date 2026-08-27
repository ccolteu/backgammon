package com.example.backgammon.ui.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.backgammon.R
import com.example.backgammon.domain.Side

const val BOARD_ASPECT = 1536f / 1024f
const val LEFT_FRAME_FRAC = 72f / 1536f
const val BAR_LEFT_FRAC = 676f / 1536f
const val BAR_RIGHT_FRAC = 745f / 1536f
const val TRAY_LEFT_FRAC = 1332f / 1536f
const val TOP_FRAME_FRAC = 105f / 1024f
const val BOTTOM_FRAME_FRAC = 119f / 1024f
const val WELL_TOP_FRAC = 80f / 1024f
const val WELL_SPLIT_TOP_FRAC = 459f / 1024f
const val WELL_SPLIT_BOTTOM_FRAC = 521f / 1024f
const val WELL_BOTTOM_FRAC = 908f / 1024f
const val CHECKER_FILL = 0.84f
const val BAR_CHECKER_MAX = 1.06f
const val STACK_OVERLAP = 0f
const val CHECKER_DP = 36
const val CHECKER_SHADOW_X = 0.12f
const val CHECKER_SHADOW_Y = 0.18f
const val CHECKER_SHADOW_ALPHA = 0.45f

data class BoardLayout(
  val leftFrame: Float,
  val topFrame: Float,
  val bottomFrame: Float,
  val barWidth: Float,
  val leftPlay: Float,
  val rightPlay: Float,
  val trayWidth: Float,
  val wellTop: Float,
  val wellSplit: Float,
  val wellBottomPad: Float,
  val wellHeight: Float,
  val blackWellHeight: Float,
  val whiteWellHeight: Float,
  val pointWidth: Float,
  val checker: Float,
  val innerHeight: Float,
  val frame: Float,
)

fun checkerDiameter(pointWidth: Float, halfHeight: Float, barWidth: Float = Float.POSITIVE_INFINITY): Float =
  minOf(pointWidth * CHECKER_FILL, halfHeight / 5f, barWidth * BAR_CHECKER_MAX)

fun boardLayout(width: Float, height: Float): BoardLayout {
  val leftFrame = width * LEFT_FRAME_FRAC
  val barLeft = width * BAR_LEFT_FRAC
  val barRight = width * BAR_RIGHT_FRAC
  val trayLeft = width * TRAY_LEFT_FRAC
  val top = height * TOP_FRAME_FRAC
  val bottom = height * BOTTOM_FRAME_FRAC
  val bar = barRight - barLeft
  val leftPlay = barLeft - leftFrame
  val rightPlay = trayLeft - barRight
  val tray = width - trayLeft
  val wellTop = height * WELL_TOP_FRAC
  val splitTop = height * WELL_SPLIT_TOP_FRAC
  val splitBottom = height * WELL_SPLIT_BOTTOM_FRAC
  val wellBottom = height * WELL_BOTTOM_FRAC
  val wellSplit = splitBottom - splitTop
  val blackWellHeight = (splitTop - wellTop).coerceAtLeast(1f)
  val whiteWellHeight = (wellBottom - splitBottom).coerceAtLeast(1f)
  val wellHeight = minOf(blackWellHeight, whiteWellHeight)
  val innerH = (height - top - bottom).coerceAtLeast(1f)
  val point = (minOf(leftPlay, rightPlay) / 6f).coerceAtLeast(1f)
  val checker = checkerDiameter(point, innerH / 2f, bar)
  return BoardLayout(
    leftFrame = leftFrame,
    topFrame = top,
    bottomFrame = bottom,
    barWidth = bar,
    leftPlay = leftPlay,
    rightPlay = rightPlay,
    trayWidth = tray,
    wellTop = wellTop,
    wellSplit = wellSplit,
    wellBottomPad = height - wellBottom,
    wellHeight = wellHeight,
    blackWellHeight = blackWellHeight,
    whiteWellHeight = whiteWellHeight,
    pointWidth = point,
    checker = checker,
    innerHeight = innerH,
    frame = leftFrame,
  )
}

val LocalCheckerSize = compositionLocalOf { CHECKER_DP.dp }

fun stackBadge(count: Int): Int? = if (count > 5) count else null

fun stackShown(count: Int): Int = minOf(count.coerceAtLeast(0), 5)

fun trayStackCount(off: Int): Int = off.coerceIn(0, 15)

fun trayEdgeThickness(checker: Float): Float = checker * 0.28f

fun trayStackStep(count: Int, wellHeight: Float, checker: Float): Float {
  val n = trayStackCount(count)
  val thickness = trayEdgeThickness(checker)
  if (n <= 1) return thickness
  val available = (wellHeight - 6f).coerceAtLeast(thickness)
  return minOf(thickness, (available - thickness) / (n - 1))
}

/** Center Y of the exposed end of the borne-off pile. White sits on the well floor and grows up; black sits on the well roof and grows down (toward the opponent). */
fun trayPileTopCenterY(
  wellTop: Float,
  wellHeight: Float,
  count: Int,
  checker: Float,
  fromTop: Boolean = false,
): Float {
  val thickness = trayEdgeThickness(checker)
  val n = trayStackCount(count)
  if (fromTop) {
    if (n <= 0) return wellTop + thickness / 2f
    val step = trayStackStep(n, wellHeight, checker)
    val stackH = thickness + (n - 1) * step
    return wellTop + stackH - thickness / 2f
  }
  if (n <= 0) return wellTop + wellHeight - thickness / 2f
  val step = trayStackStep(n, wellHeight, checker)
  val stackH = thickness + (n - 1) * step
  return wellTop + wellHeight - stackH + thickness / 2f
}

@Composable
fun CheckerDot(side: Side, size: Dp = CHECKER_DP.dp, badge: Int? = null, modifier: Modifier = Modifier) {
  val sprite = if (side == Side.WHITE) R.drawable.checker_light else R.drawable.checker_dark
  Box(
    modifier =
      modifier.requiredSize(size).drawBehind {
        val r = this.size.minDimension / 2f
        val cx = r + r * CHECKER_SHADOW_X
        val cy = r + r * CHECKER_SHADOW_Y
        drawCircle(
          brush =
            Brush.radialGradient(
              colors = listOf(Color.Black.copy(alpha = CHECKER_SHADOW_ALPHA * 0.55f), Color.Transparent),
              center = Offset(cx, cy),
              radius = r * 1.12f,
            ),
          radius = r * 1.12f,
          center = Offset(cx, cy),
        )
        drawCircle(
          color = Color.Black.copy(alpha = CHECKER_SHADOW_ALPHA),
          radius = r * 0.88f,
          center = Offset(cx, cy),
        )
      },
    contentAlignment = Alignment.Center,
  ) {
    Image(
      painter = painterResource(sprite),
      contentDescription = null,
      modifier = Modifier.fillMaxSize(),
      contentScale = ContentScale.Fit,
    )
    if (badge != null) {
      Text(
        text = "$badge",
        color = Color.White.copy(alpha = 0.92f),
        fontSize = (size.value * 0.28f).sp,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

@Composable
fun CheckerEdge(side: Side, diameter: Dp, modifier: Modifier = Modifier) {
  val sprite = if (side == Side.WHITE) R.drawable.checker_light_edge else R.drawable.checker_dark_edge
  val thickness = diameter * 0.28f
  Box(
    modifier =
      modifier.requiredSize(width = diameter, height = thickness).drawBehind {
        val w = this.size.width
        val h = this.size.height
        drawOval(
          color = Color.Black.copy(alpha = CHECKER_SHADOW_ALPHA * 0.7f),
          topLeft = Offset(w * CHECKER_SHADOW_X, h * 0.28f),
          size = Size(w * 0.92f, h * 0.85f),
        )
      },
    contentAlignment = Alignment.Center,
  ) {
    Image(
      painter = painterResource(sprite),
      contentDescription = null,
      modifier = Modifier.fillMaxSize(),
      contentScale = ContentScale.Fit,
    )
  }
}
