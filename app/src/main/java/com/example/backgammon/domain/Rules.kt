package com.example.backgammon.domain

object Rules {
  fun legalMoves(state: GameState): List<Move> {
    if (state.winner != null || state.dice.isEmpty()) return emptyList()
    val unique = state.dice.distinct()
    return unique.flatMap { die -> movesForDie(state, die) }
  }

  fun apply(state: GameState, move: Move): GameState {
    require(move in legalMoves(state)) { "Illegal move $move" }
    val points = state.points.toMutableList()
    var whiteBar = state.whiteBar
    var blackBar = state.blackBar
    var whiteOff = state.whiteOff
    var blackOff = state.blackOff
    val side = state.sideToMove

    when (move.from) {
      WHITE_BAR -> whiteBar -= 1
      BLACK_BAR -> blackBar -= 1
      else -> points[move.from] -= if (side == Side.WHITE) 1 else -1
    }

    if (move.to == WHITE_OFF) {
      whiteOff += 1
    } else if (move.to == BLACK_OFF) {
      blackOff += 1
    } else {
      if (move.hit) {
        if (side == Side.WHITE) {
          points[move.to] = 0
          blackBar += 1
        } else {
          points[move.to] = 0
          whiteBar += 1
        }
      }
      points[move.to] += if (side == Side.WHITE) 1 else -1
    }

    val used = state.dice.toMutableList()
    used.remove(move.die)
    return state.copy(
      points = points,
      whiteBar = whiteBar,
      blackBar = blackBar,
      whiteOff = whiteOff,
      blackOff = blackOff,
      dice = used,
    )
  }

  fun endTurn(state: GameState): GameState =
    state.copy(sideToMove = state.sideToMove.opposite(), dice = emptyList())

  fun withDice(state: GameState, a: Int, b: Int): GameState =
    state.copy(dice = diceFromRoll(a, b), rollA = a, rollB = b)

  fun withDice(state: GameState, dice: List<Int>): GameState =
    state.copy(
      dice = dice,
      rollA = dice.firstOrNull() ?: 0,
      rollB = dice.getOrNull(1)?.takeIf { it != dice.first() } ?: dice.firstOrNull() ?: 0,
    )

  private fun movesForDie(state: GameState, die: Int): List<Move> {
    val side = state.sideToMove
    val bar = if (side == Side.WHITE) state.whiteBar else state.blackBar
    if (bar > 0) {
      val dest = if (side == Side.WHITE) 25 - die else die
      return listOfNotNull(tryLand(state, if (side == Side.WHITE) WHITE_BAR else BLACK_BAR, dest, die))
    }
    val moves = mutableListOf<Move>()
    for (from in 1..24) {
      if (state.ownerAt(from) != side) continue
      val dest = if (side == Side.WHITE) from - die else from + die
      if (dest in 1..24) {
        tryLand(state, from, dest, die)?.let { moves += it }
      } else if (canBearOff(state, side)) {
        if (exactOrHighest(state, side, from, die)) {
          moves += Move(from, if (side == Side.WHITE) WHITE_OFF else BLACK_OFF, die)
        }
      }
    }
    return moves
  }

  private fun tryLand(state: GameState, from: Int, dest: Int, die: Int): Move? {
    val side = state.sideToMove
    val owner = state.ownerAt(dest)
    val count = state.countAt(dest)
    return when {
      owner == null || owner == side -> Move(from, dest, die)
      count == 1 -> Move(from, dest, die, hit = true)
      else -> null
    }
  }

  private fun canBearOff(state: GameState, side: Side): Boolean {
    if (side == Side.WHITE) {
      if (state.whiteBar > 0) return false
      return (7..24).none { state.ownerAt(it) == Side.WHITE }
    }
    if (state.blackBar > 0) return false
    return (1..18).none { state.ownerAt(it) == Side.BLACK }
  }

  private fun exactOrHighest(state: GameState, side: Side, from: Int, die: Int): Boolean {
    val dest = if (side == Side.WHITE) from - die else from + die
    if (side == Side.WHITE) {
      if (dest == 0) return true
      if (dest > 0) return false
      return (from + 1..6).none { state.ownerAt(it) == Side.WHITE }
    }
    if (dest == 25) return true
    if (dest < 25) return false
    return (19 until from).none { state.ownerAt(it) == Side.BLACK }
  }
}
