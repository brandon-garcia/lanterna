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

/**
 * Character pattern that matches characters pressed while ALT and CTRL keys are held down
 *
 * @author Martin, Andreas
 */
class CtrlAltAndCharacterPattern : CharacterPattern {

	override fun match(seq: List<Char>): CharacterPattern.Matching? {
		if (seq.size > 2 || seq[0] != KeyDecodingProfile.ESC_CODE) {
			return null // nope
		}
		if (seq.size == 1) {
			return CharacterPattern.Matching.NOT_YET // maybe later
		}
		if (seq[1].toInt() < 32 && seq[1].toInt() != 0x08) {
			// Control-chars: exclude Esc(^[), but still include ^\, ^], ^^ and ^_
			val ctrlCode: Char
			when (seq[1]) {
				KeyDecodingProfile.ESC_CODE -> return null // nope
				0  /* ^@ */ -> ctrlCode = ' '
				28 /* ^\ */ -> ctrlCode = '\\'
				29 /* ^] */ -> ctrlCode = ']'
				30 /* ^^ */ -> ctrlCode = '^'
				31 /* ^_ */ -> ctrlCode = '_'
				else -> ctrlCode = ('a' - 1 + seq[1].toInt()).toChar()
			}
			return CharacterPattern.Matching(KeyStroke(ctrlCode, true, true)) // yep
		} else if (seq[1].toInt() == 0x7f || seq[1].toInt() == 0x08) {
			return CharacterPattern.Matching(KeyStroke(KeyType.Backspace, false, true)) // yep
		} else {
			return null // nope
		}
	}
}
