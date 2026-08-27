package com.example.backgammon.engine

import com.example.backgammon.domain.GameState
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

  @Test
  fun medium_hitsAndCoversInsteadOfLeavingABlot() {
    val game =
      position(
        white = mapOf(20 to 1),
        black = mapOf(19 to 2, 18 to 1, 13 to 12),
        whiteOff = 14,
        side = Side.BLACK,
        a = 1,
        b = 2,
      )
    val after = playOut(game, AiLevel.MEDIUM)
    assertEquals(Side.BLACK, after.ownerAt(20))
    assertEquals(2, after.countAt(20))
    assertEquals(1, after.whiteBar)
  }

  @Test
  fun medium_holdsAnAnchorWhenAheadInTheRace() {
    val game =
      position(
        white = mapOf(7 to 1, 13 to 5, 12 to 9),
        black = mapOf(5 to 2, 16 to 2),
        blackOff = 11,
        side = Side.BLACK,
        a = 2,
        b = 1,
      )
    val after = playOut(game, AiLevel.MEDIUM)
    assertEquals(Side.BLACK, after.ownerAt(5))
    assertEquals(2, after.countAt(5))
    assertEquals(0, after.whiteBar)
  }

  @Test
  fun medium_makesTheFivePointInsteadOfHittingALooseBlot() {
    val game =
      position(
        white = mapOf(15 to 1),
        black = mapOf(17 to 3, 19 to 5, 12 to 5, 1 to 2),
        whiteOff = 14,
        side = Side.BLACK,
        a = 3,
        b = 1,
      )
    val after = playOut(game, AiLevel.MEDIUM)
    assertEquals(Side.BLACK, after.ownerAt(20))
    assertTrue(after.countAt(20) >= 2)
    assertEquals(0, after.whiteBar)
  }

  @Test
  fun medium_doesNotBreakAPointToSlotInFrontOfAPrime() {
    val game =
      position(
        white = mapOf(8 to 2, 9 to 2, 10 to 2, 11 to 2, 12 to 2, 13 to 2, 1 to 3),
        black = mapOf(6 to 2, 15 to 1),
        blackOff = 12,
        side = Side.BLACK,
        a = 1,
        b = 2,
      )
    val after = playOut(game, AiLevel.MEDIUM)
    assertEquals(2, after.countAt(6))
    assertTrue(after.ownerAt(7) != Side.BLACK)
  }

  @Test
  fun easy_picksAWeakerPlayThanMedium() {
    val rolled = Rules.withDice(startingGame().copy(sideToMove = Side.BLACK), 3, 5)
    val medium = playOut(rolled, AiLevel.MEDIUM)
    val easy = playOut(rolled, AiLevel.EASY, Random(1))
    assertTrue(
      Engine.evaluateWhiteAdvantage(easy) >= Engine.evaluateWhiteAdvantage(medium),
    )
  }

  @Test
  fun hard_hitsAndCoversLikeMedium() {
    val game =
      position(
        white = mapOf(20 to 1),
        black = mapOf(19 to 2, 18 to 1, 13 to 12),
        whiteOff = 14,
        side = Side.BLACK,
        a = 1,
        b = 2,
      )
    val after = playOut(game, AiLevel.HARD)
    assertEquals(2, after.countAt(20))
    assertEquals(1, after.whiteBar)
  }

  private fun playOut(game: GameState, level: AiLevel, random: Random = Random.Default): GameState {
    var current = game
    for (move in Engine.planTurn(game, level, random)) {
      current = Rules.apply(current, move)
    }
    return current
  }

  private fun position(
    white: Map<Int, Int> = emptyMap(),
    black: Map<Int, Int> = emptyMap(),
    whiteBar: Int = 0,
    blackBar: Int = 0,
    whiteOff: Int = 0,
    blackOff: Int = 0,
    side: Side,
    a: Int,
    b: Int,
  ): GameState {
    val points = MutableList(25) { 0 }
    white.forEach { (point, count) -> points[point] = count }
    black.forEach { (point, count) -> points[point] = -count }
    val whiteCheckers = white.values.sum() + whiteBar + whiteOff
    val blackCheckers = black.values.sum() + blackBar + blackOff
    assertEquals(15, whiteCheckers)
    assertEquals(15, blackCheckers)
    return Rules.withDice(
      GameState(
        points = points,
        whiteBar = whiteBar,
        blackBar = blackBar,
        whiteOff = whiteOff,
        blackOff = blackOff,
        sideToMove = side,
        dice = emptyList(),
      ),
      a,
      b,
    )
  }
}
