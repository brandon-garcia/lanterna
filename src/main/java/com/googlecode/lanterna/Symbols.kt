/*
 * This file is part of lanterna (http://code.google.com/p/lanterna/).
 *
 * lanterna is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2010-2017 Martin Berglund
 */

package com.googlecode.lanterna

/**
 * Some text graphics, taken from http://en.wikipedia.org/wiki/Codepage_437 but converted to its UTF-8 counterpart.
 * This class it mostly here to help out with building text GUIs when you don't have a handy Unicode chart available.
 * Previously this class was known as ACS, which was taken from ncurses (meaning "Alternative Character Set").
 * @author martin
 */
object Symbols {

	/**
	 * ☺
	 */
	val FACE_WHITE: Char = 0x263A.toChar()
	/**
	 * ☻
	 */
	val FACE_BLACK: Char = 0x263B.toChar()
	/**
	 * ♥
	 */
	val HEART: Char = 0x2665.toChar()
	/**
	 * ♣
	 */
	val CLUB: Char = 0x2663.toChar()
	/**
	 * ♦
	 */
	val DIAMOND: Char = 0x2666.toChar()
	/**
	 * ♠
	 */
	val SPADES: Char = 0x2660.toChar()
	/**
	 * •
	 */
	val BULLET: Char = 0x2022.toChar()
	/**
	 * ◘
	 */
	val INVERSE_BULLET: Char = 0x25d8.toChar()
	/**
	 * ○
	 */
	val WHITE_CIRCLE: Char = 0x25cb.toChar()
	/**
	 * ◙
	 */
	val INVERSE_WHITE_CIRCLE: Char = 0x25d9.toChar()

	/**
	 * ■
	 */
	val SOLID_SQUARE: Char = 0x25A0.toChar()
	/**
	 * ▪
	 */
	val SOLID_SQUARE_SMALL: Char = 0x25AA.toChar()
	/**
	 * □
	 */
	val OUTLINED_SQUARE: Char = 0x25A1.toChar()
	/**
	 * ▫
	 */
	val OUTLINED_SQUARE_SMALL: Char = 0x25AB.toChar()

	/**
	 * ♀
	 */
	val FEMALE: Char = 0x2640.toChar()
	/**
	 * ♂
	 */
	val MALE: Char = 0x2642.toChar()

	/**
	 * ↑
	 */
	val ARROW_UP: Char = 0x2191.toChar()
	/**
	 * ↓
	 */
	val ARROW_DOWN: Char = 0x2193.toChar()
	/**
	 * →
	 */
	val ARROW_RIGHT: Char = 0x2192.toChar()
	/**
	 * ←
	 */
	val ARROW_LEFT: Char = 0x2190.toChar()

	/**
	 * █
	 */
	val BLOCK_SOLID: Char = 0x2588.toChar()
	/**
	 * ▓
	 */
	val BLOCK_DENSE: Char = 0x2593.toChar()
	/**
	 * ▒
	 */
	val BLOCK_MIDDLE: Char = 0x2592.toChar()
	/**
	 * ░
	 */
	val BLOCK_SPARSE: Char = 0x2591.toChar()

	/**
	 * ►
	 */
	val TRIANGLE_RIGHT_POINTING_BLACK: Char = 0x25BA.toChar()
	/**
	 * ◄
	 */
	val TRIANGLE_LEFT_POINTING_BLACK: Char = 0x25C4.toChar()
	/**
	 * ▲
	 */
	val TRIANGLE_UP_POINTING_BLACK: Char = 0x25B2.toChar()
	/**
	 * ▼
	 */
	val TRIANGLE_DOWN_POINTING_BLACK: Char = 0x25BC.toChar()

	/**
	 * ⏴
	 */
	val TRIANGLE_RIGHT_POINTING_MEDIUM_BLACK: Char = 0x23F4.toChar()
	/**
	 * ⏵
	 */
	val TRIANGLE_LEFT_POINTING_MEDIUM_BLACK: Char = 0x23F5.toChar()
	/**
	 * ⏶
	 */
	val TRIANGLE_UP_POINTING_MEDIUM_BLACK: Char = 0x23F6.toChar()
	/**
	 * ⏷
	 */
	val TRIANGLE_DOWN_POINTING_MEDIUM_BLACK: Char = 0x23F7.toChar()


	/**
	 * ─
	 */
	val SINGLE_LINE_HORIZONTAL: Char = 0x2500.toChar()
	/**
	 * ━
	 */
	val BOLD_SINGLE_LINE_HORIZONTAL: Char = 0x2501.toChar()
	/**
	 * ╾
	 */
	val BOLD_TO_NORMAL_SINGLE_LINE_HORIZONTAL: Char = 0x257E.toChar()
	/**
	 * ╼
	 */
	val BOLD_FROM_NORMAL_SINGLE_LINE_HORIZONTAL: Char = 0x257C.toChar()
	/**
	 * ═
	 */
	val DOUBLE_LINE_HORIZONTAL: Char = 0x2550.toChar()
	/**
	 * │
	 */
	val SINGLE_LINE_VERTICAL: Char = 0x2502.toChar()
	/**
	 * ┃
	 */
	val BOLD_SINGLE_LINE_VERTICAL: Char = 0x2503.toChar()
	/**
	 * ╿
	 */
	val BOLD_TO_NORMAL_SINGLE_LINE_VERTICAL: Char = 0x257F.toChar()
	/**
	 * ╽
	 */
	val BOLD_FROM_NORMAL_SINGLE_LINE_VERTICAL: Char = 0x257D.toChar()
	/**
	 * ║
	 */
	val DOUBLE_LINE_VERTICAL: Char = 0x2551.toChar()

	/**
	 * ┌
	 */
	val SINGLE_LINE_TOP_LEFT_CORNER: Char = 0x250C.toChar()
	/**
	 * ╔
	 */
	val DOUBLE_LINE_TOP_LEFT_CORNER: Char = 0x2554.toChar()
	/**
	 * ┐
	 */
	val SINGLE_LINE_TOP_RIGHT_CORNER: Char = 0x2510.toChar()
	/**
	 * ╗
	 */
	val DOUBLE_LINE_TOP_RIGHT_CORNER: Char = 0x2557.toChar()

	/**
	 * └
	 */
	val SINGLE_LINE_BOTTOM_LEFT_CORNER: Char = 0x2514.toChar()
	/**
	 * ╚
	 */
	val DOUBLE_LINE_BOTTOM_LEFT_CORNER: Char = 0x255A.toChar()
	/**
	 * ┘
	 */
	val SINGLE_LINE_BOTTOM_RIGHT_CORNER: Char = 0x2518.toChar()
	/**
	 * ╝
	 */
	val DOUBLE_LINE_BOTTOM_RIGHT_CORNER: Char = 0x255D.toChar()

	/**
	 * ┼
	 */
	val SINGLE_LINE_CROSS: Char = 0x253C.toChar()
	/**
	 * ╬
	 */
	val DOUBLE_LINE_CROSS: Char = 0x256C.toChar()
	/**
	 * ╪
	 */
	val DOUBLE_LINE_HORIZONTAL_SINGLE_LINE_CROSS: Char = 0x256A.toChar()
	/**
	 * ╫
	 */
	val DOUBLE_LINE_VERTICAL_SINGLE_LINE_CROSS: Char = 0x256B.toChar()

	/**
	 * ┴
	 */
	val SINGLE_LINE_T_UP: Char = 0x2534.toChar()
	/**
	 * ┬
	 */
	val SINGLE_LINE_T_DOWN: Char = 0x252C.toChar()
	/**
	 * ├
	 */
	val SINGLE_LINE_T_RIGHT: Char = 0x251c.toChar()
	/**
	 * ┤
	 */
	val SINGLE_LINE_T_LEFT: Char = 0x2524.toChar()

	/**
	 * ╨
	 */
	val SINGLE_LINE_T_DOUBLE_UP: Char = 0x2568.toChar()
	/**
	 * ╥
	 */
	val SINGLE_LINE_T_DOUBLE_DOWN: Char = 0x2565.toChar()
	/**
	 * ╞
	 */
	val SINGLE_LINE_T_DOUBLE_RIGHT: Char = 0x255E.toChar()
	/**
	 * ╡
	 */
	val SINGLE_LINE_T_DOUBLE_LEFT: Char = 0x2561.toChar()

	/**
	 * ╩
	 */
	val DOUBLE_LINE_T_UP: Char = 0x2569.toChar()
	/**
	 * ╦
	 */
	val DOUBLE_LINE_T_DOWN: Char = 0x2566.toChar()
	/**
	 * ╠
	 */
	val DOUBLE_LINE_T_RIGHT: Char = 0x2560.toChar()
	/**
	 * ╣
	 */
	val DOUBLE_LINE_T_LEFT: Char = 0x2563.toChar()

	/**
	 * ╧
	 */
	val DOUBLE_LINE_T_SINGLE_UP: Char = 0x2567.toChar()
	/**
	 * ╤
	 */
	val DOUBLE_LINE_T_SINGLE_DOWN: Char = 0x2564.toChar()
	/**
	 * ╟
	 */
	val DOUBLE_LINE_T_SINGLE_RIGHT: Char = 0x255F.toChar()
	/**
	 * ╢
	 */
	val DOUBLE_LINE_T_SINGLE_LEFT: Char = 0x2562.toChar()
}
