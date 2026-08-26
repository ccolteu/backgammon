package com.example.backgammon.ui.game

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.backgammon.data.GameStore
import com.example.backgammon.data.PrefsGameStore
import com.example.backgammon.data.isResumable
import com.example.backgammon.domain.GameState
import com.example.backgammon.domain.Move
import com.example.backgammon.domain.PlayPhase
import com.example.backgammon.domain.Rules
import com.example.backgammon.domain.Side
import com.example.backgammon.domain.WHITE_OFF
import com.example.backgammon.domain.boardWithoutMover
import com.example.backgammon.domain.dieSpent
import com.example.backgammon.domain.startingGame
import com.example.backgammon.domain.turnStatus
import com.example.backgammon.engine.AiLevel
import com.example.backgammon.engine.Engine
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class GameUiState(
  val board: GameState,
  val selected: Int? = null,
  val legalTargets: Set<Int> = emptySet(),
  val statusText: String = "Tap to roll",
  val phase: PlayPhase = PlayPhase.AWAITING_ROLL,
  val rollA: Int = 0,
  val rollB: Int = 0,
  val usedA: Boolean = false,
  val usedB: Boolean = false,
  val animatingMove: Move? = null,
  val animId: Int = 0,
  val animatingSide: Side? = null,
  val boardInteractive: Boolean = false,
  val diceInteractive: Boolean = true,
  val askResume: Boolean = false,
  val askConfirmNewGame: Boolean = false,
  val aiLevel: AiLevel = AiLevel.MEDIUM,
)

class BackgammonViewModel(
  private val store: GameStore,
  private val rollDice: () -> Pair<Int, Int> = { Random.nextInt(1, 7) to Random.nextInt(1, 7) },
) : ViewModel() {
  private var game = startingGame()
  private var phase = PlayPhase.AWAITING_ROLL
  private var pendingRoll: Pair<Int, Int>? = null
  private var pendingMove: Move? = null
  private var animId = 0
  private val cpuQueue = ArrayDeque<Move>()
  private var pendingSaved: GameState? = null
  private var aiLevel: AiLevel = store.loadAiLevel()
  private val _ui = MutableStateFlow(toUi())
  val uiState: StateFlow<GameUiState> = _ui

  init {
    val saved = store.load()
    if (saved != null && saved.isResumable()) {
      pendingSaved = saved
      _ui.value = toUi().copy(askResume = true, diceInteractive = false, boardInteractive = false)
    } else {
      if (saved != null) store.clear()
      awaitHumanRoll()
    }
  }

  fun onDiceTapped() {
    val ui = _ui.value
    if (ui.askResume || ui.askConfirmNewGame || game.winner != null) return
    if (phase != PlayPhase.AWAITING_ROLL || game.sideToMove != Side.WHITE) return
    pendingRoll = rollDice()
    phase = PlayPhase.ROLLING
    publish()
  }

  fun onRollSettled() {
    if (phase != PlayPhase.ROLLING) return
    val rolled = pendingRoll ?: return
    pendingRoll = null
    game = Rules.withDice(game, rolled.first, rolled.second)
    persist()
    if (Rules.legalMoves(game).isEmpty()) {
      passTurn()
    } else if (game.sideToMove == Side.BLACK) {
      cpuQueue.clear()
      cpuQueue.addAll(Engine.planTurn(game, aiLevel))
      startNextCpuMove()
    } else {
      phase = PlayPhase.READY
      publish()
    }
  }

  fun onMoveSettled() {
    if (phase != PlayPhase.MOVING) return
    val move = pendingMove ?: return
    val mover = game.sideToMove
    pendingMove = null
    game = Rules.apply(game, move)
    persist()
    if (game.winner != null) {
      phase = PlayPhase.READY
      publish()
      return
    }
    if (mover == Side.BLACK) {
      startNextCpuMove()
    } else if (game.dice.isEmpty() || Rules.legalMoves(game).isEmpty()) {
      passTurn()
    } else {
      phase = PlayPhase.READY
      publish()
    }
  }

  fun onPointClicked(point: Int) {
    val ui = _ui.value
    if (!ui.boardInteractive || game.winner != null) return
    if (game.sideToMove != Side.WHITE) return
    val legal = Rules.legalMoves(game)
    if (ui.selected != null) {
      val chosen =
        legal.firstOrNull { it.from == ui.selected && it.to == point }
          ?: legal.firstOrNull { point == ui.selected && it.from == ui.selected && it.to == WHITE_OFF }
      if (chosen != null) {
        pendingMove = chosen
        animId += 1
        phase = PlayPhase.MOVING
        publish()
        return
      }
    }
    val froms = legal.filter { it.from == point }
    if (froms.isNotEmpty()) {
      _ui.update { it.copy(selected = point, legalTargets = froms.map { m -> m.to }.toSet()) }
    } else {
      _ui.update { it.copy(selected = null, legalTargets = emptySet()) }
    }
  }

  fun requestNewGame() {
    if (game == startingGame() && phase == PlayPhase.AWAITING_ROLL) {
      newGame()
    } else {
      _ui.update { it.copy(askConfirmNewGame = true) }
    }
  }

  fun confirmNewGame() = newGame()

  fun dismissNewGameConfirm() {
    _ui.update { it.copy(askConfirmNewGame = false) }
  }

  fun setAiLevel(level: AiLevel) {
    if (level == aiLevel) return
    aiLevel = level
    store.saveAiLevel(level)
    _ui.update { it.copy(aiLevel = level) }
  }

  fun resumeSavedGame() {
    val saved = pendingSaved ?: return
    pendingSaved = null
    game = saved
    persist()
    when {
      game.winner != null -> {
        phase = PlayPhase.READY
        publish()
      }
      game.sideToMove == Side.BLACK && game.dice.isEmpty() -> startCpuRoll()
      game.sideToMove == Side.BLACK -> {
        cpuQueue.clear()
        cpuQueue.addAll(Engine.planTurn(game, aiLevel))
        startNextCpuMove()
      }
      game.dice.isEmpty() -> awaitHumanRoll()
      Rules.legalMoves(game).isEmpty() -> passTurn()
      else -> {
        phase = PlayPhase.READY
        publish()
      }
    }
  }

  fun startNewGameFromPrompt() {
    pendingSaved = null
    newGame()
  }

  private fun newGame() {
    game = startingGame()
    pendingRoll = null
    pendingMove = null
    cpuQueue.clear()
    pendingSaved = null
    store.clear()
    awaitHumanRoll()
  }

  private fun awaitHumanRoll() {
    game = game.copy(sideToMove = Side.WHITE, dice = emptyList())
    phase = PlayPhase.AWAITING_ROLL
    persist()
    publish()
  }

  private fun startCpuRoll() {
    pendingRoll = rollDice()
    phase = PlayPhase.ROLLING
    publish()
  }

  private fun startNextCpuMove() {
    val next = cpuQueue.removeFirstOrNull()
    if (next == null) {
      passTurn()
    } else {
      pendingMove = next
      animId += 1
      phase = PlayPhase.MOVING
      publish()
    }
  }

  private fun passTurn() {
    game = Rules.endTurn(game)
    persist()
    when {
      game.winner != null -> {
        phase = PlayPhase.READY
        publish()
      }
      game.sideToMove == Side.BLACK -> startCpuRoll()
      else -> awaitHumanRoll()
    }
  }

  private fun persist() {
    store.save(game)
  }

  private fun publish() {
    _ui.value = toUi()
  }

  private fun toUi(): GameUiState {
    val visual = pendingMove?.let { boardWithoutMover(game, it) } ?: game
    val shown = pendingRoll ?: (game.rollA to game.rollB)
    return GameUiState(
      board = visual,
      statusText = turnStatus(game, phase),
      phase = phase,
      rollA = shown.first,
      rollB = shown.second,
      usedA = phase != PlayPhase.AWAITING_ROLL && dieSpent(game.rollA, game.rollB, game.dice),
      usedB = phase != PlayPhase.AWAITING_ROLL && dieSpent(game.rollB, game.rollA, game.dice),
      animatingMove = pendingMove,
      animId = animId,
      animatingSide = pendingMove?.let { game.sideToMove },
      boardInteractive = phase == PlayPhase.READY && game.winner == null && game.sideToMove == Side.WHITE,
      diceInteractive = phase == PlayPhase.AWAITING_ROLL && game.winner == null && game.sideToMove == Side.WHITE,
      aiLevel = aiLevel,
    )
  }

  companion object {
    fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
      initializer { BackgammonViewModel(PrefsGameStore(context.applicationContext)) }
    }
  }
}
