package com.example.backgammon.ui.game

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.backgammon.theme.BoardFelt
import kotlin.math.sin
import kotlin.random.Random

fun Modifier.walnutGrain(vertical: Boolean = false): Modifier =
  drawWithCache {
    onDrawBehind { drawWalnutGrain(vertical) }
  }

fun Modifier.feltNap(): Modifier =
  drawWithCache {
    onDrawBehind {
      drawFeltNap()
      drawRecessLighting()
    }
  }

fun DrawScope.drawWalnutGrain(vertical: Boolean = false) {
  drawRect(Color(0xFF3A241A))
  val rng = Random(if (vertical) 41 else 27)
  val span = if (vertical) size.width else size.height
  var pos = 0f
  while (pos < span) {
    val band = 1.8f + rng.nextFloat() * 4.2f
    val t = rng.nextFloat()
    val color = Color(red = 0.13f + t * 0.24f, green = 0.07f + t * 0.12f, blue = 0.045f + t * 0.07f)
    if (vertical) {
      drawRect(color = color, topLeft = Offset(pos, 0f), size = Size(band, size.height))
    } else {
      drawRect(color = color, topLeft = Offset(0f, pos), size = Size(size.width, band))
    }
    if (rng.nextFloat() < 0.18f) {
      val wobble = sin(pos * 0.05f) * 10f
      val vein = Color(0xFF1A0E0A).copy(alpha = 0.4f)
      if (vertical) {
        drawLine(
          color = vein,
          start = Offset(pos + band * 0.4f, 0f),
          end = Offset(pos + band * 0.4f + wobble * 0.15f, size.height),
          strokeWidth = 1.1f + rng.nextFloat(),
        )
      } else {
        drawLine(
          color = vein,
          start = Offset(0f, pos + band * 0.4f),
          end = Offset(size.width, pos + band * 0.4f + wobble),
          strokeWidth = 1.1f + rng.nextFloat(),
        )
      }
    }
    pos += band
  }
  drawWalnutBevel(vertical)
}

private fun DrawScope.drawWalnutBevel(vertical: Boolean) {
  val light = Color.White.copy(alpha = 0.16f)
  val shade = Color.Black.copy(alpha = 0.28f)
  if (vertical) {
    drawRect(brush = Brush.horizontalGradient(listOf(light, Color.Transparent)), size = Size(size.width * 0.22f, size.height))
    drawRect(
      brush = Brush.horizontalGradient(listOf(Color.Transparent, shade)),
      topLeft = Offset(size.width * 0.78f, 0f),
      size = Size(size.width * 0.22f, size.height),
    )
  } else {
    drawRect(brush = Brush.verticalGradient(listOf(light, Color.Transparent)), size = Size(size.width, size.height * 0.22f))
    drawRect(
      brush = Brush.verticalGradient(listOf(Color.Transparent, shade)),
      topLeft = Offset(0f, size.height * 0.78f),
      size = Size(size.width, size.height * 0.22f),
    )
  }
}

fun DrawScope.drawFeltNap() {
  drawRect(BoardFelt)
  val rng = Random(91)
  val dots = ((size.width * size.height) / 48f).toInt().coerceIn(400, 9000)
  repeat(dots) {
    val x = rng.nextFloat() * size.width
    val y = rng.nextFloat() * size.height
    val dark = rng.nextBoolean()
    drawCircle(
      color = if (dark) Color(0xFF0E1E14).copy(alpha = 0.55f) else Color(0xFF2A4A34).copy(alpha = 0.4f),
      radius = 0.7f + rng.nextFloat() * 1.2f,
      center = Offset(x, y),
    )
  }
}

fun DrawScope.drawRecessLighting() {
  val shadow = Color.Black.copy(alpha = 0.55f)
  val lip = Color.White.copy(alpha = 0.06f)
  drawRect(
    brush = Brush.horizontalGradient(listOf(shadow, Color.Transparent)),
    size = Size(size.width * 0.045f, size.height),
  )
  drawRect(
    brush = Brush.verticalGradient(listOf(shadow, Color.Transparent)),
    size = Size(size.width, size.height * 0.055f),
  )
  drawRect(
    brush = Brush.horizontalGradient(listOf(Color.Transparent, lip)),
    topLeft = Offset(size.width * 0.94f, 0f),
    size = Size(size.width * 0.06f, size.height),
  )
  drawRect(
    brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.22f))),
    topLeft = Offset(0f, size.height * 0.92f),
    size = Size(size.width, size.height * 0.08f),
  )
}
