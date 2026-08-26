package com.example.backgammon.domain

object MoveCodec {
  fun format(move: Move): String {
    val from = label(move.from)
    val to = label(move.to)
    return "$from/$to${if (move.hit) "*" else ""}"
  }

  fun encodeState(state: GameState): String =
    listOf(
        state.points.drop(1).joinToString(","),
        state.whiteBar,
        state.blackBar,
        state.whiteOff,
        state.blackOff,
        state.sideToMove.name,
        state.dice.joinToString(","),
        state.rollA,
        state.rollB,
      )
      .joinToString("|")

  fun decodeState(raw: String): GameState? {
    if (raw.isBlank()) return null
    val parts = raw.split("|")
    if (parts.size < 7) return null
    val pts = parts[0].split(",").map { it.toInt() }
    if (pts.size != 24) return null
    val dice = if (parts[6].isBlank()) emptyList() else parts[6].split(",").map { it.toInt() }
    val rollA = parts.getOrNull(7)?.toIntOrNull() ?: dice.firstOrNull() ?: 0
    val rollB = parts.getOrNull(8)?.toIntOrNull() ?: dice.getOrNull(1) ?: rollA
    return GameState(
      points = listOf(0) + pts,
      whiteBar = parts[1].toInt(),
      blackBar = parts[2].toInt(),
      whiteOff = parts[3].toInt(),
      blackOff = parts[4].toInt(),
      sideToMove = Side.valueOf(parts[5]),
      dice = dice,
      rollA = rollA,
      rollB = rollB,
    )
  }

  private fun label(point: Int): String =
    when (point) {
      WHITE_BAR, BLACK_BAR -> "bar"
      WHITE_OFF, BLACK_OFF -> "off"
      else -> point.toString()
    }
}
