package com.example.backgammon.engine

enum class AiLevel(val label: String, val topMoves: Int, val searchTurn: Boolean) {
  EASY(label = "Easy", topMoves = 3, searchTurn = false),
  MEDIUM(label = "Medium", topMoves = 1, searchTurn = false),
  HARD(label = "Hard", topMoves = 1, searchTurn = true),
  ;

  companion object {
    fun fromStorage(raw: String?): AiLevel = entries.find { it.name == raw } ?: MEDIUM
  }
}
