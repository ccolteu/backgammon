package com.example.backgammon.engine

import com.example.backgammon.domain.Rules
import com.example.backgammon.domain.Side
import com.example.backgammon.domain.startingGame
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EngineTest {
  @Test
  fun planTurn_returnsMovesWithoutSkippingTheFinalBoard() {
    val rolled = Rules.withDice(startingGame().copy(sideToMove = Side.BLACK), 3, 5)
    val planned = Engine.planTurn(rolled, AiLevel.MEDIUM)
    assertTrue(planned.isNotEmpty())
    var played = rolled
    for (move in planned) {
      played = Rules.apply(played, move)
    }
    assertEquals(Engine.playTurn(rolled, AiLevel.MEDIUM), Rules.endTurn(played))
  }

  @Test
  fun easyAndHard_planLegalTurns() {
    val rolled = Rules.withDice(startingGame().copy(sideToMove = Side.BLACK), 3, 5)
    for (level in listOf(AiLevel.EASY, AiLevel.HARD)) {
      var current = rolled
      val planned = Engine.planTurn(rolled, level, Random(1))
      assertTrue(planned.isNotEmpty())
      for (move in planned) {
        assertTrue(Rules.legalMoves(current).contains(move))
        current = Rules.apply(current, move)
      }
    }
  }
}
