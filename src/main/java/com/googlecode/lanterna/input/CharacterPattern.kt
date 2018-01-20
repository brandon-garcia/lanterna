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
 * Used to compare a list of character if they match a particular pattern, and in that case, return the kind of
 * keystroke this pattern represents
 *
 * @author Martin, Andreas
 */
interface CharacterPattern {

	/**
	 * Given a list of characters, determine whether it exactly matches
	 * any known KeyStroke, and whether a longer sequence can possibly match.
	 * @param seq of characters to check
	 * @return see `Matching`
	 */
	fun match(seq: List<Char>): Matching

	/**
	 * This immutable class describes a matching result. It wraps two items,
	 * partialMatch and fullMatch.
	 * <dl>
	 * <dt>fullMatch</dt><dd>
	 * The resulting KeyStroke if the pattern matched, otherwise null.<br></br>
	 * Example: if the tested sequence is `Esc [ A`, and if the
	 * pattern recognized this as `ArrowUp`, then this field has
	 * a value like `new KeyStroke(KeyType.ArrowUp)`</dd>
	 * <dt>partialMatch</dt><dd>
	 * `true`, if appending appropriate characters at the end of the
	 * sequence *can* produce a match.<br></br>
	 * Example: if the tested sequence is "Esc [", and the Pattern would match
	 * "Esc [ A", then this field would be set to `true`.</dd>
	</dl> *
	 * In principle, a sequence can match one KeyStroke, but also say that if
	 * another character is available, then a different KeyStroke might result.
	 * This can happen, if (e.g.) a single CharacterPattern-instance matches
	 * both the Escape key and a longer Escape-sequence.
	 */
	class Matching
	/**
	 * General constructor
	 *
	 *
	 * For mismatches rather use `null` and for "not yet" matches use NOT_YET.
	 * Use this constructor, where a sequence may yield both fullMatch and
	 * partialMatch or for merging result Matchings of multiple patterns.
	 *
	 * @param partialMatch  true if further characters could lead to a match
	 * @param fullMatch     The perfectly matching KeyStroke
	 */
	(val partialMatch: Boolean, val fullMatch: KeyStroke) {

		/**
		 * Convenience constructor for exact matches
		 *
		 * @param fullMatch  the KeyStroke that matched the sequence
		 */
		constructor(fullMatch: KeyStroke) : this(false, fullMatch) {}

		override fun toString(): String {
			return "Matching{partialMatch=$partialMatch, fullMatch=$fullMatch}"
		}

		companion object {

			/**
			 * Re-usable result for "not yet" half-matches
			 */
			val NOT_YET = Matching(true, null)
		}
	}
}
