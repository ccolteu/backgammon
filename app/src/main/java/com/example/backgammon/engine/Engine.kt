package com.example.backgammon.engine

import com.example.backgammon.domain.GameState
import com.example.backgammon.domain.Move
import com.example.backgammon.domain.Rules
import com.example.backgammon.domain.Side
import kotlin.math.max
import kotlin.random.Random

object Engine {
  private const val HARD_CANDIDATES = 8
  private const val WIN = 100_000

  fun chooseMove(state: GameState, level: AiLevel = AiLevel.MEDIUM, random: Random = Random.Default): Move? =
    planTurn(state, level, random).firstOrNull()

  fun planTurn(state: GameState, level: AiLevel = AiLevel.MEDIUM, random: Random = Random.Default): List<Move> {
    val plays = uniquePlays(state)
    if (plays.isEmpty()) return emptyList()
    val side = state.sideToMove
    return when (level) {
      AiLevel.EASY -> pickEasy(plays, side, random).path
      AiLevel.MEDIUM -> bestZeroPly(plays, side).path
      AiLevel.HARD -> bestOnePly(plays, side).path
    }
  }

  fun playTurn(state: GameState, level: AiLevel = AiLevel.MEDIUM, random: Random = Random.Default): GameState {
    var current = state
    for (move in planTurn(state, level, random)) {
      current = Rules.apply(current, move)
    }
    return Rules.endTurn(current)
  }

  internal fun evaluateWhiteAdvantage(state: GameState): Int {
    if (state.whiteOff >= 15) return WIN
    if (state.blackOff >= 15) return -WIN

    val pips = pipRace(state)
    var score = state.whiteOff * 100 - state.blackOff * 100 - pips.white + pips.black
    if (isRace(state)) return score

    val whiteAhead = pips.white + 30 < pips.black
    val blackAhead = pips.black + 30 < pips.white
    score -= state.whiteBar * if (blackAhead) 10 else 50
    score += state.blackBar * if (whiteAhead) 10 else 50

    var whitePrime = 0
    var blackPrime = 0
    var whiteRun = 0
    var blackRun = 0
    for (p in 1..24) {
      val count = state.points[p]
      when {
        count >= 2 -> {
          whiteRun += 1
          blackRun = 0
          whitePrime = max(whitePrime, whiteRun)
          score += madePointScore(p, Side.WHITE)
        }
        count <= -2 -> {
          blackRun += 1
          whiteRun = 0
          blackPrime = max(blackPrime, blackRun)
          score -= madePointScore(p, Side.BLACK)
        }
        else -> {
          whiteRun = 0
          blackRun = 0
          if (count == 1) {
            score -= blotCost(state, p, whiteBlot = true)
            if (whiteAhead) score -= 18
            score -= trapCost(state, p, Side.WHITE)
          } else if (count == -1) {
            score += blotCost(state, p, whiteBlot = false)
            if (blackAhead) score += 18
            score += trapCost(state, p, Side.BLACK)
          }
        }
      }
    }
    score += primeScore(whitePrime) - primeScore(blackPrime)
    return score
  }

  private data class Play(val path: List<Move>, val after: GameState)

  private data class BoardSnap(
    val points: List<Int>,
    val whiteBar: Int,
    val blackBar: Int,
    val whiteOff: Int,
    val blackOff: Int,
  )

  private data class Pips(val white: Int, val black: Int)

  private fun uniquePlays(state: GameState): List<Play> {
    val terminals = LinkedHashMap<BoardSnap, Play>()

    fun walk(current: GameState, path: List<Move>) {
      val hops = Rules.legalMoves(current)
      if (hops.isEmpty()) {
        val snap =
          BoardSnap(current.points, current.whiteBar, current.blackBar, current.whiteOff, current.blackOff)
        if (snap !in terminals) terminals[snap] = Play(path, current)
        return
      }
      for (hop in hops) walk(Rules.apply(current, hop), path + hop)
    }

    walk(state, emptyList())
    return terminals.values.toList()
  }

  private fun pickEasy(plays: List<Play>, side: Side, random: Random): Play {
    if (plays.size == 1) return plays.first()
    val ranked = plays.sortedBy { fitness(side, evaluateWhiteAdvantage(it.after)) }
    val worse = ranked.take((ranked.size + 1) / 2)
    return worse[random.nextInt(worse.size)]
  }

  private fun bestZeroPly(plays: List<Play>, side: Side): Play =
    plays.maxBy { fitness(side, evaluateWhiteAdvantage(it.after)) }

  private fun bestOnePly(plays: List<Play>, side: Side): Play {
    val candidates = plays.sortedByDescending { fitness(side, evaluateWhiteAdvantage(it.after)) }.take(HARD_CANDIDATES)
    return candidates.maxBy { fitness(side, expectedWhiteAdvantage(it.after)) }
  }

  private fun expectedWhiteAdvantage(afterPlay: GameState): Int {
    val handed = Rules.endTurn(afterPlay)
    if (handed.winner != null) return evaluateWhiteAdvantage(handed)
    var total = 0
    for (a in 1..6) {
      for (b in a..6) {
        val weight = if (a == b) 1 else 2
        val rolled = Rules.withDice(handed, a, b)
        val reply = bestZeroPlyTerminal(rolled)
        total += weight * evaluateWhiteAdvantage(Rules.endTurn(reply))
      }
    }
    return total
  }

  private fun bestZeroPlyTerminal(state: GameState): GameState {
    val side = state.sideToMove
    var bestState = state
    var bestScore: Int? = null

    fun walk(current: GameState) {
      val hops = Rules.legalMoves(current)
      if (hops.isEmpty()) {
        val score = evaluateWhiteAdvantage(current)
        val better =
          when (bestScore) {
            null -> true
            else -> fitness(side, score) > fitness(side, bestScore!!)
          }
        if (better) {
          bestScore = score
          bestState = current
        }
        return
      }
      for (hop in hops) walk(Rules.apply(current, hop))
    }

    walk(state)
    return bestState
  }

  private fun fitness(side: Side, whiteAdvantage: Int): Int =
    if (side == Side.WHITE) whiteAdvantage else -whiteAdvantage

  private fun pipRace(state: GameState): Pips {
    var white = state.whiteBar * 25
    var black = state.blackBar * 25
    for (p in 1..24) {
      val count = state.points[p]
      when {
        count > 0 -> white += count * p
        count < 0 -> black += -count * (25 - p)
      }
    }
    return Pips(white, black)
  }

  private fun isRace(state: GameState): Boolean {
    if (state.whiteBar > 0 || state.blackBar > 0) return false
    var maxWhite = 0
    var minBlack = 25
    for (p in 1..24) {
      val count = state.points[p]
      if (count > 0 && p > maxWhite) maxWhite = p
      if (count < 0 && p < minBlack) minBlack = p
    }
    return maxWhite < minBlack
  }

  private fun madePointScore(point: Int, side: Side): Int =
    if (side == Side.WHITE) {
      when (point) {
        5 -> 42
        4,
        6 -> 18
        7 -> 24
        20 -> 28
        in 1..3 -> 12
        in 19..24 -> 16
        else -> 8
      }
    } else {
      when (point) {
        20 -> 42
        19,
        21 -> 18
        18 -> 24
        5 -> 28
        in 22..24 -> 12
        in 1..6 -> 16
        else -> 8
      }
    }

  private fun primeScore(length: Int): Int =
    if (length < 3) 0 else 14 * (length - 1) * (length - 2)

  private fun blotCost(state: GameState, point: Int, whiteBlot: Boolean): Int {
    val shots = hittingNumbers(state, point, blotIsWhite = whiteBlot)
    return 5 + shots * 8
  }

  private fun trapCost(state: GameState, point: Int, side: Side): Int {
    val blocked = blockedAhead(state, point, side)
    return if (blocked >= 4) 20 * blocked else 0
  }

  private fun blockedAhead(state: GameState, from: Int, side: Side): Int {
    val enemy = if (side == Side.WHITE) Side.BLACK else Side.WHITE
    val step = if (side == Side.WHITE) -1 else 1
    var p = from + step
    var n = 0
    while (p in 1..24 && state.ownerAt(p) == enemy && state.countAt(p) >= 2) {
      n += 1
      p += step
    }
    return n
  }

  private fun hittingNumbers(state: GameState, blotPoint: Int, blotIsWhite: Boolean): Int {
    val attacker = if (blotIsWhite) Side.BLACK else Side.WHITE
    val bar = if (attacker == Side.WHITE) state.whiteBar else state.blackBar
    var n = 0
    for (d in 1..6) {
      val hits =
        if (bar > 0) {
          val dest = if (attacker == Side.WHITE) 25 - d else d
          dest == blotPoint
        } else {
          val from = if (attacker == Side.WHITE) blotPoint + d else blotPoint - d
          from in 1..24 && state.ownerAt(from) == attacker
        }
      if (hits) n += 1
    }
    return n
  }
}
