package com.example.backgammon.data

import android.content.Context
import com.example.backgammon.domain.GameState
import com.example.backgammon.domain.MoveCodec
import com.example.backgammon.domain.startingGame

interface GameStore {
  fun load(): GameState?

  fun save(state: GameState)

  fun loadLog(): List<String>

  fun saveLog(lines: List<String>)

  fun clear()
}

class PrefsGameStore(context: Context) : GameStore {
  private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

  override fun load(): GameState? = MoveCodec.decodeState(prefs.getString(KEY_STATE, "").orEmpty())

  override fun save(state: GameState) {
    prefs.edit().putString(KEY_STATE, MoveCodec.encodeState(state)).apply()
  }

  override fun loadLog(): List<String> {
    val raw = prefs.getString(KEY_LOG, "").orEmpty()
    return if (raw.isBlank()) emptyList() else raw.split("\n")
  }

  override fun saveLog(lines: List<String>) {
    prefs.edit().putString(KEY_LOG, lines.joinToString("\n")).apply()
  }

  override fun clear() {
    prefs.edit().remove(KEY_STATE).remove(KEY_LOG).apply()
  }

  private companion object {
    const val PREFS = "backgammon_game"
    const val KEY_STATE = "state"
    const val KEY_LOG = "log"
  }
}

fun GameState.isResumable(): Boolean = this != startingGame() && winner == null
