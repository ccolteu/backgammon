package com.cc.backgammon.ui.game

import com.cc.backgammon.data.GameStore
import com.cc.backgammon.domain.GameState
import com.cc.backgammon.domain.PlayPhase
import com.cc.backgammon.domain.Rules
import com.cc.backgammon.domain.Side
import com.cc.backgammon.domain.WHITE_OFF
import com.cc.backgammon.engine.AiLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BackgammonViewModelTest {
  private val mainDispatcher = UnconfinedTestDispatcher()

  @Before
  fun setMainDispatcher() {
    Dispatchers.setMain(mainDispatcher)
  }

  @After
  fun resetMainDispatcher() {
    Dispatchers.resetMain()
  }

  private fun vm(store: GameStore = MemoryGameStore(), rollDice: () -> Pair<Int, Int> = { 3 to 5 }) =
    BackgammonViewModel(store, rollDice = rollDice, computeDispatcher = mainDispatcher)

  @Test
  fun doesNotRollUntilDiceAreTapped() {
    val vm = vm()
    assertEquals(PlayPhase.AWAITING_ROLL, vm.uiState.value.phase)
    assertTrue(vm.uiState.value.board.dice.isEmpty())
    assertEquals("Tap to roll", vm.uiState.value.statusText)

    vm.onDiceTapped()
    assertEquals(PlayPhase.ROLLING, vm.uiState.value.phase)
    assertEquals(3, vm.uiState.value.rollA)
    assertEquals(5, vm.uiState.value.rollB)
    assertEquals("", vm.uiState.value.statusText)

    vm.onRollSettled()
    assertEquals(PlayPhase.READY, vm.uiState.value.phase)
    assertEquals(listOf(3, 5), vm.uiState.value.board.dice)
    assertEquals("Move pieces", vm.uiState.value.statusText)
  }

  @Test
  fun humanMoveAnimatesBeforeTheBoardCommits() {
    val vm = vm()
    vm.onDiceTapped()
    vm.onRollSettled()
    vm.onPointClicked(24)
    vm.onPointClicked(21)

    assertEquals(PlayPhase.MOVING, vm.uiState.value.phase)
    assertEquals(24, vm.uiState.value.animatingMove?.from)
    assertEquals(21, vm.uiState.value.animatingMove?.to)
    assertEquals(1, vm.uiState.value.board.countAt(24))
    assertEquals(0, vm.uiState.value.board.countAt(21))

    vm.onMoveSettled()
    assertEquals(1, vm.uiState.value.board.countAt(21))
    assertEquals(PlayPhase.READY, vm.uiState.value.phase)
    assertNull(vm.uiState.value.animatingMove)
  }

  @Test
  fun afterHumanTurnCpuStartsItsOwnRoll() {
    val vm = vm()
    vm.onDiceTapped()
    vm.onRollSettled()
    play(vm, 24, 21)
    play(vm, 13, 8)

    assertEquals(PlayPhase.ROLLING, vm.uiState.value.phase)
    assertEquals(com.cc.backgammon.domain.Side.BLACK, vm.uiState.value.board.sideToMove)

    vm.onRollSettled()
    assertTrue(
      vm.uiState.value.phase == PlayPhase.MOVING ||
        vm.uiState.value.phase == PlayPhase.NO_MOVES ||
        Rules.legalMoves(vm.uiState.value.board).isEmpty(),
    )
  }

  @Test
  fun afterCpuTurnDiceKeepShowingTheLastRoll() {
    val vm = vm()
    vm.onDiceTapped()
    vm.onRollSettled()
    play(vm, 24, 21)
    play(vm, 13, 8)
    vm.onRollSettled()
    var guard = 0
    while (guard++ < 12) {
      when (vm.uiState.value.phase) {
        PlayPhase.MOVING -> vm.onMoveSettled()
        PlayPhase.NO_MOVES -> vm.onNoMovesSettled()
        else -> break
      }
    }
    assertEquals(PlayPhase.AWAITING_ROLL, vm.uiState.value.phase)
    assertEquals(3, vm.uiState.value.rollA)
    assertEquals(5, vm.uiState.value.rollB)
    assertTrue(!vm.uiState.value.usedA)
    assertTrue(!vm.uiState.value.usedB)
  }

  @Test
  fun tappingSelectedCheckerAgainBearsOff() {
    val points = MutableList(25) { 0 }
    points[6] = 1
    points[24] = -15
    val store = MemoryGameStore()
    store.save(
      GameState(
        points = points,
        whiteBar = 0,
        blackBar = 0,
        whiteOff = 14,
        blackOff = 0,
        sideToMove = Side.WHITE,
        dice = listOf(6),
        rollA = 6,
        rollB = 6,
      ),
    )
    val vm = vm(store)
    vm.resumeSavedGame()

    vm.onPointClicked(6)
    assertEquals(setOf(WHITE_OFF), vm.uiState.value.legalTargets)

    vm.onPointClicked(6)
    assertEquals(PlayPhase.MOVING, vm.uiState.value.phase)
    assertEquals(6, vm.uiState.value.animatingMove?.from)
    assertEquals(WHITE_OFF, vm.uiState.value.animatingMove?.to)
  }

  @Test
  fun tappingTheOffTrayBearsOff() {
    val points = MutableList(25) { 0 }
    points[6] = 1
    points[24] = -15
    val store = MemoryGameStore()
    store.save(
      GameState(
        points = points,
        whiteBar = 0,
        blackBar = 0,
        whiteOff = 14,
        blackOff = 0,
        sideToMove = Side.WHITE,
        dice = listOf(6),
        rollA = 6,
        rollB = 6,
      ),
    )
    val vm = vm(store)
    vm.resumeSavedGame()

    vm.onPointClicked(6)
    assertEquals(setOf(WHITE_OFF), vm.uiState.value.legalTargets)

    vm.onPointClicked(WHITE_OFF)
    assertEquals(PlayPhase.MOVING, vm.uiState.value.phase)
    assertEquals(6, vm.uiState.value.animatingMove?.from)
    assertEquals(WHITE_OFF, vm.uiState.value.animatingMove?.to)
  }

  @Test
  fun newGameIsDisabledOnAFreshBoardAndEnabledAfterTheFirstRoll() {
    val vm = vm()
    assertFalse(vm.uiState.value.newGameEnabled)
    vm.requestNewGame()
    assertFalse(vm.uiState.value.askConfirmNewGame)

    vm.onDiceTapped()
    assertFalse(vm.uiState.value.newGameEnabled)
    vm.onRollSettled()
    assertTrue(vm.uiState.value.newGameEnabled)
    vm.requestNewGame()
    assertTrue(vm.uiState.value.askConfirmNewGame)
    vm.confirmNewGame()
    assertFalse(vm.uiState.value.newGameEnabled)
  }

  @Test
  fun setAiLevel_persists() {
    val store = MemoryGameStore()
    val vm = vm(store)
    assertEquals(com.cc.backgammon.engine.AiLevel.STANDARD, vm.uiState.value.aiLevel)
    vm.setAiLevel(com.cc.backgammon.engine.AiLevel.ADVANCED)
    assertEquals(com.cc.backgammon.engine.AiLevel.ADVANCED, vm.uiState.value.aiLevel)
    assertEquals(com.cc.backgammon.engine.AiLevel.ADVANCED, store.loadAiLevel())
  }

  @Test
  fun setBoardStyle_persistsWithoutResettingTheGame() {
    val store = MemoryGameStore()
    val vm = vm(store)
    vm.onDiceTapped()
    vm.onRollSettled()
    assertEquals(BoardStyle.OAK_CHARCOAL, vm.uiState.value.boardStyle)
    vm.setBoardStyle(BoardStyle.MAHOGANY_CLARET)
    assertEquals(BoardStyle.MAHOGANY_CLARET, vm.uiState.value.boardStyle)
    assertEquals(BoardStyle.MAHOGANY_CLARET, store.loadBoardStyle())
    assertEquals(PlayPhase.READY, vm.uiState.value.phase)
    assertEquals(listOf(3, 5), vm.uiState.value.board.dice)
  }

  @Test
  fun deadRoll_showsNoMovesThenPassesToCpu() {
    val points = MutableList(25) { 0 }
    points[24] = -2
    points[23] = -2
    val store = MemoryGameStore()
    store.save(
      GameState(
        points = points,
        whiteBar = 1,
        blackBar = 0,
        whiteOff = 14,
        blackOff = 11,
        sideToMove = Side.WHITE,
        dice = emptyList(),
      ),
    )
    val vm = vm(store, rollDice = { 1 to 2 })
    vm.resumeSavedGame()
    vm.onDiceTapped()
    vm.onRollSettled()
    assertEquals(PlayPhase.NO_MOVES, vm.uiState.value.phase)
    assertEquals("No moves", vm.uiState.value.statusText)
    vm.onNoMovesSettled()
    assertEquals(PlayPhase.ROLLING, vm.uiState.value.phase)
    assertEquals(Side.BLACK, vm.uiState.value.board.sideToMove)
  }

  @Test
  fun cpuPlanning_showsThinkingUntilThePlanIsReady() {
    val dispatcher = StandardTestDispatcher()
    Dispatchers.setMain(dispatcher)
    val store = MemoryGameStore()
    store.saveAiLevel(AiLevel.EXPERT)
    val vm = BackgammonViewModel(store, rollDice = { 3 to 5 }, computeDispatcher = dispatcher)
    vm.onDiceTapped()
    vm.onRollSettled()
    play(vm, 24, 21)
    play(vm, 13, 8)
    vm.onRollSettled()
    assertEquals(PlayPhase.THINKING, vm.uiState.value.phase)
    assertEquals("CPU thinking", vm.uiState.value.statusText)
    dispatcher.scheduler.advanceUntilIdle()
    assertTrue(
      vm.uiState.value.phase == PlayPhase.MOVING || vm.uiState.value.phase == PlayPhase.NO_MOVES,
    )
  }

  private fun play(vm: BackgammonViewModel, from: Int, to: Int) {
    vm.onPointClicked(from)
    vm.onPointClicked(to)
    vm.onMoveSettled()
  }
}

private class MemoryGameStore : GameStore {
  private var state: GameState? = null
  private var level = com.cc.backgammon.engine.AiLevel.STANDARD
  private var board = BoardStyle.OAK_CHARCOAL

  override fun load(): GameState? = state

  override fun save(state: GameState) {
    this.state = state
  }

  override fun clear() {
    state = null
  }

  override fun loadAiLevel() = level

  override fun saveAiLevel(level: com.cc.backgammon.engine.AiLevel) {
    this.level = level
  }

  override fun loadBoardStyle() = board

  override fun saveBoardStyle(style: BoardStyle) {
    board = style
  }
}
