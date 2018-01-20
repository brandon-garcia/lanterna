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
 * Character pattern that matches one character as one KeyStroke with the character that was read
 *
 * @author Martin, Andreas
 */
class NormalCharacterPattern : CharacterPattern {
	override fun match(seq: List<Char>): CharacterPattern.Matching? {
		if (seq.size != 1) {
			return null // nope
		}
		val ch = seq[0]
		if (isPrintableChar(ch)) {
			val ks = KeyStroke(ch, false, false)
			return CharacterPattern.Matching(ks)
		} else {
			return null // nope
		}
	}

	/**
	 * From http://stackoverflow.com/questions/220547/printable-char-in-java
	 * @param c character to test
	 * @return True if this is a 'normal', printable character, false otherwise
	 */
	private fun isPrintableChar(c: Char): Boolean {
		if (Character.isISOControl(c)) {
			return false
		}
		val block = Character.UnicodeBlock.of(c)
		return block != null && block !== Character.UnicodeBlock.SPECIALS
	}
}
