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

import sun.management.snmp.jvminstr.JvmThreadInstanceEntryImpl.ThreadStateMap.Byte1.other
import java.util.Arrays

/**
 * Very simple pattern that matches the input stream against a pre-defined list of characters. For the pattern to match,
 * the list of characters must match exactly what's coming in on the input stream.
 *
 * @author Martin, Andreas
 */
class BasicCharacterPattern
/**
 * Creates a new BasicCharacterPattern that matches a particular sequence of characters into a `KeyStroke`
 * @param result `KeyStroke` that this pattern will translate to
 * @param pattern Sequence of characters that translates into the `KeyStroke`
 */
(
	/**
	 * Returns the keystroke that this pattern results in
	 * @return The keystoke this pattern will return if it matches
	 */
	val result: KeyStroke, vararg pattern: Char) : CharacterPattern {
	private val pattern: CharArray

	init {
		this.pattern = pattern
	}

	/**
	 * Returns the characters that makes up this pattern, as an array that is a copy of the array used internally
	 * @return Array of characters that defines this pattern
	 */
	fun getPattern() =
		Arrays.copyOf(pattern, pattern.size)

	override fun match(seq: List<Char>): CharacterPattern.Matching? {
		val size = seq.size

		if (size > pattern.size) {
			return null // nope
		}
		for (i in 0 until size) {
			if (pattern[i] != seq[i]) {
				return null // nope
			}
		}
		return if (size == pattern.size) {
			CharacterPattern.Matching(result) // yep
		} else {
			CharacterPattern.Matching.NOT_YET // maybe later
		}
	}

	override fun equals(obj: Any?) =
		if (obj == null || obj !is BasicCharacterPattern) {
			false
		} else {
			Arrays.equals(this.pattern, obj.pattern)
		}

	override fun hashCode() =
		53 * 3 + Arrays.hashCode(this.pattern)
}
