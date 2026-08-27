package com.example.backgammon.engine

enum class AiLevel(val label: String) {
  EASY(label = "Easy"),
  MEDIUM(label = "Medium"),
  HARD(label = "Hard"),
  EXTREME(label = "Extreme"),
  ;

  companion object {
    fun fromStorage(raw: String?): AiLevel = entries.find { it.name == raw } ?: MEDIUM
  }
}
