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

import com.googlecode.lanterna.TextCharacter
import com.googlecode.lanterna.TerminalSize

/**
 * This TextGraphics implementation wraps another TextGraphics and forwards all operations to it, but with a few
 * differences. First of all, each individual character being printed is printed twice. Secondly, if you call
 * `getSize()`, it will return a size that has half the width of the underlying TextGraphics. This presents the
 * writable view as somewhat squared, since normally terminal characters are twice as tall as wide. You can see some
 * examples of how this looks by running the Triangle test in `com.googlecode.lanterna.screen.ScreenTriangleTest`
 * and compare it when running with the --square parameter and without.
 */
class DoublePrintingTextGraphics
/**
 * Creates a new `DoublePrintingTextGraphics` on top of a supplied `TextGraphics`
 * @param underlyingTextGraphics backend `TextGraphics` to forward all the calls to
 */
(private val underlyingTextGraphics: TextGraphics) : AbstractTextGraphics() {

	override val size: TerminalSize
		get() {
			val size = underlyingTextGraphics.size
			return size.withColumns(size.columns / 2)
		}

	override fun setCharacter(columnIndex: Int, rowIndex: Int, textCharacter: TextCharacter): TextGraphics {
		var columnIndex = columnIndex
		columnIndex = columnIndex * 2
		underlyingTextGraphics.setCharacter(columnIndex, rowIndex, textCharacter)
		underlyingTextGraphics.setCharacter(columnIndex + 1, rowIndex, textCharacter)
		return this
	}

	override fun getCharacter(columnIndex: Int, rowIndex: Int): TextCharacter {
		var columnIndex = columnIndex
		columnIndex = columnIndex * 2
		return underlyingTextGraphics.getCharacter(columnIndex, rowIndex)

	}
}
