package com.example.backgammon.ui.game

import com.example.backgammon.data.GameStore
import com.example.backgammon.domain.GameState
import com.example.backgammon.domain.PlayPhase
import com.example.backgammon.domain.Rules
import com.example.backgammon.domain.Side
import com.example.backgammon.domain.WHITE_OFF
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackgammonViewModelTest {
  @Test
  fun doesNotRollUntilDiceAreTapped() {
    val vm = BackgammonViewModel(MemoryGameStore(), rollDice = { 3 to 5 })
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
    val vm = BackgammonViewModel(MemoryGameStore(), rollDice = { 3 to 5 })
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
    val vm = BackgammonViewModel(MemoryGameStore(), rollDice = { 3 to 5 })
    vm.onDiceTapped()
    vm.onRollSettled()
    play(vm, 24, 21)
    play(vm, 13, 8)

    assertEquals(PlayPhase.ROLLING, vm.uiState.value.phase)
    assertEquals(com.example.backgammon.domain.Side.BLACK, vm.uiState.value.board.sideToMove)

    vm.onRollSettled()
    assertTrue(vm.uiState.value.phase == PlayPhase.MOVING || Rules.legalMoves(vm.uiState.value.board).isEmpty())
  }

  @Test
  fun afterCpuTurnDiceKeepShowingTheLastRoll() {
    val vm = BackgammonViewModel(MemoryGameStore(), rollDice = { 3 to 5 })
    vm.onDiceTapped()
    vm.onRollSettled()
    play(vm, 24, 21)
    play(vm, 13, 8)
    vm.onRollSettled()
    var guard = 0
    while (vm.uiState.value.phase == PlayPhase.MOVING && guard++ < 8) {
      vm.onMoveSettled()
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
    val vm = BackgammonViewModel(store)
    vm.resumeSavedGame()

    vm.onPointClicked(6)
    assertEquals(setOf(WHITE_OFF), vm.uiState.value.legalTargets)

    vm.onPointClicked(6)
    assertEquals(PlayPhase.MOVING, vm.uiState.value.phase)
    assertEquals(6, vm.uiState.value.animatingMove?.from)
    assertEquals(WHITE_OFF, vm.uiState.value.animatingMove?.to)
  }

  @Test
  fun setAiLevel_persists() {
    val store = MemoryGameStore()
    val vm = BackgammonViewModel(store, rollDice = { 3 to 5 })
    assertEquals(com.example.backgammon.engine.AiLevel.MEDIUM, vm.uiState.value.aiLevel)
    vm.setAiLevel(com.example.backgammon.engine.AiLevel.HARD)
    assertEquals(com.example.backgammon.engine.AiLevel.HARD, vm.uiState.value.aiLevel)
    assertEquals(com.example.backgammon.engine.AiLevel.HARD, store.loadAiLevel())
  }

  private fun play(vm: BackgammonViewModel, from: Int, to: Int) {
    vm.onPointClicked(from)
    vm.onPointClicked(to)
    vm.onMoveSettled()
  }
}

private class MemoryGameStore : GameStore {
  private var state: GameState? = null
  private var level = com.example.backgammon.engine.AiLevel.MEDIUM

  override fun load(): GameState? = state

  override fun save(state: GameState) {
    this.state = state
  }

  override fun clear() {
    state = null
  }

  override fun loadAiLevel() = level

  override fun saveAiLevel(level: com.example.backgammon.engine.AiLevel) {
    this.level = level
  }
}
