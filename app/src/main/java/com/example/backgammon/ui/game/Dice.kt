package com.example.backgammon.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.backgammon.domain.PlayPhase
import com.example.backgammon.theme.Brass
import com.example.backgammon.theme.Cream
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Ivory = Color(0xFFF7F0E2)
private val IvoryShade = Color(0xFFC9B48A)
private val PipHole = Color(0xFF1A120C)

private class DieSpin(initial: Euler) {
  val rotX = Animatable(initial.x)
  val rotY = Animatable(initial.y)
  val rotZ = Animatable(initial.z)
  val lift = Animatable(0f)

  suspend fun snapTo(pose: Euler) {
    rotX.snapTo(pose.x)
    rotY.snapTo(pose.y)
    rotZ.snapTo(pose.z)
    lift.snapTo(0f)
  }

  suspend fun tumbleTo(pose: Euler, turnsX: Int, turnsY: Int, turnsZ: Int, durationMs: Int) {
    coroutineScope {
      launch { rotX.animateTo(pose.x + 360f * turnsX, tween(durationMs, easing = FastOutSlowInEasing)) }
      launch { rotY.animateTo(pose.y + 360f * turnsY, tween(durationMs, easing = FastOutSlowInEasing)) }
      launch { rotZ.animateTo(pose.z + 360f * turnsZ, tween(durationMs, easing = FastOutSlowInEasing)) }
      launch {
        lift.animateTo(-30f, tween(durationMs / 3, easing = FastOutSlowInEasing))
        lift.animateTo(0f, spring(dampingRatio = 0.38f, stiffness = Spring.StiffnessMediumLow))
      }
    }
  }
}

@Composable
fun DiceTray(
  rollA: Int,
  rollB: Int,
  usedA: Boolean,
  usedB: Boolean,
  phase: PlayPhase,
  enabled: Boolean,
  onTap: () -> Unit,
  onRollSettled: () -> Unit,
  modifier: Modifier = Modifier,
  dieSize: Dp = 88.dp,
  showHint: Boolean = true,
) {
  val shownA = rollA.coerceAtLeast(1)
  val shownB = rollB.coerceAtLeast(1)
  val spinA = remember { DieSpin(settledPose(shownA, -10f)) }
  val spinB = remember { DieSpin(settledPose(shownB, 10f)) }

  LaunchedEffect(phase, rollA, rollB) {
    val poseA = settledPose(shownA, -10f)
    val poseB = settledPose(shownB, 10f)
    if (phase == PlayPhase.ROLLING) {
      coroutineScope {
        launch { spinA.tumbleTo(poseA, turnsX = 3, turnsY = 4, turnsZ = 2, durationMs = 780) }
        launch {
          delay(70)
          spinB.tumbleTo(poseB, turnsX = 4, turnsY = 3, turnsZ = 2, durationMs = 860)
        }
      }
      onRollSettled()
    } else {
      spinA.snapTo(poseA)
      spinB.snapTo(poseB)
    }
  }

  Column(
    modifier =
      modifier
        .clickable(
          enabled = enabled,
          indication = null,
          interactionSource = remember { MutableInteractionSource() },
          onClick = onTap,
        )
        .padding(vertical = if (showHint) 10.dp else 0.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Row(horizontalArrangement = Arrangement.spacedBy(dieSize * 0.28f), verticalAlignment = Alignment.CenterVertically) {
      CubeDie(spin = spinA, spent = usedA && phase != PlayPhase.ROLLING, value = shownA, dieSize = dieSize)
      CubeDie(spin = spinB, spent = usedB && phase != PlayPhase.ROLLING, value = shownB, dieSize = dieSize)
    }
    if (showHint) {
      Text(
        text = if (enabled) "Tap to roll" else " ",
        color = if (enabled) Brass else Cream.copy(alpha = 0f),
        fontSize = 13.sp,
        modifier = Modifier.padding(top = 10.dp),
      )
    }
  }
}

@Composable
private fun CubeDie(spin: DieSpin, spent: Boolean, value: Int, dieSize: Dp, modifier: Modifier = Modifier) {
  val rotZ = spin.rotZ.value
  val lift = spin.lift.value
  val tumbling = kotlin.math.abs(lift) > 1.5f
  val face = if (tumbling) ((kotlin.math.abs(spin.rotX.value) / 30f).toInt() % 6) + 1 else value.coerceIn(1, 6)
  Box(
    modifier = modifier.size(dieSize + 16.dp),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier =
        Modifier
          .offset(y = (dieSize.value * 0.46f).dp)
          .size(width = dieSize * 0.78f, height = 12.dp)
          .graphicsLayer {
            scaleX = (1f - (-lift) / 90f).coerceIn(0.55f, 1f)
            alpha = (0.3f - (-lift) / 140f).coerceIn(0.06f, 0.3f)
          }
          .background(Color.Black.copy(alpha = 0.55f), CircleShape),
    )
    Canvas(
      modifier =
        Modifier
          .size(dieSize)
          .graphicsLayer {
            translationY = lift
            rotationZ = rotZ
            rotationX = 12f
            cameraDistance = 14f
            alpha = if (spent) 0.45f else 1f
          },
    ) {
      val pad = size.minDimension * 0.06f
      val body = size.minDimension - pad * 2f
      val origin = Offset(pad, pad * 0.7f)
      val radius = body * 0.18f
      val pipR = body * 0.118f
      drawRoundRect(
        color = IvoryShade,
        topLeft = Offset(origin.x, origin.y + body * 0.11f),
        size = androidx.compose.ui.geometry.Size(body, body),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius),
      )
      drawRoundRect(
        color = Ivory,
        topLeft = origin,
        size = androidx.compose.ui.geometry.Size(body, body),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius),
      )
      drawRoundRect(
        color = Color.White.copy(alpha = 0.22f),
        topLeft = Offset(origin.x + body * 0.08f, origin.y + body * 0.07f),
        size = androidx.compose.ui.geometry.Size(body * 0.55f, body * 0.28f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius * 0.7f),
      )
      drawRoundRect(
        color = Color.Black.copy(alpha = 0.07f),
        topLeft = Offset(origin.x + body * 0.08f, origin.y + body * 0.08f),
        size = androidx.compose.ui.geometry.Size(body * 0.84f, body * 0.84f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius * 0.75f),
        style = Stroke(width = body * 0.035f),
      )
      pipUvs(face).forEach { (u, v) ->
        val pip =
          Offset(
            origin.x + body * u,
            origin.y + body * v,
          )
        drawCircle(color = Color.Black.copy(alpha = 0.18f), radius = pipR * 1.08f, center = pip + Offset(pipR * 0.12f, pipR * 0.18f))
        drawCircle(color = PipHole, radius = pipR, center = pip)
        drawCircle(color = Color.White.copy(alpha = 0.2f), radius = pipR * 0.22f, center = pip + Offset(-pipR * 0.28f, -pipR * 0.28f))
      }
    }
  }
}

private fun pipUvs(value: Int): List<Pair<Float, Float>> =
  pipSlots(value).map { index ->
    val col = index % 3
    val row = index / 3
    0.22f + col * 0.28f to 0.22f + row * 0.28f
  }

fun pipSlots(value: Int): Set<Int> =
  when (value) {
    1 -> setOf(4)
    2 -> setOf(0, 8)
    3 -> setOf(0, 4, 8)
    4 -> setOf(0, 2, 6, 8)
    5 -> setOf(0, 2, 4, 6, 8)
    6 -> setOf(0, 2, 3, 5, 6, 8)
    else -> emptySet()
  }
