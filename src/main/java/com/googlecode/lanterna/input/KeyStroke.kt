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

import java.util.ArrayList
import java.util.Arrays

/**
 * Represents the user pressing a key on the keyboard. If the user held down ctrl and/or alt before pressing the key,
 * this may be recorded in this class, depending on the terminal implementation and if such information in available.
 * KeyStroke objects are normally constructed by a KeyDecodingProfile, which works off a character stream that likely
 * coming from the system's standard input. Because of this, the class can only represent what can be read and
 * interpreted from the input stream; for example, certain key-combinations like ctrl+i is indistinguishable from a tab
 * key press.
 *
 *
 * Use the <tt>keyType</tt> field to determine what kind of key was pressed. For ordinary letters, numbers and symbols, the
 * <tt>keyType</tt> will be <tt>KeyType.Character</tt> and the actual character value of the key is in the
 * <tt>character</tt> field. Please note that return (\n) and tab (\t) are not sorted under type <tt>KeyType.Character</tt>
 * but <tt>KeyType.Enter</tt> and <tt>KeyType.Tab</tt> instead.
 * @author martin
 */
open class KeyStroke private constructor(
	/**
	 * Type of key that was pressed on the keyboard, as represented by the KeyType enum. If the value if
	 * KeyType.Character, you need to call getCharacter() to find out which letter, number or symbol that was actually
	 * pressed.
	 * @return Type of key on the keyboard that was pressed
	 */
	val keyType: KeyType?, character: Char?,
	/**
	 * @return Returns true if ctrl was help down while the key was typed (depending on terminal implementation)
	 */
	val isCtrlDown: Boolean,
	/**
	 * @return Returns true if alt was help down while the key was typed (depending on terminal implementation)
	 */
	val isAltDown: Boolean,
	/**
	 * @return Returns true if shift was help down while the key was typed (depending on terminal implementation)
	 */
	val isShiftDown: Boolean) {
	/**
	 * For keystrokes of ordinary keys (letters, digits, symbols), this method returns the actual character value of the
	 * key. For all other key types, it returns null.
	 * @return Character value of the key pressed, or null if it was a special key
	 */
	val character: Char?
	/**
	 * Gets the time when the keystroke was recorded. This isn't necessarily the time the keystroke happened, but when
	 * Lanterna received the event, so it may not be accurate down to the millisecond.
	 * @return The unix time of when the keystroke happened, in milliseconds
	 */
	val eventTime: Long

	/**
	 * Constructs a KeyStroke based on a supplied keyType; character will be null.
	 * If you try to construct a KeyStroke with type KeyType.Character with this constructor, it
	 * will always throw an exception; use another overload that allows you to specify the character value instead.
	 * @param keyType Type of the key pressed by this keystroke
	 * @param ctrlDown Was ctrl held down when the main key was pressed?
	 * @param altDown Was alt held down when the main key was pressed?
	 */
	@JvmOverloads constructor(keyType: KeyType, ctrlDown: Boolean = false, altDown: Boolean = false) : this(keyType, null, ctrlDown, altDown, false) {}

	/**
	 * Constructs a KeyStroke based on a supplied keyType; character will be null.
	 * If you try to construct a KeyStroke with type KeyType.Character with this constructor, it
	 * will always throw an exception; use another overload that allows you to specify the character value instead.
	 * @param keyType Type of the key pressed by this keystroke
	 * @param ctrlDown Was ctrl held down when the main key was pressed?
	 * @param altDown Was alt held down when the main key was pressed?
	 * @param shiftDown Was shift held down when the main key was pressed?
	 */
	constructor(keyType: KeyType, ctrlDown: Boolean, altDown: Boolean, shiftDown: Boolean) : this(keyType, null, ctrlDown, altDown, shiftDown) {}

	/**
	 * Constructs a KeyStroke based on a supplied character, keyType is implicitly KeyType.Character.
	 *
	 *
	 * A character-based KeyStroke does not support the shiftDown flag, as the shift state has
	 * already been accounted for in the character itself, depending on user's keyboard layout.
	 * @param character Character that was typed on the keyboard
	 * @param ctrlDown Was ctrl held down when the main key was pressed?
	 * @param altDown Was alt held down when the main key was pressed?
	 */
	constructor(character: Char?, ctrlDown: Boolean, altDown: Boolean) : this(KeyType.Character, character, ctrlDown, altDown, false) {}

	/**
	 * Constructs a KeyStroke based on a supplied character, keyType is implicitly KeyType.Character.
	 *
	 *
	 * A character-based KeyStroke does not support the shiftDown flag, as the shift state has
	 * already been accounted for in the character itself, depending on user's keyboard layout.
	 * @param character Character that was typed on the keyboard
	 * @param ctrlDown Was ctrl held down when the main key was pressed?
	 * @param altDown Was alt held down when the main key was pressed?
	 * @param shiftDown Was shift held down when the main key was pressed?
	 */
	constructor(character: Char?, ctrlDown: Boolean, altDown: Boolean, shiftDown: Boolean) : this(KeyType.Character, character, ctrlDown, altDown, shiftDown) {}

	init {
		var character = character
		if (keyType == KeyType.Character && character == null) {
			throw IllegalArgumentException("Cannot construct a KeyStroke with type KeyType.Character but no character information")
		}
		//Enforce character for some key types
		when (keyType) {
			KeyType.Backspace -> character = '\b'
			KeyType.Enter -> character = '\n'
			KeyType.Tab -> character = '\t'
		}
		this.character = character
		this.eventTime = System.currentTimeMillis()
	}

	override fun toString(): String {
		val sb = StringBuilder()
		sb.append("KeyStroke{keytype=").append(keyType)
		if (character != null) {
			val ch = character
			sb.append(", character='")
			when (ch) {
			// many of these cases can only happen through user code:
				0x00 -> sb.append("^@")
				0x08 -> sb.append("\\b")
				0x09 -> sb.append("\\t")
				0x0a -> sb.append("\\n")
				0x0d -> sb.append("\\r")
				0x1b -> sb.append("^[")
				0x1c -> sb.append("^\\")
				0x1d -> sb.append("^]")
				0x1e -> sb.append("^^")
				0x1f -> sb.append("^_")
				else -> if (ch.toInt() <= 26) {
					sb.append('^').append((ch.toInt() + 64).toChar())
				} else {
					sb.append(ch)
				}
			}
			sb.append('\'')
		}
		if (isCtrlDown || isAltDown || isShiftDown) {
			var sep = ""
			sb.append(", modifiers=[")
			if (isCtrlDown) {
				sb.append(sep).append("ctrl")
				sep = ","
			}
			if (isAltDown) {
				sb.append(sep).append("alt")
				sep = ","
			}
			if (isShiftDown) {
				sb.append(sep).append("shift")
				sep = ","
			}
			sb.append("]")
		}
		return sb.append('}').toString()
	}

	override fun hashCode(): Int {
		var hash = 3
		hash = 41 * hash + if (this.keyType != null) this.keyType.hashCode() else 0
		hash = 41 * hash + if (this.character != null) this.character.hashCode() else 0
		hash = 41 * hash + if (this.isCtrlDown) 1 else 0
		hash = 41 * hash + if (this.isAltDown) 1 else 0
		hash = 41 * hash + if (this.isShiftDown) 1 else 0
		return hash
	}

	override fun equals(obj: Any?): Boolean {
		if (obj == null) {
			return false
		}
		if (javaClass != obj.javaClass) {
			return false
		}
		val other = obj as KeyStroke?
		if (this.keyType != other!!.keyType) {
			return false
		}
		return if (this.character !== other.character && (this.character == null || this.character != other.character)) {
			false
		} else this.isCtrlDown == other.isCtrlDown &&
			this.isAltDown == other.isAltDown &&
			this.isShiftDown == other.isShiftDown
	}

	companion object {

		/**
		 * Creates a Key from a string representation in Vim's key notation.
		 *
		 * @param keyStr the string representation of this key
		 * @return the created [KeyType]
		 */
		fun fromString(keyStr: String): KeyStroke {
			val keyStrLC = keyStr.toLowerCase()
			val k: KeyStroke
			if (keyStr.length == 1) {
				k = KeyStroke(KeyType.Character, keyStr[0], false, false, false)
			} else if (keyStr.startsWith("<") && keyStr.endsWith(">")) {
				if (keyStrLC == "<s-tab>") {
					k = KeyStroke(KeyType.ReverseTab)
				} else if (keyStr.contains("-")) {
					val segments = ArrayList(Arrays.asList<String>(*keyStr.substring(1, keyStr.length - 1).split("-".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()))
					if (segments.size < 2) {
						throw IllegalArgumentException("Invalid vim notation: " + keyStr)
					}
					var characterStr = segments.removeAt(segments.size - 1)
					var altPressed = false
					var ctrlPressed = false
					for (modifier in segments) {
						if ("c" == modifier.toLowerCase()) {
							ctrlPressed = true
						} else if ("a" == modifier.toLowerCase()) {
							altPressed = true
						} else if ("s" == modifier.toLowerCase()) {
							characterStr = characterStr.toUpperCase()
						}
					}
					k = KeyStroke(characterStr[0], ctrlPressed, altPressed)
				} else {
					if (keyStrLC.startsWith("<esc")) {
						k = KeyStroke(KeyType.Escape)
					} else if (keyStrLC == "<cr>" || keyStrLC == "<enter>" || keyStrLC == "<return>") {
						k = KeyStroke(KeyType.Enter)
					} else if (keyStrLC == "<bs>") {
						k = KeyStroke(KeyType.Backspace)
					} else if (keyStrLC == "<tab>") {
						k = KeyStroke(KeyType.Tab)
					} else if (keyStrLC == "<space>") {
						k = KeyStroke(' ', false, false)
					} else if (keyStrLC == "<up>") {
						k = KeyStroke(KeyType.ArrowUp)
					} else if (keyStrLC == "<down>") {
						k = KeyStroke(KeyType.ArrowDown)
					} else if (keyStrLC == "<left>") {
						k = KeyStroke(KeyType.ArrowLeft)
					} else if (keyStrLC == "<right>") {
						k = KeyStroke(KeyType.ArrowRight)
					} else if (keyStrLC == "<insert>") {
						k = KeyStroke(KeyType.Insert)
					} else if (keyStrLC == "<del>") {
						k = KeyStroke(KeyType.Delete)
					} else if (keyStrLC == "<home>") {
						k = KeyStroke(KeyType.Home)
					} else if (keyStrLC == "<end>") {
						k = KeyStroke(KeyType.End)
					} else if (keyStrLC == "<pageup>") {
						k = KeyStroke(KeyType.PageUp)
					} else if (keyStrLC == "<pagedown>") {
						k = KeyStroke(KeyType.PageDown)
					} else if (keyStrLC == "<f1>") {
						k = KeyStroke(KeyType.F1)
					} else if (keyStrLC == "<f2>") {
						k = KeyStroke(KeyType.F2)
					} else if (keyStrLC == "<f3>") {
						k = KeyStroke(KeyType.F3)
					} else if (keyStrLC == "<f4>") {
						k = KeyStroke(KeyType.F4)
					} else if (keyStrLC == "<f5>") {
						k = KeyStroke(KeyType.F5)
					} else if (keyStrLC == "<f6>") {
						k = KeyStroke(KeyType.F6)
					} else if (keyStrLC == "<f7>") {
						k = KeyStroke(KeyType.F7)
					} else if (keyStrLC == "<f8>") {
						k = KeyStroke(KeyType.F8)
					} else if (keyStrLC == "<f9>") {
						k = KeyStroke(KeyType.F9)
					} else if (keyStrLC == "<f10>") {
						k = KeyStroke(KeyType.F10)
					} else if (keyStrLC == "<f11>") {
						k = KeyStroke(KeyType.F11)
					} else if (keyStrLC == "<f12>") {
						k = KeyStroke(KeyType.F12)
					} else {
						throw IllegalArgumentException("Invalid vim notation: " + keyStr)
					}
				}
			} else {
				throw IllegalArgumentException("Invalid vim notation: " + keyStr)
			}
			return k
		}
	}
}
/**
 * Constructs a KeyStroke based on a supplied keyType; character will be null and both ctrl and alt will be
 * considered not pressed. If you try to construct a KeyStroke with type KeyType.Character with this constructor, it
 * will always throw an exception; use another overload that allows you to specify the character value instead.
 * @param keyType Type of the key pressed by this keystroke
 */
