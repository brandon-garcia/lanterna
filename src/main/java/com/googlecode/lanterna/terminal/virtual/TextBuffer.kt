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
package com.googlecode.lanterna.terminal.virtual

import com.googlecode.lanterna.TextCharacter

import java.util.ArrayList
import java.util.LinkedList

/**
 * This class is used to store lines of text inside of a terminal emulator. As used by [DefaultVirtualTerminal], it keeps
 * two [TextBuffer]s, one for private mode and one for normal mode and it can switch between them as needed.
 */
internal class TextBuffer {

	private val lines: LinkedList<List<TextCharacter>>

	val lineCount: Int
		@Synchronized get() = lines.size

	init {
		this.lines = LinkedList()
		newLine()
	}

	@Synchronized
	fun newLine() {
		lines.add(ArrayList(200))
	}

	@Synchronized
	fun removeTopLines(numberOfLinesToRemove: Int) {
		for (i in 0 until numberOfLinesToRemove) {
			lines.removeFirst()
		}
	}

	@Synchronized
	fun clear() {
		lines.clear()
		newLine()
	}

	fun getLinesFrom(rowNumber: Int): ListIterator<List<TextCharacter>> {
		return lines.listIterator(rowNumber)
	}

	@Synchronized
	fun setCharacter(lineNumber: Int, columnIndex: Int, textCharacter: TextCharacter?): Int {
		var textCharacter = textCharacter
		if (lineNumber < 0 || columnIndex < 0) {
			throw IllegalArgumentException("Illegal argument to TextBuffer.setCharacter(..), lineNumber = " +
				lineNumber + ", columnIndex = " + columnIndex)
		}
		if (textCharacter == null) {
			textCharacter = TextCharacter.DEFAULT_CHARACTER
		}
		while (lineNumber >= lines.size) {
			newLine()
		}
		val line = lines[lineNumber]
		while (line.size <= columnIndex) {
			line.add(TextCharacter.DEFAULT_CHARACTER)
		}

		// Default
		var returnStyle = 0

		// Check if we are overwriting a double-width character, in that case we need to reset the other half
		if (line[columnIndex].isDoubleWidth) {
			line.set(columnIndex + 1, line[columnIndex].withCharacter(' '))
			returnStyle = 1 // this character and the one to the right
		} else if (line[columnIndex] === DOUBLE_WIDTH_CHAR_PADDING) {
			line.set(columnIndex - 1, TextCharacter.DEFAULT_CHARACTER)
			returnStyle = 2 // this character and the one to the left
		}
		line.set(columnIndex, textCharacter)

		if (textCharacter.isDoubleWidth) {
			// We don't report this column as dirty (yet), it's implied since a double-width character is reported
			setCharacter(lineNumber, columnIndex + 1, DOUBLE_WIDTH_CHAR_PADDING)
		}
		return returnStyle
	}

	@Synchronized
	fun getCharacter(lineNumber: Int, columnIndex: Int): TextCharacter {
		if (lineNumber < 0 || columnIndex < 0) {
			throw IllegalArgumentException("Illegal argument to TextBuffer.getCharacter(..), lineNumber = " +
				lineNumber + ", columnIndex = " + columnIndex)
		}
		if (lineNumber >= lines.size) {
			return TextCharacter.DEFAULT_CHARACTER
		}
		val line = lines[lineNumber]
		if (line.size <= columnIndex) {
			return TextCharacter.DEFAULT_CHARACTER
		}
		val textCharacter = line[columnIndex]
		return if (textCharacter === DOUBLE_WIDTH_CHAR_PADDING) {
			line[columnIndex - 1]
		} else textCharacter
	}

	companion object {
		private val DOUBLE_WIDTH_CHAR_PADDING = TextCharacter(' ')
	}
}
