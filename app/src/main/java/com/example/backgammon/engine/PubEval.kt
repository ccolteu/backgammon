package com.example.backgammon.engine

import com.example.backgammon.domain.GameState
import com.example.backgammon.domain.Side

/** Tesauro's 1993 pubeval linear evaluator. Score is for the side to move. */
object PubEval {
  private const val WIN = 99_999_999f

  fun isRace(state: GameState): Boolean {
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

  fun score(state: GameState, race: Boolean): Float {
    val meOff = if (state.sideToMove == Side.WHITE) state.whiteOff else state.blackOff
    val himOff = if (state.sideToMove == Side.WHITE) state.blackOff else state.whiteOff
    if (meOff >= 15) return WIN
    if (himOff >= 15) return -WIN
    val x = features(state)
    val w = if (race) RACE else CONTACT
    var s = 0f
    for (i in 0 until 122) s += w[i] * x[i]
    return s
  }

  fun features(state: GameState): FloatArray {
    val pos = toPos(state)
    val x = FloatArray(122)
    for (j in 1..24) {
      val n = pos[25 - j]
      if (n == 0) continue
      val base = 5 * (j - 1)
      if (n == -1) x[base] = 1f
      if (n == 1) x[base + 1] = 1f
      if (n >= 2) x[base + 2] = 1f
      if (n == 3) x[base + 3] = 1f
      if (n >= 4) x[base + 4] = (n - 3) / 2f
    }
    x[120] = -pos[0] / 2f
    x[121] = pos[26] / 15f
    return x
  }

  internal fun toPos(state: GameState): IntArray {
    val pos = IntArray(28)
    if (state.sideToMove == Side.WHITE) {
      for (p in 1..24) pos[p] = state.points[p]
      pos[0] = -state.blackBar
      pos[25] = state.whiteBar
      pos[26] = state.whiteOff
      pos[27] = -state.blackOff
    } else {
      for (p in 1..24) pos[p] = -state.points[25 - p]
      pos[0] = -state.whiteBar
      pos[25] = state.blackBar
      pos[26] = state.blackOff
      pos[27] = -state.whiteOff
    }
    return pos
  }

  private fun parse(raw: String): FloatArray {
    val values = raw.trim().split(Regex("\\s+")).map { it.toFloat() }.toFloatArray()
    require(values.size == 122) { "expected 122 weights, got ${values.size}" }
    return values
  }

  private val RACE =
    parse(
      """
      .00000 -.17160 .27010 .29906 -.08471 .00000 -1.40375 -1.05121 .07217 -.01351
      .00000 -1.29506 -2.16183 .13246 -1.03508 .00000 -2.29847 -2.34631 .17253 .08302
      .00000 -1.27266 -2.87401 -.07456 -.34240 .00000 -1.34640 -2.46556 -.13022 -.01591
      .00000 .27448 .60015 .48302 .25236 .00000 .39521 .68178 .05281 .09266
      .00000 .24855 -.06844 -.37646 .05685 .00000 .17405 .00430 .74427 .00576
      .00000 .12392 .31202 -.91035 -.16270 .00000 .01418 -.10839 -.02781 -.88035
      .00000 1.07274 2.00366 1.16242 .22520 .00000 .85631 1.06349 1.49549 .18966
      .00000 .37183 -.50352 -.14818 .12039 .00000 .13681 .13978 1.11245 -.12707
      .00000 -.22082 .20178 -.06285 -.52728 .00000 -.13597 -.19412 -.09308 -1.26062
      .00000 3.05454 5.16874 1.50680 5.35000 .00000 2.19605 3.85390 .88296 2.30052
      .00000 .92321 1.08744 -.11696 -.78560 .00000 -.09795 -.83050 -1.09167 -4.94251
      .00000 -1.00316 -3.66465 -2.56906 -9.67677 .00000 -2.77982 -7.26713 -3.40177 -12.32252
      .00000 3.42040
      """,
    )

  private val CONTACT =
    parse(
      """
      .25696 -.66937 -1.66135 -2.02487 -2.53398 -.16092 -1.11725 -1.06654 -.92830 -1.99558
      -1.10388 -.80802 .09856 -.62086 -1.27999 -.59220 -.73667 .89032 -.38933 -1.59847
      -1.50197 -.60966 1.56166 -.47389 -1.80390 -.83425 -.97741 -1.41371 .24500 .10970
      -1.36476 -1.05572 1.15420 .11069 -.38319 -.74816 -.59244 .81116 -.39511 .11424
      -.73169 -.56074 1.09792 .15977 .13786 -1.18435 -.43363 1.06169 -.21329 .04798
      -.94373 -.22982 1.22737 -.13099 -.06295 -.75882 -.13658 1.78389 .30416 .36797
      -.69851 .13003 1.23070 .40868 -.21081 -.64073 .31061 1.59554 .65718 .25429
      -.80789 .08240 1.78964 .54304 .41174 -1.06161 .07851 2.01451 .49786 .91936
      -.90750 .05941 1.83120 .58722 1.28777 -.83711 -.33248 2.64983 .52698 .82132
      -.58897 -1.18223 3.35809 .62017 .57353 -.07276 -.36214 4.37655 .45481 .21746
      .10504 -.61977 3.54001 .04612 -.18108 .63211 -.87046 2.47673 -.48016 -1.27157
      .86505 -1.11342 1.24612 -.82385 -2.77082 1.23606 -1.59529 .10438 -1.30206 -4.11520
      5.62596 -2.75800
      """,
    )
}
