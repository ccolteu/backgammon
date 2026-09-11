package com.cc.backgammon.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoveCodecTest {
  @Test
  fun stateRoundTrip_preservesStartingPosition() {
    val start = startingGame()
    assertEquals(start, MoveCodec.decodeState(MoveCodec.encodeState(start)))
  }

  @Test
  fun stateRoundTrip_preservesPhysicalDiceOnDoubles() {
    val mid = Rules.withDice(startingGame(), 5, 5).copy(dice = listOf(5, 5))
    val decoded = MoveCodec.decodeState(MoveCodec.encodeState(mid))
    assertEquals(5, decoded?.rollA)
    assertEquals(5, decoded?.rollB)
    assertEquals(listOf(5, 5), decoded?.dice)
  }

  @Test
  fun decodeState_treatsJunkAsNoSave() {
    assertNull(MoveCodec.decodeState(""))
    assertNull(MoveCodec.decodeState("not-a-game"))
    assertNull(MoveCodec.decodeState("a,b,c|0|0|0|0|WHITE||"))
    val valid = MoveCodec.encodeState(startingGame())
    assertNull(MoveCodec.decodeState(valid.replace("WHITE", "BLUE")))
    assertNull(MoveCodec.decodeState(valid.replaceFirst("2", "x")))
  }
}
