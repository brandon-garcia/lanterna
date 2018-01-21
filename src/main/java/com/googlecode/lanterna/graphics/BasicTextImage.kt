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
package com.googlecode.lanterna.graphics

import java.util.Arrays

import com.googlecode.lanterna.TerminalPosition
import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.TextCharacter
import com.googlecode.lanterna.TextColor

/**
 * Simple implementation of TextImage that keeps the content as a two-dimensional TextCharacter array. Copy operations
 * between two BasicTextImage classes are semi-optimized by using System.arraycopy instead of iterating over each
 * character and copying them over one by one.
 * @author martin
 */
class BasicTextImage
/**
 * Creates a new BasicTextImage by copying a region of a two-dimensional array of TextCharacter:s. If the area to be
 * copied to larger than the source array, a filler character is used.
 * @param size Size to create the new BasicTextImage as (and size to copy from the array)
 * @param toCopy Array to copy initial data from
 * @param initialContent Filler character to use if the source array is smaller than the requested size
 */
private constructor(override val size: TerminalSize?, toCopy: Array<Array<TextCharacter>>?, initialContent: TextCharacter?) : TextImage {
	private val buffer: Array<Array<TextCharacter>>

	/**
	 * Creates a new BasicTextImage with the specified size and fills it initially with space characters using the
	 * default foreground and background color
	 * @param columns Size of the image in number of columns
	 * @param rows Size of the image in number of rows
	 */
	constructor(columns: Int, rows: Int) : this(TerminalSize(columns, rows)) {}

	/**
	 * Creates a new BasicTextImage with a given size and a TextCharacter to initially fill it with
	 * @param size Size of the image
	 * @param initialContent What character to set as the initial content
	 */
	@JvmOverloads constructor(size: TerminalSize, initialContent: TextCharacter = TextCharacter(' ', TextColor.ANSI.DEFAULT, TextColor.ANSI.DEFAULT)) : this(size, arrayOfNulls<Array<TextCharacter>>(0), initialContent) {}

	init {
		if (size == null || toCopy == null || initialContent == null) {
			throw IllegalArgumentException("Cannot create BasicTextImage with null " + if (size == null) "size" else if (toCopy == null) "toCopy" else "filler")
		}

		val rows = size.rows
		val columns = size.columns
		buffer = arrayOfNulls(rows)
		for (y in 0 until rows) {
			buffer[y] = arrayOfNulls(columns)
			for (x in 0 until columns) {
				if (y < toCopy.size && x < toCopy[y].size) {
					buffer[y][x] = toCopy[y][x]
				} else {
					buffer[y][x] = initialContent
				}
			}
		}
	}

	override fun setAll(character: TextCharacter?) {
		if (character == null) {
			throw IllegalArgumentException("Cannot call BasicTextImage.setAll(..) with null character")
		}
		for (line in buffer) {
			Arrays.fill(line, character)
		}
	}

	override fun resize(newSize: TerminalSize?, filler: TextCharacter?): BasicTextImage {
		if (newSize == null || filler == null) {
			throw IllegalArgumentException("Cannot resize BasicTextImage with null " + if (newSize == null) "newSize" else "filler")
		}
		return if (newSize.rows == buffer.size && (buffer.size == 0 || newSize.columns == buffer[0].size)) {
			this
		} else BasicTextImage(newSize, buffer, filler)
	}

	override fun setCharacterAt(position: TerminalPosition?, character: TextCharacter) {
		if (position == null) {
			throw IllegalArgumentException("Cannot call BasicTextImage.setCharacterAt(..) with null position")
		}
		setCharacterAt(position.column, position.row, character)
	}

	override fun setCharacterAt(column: Int, row: Int, character: TextCharacter?) {
		if (character == null) {
			throw IllegalArgumentException("Cannot call BasicTextImage.setCharacterAt(..) with null character")
		}
		if (column < 0 || row < 0 || row >= buffer.size || column >= buffer[0].size) {
			return
		}

		// Double width character adjustments
		if (column > 0 && buffer[row][column - 1].isDoubleWidth) {
			buffer[row][column - 1] = buffer[row][column - 1].withCharacter(' ')
		}

		// Assign the character at location we specified
		buffer[row][column] = character

		// Double width character adjustments
		if (character.isDoubleWidth && column + 1 < buffer[0].size) {
			buffer[row][column + 1] = character.withCharacter(' ')
		}
	}

	override fun getCharacterAt(position: TerminalPosition?): TextCharacter? {
		if (position == null) {
			throw IllegalArgumentException("Cannot call BasicTextImage.getCharacterAt(..) with null position")
		}
		return getCharacterAt(position.column, position.row)
	}

	override fun getCharacterAt(column: Int, row: Int) =
		if (column < 0 || row < 0 || row >= buffer.size || column >= buffer[0].size) {
			null
		} else buffer[row][column]

	override fun copyTo(destination: TextImage) {
		copyTo(destination, 0, buffer.size, 0, buffer[0].size, 0, 0)
	}

	override fun copyTo(
		destination: TextImage,
		startRowIndex: Int,
		rows: Int,
		startColumnIndex: Int,
		columns: Int,
		destinationRowOffset: Int,
		destinationColumnOffset: Int) {
		var startRowIndex = startRowIndex
		var rows = rows
		var startColumnIndex = startColumnIndex
		var columns = columns
		var destinationRowOffset = destinationRowOffset
		var destinationColumnOffset = destinationColumnOffset

		// If the source image position is negative, offset the whole image
		if (startColumnIndex < 0) {
			destinationColumnOffset += -startColumnIndex
			columns += startColumnIndex
			startColumnIndex = 0
		}
		if (startRowIndex < 0) {
			destinationRowOffset += -startRowIndex
			rows += startRowIndex
			startRowIndex = 0
		}

		// If the destination offset is negative, adjust the source start indexes
		if (destinationColumnOffset < 0) {
			startColumnIndex -= destinationColumnOffset
			columns += destinationColumnOffset
			destinationColumnOffset = 0
		}
		if (destinationRowOffset < 0) {
			startRowIndex -= destinationRowOffset
			rows += destinationRowOffset
			destinationRowOffset = 0
		}

		//Make sure we can't copy more than is available
		columns = Math.min(buffer[0].size - startColumnIndex, columns)
		rows = Math.min(buffer.size - startRowIndex, rows)

		//Adjust target lengths as well
		columns = Math.min(destination.size.columns - destinationColumnOffset, columns)
		rows = Math.min(destination.size.rows - destinationRowOffset, rows)

		if (columns <= 0 || rows <= 0) {
			return
		}

		val destinationSize = destination.size
		if (destination is BasicTextImage) {
			var targetRow = destinationRowOffset
			var y = startRowIndex
			while (y < startRowIndex + rows && targetRow < destinationSize.rows) {
				System.arraycopy(buffer[y], startColumnIndex, destination.buffer[targetRow++], destinationColumnOffset, columns)
				y++
			}
		} else {
			//Manually copy character by character
			for (y in startRowIndex until startRowIndex + rows) {
				for (x in startColumnIndex until startColumnIndex + columns) {
					destination.setCharacterAt(
						x - startColumnIndex + destinationColumnOffset,
						y - startRowIndex + destinationRowOffset,
						buffer[y][x])
				}
			}
		}
	}

	override fun newTextGraphics(): TextGraphics =
		object : AbstractTextGraphics() {

			override val size: TerminalSize
				get() = size

			override fun setCharacter(columnIndex: Int, rowIndex: Int, textCharacter: TextCharacter): TextGraphics {
				this@BasicTextImage.setCharacterAt(columnIndex, rowIndex, textCharacter)
				return this
			}

			override fun getCharacter(column: Int, row: Int) =
				this@BasicTextImage.getCharacterAt(column, row)
		}

	private fun newBlankLine(): Array<TextCharacter> {
		val line = arrayOfNulls<TextCharacter>(size.columns)
		Arrays.fill(line, TextCharacter.DEFAULT_CHARACTER)
		return line
	}

	override fun scrollLines(firstLine: Int, lastLine: Int, distance: Int) {
		var firstLine = firstLine
		var lastLine = lastLine
		var distance = distance
		if (firstLine < 0) {
			firstLine = 0
		}
		if (lastLine >= size.rows) {
			lastLine = size.rows - 1
		}
		if (firstLine < lastLine) {
			if (distance > 0) {
				// scrolling up: start with first line as target:
				var curLine = firstLine
				// copy lines from further "below":
				while (curLine <= lastLine - distance) {
					buffer[curLine] = buffer[curLine + distance]
					curLine++
				}
				// blank out the remaining lines:
				while (curLine <= lastLine) {
					buffer[curLine] = newBlankLine()
					curLine++
				}
			} else if (distance < 0) {
				// scrolling down: start with last line as target:
				var curLine = lastLine
				distance = -distance
				// copy lines from further "above":
				while (curLine >= firstLine + distance) {
					buffer[curLine] = buffer[curLine - distance]
					curLine--
				}
				// blank out the remaining lines:
				while (curLine >= firstLine) {
					buffer[curLine] = newBlankLine()
					curLine--
				}
			} /* else: distance == 0 => no-op */
		}
	}

	override fun toString(): String {
		val sb = StringBuilder(size.rows * (size.columns + 1) + 50)
		sb.append('{').append(size.columns).append('x').append(size.rows).append('}').append('\n')
		for (line in buffer) {
			for (tc in line) {
				sb.append(tc.character)
			}
			sb.append('\n')
		}
		return sb.toString()
	}
}
/**
 * Creates a new BasicTextImage with the specified size and fills it initially with space characters using the
 * default foreground and background color
 * @param size Size to make the image
 */
