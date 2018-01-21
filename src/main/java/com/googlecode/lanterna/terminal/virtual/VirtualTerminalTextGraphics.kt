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

import com.googlecode.lanterna.TerminalPosition
import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.TextCharacter
import com.googlecode.lanterna.graphics.AbstractTextGraphics
import com.googlecode.lanterna.graphics.TextGraphics

/**
 * Implementation of [TextGraphics] for [VirtualTerminal]
 * @author Martin
 */
internal class VirtualTerminalTextGraphics(private val virtualTerminal: DefaultVirtualTerminal) : AbstractTextGraphics() {

	override fun setCharacter(columnIndex: Int, rowIndex: Int, textCharacter: TextCharacter): TextGraphics {
		val size = size
		if (columnIndex < 0 || columnIndex >= size.columns ||
			rowIndex < 0 || rowIndex >= size.rows) {
			return this
		}
		synchronized(virtualTerminal) {
			virtualTerminal.cursorPosition = TerminalPosition(columnIndex, rowIndex)
			virtualTerminal.putCharacter(textCharacter)
		}
		return this
	}

	override fun getCharacter(position: TerminalPosition) =
		virtualTerminal.getCharacter(position)

	override fun getCharacter(column: Int, row: Int) =
		getCharacter(TerminalPosition(column, row))

	override fun getSize() =
		virtualTerminal.terminalSize
}
