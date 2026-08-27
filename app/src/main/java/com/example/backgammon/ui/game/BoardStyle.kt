package com.example.backgammon.ui.game

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.backgammon.R
import com.example.backgammon.theme.Brass
import com.example.backgammon.theme.Cream
import com.example.backgammon.theme.Parchment
import com.example.backgammon.theme.WalnutBackground
import com.example.backgammon.theme.WalnutRail

data class HudChrome(
  val fill: Color,
  val border: Color,
  val onFill: Color,
  val accent: Color,
  val status: Color,
)

enum class BoardStyle(
  val label: String,
  @param:DrawableRes val drawable: Int,
  @param:DrawableRes val cloth: Int,
  val backdrop: Color,
  val chrome: HudChrome,
) {
  ORIGINAL(
    label = "Original",
    drawable = R.drawable.board_original,
    cloth = R.drawable.cloth_oak_table,
    backdrop = WalnutBackground,
    chrome =
      HudChrome(
        fill = WalnutRail,
        border = Brass,
        onFill = Cream,
        accent = Brass,
        status = Cream,
      ),
  ),
  CLUB_WALNUT(
    label = "Club walnut",
    drawable = R.drawable.board_club_walnut,
    cloth = R.drawable.cloth_forest_baize,
    backdrop = Color(0xFF121810),
    chrome =
      HudChrome(
        fill = Color(0xFF1E3A28),
        border = Color(0xFF8FA86A),
        onFill = Parchment,
        accent = Color(0xFFB4C878),
        status = Parchment,
      ),
  ),
  EBONY_NAVY(
    label = "Ebony navy",
    drawable = R.drawable.board_ebony_navy,
    cloth = R.drawable.cloth_blue_silk,
    backdrop = Color(0xFF0C1834),
    chrome =
      HudChrome(
        fill = Color(0xFF1A2A4C),
        border = Color(0xFF8AA0C8),
        onFill = Color(0xFFE4E8F0),
        accent = Color(0xFFB8C8E0),
        status = Color(0xFFE4E8F0),
      ),
  ),
  ASH_SAGE(
    label = "Ash sage",
    drawable = R.drawable.board_ash_sage,
    cloth = R.drawable.cloth_oat_linen,
    backdrop = Color(0xFF161410),
    chrome =
      HudChrome(
        fill = Color(0xFFF2EDE3),
        border = Color(0xFF8A8B6E),
        onFill = Color(0xFF3E4134),
        accent = Color(0xFF5C5E46),
        status = Color(0xFF3E4134),
      ),
  ),
  MAHOGANY_CLARET(
    label = "Mahogany claret",
    drawable = R.drawable.board_mahogany_claret,
    cloth = R.drawable.cloth_oxblood_damask,
    backdrop = Color(0xFF1A0A0E),
    chrome =
      HudChrome(
        fill = Color(0xFF4A1822),
        border = Brass,
        onFill = Cream,
        accent = Brass,
        status = Cream,
      ),
  ),
  ;

  val statusColor: Color
    get() = chrome.status

  companion object {
    fun fromStorage(raw: String?): BoardStyle = entries.find { it.name == raw } ?: ORIGINAL
  }
}
