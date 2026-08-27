package com.example.backgammon.ui.game

import com.example.backgammon.R
import com.example.backgammon.theme.BoardFelt
import com.example.backgammon.theme.WalnutBackground
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
    assertEquals(105f, layout.topFrame, 0.5f)
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
    assertEquals(R.drawable.cloth_navy_wool, BoardStyle.EBONY_NAVY.cloth)
    assertEquals(R.drawable.cloth_oat_linen, BoardStyle.ASH_SAGE.cloth)
    assertEquals(R.drawable.cloth_oxblood_damask, BoardStyle.MAHOGANY_CLARET.cloth)
  }
}
