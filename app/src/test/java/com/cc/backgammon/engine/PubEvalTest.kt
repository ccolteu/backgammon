package com.cc.backgammon.engine

import com.cc.backgammon.domain.GameState
import com.cc.backgammon.domain.Rules
import com.cc.backgammon.domain.Side
import com.cc.backgammon.domain.opposite
import com.cc.backgammon.domain.startingGame
import kotlin.random.Random
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PubEvalTest {
  @Test
  fun startingPositionLooksTheSameFromEitherSeat() {
    val white = PubEval.features(startingGame())
    val black = PubEval.features(startingGame().copy(sideToMove = Side.BLACK))
    assertArrayEquals(white, black, 0f)
  }

  @Test
  fun flippedPositionMatchesTheOtherSideToMove() {
    val points = startingGame().points.toMutableList()
    points[24] = 1
    points[18] = 1
    val white =
      startingGame().copy(points = points, sideToMove = Side.WHITE)
    val black = mirror(white)
    assertArrayEquals(PubEval.features(white), PubEval.features(black), 0f)
  }

  @Test
  fun opening31_makesTheFivePoint() {
    val rolled = Rules.withDice(startingGame(), 3, 1)
    var current = rolled
    for (move in Engine.planTurn(rolled, AiLevel.EXPERT)) {
      current = Rules.apply(current, move)
    }
    assertEquals(Side.WHITE, current.ownerAt(5))
    assertEquals(2, current.countAt(5))
    assertEquals(4, current.countAt(6))
    assertEquals(2, current.countAt(8))
  }

  @Test
  fun expert_plansLegalTurnsAsBlack() {
    val rolled = Rules.withDice(startingGame().copy(sideToMove = Side.BLACK), 3, 5)
    var current = rolled
    val planned = Engine.planTurn(rolled, AiLevel.EXPERT)
    assertTrue(planned.isNotEmpty())
    for (move in planned) {
      assertTrue(Rules.legalMoves(current).contains(move))
      current = Rules.apply(current, move)
    }
  }

  @Test
  fun expert_finishesAShortMatchAgainstIntermediate() {
    val rng = Random(1)
    var finished = 0
    var expertWins = 0
    repeat(8) { game ->
      var state = startingGame()
      if (game % 2 == 1) state = state.copy(sideToMove = Side.BLACK)
      var plies = 0
      while (state.winner == null && plies++ < 400) {
        val a = rng.nextInt(1, 7)
        val b = rng.nextInt(1, 7)
        state = Rules.withDice(state, a, b)
        val level = if (state.sideToMove == Side.WHITE) AiLevel.STANDARD else AiLevel.EXPERT
        state = Engine.playTurn(state, level)
      }
      if (state.winner != null) finished += 1
      if (state.winner == Side.BLACK) expertWins += 1
    }
    assertEquals(8, finished)
    assertTrue("Expert should at least be in the game ($expertWins / 8)", expertWins >= 3)
  }

  private fun mirror(state: GameState): GameState {
    val points = MutableList(25) { 0 }
    for (p in 1..24) points[25 - p] = -state.points[p]
    return state.copy(
      points = points,
      whiteBar = state.blackBar,
      blackBar = state.whiteBar,
      whiteOff = state.blackOff,
      blackOff = state.whiteOff,
      sideToMove = state.sideToMove.opposite(),
    )
  }
}
