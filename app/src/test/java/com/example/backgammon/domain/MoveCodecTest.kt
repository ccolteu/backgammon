package com.example.backgammon.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class MoveCodecTest {
  @Test
  fun stateRoundTrip_preservesStartingPosition() {
    val start = startingGame()
    assertEquals(start, MoveCodec.decodeState(MoveCodec.encodeState(start)))
  }

  @Test
  fun format_marksHitsAndOff() {
    assertEquals("24/18*", MoveCodec.format(Move(24, 18, 6, hit = true)))
    assertEquals("6/off", MoveCodec.format(Move(6, WHITE_OFF, 6)))
  }

  @Test
  fun stateRoundTrip_preservesPhysicalDiceOnDoubles() {
    val mid = Rules.withDice(startingGame(), 5, 5).copy(dice = listOf(5, 5))
    val decoded = MoveCodec.decodeState(MoveCodec.encodeState(mid))
    assertEquals(5, decoded?.rollA)
    assertEquals(5, decoded?.rollB)
    assertEquals(listOf(5, 5), decoded?.dice)
  }
}
