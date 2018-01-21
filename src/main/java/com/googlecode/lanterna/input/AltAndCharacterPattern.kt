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
 * Character pattern that matches characters pressed while ALT key is held down
 *
 * @author Martin, Andreas
 */
class AltAndCharacterPattern : CharacterPattern {

	override fun match(seq: List<Char>) =
		if (seq.size > 2 || seq[0] != KeyDecodingProfile.ESC_CODE) {
			null // nope
		}
		else if (seq.size == 1) {
			CharacterPattern.Matching.NOT_YET // maybe later
		}
		else if (Character.isISOControl(seq[1])) {
			null // nope
		} else {
			CharacterPattern.Matching(KeyStroke(seq[1], false, true)) // yep
		}
}
