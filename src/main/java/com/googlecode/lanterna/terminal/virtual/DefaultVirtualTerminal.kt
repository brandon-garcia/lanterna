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

import com.googlecode.lanterna.*
import com.googlecode.lanterna.graphics.TextGraphics
import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.screen.TabBehaviour
import com.googlecode.lanterna.terminal.AbstractTerminal

import java.util.*
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class DefaultVirtualTerminal
/**
 * Creates a new virtual terminal with an initial size set
 * @param initialTerminalSize Starting size of the virtual terminal
 */
@JvmOverloads constructor(initialTerminalSize: TerminalSize = TerminalSize(80, 24)) : AbstractTerminal(), VirtualTerminal {
	private val regularTextBuffer: TextBuffer
	private val privateModeTextBuffer: TextBuffer
	private val dirtyTerminalCells: TreeSet<TerminalPosition>
	private val listeners: MutableList<VirtualTerminalListener>

	private var currentTextBuffer: TextBuffer? = null
	private var wholeBufferDirty: Boolean = false

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Terminal interface methods (and related)
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	@get:Synchronized override var terminalSize: TerminalSize? = null
		private set(value: TerminalSize?) {
			super.terminalSize = value
		}
	@get:Synchronized override var isCursorVisible: Boolean = false
		private set
	private var backlogSize: Int = 0

	private val inputQueue: BlockingQueue<KeyStroke>
	private val activeModifiers: EnumSet<SGR>
	private var activeForegroundColor: TextColor? = null
	private var activeBackgroundColor: TextColor? = null

	// Global coordinates, i.e. relative to the top-left corner of the full buffer
	@get:Synchronized override var cursorBufferPosition: TerminalPosition? = null
		private set

	// Used when switching back from private mode, to restore the earlier cursor position
	private var savedCursorPosition: TerminalPosition? = null

	override var cursorPosition: TerminalPosition
		@Synchronized get() = if (bufferLineCount <= terminalSize!!.rows) {
			cursorBufferPosition
		} else {
			cursorBufferPosition!!.withRelativeRow(-(bufferLineCount - terminalSize!!.rows))
		}
		@Synchronized set(cursorPosition) {
			var cursorPosition = cursorPosition
			if (terminalSize!!.rows < bufferLineCount) {
				cursorPosition = cursorPosition.withRelativeRow(bufferLineCount - terminalSize!!.rows)
			}
			this.cursorBufferPosition = cursorPosition
			correctCursor()
		}

	val dirtyCells: TreeSet<TerminalPosition>
		@Synchronized get() = TreeSet(dirtyTerminalCells)

	val andResetDirtyCells: TreeSet<TerminalPosition>
		@Synchronized get() {
			val copy = TreeSet(dirtyTerminalCells)
			dirtyTerminalCells.clear()
			return copy
		}

	val isWholeBufferDirtyThenReset: Boolean
		@Synchronized get() {
			val copy = wholeBufferDirty
			wholeBufferDirty = false
			return copy
		}

	override val bufferLineCount: Int
		@Synchronized get() = currentTextBuffer!!.lineCount

	init {
		this.regularTextBuffer = TextBuffer()
		this.privateModeTextBuffer = TextBuffer()
		this.dirtyTerminalCells = TreeSet()
		this.listeners = ArrayList()

		// Terminal state
		this.inputQueue = LinkedBlockingQueue()
		this.activeModifiers = EnumSet.noneOf(SGR::class.java)
		this.activeForegroundColor = TextColor.ANSI.DEFAULT
		this.activeBackgroundColor = TextColor.ANSI.DEFAULT

		// Start with regular mode
		this.currentTextBuffer = regularTextBuffer
		this.wholeBufferDirty = false
		this.terminalSize = initialTerminalSize
		this.isCursorVisible = true
		this.cursorBufferPosition = TerminalPosition.TOP_LEFT_CORNER
		this.savedCursorPosition = TerminalPosition.TOP_LEFT_CORNER
		this.backlogSize = 1000
	}

	@Synchronized override fun setTerminalSize(newSize: TerminalSize) {
		this.terminalSize = newSize
		trimBufferBacklog()
		correctCursor()
		for (listener in listeners) {
			listener.onResized(this, terminalSize!!)
		}
		super.onResized(newSize.columns, newSize.rows)
	}

	@Synchronized override fun enterPrivateMode() {
		currentTextBuffer = privateModeTextBuffer
		savedCursorPosition = cursorBufferPosition
		cursorPosition = TerminalPosition.TOP_LEFT_CORNER
		setWholeBufferDirty()
	}

	@Synchronized override fun exitPrivateMode() {
		currentTextBuffer = regularTextBuffer
		cursorBufferPosition = savedCursorPosition
		setWholeBufferDirty()
	}

	@Synchronized override fun clearScreen() {
		currentTextBuffer!!.clear()
		setWholeBufferDirty()
		cursorPosition = TerminalPosition.TOP_LEFT_CORNER
	}

	@Synchronized override fun setCursorPosition(x: Int, y: Int) {
		cursorPosition = cursorBufferPosition!!.withColumn(x).withRow(y)
	}

	@Synchronized override fun setCursorVisible(visible: Boolean) {
		this.isCursorVisible = visible
	}

	@Synchronized override fun putCharacter(c: Char) {
		if (c == '\n') {
			moveCursorToNextLine()
		} else if (TerminalTextUtils.isPrintableCharacter(c)) {
			putCharacter(TextCharacter(c, activeForegroundColor, activeBackgroundColor, activeModifiers))
		}
	}

	@Synchronized override fun enableSGR(sgr: SGR) {
		activeModifiers.add(sgr)
	}

	@Synchronized override fun disableSGR(sgr: SGR) {
		activeModifiers.remove(sgr)
	}

	@Synchronized override fun resetColorAndSGR() {
		this.activeModifiers.clear()
		this.activeForegroundColor = TextColor.ANSI.DEFAULT
		this.activeBackgroundColor = TextColor.ANSI.DEFAULT
	}

	@Synchronized override fun setForegroundColor(color: TextColor) {
		this.activeForegroundColor = color
	}

	@Synchronized override fun setBackgroundColor(color: TextColor) {
		this.activeBackgroundColor = color
	}

	@Synchronized override fun enquireTerminal(timeout: Int, timeoutUnit: TimeUnit) =
		javaClass.getName().toByteArray()

	@Synchronized override fun bell() {
		for (listener in listeners) {
			listener.onBell()
		}
	}

	@Synchronized override fun flush() {
		for (listener in listeners) {
			listener.onFlush()
		}
	}

	override fun close() {
		for (listener in listeners) {
			listener.onClose()
		}
	}

	@Synchronized override fun pollInput() =
		inputQueue.poll()

	@Synchronized override fun readInput(): KeyStroke {
		try {
			return inputQueue.take()
		} catch (e: InterruptedException) {
			throw RuntimeException("Unexpected interrupt", e)
		}

	}

	override fun newTextGraphics(): TextGraphics =
		VirtualTerminalTextGraphics(this)

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// VirtualTerminal specific methods
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Synchronized override fun addVirtualTerminalListener(listener: VirtualTerminalListener?) {
		if (listener != null) {
			listeners.add(listener)
		}
	}

	@Synchronized override fun removeVirtualTerminalListener(listener: VirtualTerminalListener) {
		listeners.remove(listener)
	}

	@Synchronized override fun setBacklogSize(backlogSize: Int) {
		this.backlogSize = backlogSize
	}

	override fun addInput(keyStroke: KeyStroke) {
		inputQueue.add(keyStroke)
	}

	@Synchronized override fun getCharacter(position: TerminalPosition) =
		getCharacter(position.column, position.row)

	@Synchronized override fun getCharacter(column: Int, row: Int): TextCharacter {
		var row = row
		if (terminalSize!!.rows < currentTextBuffer!!.lineCount) {
			row += currentTextBuffer!!.lineCount - terminalSize!!.rows
		}
		return getBufferCharacter(column, row)
	}

	override fun getBufferCharacter(column: Int, row: Int) =
		currentTextBuffer!!.getCharacter(row, column)

	override fun getBufferCharacter(position: TerminalPosition) =
		getBufferCharacter(position.column, position.row)

	@Synchronized override fun forEachLine(startRow: Int, endRow: Int, bufferWalker: VirtualTerminal.BufferWalker) {
		val emptyLine: VirtualTerminal.BufferLine = VirtualTerminal.BufferLine { TextCharacter.DEFAULT_CHARACTER }
		val iterator = currentTextBuffer!!.getLinesFrom(startRow)
		for (row in startRow..endRow) {
			var bufferLine = emptyLine
			if (iterator.hasNext()) {
				val list = iterator.next()
				bufferLine = VirtualTerminal.BufferLine { column ->
					if (column >= list.size) {
						TextCharacter.DEFAULT_CHARACTER
					} else list[column]
				}
			}
			bufferWalker.onLine(row, bufferLine)
		}
	}

	@Synchronized internal fun putCharacter(terminalCharacter: TextCharacter) {
		if (terminalCharacter.character == '\t') {
			val nrOfSpaces = TabBehaviour.ALIGN_TO_COLUMN_4.getTabReplacement(cursorBufferPosition!!.column).length
			var i = 0
			while (i < nrOfSpaces && cursorBufferPosition!!.column < terminalSize!!.columns - 1) {
				putCharacter(terminalCharacter.withCharacter(' '))
				i++
			}
		} else {
			val doubleWidth = TerminalTextUtils.isCharDoubleWidth(terminalCharacter.character)
			// If we're at the last column and the user tries to print a double-width character, reset the cell and move
			// to the next line
			if (cursorBufferPosition!!.column == terminalSize!!.columns - 1 && doubleWidth) {
				currentTextBuffer!!.setCharacter(cursorBufferPosition!!.row, cursorBufferPosition!!.column, TextCharacter.DEFAULT_CHARACTER)
				moveCursorToNextLine()
			}
			if (cursorBufferPosition!!.column == terminalSize!!.columns) {
				moveCursorToNextLine()
			}

			// Update the buffer
			val i = currentTextBuffer!!.setCharacter(cursorBufferPosition!!.row, cursorBufferPosition!!.column, terminalCharacter)
			if (!wholeBufferDirty) {
				dirtyTerminalCells.add(TerminalPosition(cursorBufferPosition!!.column, cursorBufferPosition!!.row))
				if (i == 1) {
					dirtyTerminalCells.add(TerminalPosition(cursorBufferPosition!!.column + 1, cursorBufferPosition!!.row))
				} else if (i == 2) {
					dirtyTerminalCells.add(TerminalPosition(cursorBufferPosition!!.column - 1, cursorBufferPosition!!.row))
				}
				if (dirtyTerminalCells.size > terminalSize!!.columns.toDouble() * terminalSize!!.rows.toDouble() * 0.9) {
					setWholeBufferDirty()
				}
			}

			//Advance cursor
			cursorBufferPosition = cursorBufferPosition!!.withRelativeColumn(if (doubleWidth) 2 else 1)
			if (cursorBufferPosition!!.column > terminalSize!!.columns) {
				moveCursorToNextLine()
			}
		}
	}

	/**
	 * Moves the text cursor to the first column of the next line and trims the backlog of necessary
	 */
	private fun moveCursorToNextLine() {
		cursorBufferPosition = cursorBufferPosition!!.withColumn(0).withRelativeRow(1)
		if (cursorBufferPosition!!.row >= currentTextBuffer!!.lineCount) {
			currentTextBuffer!!.newLine()
		}
		trimBufferBacklog()
		correctCursor()
	}

	/**
	 * Marks the whole buffer as dirty so every cell is considered in need to repainting. This is used by methods such
	 * as clear and bell that will affect all content at once.
	 */
	private fun setWholeBufferDirty() {
		wholeBufferDirty = true
		dirtyTerminalCells.clear()
	}

	private fun trimBufferBacklog() {
		// Now see if we need to discard lines from the backlog
		var bufferBacklogSize = backlogSize
		if (currentTextBuffer === privateModeTextBuffer) {
			bufferBacklogSize = 0
		}
		val trimBacklogRows = currentTextBuffer!!.lineCount - (bufferBacklogSize + terminalSize!!.rows)
		if (trimBacklogRows > 0) {
			currentTextBuffer!!.removeTopLines(trimBacklogRows)
			// Adjust cursor position
			cursorBufferPosition = cursorBufferPosition!!.withRelativeRow(-trimBacklogRows)
			correctCursor()
			if (!wholeBufferDirty) {
				// Adjust all "dirty" positions
				val newDirtySet = TreeSet<TerminalPosition>()
				for (dirtyPosition in dirtyTerminalCells) {
					val adjustedPosition = dirtyPosition.withRelativeRow(-trimBacklogRows)
					if (adjustedPosition.row >= 0) {
						newDirtySet.add(adjustedPosition)
					}
				}
				dirtyTerminalCells.clear()
				dirtyTerminalCells.addAll(newDirtySet)
			}
		}
	}

	private fun correctCursor() {
		this.cursorBufferPosition = cursorBufferPosition!!.withColumn(Math.min(cursorBufferPosition!!.column, terminalSize!!.columns - 1))
		this.cursorBufferPosition = cursorBufferPosition!!.withRow(Math.min(cursorBufferPosition!!.row, Math.max(terminalSize!!.rows, bufferLineCount) - 1))
		this.cursorBufferPosition = TerminalPosition(
			Math.max(cursorBufferPosition!!.column, 0),
			Math.max(cursorBufferPosition!!.row, 0))
	}

}
/**
 * Creates a new virtual terminal with an initial size set
 */
