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
package com.googlecode.lanterna.terminal

import com.googlecode.lanterna.SGR
import com.googlecode.lanterna.TerminalPosition
import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.graphics.AbstractTextGraphics
import com.googlecode.lanterna.TextCharacter
import com.googlecode.lanterna.graphics.TextGraphics

import java.io.IOException
import java.util.HashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * This is the terminal's implementation of TextGraphics. Upon creation it takes a snapshot for the terminal's size, so
 * that it won't require to do an expensive lookup on every call to `getSize()`, but this also means that it can
 * go stale quickly if the terminal is resized. You should try to use the object quickly and then let it be GC:ed. It
 * will not pick up on terminal resize! Also, the state of the Terminal after an operation performed by this
 * TextGraphics implementation is undefined and you should probably re-initialize colors and modifiers.
 *
 *
 * Any write operation that results in an IOException will be wrapped by a RuntimeException since the TextGraphics
 * interface doesn't allow throwing IOException
 */
internal class TerminalTextGraphics @Throws(IOException::class)
constructor(private val terminal: Terminal) : AbstractTextGraphics() {
	private val terminalSize: TerminalSize

	private val writeHistory: MutableMap<TerminalPosition, TextCharacter>

	private val manageCallStackSize: AtomicInteger
	private var lastCharacter: TextCharacter? = null
	private var lastPosition: TerminalPosition? = null

	init {
		this.terminalSize = terminal.terminalSize
		this.manageCallStackSize = AtomicInteger(0)
		this.writeHistory = HashMap()
		this.lastCharacter = null
		this.lastPosition = null
	}

	override fun setCharacter(columnIndex: Int, rowIndex: Int, textCharacter: TextCharacter): TextGraphics {
		return setCharacter(TerminalPosition(columnIndex, rowIndex), textCharacter)
	}

	@Synchronized override fun setCharacter(position: TerminalPosition, textCharacter: TextCharacter): TextGraphics {
		try {
			if (manageCallStackSize.get() > 0) {
				if (lastCharacter == null || lastCharacter != textCharacter) {
					applyGraphicState(textCharacter)
					lastCharacter = textCharacter
				}
				if (lastPosition == null || lastPosition != position) {
					terminal.setCursorPosition(position.column, position.row)
					lastPosition = position
				}
			} else {
				terminal.setCursorPosition(position.column, position.row)
				applyGraphicState(textCharacter)
			}
			terminal.putCharacter(textCharacter.character)
			if (manageCallStackSize.get() > 0) {
				lastPosition = position.withRelativeColumn(1)
			}
			writeHistory.put(position, textCharacter)
		} catch (e: IOException) {
			throw RuntimeException(e)
		}

		return this
	}

	override fun getCharacter(column: Int, row: Int): TextCharacter {
		return getCharacter(TerminalPosition(column, row))
	}

	@Synchronized override fun getCharacter(position: TerminalPosition): TextCharacter {
		return writeHistory[position]
	}

	@Throws(IOException::class)
	private fun applyGraphicState(textCharacter: TextCharacter) {
		terminal.resetColorAndSGR()
		terminal.setForegroundColor(textCharacter.foregroundColor)
		terminal.setBackgroundColor(textCharacter.backgroundColor)
		for (sgr in textCharacter.getModifiers()) {
			terminal.enableSGR(sgr)
		}
	}

	override fun getSize(): TerminalSize {
		return terminalSize
	}

	@Synchronized override fun drawLine(fromPoint: TerminalPosition, toPoint: TerminalPosition, character: Char): TextGraphics {
		try {
			enterAtomic()
			super.drawLine(fromPoint, toPoint, character)
			return this
		} finally {
			leaveAtomic()
		}
	}

	@Synchronized override fun drawTriangle(p1: TerminalPosition, p2: TerminalPosition, p3: TerminalPosition, character: Char): TextGraphics {
		try {
			enterAtomic()
			super.drawTriangle(p1, p2, p3, character)
			return this
		} finally {
			leaveAtomic()
		}
	}

	@Synchronized override fun fillTriangle(p1: TerminalPosition, p2: TerminalPosition, p3: TerminalPosition, character: Char): TextGraphics {
		try {
			enterAtomic()
			super.fillTriangle(p1, p2, p3, character)
			return this
		} finally {
			leaveAtomic()
		}
	}

	@Synchronized override fun fillRectangle(topLeft: TerminalPosition, size: TerminalSize, character: Char): TextGraphics {
		try {
			enterAtomic()
			super.fillRectangle(topLeft, size, character)
			return this
		} finally {
			leaveAtomic()
		}
	}

	@Synchronized override fun drawRectangle(topLeft: TerminalPosition, size: TerminalSize, character: Char): TextGraphics {
		try {
			enterAtomic()
			super.drawRectangle(topLeft, size, character)
			return this
		} finally {
			leaveAtomic()
		}
	}

	@Synchronized override fun putString(column: Int, row: Int, string: String): TextGraphics {
		try {
			enterAtomic()
			return super.putString(column, row, string)
		} finally {
			leaveAtomic()
		}
	}

	/**
	 * It's tricky with this implementation because we can't rely on any state in between two calls to setCharacter
	 * since the caller might modify the terminal's state outside of this writer. However, many calls inside
	 * TextGraphics will indeed make multiple calls in setCharacter where we know that the state won't change (actually,
	 * we can't be 100% sure since the caller might create a separate thread and maliciously write directly to the
	 * terminal while call one of the draw/fill/put methods in here). We could just set the state before writing every
	 * single character but that would be inefficient. Rather, we keep a counter of if we are inside an 'atomic'
	 * (meaning we know multiple calls to setCharacter will have the same state). Some drawing methods call other
	 * drawing methods internally for their implementation so that's why this is implemented with an integer value
	 * instead of a boolean; when the counter reaches zero we remove the memory of what state the terminal is in.
	 */
	private fun enterAtomic() {
		manageCallStackSize.incrementAndGet()
	}

	private fun leaveAtomic() {
		if (manageCallStackSize.decrementAndGet() == 0) {
			lastPosition = null
			lastCharacter = null
		}
	}
}
