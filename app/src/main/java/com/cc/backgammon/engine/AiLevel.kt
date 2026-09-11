package com.cc.backgammon.engine

enum class AiLevel(val label: String) {
  BEGINNER(label = "Beginner"),
  INTERMEDIATE(label = "Intermediate"),
  ADVANCED(label = "Advanced"),
  EXPERT(label = "Expert"),
  ;

  companion object {
    fun fromStorage(raw: String?): AiLevel = entries.find { it.name == raw } ?: INTERMEDIATE
  }
}
