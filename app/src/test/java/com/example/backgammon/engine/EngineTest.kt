package com.example.backgammon.engine

import com.example.backgammon.domain.Rules
import com.example.backgammon.domain.startingGame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EngineTest {
  @Test
  fun planTurn_returnsMovesWithoutSkippingTheFinalBoard() {
    val rolled = Rules.withDice(startingGame().copy(sideToMove = com.example.backgammon.domain.Side.BLACK), 3, 5)
    val planned = Engine.planTurn(rolled)
    assertTrue(planned.isNotEmpty())
    var played = rolled
    for (move in planned) {
      played = Rules.apply(played, move)
    }
    assertEquals(Engine.playTurn(rolled), Rules.endTurn(played))
  }
}
