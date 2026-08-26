package com.example.backgammon.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RulesTest {
  @Test
  fun startingPosition_hasFifteenEach() {
    val s = startingGame()
    val white = (1..24).sumOf { if (s.ownerAt(it) == Side.WHITE) s.countAt(it) else 0 }
    val black = (1..24).sumOf { if (s.ownerAt(it) == Side.BLACK) s.countAt(it) else 0 }
    assertEquals(15, white)
    assertEquals(15, black)
  }

  @Test
  fun white_movesDownTheBoard() {
    val s = Rules.withDice(startingGame(), listOf(6))
    val moves = Rules.legalMoves(s)
    assertTrue(moves.any { it.from == 24 && it.to == 18 && it.die == 6 })
    assertTrue(moves.any { it.from == 13 && it.to == 7 && it.die == 6 })
  }

  @Test
  fun cannotLandOnTwoOpponents() {
    val points = MutableList(25) { 0 }
    points[8] = 1
    points[6] = -2
    val s =
      GameState(
        points = points,
        whiteBar = 0,
        blackBar = 0,
        whiteOff = 14,
        blackOff = 13,
        sideToMove = Side.WHITE,
        dice = listOf(2),
      )
    assertFalse(Rules.legalMoves(s).any { it.from == 8 && it.to == 6 })
  }

  @Test
  fun hittingSendsBlotToBar() {
    val points = MutableList(25) { 0 }
    points[8] = 1
    points[6] = -1
    val s =
      GameState(
        points = points,
        whiteBar = 0,
        blackBar = 0,
        whiteOff = 14,
        blackOff = 14,
        sideToMove = Side.WHITE,
        dice = listOf(2),
      )
    val hit = Rules.legalMoves(s).first { it.to == 6 }
    assertTrue(hit.hit)
    val next = Rules.apply(s, hit)
    assertEquals(1, next.blackBar)
    assertEquals(Side.WHITE, next.ownerAt(6))
  }

  @Test
  fun barMustBeEnteredFirst() {
    val s = startingGame().copy(whiteBar = 1, points = startingGame().points.toMutableList().also { it[24] = 1 })
    val withDice = Rules.withDice(s, listOf(1, 2))
    val moves = Rules.legalMoves(withDice)
    assertTrue(moves.isNotEmpty())
    assertTrue(moves.all { it.from == WHITE_BAR })
  }

  @Test
  fun whiteBearsOffFromHome() {
    val points = MutableList(25) { 0 }
    points[6] = 1
    val s =
      GameState(
        points = points,
        whiteBar = 0,
        blackBar = 0,
        whiteOff = 14,
        blackOff = 0,
        sideToMove = Side.WHITE,
        dice = listOf(6),
      )
    val move = Rules.legalMoves(s).single()
    assertEquals(WHITE_OFF, move.to)
    val next = Rules.apply(s, move)
    assertEquals(15, next.whiteOff)
    assertEquals(Side.WHITE, next.winner)
  }

  @Test
  fun diceFromRoll_doublesGiveFour() {
    assertEquals(listOf(3, 3, 3, 3), diceFromRoll(3, 3))
    assertEquals(listOf(2, 5), diceFromRoll(2, 5))
  }

  @Test
  fun withDice_keepsTwoPhysicalFacesOnDoubles() {
    val rolled = Rules.withDice(startingGame(), 5, 5)
    assertEquals(5, rolled.rollA)
    assertEquals(5, rolled.rollB)
    assertEquals(listOf(5, 5, 5, 5), rolled.dice)
    val after = Rules.apply(rolled, Rules.legalMoves(rolled).first())
    assertEquals(5, after.rollA)
    assertEquals(5, after.rollB)
    assertEquals(3, after.dice.size)
  }

  @Test
  fun turnStatus_onlyPromptsWhenThePlayerShouldAct() {
    assertEquals("Tap to roll", turnStatus(startingGame(), PlayPhase.AWAITING_ROLL))
    assertEquals("", turnStatus(startingGame(), PlayPhase.ROLLING))
    assertEquals("", turnStatus(startingGame().copy(sideToMove = Side.BLACK), PlayPhase.ROLLING))
    assertEquals("", turnStatus(startingGame().copy(sideToMove = Side.BLACK), PlayPhase.MOVING))
    val rolled = Rules.withDice(startingGame(), 2, 5)
    assertEquals("Move pieces", turnStatus(rolled, PlayPhase.READY))
    assertEquals("", turnStatus(rolled.copy(sideToMove = Side.BLACK), PlayPhase.READY))
    assertEquals("No moves", turnStatus(rolled, PlayPhase.NO_MOVES))
  }

  @Test
  fun turnStatus_isBlankWhenSomeoneHasWon() {
    val points = MutableList(25) { 0 }
    val won =
      GameState(
        points = points,
        whiteBar = 0,
        blackBar = 0,
        whiteOff = 15,
        blackOff = 0,
        sideToMove = Side.WHITE,
        dice = emptyList(),
      )
    assertEquals("", turnStatus(won))
  }

  @Test
  fun boardWithoutMover_liftsCheckerFromSourceOnly() {
    val rolled = Rules.withDice(startingGame(), 3, 5)
    val move = Rules.legalMoves(rolled).first { it.from == 24 && it.die == 3 }
    val visual = boardWithoutMover(rolled, move)
    assertEquals(1, visual.countAt(24))
    assertEquals(0, visual.countAt(move.to))
    assertEquals(2, rolled.countAt(24))
  }

  @Test
  fun mustUseBothDiceWhenPossible() {
    val points = MutableList(25) { 0 }
    points[10] = 1
    points[1] = -2
    val s =
      GameState(
        points = points,
        whiteBar = 0,
        blackBar = 0,
        whiteOff = 14,
        blackOff = 13,
        sideToMove = Side.WHITE,
        dice = listOf(3, 6),
      )
    val moves = Rules.legalMoves(s)
    assertTrue(moves.any { it.from == 10 && it.to == 4 && it.die == 6 })
    assertFalse(moves.any { it.die == 3 })
  }

  @Test
  fun closedBoard_noEntryFromBar() {
    val points = MutableList(25) { 0 }
    points[24] = -2
    points[23] = -2
    val s =
      GameState(
        points = points,
        whiteBar = 1,
        blackBar = 0,
        whiteOff = 14,
        blackOff = 11,
        sideToMove = Side.WHITE,
        dice = listOf(1, 2),
      )
    assertTrue(Rules.legalMoves(s).isEmpty())
  }
}
