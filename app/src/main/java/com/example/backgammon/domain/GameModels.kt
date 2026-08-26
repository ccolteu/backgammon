package com.example.backgammon.domain

enum class Side {
  WHITE,
  BLACK,
}

enum class PlayPhase {
  AWAITING_ROLL,
  ROLLING,
  READY,
  MOVING,
  NO_MOVES,
}

fun Side.opposite(): Side = if (this == Side.WHITE) Side.BLACK else Side.WHITE

const val WHITE_BAR = 0
const val BLACK_BAR = 25
const val WHITE_OFF = -1
const val BLACK_OFF = 26

data class Move(val from: Int, val to: Int, val die: Int, val hit: Boolean = false)

data class GameState(
  val points: List<Int>,
  val whiteBar: Int,
  val blackBar: Int,
  val whiteOff: Int,
  val blackOff: Int,
  val sideToMove: Side,
  val dice: List<Int>,
  val rollA: Int = 0,
  val rollB: Int = 0,
) {
  init {
    require(points.size == 25)
  }

  fun countAt(point: Int): Int = if (point in 1..24) kotlin.math.abs(points[point]) else 0

  fun ownerAt(point: Int): Side? =
    when {
      point !in 1..24 -> null
      points[point] > 0 -> Side.WHITE
      points[point] < 0 -> Side.BLACK
      else -> null
    }

  val winner: Side?
    get() =
      when {
        whiteOff >= 15 -> Side.WHITE
        blackOff >= 15 -> Side.BLACK
        else -> null
      }
}

fun startingGame(): GameState {
  val points = MutableList(25) { 0 }
  points[24] = 2
  points[13] = 5
  points[8] = 3
  points[6] = 5
  points[1] = -2
  points[12] = -5
  points[17] = -3
  points[19] = -5
  return GameState(
    points = points,
    whiteBar = 0,
    blackBar = 0,
    whiteOff = 0,
    blackOff = 0,
    sideToMove = Side.WHITE,
    dice = emptyList(),
  )
}

fun diceFromRoll(a: Int, b: Int): List<Int> = if (a == b) listOf(a, a, a, a) else listOf(a, b)

fun turnStatus(game: GameState, phase: PlayPhase = PlayPhase.READY): String =
  when {
    game.winner != null -> ""
    phase == PlayPhase.AWAITING_ROLL && game.sideToMove == Side.WHITE -> "Tap to roll"
    phase == PlayPhase.READY && game.sideToMove == Side.WHITE -> "Move pieces"
    phase == PlayPhase.NO_MOVES -> "No moves"
    else -> ""
  }

fun boardWithoutMover(state: GameState, move: Move): GameState {
  val points = state.points.toMutableList()
  var whiteBar = state.whiteBar
  var blackBar = state.blackBar
  when (move.from) {
    WHITE_BAR -> whiteBar -= 1
    BLACK_BAR -> blackBar -= 1
    else -> points[move.from] -= if (state.sideToMove == Side.WHITE) 1 else -1
  }
  return state.copy(points = points, whiteBar = whiteBar, blackBar = blackBar)
}

fun dieSpent(face: Int, other: Int, remaining: List<Int>): Boolean {
  if (face <= 0) return false
  if (face == other) return remaining.isEmpty()
  return remaining.none { it == face }
}
