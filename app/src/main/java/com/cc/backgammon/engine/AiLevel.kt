package com.cc.backgammon.engine

enum class AiLevel(val label: String) {
  BEGINNER(label = "Beginner"),
  STANDARD(label = "Standard"),
  ADVANCED(label = "Advanced"),
  EXPERT(label = "Expert"),
  ;

  companion object {
    fun fromStorage(raw: String?): AiLevel =
      when (raw) {
        "INTERMEDIATE" -> STANDARD
        else -> entries.find { it.name == raw } ?: STANDARD
      }
  }
}
