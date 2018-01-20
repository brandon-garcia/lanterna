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
 * Character pattern that matches characters pressed while CTRL key is held down
 *
 * @author Martin, Andreas
 */
class CtrlAndCharacterPattern : CharacterPattern {
	override fun match(seq: List<Char>): CharacterPattern.Matching? {
		val size = seq.size
		val ch = seq[0]
		if (size != 1) {
			return null // nope
		}
		if (ch.toInt() < 32) {
			// Control-chars: exclude lf,cr,Tab,Esc(^[), but still include ^\, ^], ^^ and ^_
			val ctrlCode: Char
			when (ch) {
				'\n', '\r', '\t', 0x08, KeyDecodingProfile.ESC_CODE -> return null // nope
				0  /* ^@ */ -> ctrlCode = ' '
				28 /* ^\ */ -> ctrlCode = '\\'
				29 /* ^] */ -> ctrlCode = ']'
				30 /* ^^ */ -> ctrlCode = '^'
				31 /* ^_ */ -> ctrlCode = '_'
				else -> ctrlCode = ('a' - 1 + ch.toInt()).toChar()
			}
			val ks = KeyStroke(ctrlCode, true, false)
			return CharacterPattern.Matching(ks) // yep
		} else {
			return null // nope
		}
	}
}
