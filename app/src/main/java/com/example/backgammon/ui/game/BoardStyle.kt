package com.example.backgammon.ui.game

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.backgammon.R
import com.example.backgammon.theme.WalnutBackground

enum class BoardStyle(
  val label: String,
  @param:DrawableRes val drawable: Int,
  @param:DrawableRes val cloth: Int,
  val backdrop: Color,
) {
  ORIGINAL(
    label = "Original",
    drawable = R.drawable.board_original,
    cloth = R.drawable.cloth_oak_table,
    backdrop = WalnutBackground,
  ),
  CLUB_WALNUT(
    label = "Club walnut",
    drawable = R.drawable.board_club_walnut,
    cloth = R.drawable.cloth_forest_baize,
    backdrop = Color(0xFF121810),
  ),
  EBONY_NAVY(
    label = "Ebony navy",
    drawable = R.drawable.board_ebony_navy,
    cloth = R.drawable.cloth_blue_silk,
    backdrop = Color(0xFF0C1834),
  ),
  ASH_SAGE(
    label = "Ash sage",
    drawable = R.drawable.board_ash_sage,
    cloth = R.drawable.cloth_oat_linen,
    backdrop = Color(0xFF161410),
  ),
  MAHOGANY_CLARET(
    label = "Mahogany claret",
    drawable = R.drawable.board_mahogany_claret,
    cloth = R.drawable.cloth_oxblood_damask,
    backdrop = Color(0xFF1A0A0E),
  ),
  ;

  companion object {
    fun fromStorage(raw: String?): BoardStyle = entries.find { it.name == raw } ?: ORIGINAL
  }
}
