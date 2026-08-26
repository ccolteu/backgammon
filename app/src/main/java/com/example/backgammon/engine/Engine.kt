package com.example.backgammon.engine

import com.example.backgammon.domain.BLACK_OFF
import com.example.backgammon.domain.GameState
import com.example.backgammon.domain.Move
import com.example.backgammon.domain.Rules
import com.example.backgammon.domain.Side
import com.example.backgammon.domain.WHITE_OFF
import kotlin.random.Random

object Engine {
  fun chooseMove(state: GameState, level: AiLevel = AiLevel.MEDIUM, random: Random = Random.Default): Move? =
    planTurn(state, level, random).firstOrNull()

  fun planTurn(state: GameState, level: AiLevel = AiLevel.MEDIUM, random: Random = Random.Default): List<Move> {
    if (level.searchTurn) return bestSequence(state)
    val planned = mutableListOf<Move>()
    var current = state
    while (true) {
      val move = pickHop(current, level, random) ?: break
      planned += move
      current = Rules.apply(current, move)
    }
    return planned
  }

  fun playTurn(state: GameState, level: AiLevel = AiLevel.MEDIUM, random: Random = Random.Default): GameState {
    var current = state
    for (move in planTurn(state, level, random)) {
      current = Rules.apply(current, move)
    }
    return Rules.endTurn(current)
  }

  private fun pickHop(state: GameState, level: AiLevel, random: Random): Move? {
    val moves = Rules.legalMoves(state)
    if (moves.isEmpty()) return null
    val scored = moves.map { it to hopScore(it) }.sortedByDescending { it.second }
    val best = scored.first().second
    val window =
      if (level.topMoves <= 1) {
        listOf(scored.first())
      } else {
        scored.filter { it.second >= best - 20 }.take(level.topMoves)
      }
    return window[random.nextInt(window.size)].first
  }

  private fun bestSequence(state: GameState): List<Move> {
    val legal = Rules.legalMoves(state)
    if (legal.isEmpty()) return emptyList()
    var bestPath = emptyList<Move>()
    var bestScore: Int? = null

    fun walk(current: GameState, path: List<Move>) {
      val nextMoves = Rules.legalMoves(current)
      if (nextMoves.isEmpty()) {
        val score = evaluateWhiteAdvantage(current)
        val better =
          when (bestScore) {
            null -> true
            else -> if (state.sideToMove == Side.WHITE) score > bestScore!! else score < bestScore!!
          }
        if (better) {
          bestScore = score
          bestPath = path
        }
        return
      }
      for (move in nextMoves) {
        walk(Rules.apply(current, move), path + move)
      }
    }

    walk(state, emptyList())
    return bestPath
  }

  private fun hopScore(move: Move): Int {
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

  internal fun evaluateWhiteAdvantage(state: GameState): Int {
    var whitePips = state.whiteBar * 25
    var blackPips = state.blackBar * 25
    var whiteBlots = 0
    var blackBlots = 0
    for (p in 1..24) {
      val count = state.points[p]
      when {
        count > 0 -> {
          whitePips += count * p
          if (count == 1) whiteBlots += 1
        }
        count < 0 -> {
          blackPips += -count * (25 - p)
          if (count == -1) blackBlots += 1
        }
      }
    }
    return state.whiteOff * 250 - state.blackOff * 250 - whitePips + blackPips - whiteBlots * 20 + blackBlots * 20
  }
}
