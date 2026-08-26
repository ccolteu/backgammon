package com.example.backgammon.ui.game

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.backgammon.R
import com.example.backgammon.theme.WalnutBackground

enum class BoardStyle(val label: String, @param:DrawableRes val drawable: Int, val backdrop: Color) {
  ORIGINAL(label = "Original", drawable = R.drawable.board_original, backdrop = WalnutBackground),
  CLUB_WALNUT(label = "Club walnut", drawable = R.drawable.board_club_walnut, backdrop = Color(0xFF121810)),
  EBONY_NAVY(label = "Ebony navy", drawable = R.drawable.board_ebony_navy, backdrop = Color(0xFF0B1018)),
  ASH_SAGE(label = "Ash sage", drawable = R.drawable.board_ash_sage, backdrop = Color(0xFF161410)),
  WALNUT_FOREST(label = "Walnut forest", drawable = R.drawable.board_walnut_forest, backdrop = Color(0xFF10180F)),
  MAHOGANY_CLARET(label = "Mahogany claret", drawable = R.drawable.board_mahogany_claret, backdrop = Color(0xFF1A0A0E)),
  ;

  companion object {
    fun fromStorage(raw: String?): BoardStyle = entries.find { it.name == raw } ?: ORIGINAL
  }
}
