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
package com.googlecode.lanterna

import java.util.Arrays
import java.util.EnumSet

/**
 * Represents a single character with additional metadata such as colors and modifiers. This class is immutable and
 * cannot be modified after creation.
 * @author Martin
 */
class TextCharacter
/**
 * Creates a new `ScreenCharacter` based on a physical character, color information and a set of modifiers.
 * @param character Physical character to refer to
 * @param foregroundColor Foreground color the character has
 * @param backgroundColor Background color the character has
 * @param modifiers Set of modifiers to apply when drawing the character
 */
(
	/**
	 * The actual character this TextCharacter represents
	 * @return character of the TextCharacter
	 */
	val character: Char,
	foregroundColor: TextColor?,
	backgroundColor: TextColor?,
	modifiers: EnumSet<SGR>) {
	/**
	 * Foreground color specified for this TextCharacter
	 * @return Foreground color of this TextCharacter
	 */
	val foregroundColor: TextColor?
	/**
	 * Background color specified for this TextCharacter
	 * @return Background color of this TextCharacter
	 */
	val backgroundColor: TextColor?
	private val modifiers: EnumSet<SGR>?  //This isn't immutable, but we should treat it as such and not expose it!

	/**
	 * Returns true if this TextCharacter has the bold modifier active
	 * @return `true` if this TextCharacter has the bold modifier active
	 */
	val isBold: Boolean
		get() = modifiers!!.contains(SGR.BOLD)

	/**
	 * Returns true if this TextCharacter has the reverse modifier active
	 * @return `true` if this TextCharacter has the reverse modifier active
	 */
	val isReversed: Boolean
		get() = modifiers!!.contains(SGR.REVERSE)

	/**
	 * Returns true if this TextCharacter has the underline modifier active
	 * @return `true` if this TextCharacter has the underline modifier active
	 */
	val isUnderlined: Boolean
		get() = modifiers!!.contains(SGR.UNDERLINE)

	/**
	 * Returns true if this TextCharacter has the blink modifier active
	 * @return `true` if this TextCharacter has the blink modifier active
	 */
	val isBlinking: Boolean
		get() = modifiers!!.contains(SGR.BLINK)

	/**
	 * Returns true if this TextCharacter has the bordered modifier active
	 * @return `true` if this TextCharacter has the bordered modifier active
	 */
	val isBordered: Boolean
		get() = modifiers!!.contains(SGR.BORDERED)

	/**
	 * Returns true if this TextCharacter has the crossed-out modifier active
	 * @return `true` if this TextCharacter has the crossed-out modifier active
	 */
	val isCrossedOut: Boolean
		get() = modifiers!!.contains(SGR.CROSSED_OUT)

	/**
	 * Returns true if this TextCharacter has the italic modifier active
	 * @return `true` if this TextCharacter has the italic modifier active
	 */
	val isItalic: Boolean
		get() = modifiers!!.contains(SGR.ITALIC)

	val isDoubleWidth: Boolean
		get() = TerminalTextUtils.isCharDoubleWidth(character)

	/**
	 * Creates a `ScreenCharacter` based on a supplied character, with default colors and no extra modifiers.
	 * @param character Physical character to use
	 */
	constructor(character: Char) : this(character, TextColor.ANSI.DEFAULT, TextColor.ANSI.DEFAULT) {}

	/**
	 * Copies another `ScreenCharacter`
	 * @param character screenCharacter to copy from
	 */
	constructor(character: TextCharacter) : this(character.character,
		character.foregroundColor,
		character.backgroundColor,
		*character.getModifiers().toTypedArray<SGR>()) {
	}

	/**
	 * Creates a new `ScreenCharacter` based on a physical character, color information and optional modifiers.
	 * @param character Physical character to refer to
	 * @param foregroundColor Foreground color the character has
	 * @param backgroundColor Background color the character has
	 * @param styles Optional list of modifiers to apply when drawing the character
	 */
	constructor(
		character: Char,
		foregroundColor: TextColor?,
		backgroundColor: TextColor?,
		vararg styles: SGR) : this(character,
		foregroundColor,
		backgroundColor,
		toEnumSet(*styles)) {
	}

	init {
		var foregroundColor = foregroundColor
		var backgroundColor = backgroundColor

		// Don't allow creating a TextCharacter containing a control character
		// For backward-compatibility, do allow tab for now
		// TODO: In lanterna 3.1, don't allow tab
		if (TerminalTextUtils.isControlCharacter(character) && character != '\t') {
			throw IllegalArgumentException("Cannot create a TextCharacter from a control character (0x" + Integer.toHexString(character.toInt()) + ")")
		}

		if (foregroundColor == null) {
			foregroundColor = TextColor.ANSI.DEFAULT
		}
		if (backgroundColor == null) {
			backgroundColor = TextColor.ANSI.DEFAULT
		}
		this.foregroundColor = foregroundColor
		this.backgroundColor = backgroundColor
		this.modifiers = EnumSet.copyOf(modifiers)
	}

	/**
	 * Returns a set of all active modifiers on this TextCharacter
	 * @return Set of active SGR codes
	 */
	fun getModifiers() =
		EnumSet.copyOf(modifiers!!)

	/**
	 * Returns a new TextCharacter with the same colors and modifiers but a different underlying character
	 * @param character Character the copy should have
	 * @return Copy of this TextCharacter with different underlying character
	 */
	fun withCharacter(character: Char) =
		if (this.character == character) {
			this
		} else TextCharacter(character, foregroundColor, backgroundColor, modifiers)

	/**
	 * Returns a copy of this TextCharacter with a specified foreground color
	 * @param foregroundColor Foreground color the copy should have
	 * @return Copy of the TextCharacter with a different foreground color
	 */
	fun withForegroundColor(foregroundColor: TextColor) =
		if (this.foregroundColor === foregroundColor || this.foregroundColor == foregroundColor) {
			this
		} else TextCharacter(character, foregroundColor, backgroundColor, modifiers)

	/**
	 * Returns a copy of this TextCharacter with a specified background color
	 * @param backgroundColor Background color the copy should have
	 * @return Copy of the TextCharacter with a different background color
	 */
	fun withBackgroundColor(backgroundColor: TextColor) =
		if (this.backgroundColor === backgroundColor || this.backgroundColor == backgroundColor) {
			this
		} else TextCharacter(character, foregroundColor, backgroundColor, modifiers)

	/**
	 * Returns a copy of this TextCharacter with specified list of SGR modifiers. None of the currently active SGR codes
	 * will be carried over to the copy, only those in the passed in value.
	 * @param modifiers SGR modifiers the copy should have
	 * @return Copy of the TextCharacter with a different set of SGR modifiers
	 */
	fun withModifiers(modifiers: Collection<SGR>) =
		EnumSet.copyOf(modifiers).let {
			if (modifiers == it) {
				this
			} else TextCharacter(character, foregroundColor, backgroundColor, it)
		}

	/**
	 * Returns a copy of this TextCharacter with an additional SGR modifier. All of the currently active SGR codes
	 * will be carried over to the copy, in addition to the one specified.
	 * @param modifier SGR modifiers the copy should have in additional to all currently present
	 * @return Copy of the TextCharacter with a new SGR modifier
	 */
	fun withModifier(modifier: SGR): TextCharacter {
		if (modifiers!!.contains(modifier)) {
			return this
		}
		val newSet = EnumSet.copyOf(this.modifiers)
		newSet.add(modifier)
		return TextCharacter(character, foregroundColor, backgroundColor, newSet)
	}

	/**
	 * Returns a copy of this TextCharacter with an SGR modifier removed. All of the currently active SGR codes
	 * will be carried over to the copy, except for the one specified. If the current TextCharacter doesn't have the
	 * SGR specified, it will return itself.
	 * @param modifier SGR modifiers the copy should not have
	 * @return Copy of the TextCharacter without the SGR modifier
	 */
	fun withoutModifier(modifier: SGR): TextCharacter {
		if (!modifiers!!.contains(modifier)) {
			return this
		}
		val newSet = EnumSet.copyOf(this.modifiers)
		newSet.remove(modifier)
		return TextCharacter(character, foregroundColor, backgroundColor, newSet)
	}

	override fun equals(obj: Any?): Boolean {
		if (obj == null) {
			return false
		}
		if (javaClass != obj.javaClass) {
			return false
		}
		val other = obj as TextCharacter?
		if (this.character != other!!.character) {
			return false
		}
		if (this.foregroundColor !== other.foregroundColor && (this.foregroundColor == null || this.foregroundColor != other.foregroundColor)) {
			return false
		}
		return if (this.backgroundColor !== other.backgroundColor && (this.backgroundColor == null || this.backgroundColor != other.backgroundColor)) {
			false
		} else !(this.modifiers !== other.modifiers && (this.modifiers == null || this.modifiers != other.modifiers))
	}

	override fun hashCode(): Int {
		var hash = 7
		hash = 37 * hash + this.character.toInt()
		hash = 37 * hash + if (this.foregroundColor != null) this.foregroundColor.hashCode() else 0
		hash = 37 * hash + if (this.backgroundColor != null) this.backgroundColor.hashCode() else 0
		hash = 37 * hash + if (this.modifiers != null) this.modifiers.hashCode() else 0
		return hash
	}

	override fun toString() =
		"TextCharacter{character=$character, foregroundColor=$foregroundColor, backgroundColor=$backgroundColor, modifiers=$modifiers}"

	companion object {
		private fun toEnumSet(vararg modifiers: SGR) =
			if (modifiers.size == 0) {
				EnumSet.noneOf(SGR::class.java)
			} else {
				EnumSet.copyOf(Arrays.asList(*modifiers))
			}

		val DEFAULT_CHARACTER = TextCharacter(' ', TextColor.ANSI.DEFAULT, TextColor.ANSI.DEFAULT)
	}
}
