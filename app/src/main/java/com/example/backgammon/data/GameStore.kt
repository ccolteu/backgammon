package com.example.backgammon.data

import android.content.Context
import com.example.backgammon.domain.GameState
import com.example.backgammon.domain.MoveCodec
import com.example.backgammon.domain.startingGame
import com.example.backgammon.engine.AiLevel

interface GameStore {
  fun load(): GameState?

  fun save(state: GameState)

  fun clear()

  fun loadAiLevel(): AiLevel

  fun saveAiLevel(level: AiLevel)
}

class PrefsGameStore(context: Context) : GameStore {
  private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

  override fun load(): GameState? = MoveCodec.decodeState(prefs.getString(KEY_STATE, "").orEmpty())

  override fun save(state: GameState) {
    prefs.edit().putString(KEY_STATE, MoveCodec.encodeState(state)).apply()
  }

  override fun clear() {
    prefs.edit().remove(KEY_STATE).remove(KEY_LOG).apply()
  }

  override fun loadAiLevel(): AiLevel = AiLevel.fromStorage(prefs.getString(KEY_LEVEL, null))

  override fun saveAiLevel(level: AiLevel) {
    prefs.edit().putString(KEY_LEVEL, level.name).apply()
  }

  private companion object {
    const val PREFS = "backgammon_game"
    const val KEY_STATE = "state"
    const val KEY_LOG = "log"
    const val KEY_LEVEL = "ai_level"
  }
}

fun GameState.isResumable(): Boolean = this != startingGame() && winner == null
