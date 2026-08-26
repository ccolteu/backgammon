package com.example.backgammon.ui.game

import org.junit.Assert.assertEquals
import org.junit.Test

class DiceMathTest {
  @Test
  fun oppositeFacesDifferByAHalfTurn() {
    val one = topFaceRotation(1)
    val six = topFaceRotation(6)
    assertEquals(180f, kotlin.math.abs(six.x - one.x), 0.01f)
    assertEquals(0f, one.y, 0.01f)
    assertEquals(0f, one.z, 0.01f)
  }

  @Test
  fun eachFaceHasADistinctTopRotation() {
    val poses = (1..6).map { topFaceRotation(it) }.toSet()
    assertEquals(6, poses.size)
  }

  @Test
  fun topFaceNormalPointsOutward() {
    val n = faceNormal(cubeFaces.first { it.value == 1 }.corners)
    assertEquals(0f, n.x, 0.01f)
    assertEquals(true, n.y > 0f)
    assertEquals(0f, n.z, 0.01f)
  }

  @Test
  fun roundBoxPullsCornersInLikeARealDie() {
    val corner = roundBoxPoint(Vec3(1f, 1f, 1f), radius = 0.32f)
    val flat = kotlin.math.sqrt(3f)
    val rounded = kotlin.math.sqrt(corner.x * corner.x + corner.y * corner.y + corner.z * corner.z)
    assertEquals(true, rounded < flat - 0.1f)
    val center = roundBoxPoint(Vec3(0f, 1f, 0f), radius = 0.32f)
    assertEquals(0f, center.x, 0.01f)
    assertEquals(1f, center.y, 0.01f)
    assertEquals(0f, center.z, 0.01f)
  }
}
