package com.example.backgammon.ui.game

import androidx.annotation.RawRes
import com.example.backgammon.R

enum class Ambience(
  val label: String,
  @param:RawRes val rawRes: Int?,
) {
  JAZZ(label = "Jazz", rawRes = R.raw.ambience_jazz),
  OFF(label = "Off", rawRes = null),
  ;

  companion object {
    fun fromStorage(raw: String?): Ambience = entries.find { it.name == raw } ?: OFF
  }
}
