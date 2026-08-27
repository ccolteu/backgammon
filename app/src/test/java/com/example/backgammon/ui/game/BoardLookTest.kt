package com.example.backgammon.ui.game

import androidx.compose.ui.graphics.Color
import com.example.backgammon.R
import com.example.backgammon.theme.BoardFelt
import com.example.backgammon.theme.Brass
import com.example.backgammon.theme.Cream
import com.example.backgammon.theme.WalnutBackground
import com.example.backgammon.theme.WalnutRail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardLookTest {
  @Test
  fun overlayTracksTheBoardPhoto() {
    assertEquals(1536f / 1024f, BOARD_ASPECT, 0.001f)
    assertEquals(72f / 1536f, LEFT_FRAME_FRAC, 0.0001f)
    assertEquals(676f / 1536f, BAR_LEFT_FRAC, 0.0001f)
    assertEquals(745f / 1536f, BAR_RIGHT_FRAC, 0.0001f)
    assertEquals(1332f / 1536f, TRAY_LEFT_FRAC, 0.0001f)
    val sum = LEFT_FRAME_FRAC + (BAR_LEFT_FRAC - LEFT_FRAME_FRAC) + (BAR_RIGHT_FRAC - BAR_LEFT_FRAC) +
      (TRAY_LEFT_FRAC - BAR_RIGHT_FRAC) + (1f - TRAY_LEFT_FRAC)
    assertEquals(1f, sum, 0.0001f)
  }

  @Test
  fun layoutPlacesRailsOnThePhoto() {
    val layout = boardLayout(width = 1536f, height = 1024f)
    assertEquals(72f, layout.leftFrame, 0.5f)
    assertEquals(69f, layout.barWidth, 1.5f)
    assertEquals(1536f - 1332f, layout.trayWidth, 0.5f)
    assertEquals(99f, layout.topFrame, 0.5f)
    val trayColumn =
      layout.wellTop + layout.blackWellHeight + layout.wellSplit + layout.whiteWellHeight + layout.wellBottomPad
    assertEquals(1024f, trayColumn, 0.5f)
    assertEquals(80f, layout.wellTop, 0.5f)
    assertEquals(908f, 1024f - layout.wellBottomPad, 0.5f)
    assertTrue(layout.checker <= layout.barWidth * BAR_CHECKER_MAX + 0.01f)
    assertTrue(layout.checker <= layout.pointWidth * CHECKER_FILL + 0.01f)
  }

  @Test
  fun feltIsDarkForestGreenNotBlack() {
    assertTrue("felt should look green, not charcoal-black", BoardFelt.green > BoardFelt.red + 0.04f)
    assertTrue(BoardFelt.green > BoardFelt.blue)
    assertTrue(BoardFelt.red + BoardFelt.green + BoardFelt.blue > 0.18f)
  }

  @Test
  fun checkerDiameterMatchesOnEveryPointAndTheBar() {
    assertEquals(33.6f, checkerDiameter(pointWidth = 40f, halfHeight = 250f), 0.05f)
  }

  @Test
  fun checkerDiameterFitsFiveWithoutOverlap() {
    assertEquals(40f, checkerDiameter(pointWidth = 50f, halfHeight = 200f), 0.05f)
  }

  @Test
  fun checkerNeverOutgrowsTheBar() {
    assertEquals(31.8f, checkerDiameter(pointWidth = 50f, halfHeight = 200f, barWidth = 30f), 0.05f)
  }

  @Test
  fun stackBadgeShowsFullCountAboveFive() {
    assertNull(stackBadge(4))
    assertNull(stackBadge(5))
    assertEquals(6, stackBadge(6))
    assertEquals(15, stackBadge(15))
  }

  @Test
  fun trayShowsEveryBorneOffChecker() {
    assertEquals(0, trayStackCount(0))
    assertEquals(8, trayStackCount(8))
    assertEquals(9, trayStackCount(9))
    assertEquals(15, trayStackCount(15))
  }

  @Test
  fun trayStackFitsFifteenInTheWell() {
    val layout = boardLayout(width = 1536f, height = 1024f)
    val step = trayStackStep(15, layout.whiteWellHeight, layout.checker)
    val thickness = trayEdgeThickness(layout.checker)
    val stack = thickness + step * 14
    assertTrue(stack <= layout.whiteWellHeight)
    assertTrue(step > 0f)
  }

  @Test
  fun checkerDropShadowMatchesTheMock() {
    assertEquals(0.12f, CHECKER_SHADOW_X, 0.001f)
    assertEquals(0.18f, CHECKER_SHADOW_Y, 0.001f)
    assertTrue(CHECKER_SHADOW_ALPHA > 0.3f)
  }

  @Test
  fun defaultBoardStyleIsOriginalWalnut() {
    assertEquals(BoardStyle.ORIGINAL, BoardStyle.fromStorage(null))
    assertEquals(WalnutBackground, BoardStyle.ORIGINAL.backdrop)
    assertEquals("Original", BoardStyle.ORIGINAL.label)
  }

  @Test
  fun retiredWalnutForestFallsBackToOriginal() {
    assertEquals(BoardStyle.ORIGINAL, BoardStyle.fromStorage("WALNUT_FOREST"))
  }

  @Test
  fun eachBoardSitsOnItsPairedCloth() {
    assertEquals(R.drawable.cloth_oak_table, BoardStyle.ORIGINAL.cloth)
    assertEquals(R.drawable.cloth_forest_baize, BoardStyle.CLUB_WALNUT.cloth)
    assertEquals(R.drawable.cloth_blue_silk, BoardStyle.EBONY_NAVY.cloth)
    assertEquals(R.drawable.cloth_oat_linen, BoardStyle.ASH_SAGE.cloth)
    assertEquals(R.drawable.cloth_oxblood_damask, BoardStyle.MAHOGANY_CLARET.cloth)
  }

  @Test
  fun ashSageStatusInkReadsOnOatLinen() {
    assertEquals(Color(0xFF3E4134), BoardStyle.ASH_SAGE.statusColor)
    assertEquals(Cream, BoardStyle.ORIGINAL.statusColor)
  }

  @Test
  fun hudChromeFollowsTheBoard() {
    assertEquals(WalnutRail, BoardStyle.ORIGINAL.chrome.fill)
    assertEquals(Brass, BoardStyle.ORIGINAL.chrome.border)
    assertEquals(Color(0xFF1A2A4C), BoardStyle.EBONY_NAVY.chrome.fill)
    assertEquals(Color(0xFFE4E8F0), BoardStyle.EBONY_NAVY.chrome.onFill)
    assertEquals(Color(0xFFF2EDE3), BoardStyle.ASH_SAGE.chrome.fill)
    assertEquals(Color(0xFF3E4134), BoardStyle.ASH_SAGE.chrome.onFill)
    assertEquals(Color(0xFF4A1822), BoardStyle.MAHOGANY_CLARET.chrome.fill)
    assertEquals(Brass, BoardStyle.MAHOGANY_CLARET.chrome.border)
  }

  @Test
  fun borneOffFlightAimsAtTheTopOfThePile() {
    val wellTop = 100f
    val wellH = 200f
    val checker = 40f
    val empty = trayPileTopCenterY(wellTop, wellH, 0, checker)
    val one = trayPileTopCenterY(wellTop, wellH, 1, checker)
    val five = trayPileTopCenterY(wellTop, wellH, 5, checker)
    val mid = wellTop + wellH / 2f
    assertEquals(wellTop + wellH - trayEdgeThickness(checker) / 2f, empty, 0.05f)
    assertEquals(empty, one, 0.05f)
    assertTrue(five < one)
    assertTrue(five > wellTop)
    assertTrue(empty > mid)
  }

  @Test
  fun pointStackTopIsTheCenterFacingChecker() {
    val top = 0f
    val height = 200f
    val checker = 40f
    val oneUp = pointStackTopCenterY(top, height, 1, checker, pointUp = true)
    val twoUp = pointStackTopCenterY(top, height, 2, checker, pointUp = true)
    assertEquals(top + height - checker / 2f, oneUp, 0.05f)
    assertEquals(oneUp - checker, twoUp, 0.05f)
    val oneDown = pointStackTopCenterY(top, height, 1, checker, pointUp = false)
    val twoDown = pointStackTopCenterY(top, height, 2, checker, pointUp = false)
    assertEquals(top + checker / 2f, oneDown, 0.05f)
    assertEquals(oneDown + checker, twoDown, 0.05f)
  }

  @Test
  fun barStackTopFacesTheBoardCenter() {
    val checker = 40f
    val blackOne = barStackTopCenterY(0f, 200f, 1, checker, black = true)
    val blackTwo = barStackTopCenterY(0f, 200f, 2, checker, black = true)
    assertTrue(blackTwo > blackOne)
    val whiteOne = barStackTopCenterY(0f, 200f, 1, checker, black = false)
    val whiteTwo = barStackTopCenterY(0f, 200f, 2, checker, black = false)
    assertTrue(whiteTwo < whiteOne)
  }
}
