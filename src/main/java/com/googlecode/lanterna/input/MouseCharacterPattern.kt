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
 * Pattern used to detect Xterm-protocol mouse events coming in on the standard input channel
 * Created by martin on 19/07/15.
 *
 * @author Martin, Andreas
 */
class MouseCharacterPattern : CharacterPattern {

	override fun match(seq: List<Char>): CharacterPattern.Matching? {
		val size = seq.size
		if (size > 6) {
			return null // nope
		}
		// check first 3 chars:
		for (i in 0..2) {
			if (i >= size) {
				return CharacterPattern.Matching.NOT_YET // maybe later
			}
			if (seq[i] != PATTERN[i]) {
				return null // nope
			}
		}
		if (size < 6) {
			return CharacterPattern.Matching.NOT_YET // maybe later
		}
		var actionType: MouseActionType? = null
		var button = (seq[3] and 0x3) + 1
		if (button == 4) {
			//If last two bits are both set, it means button click release
			button = 0
		}
		val actionCode = seq[3] and 0x60 shr 5
		when (actionCode) {
			1 -> if (button > 0) {
				actionType = MouseActionType.CLICK_DOWN
			} else {
				actionType = MouseActionType.CLICK_RELEASE
			}
			2, 0 -> if (button == 0) {
				actionType = MouseActionType.MOVE
			} else {
				actionType = MouseActionType.DRAG
			}
			3 -> if (button == 1) {
				actionType = MouseActionType.SCROLL_UP
				button = 4
			} else {
				actionType = MouseActionType.SCROLL_DOWN
				button = 5
			}
		}
		val pos = TerminalPosition(seq[4] - 33, seq[5] - 33)

		val ma = MouseAction(actionType, button, pos)
		return CharacterPattern.Matching(ma) // yep
	}

	companion object {
		private val PATTERN = charArrayOf(KeyDecodingProfile.ESC_CODE, '[', 'M')
	}
}
