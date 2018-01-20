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
package com.googlecode.lanterna.input

import com.googlecode.lanterna.TerminalPosition

/**
 * This class recognizes character combinations which are actually a cursor position report. See
 * [Wikipedia](http://en.wikipedia.org/wiki/ANSI_escape_code)'s article on ANSI escape codes for more
 * information about how cursor position reporting works ("DSR – Device Status Report").
 *
 * @author Martin, Andreas
 */
class ScreenInfoCharacterPattern : EscapeSequenceCharacterPattern() {
	init {
		useEscEsc = false // stdMap and finMap don't matter here.
	}

	override fun getKeyStrokeRaw(first: Char, num1: Int, num2: Int, last: Char, bEsc: Boolean): KeyStroke? {
		if (first != '[' || last != 'R' || num1 == 0 || num2 == 0 || bEsc) {
			return null // nope
		}
		if (num1 == 1 && num2 <= 8) {
			return null // nope: much more likely it's an F3 with modifiers
		}
		val pos = TerminalPosition(num2, num1)
		return ScreenInfoAction(pos) // yep
	}

	companion object {

		fun tryToAdopt(ks: KeyStroke?): ScreenInfoAction? {
			if (ks == null) {
				return null
			}
			when (ks.keyType) {
				KeyType.CursorLocation -> return ks as ScreenInfoAction?
				KeyType.F3 // reconstruct position from F3's modifiers.
				-> {
					val col = (1 + (if (ks.isAltDown) EscapeSequenceCharacterPattern.ALT else 0)
						+ (if (ks.isCtrlDown) EscapeSequenceCharacterPattern.CTRL else 0)
						+ if (ks.isShiftDown) EscapeSequenceCharacterPattern.SHIFT else 0)
					val pos = TerminalPosition(col, 1)
					return ScreenInfoAction(pos)
				}
				else -> return null
			}
		}
	}


}
