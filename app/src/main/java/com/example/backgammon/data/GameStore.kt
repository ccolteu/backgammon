package com.example.backgammon.data

import android.content.Context
import com.example.backgammon.domain.GameState
import com.example.backgammon.domain.MoveCodec
import com.example.backgammon.domain.startingGame
import com.example.backgammon.engine.AiLevel
import com.example.backgammon.ui.game.Ambience
import com.example.backgammon.ui.game.BoardStyle

interface GameStore {
  fun load(): GameState?

  fun save(state: GameState)

  fun clear()

  fun loadAiLevel(): AiLevel

  fun saveAiLevel(level: AiLevel)

  fun loadBoardStyle(): BoardStyle

  fun saveBoardStyle(style: BoardStyle)

  fun loadAmbience(): Ambience

  fun saveAmbience(ambience: Ambience)
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

  override fun loadBoardStyle(): BoardStyle = BoardStyle.fromStorage(prefs.getString(KEY_BOARD, null))

  override fun saveBoardStyle(style: BoardStyle) {
    prefs.edit().putString(KEY_BOARD, style.name).apply()
  }

  override fun loadAmbience(): Ambience = Ambience.fromStorage(prefs.getString(KEY_AMBIENCE, null))

  override fun saveAmbience(ambience: Ambience) {
    prefs.edit().putString(KEY_AMBIENCE, ambience.name).apply()
  }

  private companion object {
    const val PREFS = "backgammon_game"
    const val KEY_STATE = "state"
    const val KEY_LOG = "log"
    const val KEY_LEVEL = "ai_level"
    const val KEY_BOARD = "board_style"
    const val KEY_AMBIENCE = "ambience"
  }
}

fun GameState.isResumable(): Boolean = this != startingGame() && winner == null
