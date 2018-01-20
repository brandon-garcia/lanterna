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

import com.googlecode.lanterna.input.KeyDecodingProfile.ESC_CODE

import java.util.HashMap

/**
 * This implementation of CharacterPattern matches two similar patterns
 * of Escape sequences, that many terminals produce for special keys.
 *
 *
 *
 * These sequences all start with Escape, followed by either an open bracket
 * or a capital letter O (these two are treated as equivalent).
 *
 *
 *
 * Then follows a list of zero or up to two decimals separated by a
 * semicolon, and a non-digit last character.
 *
 *
 *
 * If the last character is a tilde (~) then the first number defines
 * the key (through stdMap), otherwise the last character itself defines
 * the key (through finMap).
 *
 *
 *
 * The second number, if provided by the terminal, specifies the modifier
 * state (shift,alt,ctrl). The value is 1 + sum(modifiers), where shift is 1,
 * alt is 2 and ctrl is 4.
 *
 *
 *
 * The two maps stdMap and finMap can be customized in subclasses to add,
 * remove or replace keys - to support non-standard Terminals.
 *
 *
 *
 * Examples: (on a gnome terminal)<br></br>
 * ArrowUp is "Esc [ A"; Alt-ArrowUp is "Esc [ 1 ; 3 A"<br></br>
 * both are handled by finMap mapping 'A' to ArrowUp <br></br><br></br>
 * F6 is "Esc [ 1 7 ~"; Ctrl-Shift-F6 is "Esc [ 1 7 ; 6 R"<br></br>
 * both are handled by stdMap mapping 17 to F6 <br></br><br></br>
 *
 * @author Andreas
 */
open class EscapeSequenceCharacterPattern : CharacterPattern {

	/**
	 * Map of recognized "standard pattern" sequences:<br></br>
	 * e.g.: 24 -&gt; F12 : "Esc [ **24** ~"
	 */
	protected val stdMap: MutableMap<Int, KeyType> = HashMap()
	/**
	 * Map of recognized "finish pattern" sequences:<br></br>
	 * e.g.: 'A' -&gt; ArrowUp : "Esc [ **A**"
	 */
	protected val finMap: MutableMap<Char, KeyType> = HashMap()
	/**
	 * A flag to control, whether an Esc-prefix for an Esc-sequence is to be treated
	 * as Alt-pressed. Some Terminals (e.g. putty) report the Alt-modifier like that.
	 *
	 *
	 * If the application is e.g. more interested in seeing separate Escape and plain
	 * Arrow keys, then it should replace this class by a subclass that sets this flag
	 * to false. (It might then also want to remove the CtrlAltAndCharacterPattern.)
	 */
	protected var useEscEsc = true

	// state machine used to match key sequence:
	private enum class State {
		START, INTRO, NUM1, NUM2, DONE
	}

	/**
	 * Create an instance with a standard set of mappings.
	 */
	init {
		finMap.put('A', KeyType.ArrowUp)
		finMap.put('B', KeyType.ArrowDown)
		finMap.put('C', KeyType.ArrowRight)
		finMap.put('D', KeyType.ArrowLeft)
		finMap.put('E', KeyType.Unknown) // gnome-terminal center key on numpad
		finMap.put('G', KeyType.Unknown) // putty center key on numpad
		finMap.put('H', KeyType.Home)
		finMap.put('F', KeyType.End)
		finMap.put('P', KeyType.F1)
		finMap.put('Q', KeyType.F2)
		finMap.put('R', KeyType.F3)
		finMap.put('S', KeyType.F4)
		finMap.put('Z', KeyType.ReverseTab)

		stdMap.put(1, KeyType.Home)
		stdMap.put(2, KeyType.Insert)
		stdMap.put(3, KeyType.Delete)
		stdMap.put(4, KeyType.End)
		stdMap.put(5, KeyType.PageUp)
		stdMap.put(6, KeyType.PageDown)
		stdMap.put(11, KeyType.F1)
		stdMap.put(12, KeyType.F2)
		stdMap.put(13, KeyType.F3)
		stdMap.put(14, KeyType.F4)
		stdMap.put(15, KeyType.F5)
		stdMap.put(16, KeyType.F5)
		stdMap.put(17, KeyType.F6)
		stdMap.put(18, KeyType.F7)
		stdMap.put(19, KeyType.F8)
		stdMap.put(20, KeyType.F9)
		stdMap.put(21, KeyType.F10)
		stdMap.put(23, KeyType.F11)
		stdMap.put(24, KeyType.F12)
		stdMap.put(25, KeyType.F13)
		stdMap.put(26, KeyType.F14)
		stdMap.put(28, KeyType.F15)
		stdMap.put(29, KeyType.F16)
		stdMap.put(31, KeyType.F17)
		stdMap.put(32, KeyType.F18)
		stdMap.put(33, KeyType.F19)
	}

	/**
	 * combines a KeyType and modifiers into a KeyStroke.
	 * Subclasses can override this for customization purposes.
	 *
	 * @param key the KeyType as determined by parsing the sequence.
	 * It will be null, if the pattern looked like a key sequence but wasn't
	 * identified.
	 * @param mods the bitmask of the modifer keys pressed along with the key.
	 * @return either null (to report mis-match), or a valid KeyStroke.
	 */
	protected fun getKeyStroke(key: KeyType?, mods: Int): KeyStroke? {
		var bShift = false
		var bCtrl = false
		var bAlt = false
		if (key == null) {
			return null
		} // alternative: key = KeyType.Unknown;
		if (mods >= 0) { // only use when non-negative!
			bShift = mods and SHIFT != 0
			bAlt = mods and ALT != 0
			bCtrl = mods and CTRL != 0
		}
		return KeyStroke(key, bCtrl, bAlt, bShift)
	}

	/**
	 * combines the raw parts of the sequence into a KeyStroke.
	 * This method does not check the first char, but overrides may do so.
	 *
	 * @param first  the char following after Esc in the sequence (either [ or O)
	 * @param num1   the first decimal, or 0 if not in the sequence
	 * @param num2   the second decimal, or 0 if not in the sequence
	 * @param last   the terminating char.
	 * @param bEsc   whether an extra Escape-prefix was found.
	 * @return either null (to report mis-match), or a valid KeyStroke.
	 */
	protected open fun getKeyStrokeRaw(first: Char, num1: Int, num2: Int, last: Char, bEsc: Boolean): KeyStroke? {
		val kt: KeyType?
		var bPuttyCtrl = false
		if (last == '~' && stdMap.containsKey(num1)) {
			kt = stdMap[num1]
		} else if (finMap.containsKey(last)) {
			kt = finMap[last]
			// Putty sends ^[OA for ctrl arrow-up, ^[[A for plain arrow-up:
			// but only for A-D -- other ^[O... sequences are just plain keys
			if (first == 'O' && last >= 'A' && last <= 'D') {
				bPuttyCtrl = true
			}
			// if we ever stumble into "keypad-mode", then it will end up inverted.
		} else {
			kt = null // unknown key.
		}
		var mods = num2 - 1
		if (bEsc) {
			if (mods >= 0) {
				mods = mods or ALT
			} else {
				mods = ALT
			}
		}
		if (bPuttyCtrl) {
			if (mods >= 0) {
				mods = mods or CTRL
			} else {
				mods = CTRL
			}
		}
		return getKeyStroke(kt, mods)
	}

	override fun match(cur: List<Char>): CharacterPattern.Matching? {
		var state = State.START
		var num1 = 0
		var num2 = 0
		var first = '\u0000'
		var last = '\u0000'
		var bEsc = false

		for (ch in cur) {
			when (state) {
				EscapeSequenceCharacterPattern.State.START -> {
					if (ch != ESC_CODE) {
						return null // nope
					}
					state = State.INTRO
					continue
				}
				EscapeSequenceCharacterPattern.State.INTRO -> {
					// Recognize a second Escape to mean "Alt is pressed".
					// (at least putty sends it that way)
					if (useEscEsc && ch == ESC_CODE && !bEsc) {
						bEsc = true
						continue
					}

					// Key sequences supported by this class must
					//    start either with Esc-[ or Esc-O
					if (ch != '[' && ch != 'O') {
						return null // nope
					}
					first = ch
					state = State.NUM1
					continue
				}
				EscapeSequenceCharacterPattern.State.NUM1 -> {
					if (ch == ';') {
						state = State.NUM2
					} else if (Character.isDigit(ch)) {
						num1 = num1 * 10 + Character.digit(ch, 10)
					} else {
						last = ch
						state = State.DONE
					}
					continue
				}
				EscapeSequenceCharacterPattern.State.NUM2 -> {
					if (Character.isDigit(ch)) {
						num2 = num2 * 10 + Character.digit(ch, 10)
					} else {
						last = ch
						state = State.DONE
					}
					continue
				}
				EscapeSequenceCharacterPattern.State.DONE // once done, extra characters spoil it
				-> return null // nope
			}
		}
		if (state == State.DONE) {
			val ks = getKeyStrokeRaw(first, num1, num2, last, bEsc)
			return if (ks != null) CharacterPattern.Matching(ks) else null // depends
		} else {
			return CharacterPattern.Matching.NOT_YET // maybe later
		}
	}

	companion object {
		// bit-values for modifier keys: only used internally
		val SHIFT = 1
		val ALT = 2
		val CTRL = 4
	}
}
