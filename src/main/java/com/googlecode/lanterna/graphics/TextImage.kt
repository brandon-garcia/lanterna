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

import com.googlecode.lanterna.TerminalPosition
import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.TextCharacter

/**
 * An 'image' build up of text characters with color and style information. These are completely in memory and not
 * visible anyway, but can be used when drawing with a TextGraphics objects.
 * @author martin
 */
interface TextImage : Scrollable {
	/**
	 * Returns the dimensions of this TextImage, in columns and rows
	 * @return Size of this TextImage
	 */
	val size: TerminalSize

	/**
	 * Returns the character stored at a particular position in this image
	 * @param position Coordinates of the character
	 * @return TextCharacter stored at the specified position
	 */
	fun getCharacterAt(position: TerminalPosition): TextCharacter

	/**
	 * Returns the character stored at a particular position in this image
	 * @param column Column coordinate of the character
	 * @param row Row coordinate of the character
	 * @return TextCharacter stored at the specified position
	 */
	fun getCharacterAt(column: Int, row: Int): TextCharacter

	/**
	 * Sets the character at a specific position in the image to a particular TextCharacter. If the position is outside
	 * of the image's size, this method does nothing.
	 * @param position Coordinates of the character
	 * @param character What TextCharacter to assign at the specified position
	 */
	fun setCharacterAt(position: TerminalPosition, character: TextCharacter)

	/**
	 * Sets the character at a specific position in the image to a particular TextCharacter. If the position is outside
	 * of the image's size, this method does nothing.
	 * @param column Column coordinate of the character
	 * @param row Row coordinate of the character
	 * @param character What TextCharacter to assign at the specified position
	 */
	fun setCharacterAt(column: Int, row: Int, character: TextCharacter)

	/**
	 * Sets the text image content to one specified character (including color and style)
	 * @param character The character to fill the image with
	 */
	fun setAll(character: TextCharacter)

	/**
	 * Creates a TextGraphics object that targets this TextImage for all its drawing operations.
	 * @return TextGraphics object for this TextImage
	 */
	fun newTextGraphics(): TextGraphics

	/**
	 * Returns a copy of this image resized to a new size and using a specified filler character if the new size is
	 * larger than the old and we need to fill in empty areas. The copy will be independent from the one this method is
	 * invoked on, so modifying one will not affect the other.
	 * @param newSize Size of the new image
	 * @param filler Filler character to use on the new areas when enlarging the image (is not used when shrinking)
	 * @return Copy of this image, but resized
	 */
	fun resize(newSize: TerminalSize, filler: TextCharacter): TextImage


	/**
	 * Copies this TextImage's content to another TextImage. If the destination TextImage is larger than this
	 * ScreenBuffer, the areas outside of the area that is written to will be untouched.
	 * @param destination TextImage to copy to
	 */
	fun copyTo(destination: TextImage)

	/**
	 * Copies this TextImage's content to another TextImage. If the destination TextImage is larger than this
	 * TextImage, the areas outside of the area that is written to will be untouched.
	 * @param destination TextImage to copy to
	 * @param startRowIndex Which row in this image to copy from
	 * @param rows How many rows to copy
	 * @param startColumnIndex Which column in this image to copy from
	 * @param columns How many columns to copy
	 * @param destinationRowOffset Offset (in number of rows) in the target image where we want to first copied row to be
	 * @param destinationColumnOffset Offset (in number of columns) in the target image where we want to first copied column to be
	 */
	fun copyTo(
		destination: TextImage,
		startRowIndex: Int,
		rows: Int,
		startColumnIndex: Int,
		columns: Int,
		destinationRowOffset: Int,
		destinationColumnOffset: Int)

	/**
	 * Scroll a range of lines of this TextImage according to given distance.
	 *
	 * TextImage implementations of this method do **not** throw IOException.
	 */
	override fun scrollLines(firstLine: Int, lastLine: Int, distance: Int)
}
