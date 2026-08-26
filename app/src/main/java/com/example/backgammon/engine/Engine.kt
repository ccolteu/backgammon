package com.example.backgammon.engine

import com.example.backgammon.domain.GameState
import com.example.backgammon.domain.Move
import com.example.backgammon.domain.Rules
import com.example.backgammon.domain.WHITE_OFF
import com.example.backgammon.domain.BLACK_OFF

object Engine {
  fun chooseMove(state: GameState): Move? {
    val moves = Rules.legalMoves(state)
    if (moves.isEmpty()) return null
    return moves.maxBy { score(it) }
  }

  private fun score(move: Move): Int {
    var s = 0
    if (move.hit) s += 50
    if (move.to == WHITE_OFF || move.to == BLACK_OFF) s += 40
    s +=
      when (move.to) {
        in 1..6 -> if (move.to != WHITE_OFF) 6 - move.to else 0
        in 19..24 -> move.to - 18
        else -> 0
      }
    return s
  }

  fun planTurn(state: GameState): List<Move> {
    val planned = mutableListOf<Move>()
    var current = state
    while (true) {
      val move = chooseMove(current) ?: break
      planned += move
      current = Rules.apply(current, move)
    }
    return planned
  }

  fun playTurn(state: GameState): GameState {
    var current = state
    for (move in planTurn(state)) {
      current = Rules.apply(current, move)
    }
    return Rules.endTurn(current)
  }
}
