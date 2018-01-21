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
package com.googlecode.lanterna.screen

import com.googlecode.lanterna.*
import com.googlecode.lanterna.graphics.Scrollable
import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.input.KeyType
import com.googlecode.lanterna.terminal.Terminal
import com.googlecode.lanterna.terminal.TerminalResizeListener

import java.io.IOException
import java.util.Comparator
import java.util.EnumSet
import java.util.TreeMap

/**
 * This is the default concrete implementation of the Screen interface, a buffered layer sitting on top of a Terminal.
 * If you want to get started with the Screen layer, this is probably the class you want to use. Remember to start the
 * screen before you can use it and stop it when you are done with it. This will place the terminal in private mode
 * during the screen operations and leave private mode afterwards.
 * @author martin
 */
class TerminalScreen
/**
 * Creates a new Screen on top of a supplied terminal, will query the terminal for its size. The screen is initially
 * blank. The default character used for unused space (the newly initialized state of the screen and new areas after
 * expanding the terminal size) will be a blank space in 'default' ANSI front- and background color.
 *
 *
 * Before you can display the content of this buffered screen to the real underlying terminal, you must call the
 * `startScreen()` method. This will ask the terminal to enter private mode (which is required for Screens to
 * work properly). Similarly, when you are done, you should call `stopScreen()` which will exit private mode.
 *
 * @param terminal Terminal object to create the DefaultScreen on top of.
 * @param defaultCharacter What character to use for the initial state of the screen and expanded areas
 * @throws java.io.IOException If there was an underlying I/O error when querying the size of the terminal
 */
@Throws(IOException::class)
@JvmOverloads constructor(
	/**
	 * Returns the underlying `Terminal` interface that this Screen is using.
	 *
	 *
	 * **Be aware:** directly modifying the underlying terminal will most likely result in unexpected behaviour if
	 * you then go on and try to interact with the Screen. The Screen's back-buffer/front-buffer will not know about
	 * the operations you are going on the Terminal and won't be able to properly generate a refresh unless you enforce
	 * a `Screen.RefreshType.COMPLETE`, at which the entire terminal area will be repainted according to the
	 * back-buffer of the `Screen`.
	 * @return Underlying terminal used by the screen
	 */
	val terminal: Terminal, defaultCharacter: TextCharacter = Screen.DEFAULT_CHARACTER) : AbstractScreen(terminal.terminalSize, defaultCharacter) {
	private var isStarted: Boolean = false
	private var fullRedrawHint: Boolean = false
	private var scrollHint: ScrollHint? = null

	init {
		this.terminal.addResizeListener(TerminalScreenResizeListener())
		this.isStarted = false
		this.fullRedrawHint = true
	}

	@Synchronized
	@Throws(IOException::class)
	override fun startScreen() {
		if (isStarted) {
			return
		}

		isStarted = true
		terminal.enterPrivateMode()
		terminal.terminalSize
		terminal.clearScreen()
		this.fullRedrawHint = true
		val cursorPosition = cursorPosition
		if (cursorPosition != null) {
			terminal.setCursorVisible(true)
			terminal.setCursorPosition(cursorPosition.column, cursorPosition.row)
		} else {
			terminal.setCursorVisible(false)
		}
	}

	@Throws(IOException::class)
	override fun stopScreen() {
		stopScreen(true)
	}

	@Synchronized
	@Throws(IOException::class)
	fun stopScreen(flushInput: Boolean) {
		if (!isStarted) {
			return
		}

		if (flushInput) {
			//Drain the input queue
			var keyStroke: KeyStroke?
			do {
				keyStroke = pollInput()
			} while (keyStroke != null && keyStroke.keyType !== KeyType.EOF)
		}

		terminal.exitPrivateMode()
		isStarted = false
	}

	@Synchronized
	@Throws(IOException::class)
	override fun refresh(refreshType: Screen.RefreshType) {
		if (!isStarted) {
			return
		}
		if (refreshType == Screen.RefreshType.AUTOMATIC && fullRedrawHint || refreshType == Screen.RefreshType.COMPLETE) {
			refreshFull()
			fullRedrawHint = false
		} else if (refreshType == Screen.RefreshType.AUTOMATIC && (scrollHint == null || scrollHint === ScrollHint.INVALID)) {
			val threshold = terminalSize!!.rows.toDouble() * terminalSize!!.columns.toDouble() * 0.75
			if (backBuffer!!.isVeryDifferent(frontBuffer, threshold.toInt())) {
				refreshFull()
			} else {
				refreshByDelta()
			}
		} else {
			refreshByDelta()
		}
		backBuffer!!.copyTo(frontBuffer)
		val cursorPosition = cursorPosition
		if (cursorPosition != null) {
			terminal.setCursorVisible(true)
			//If we are trying to move the cursor to the padding of a CJK character, put it on the actual character instead
			if (cursorPosition.column > 0 && TerminalTextUtils.isCharCJK(frontBuffer!!.getCharacterAt(cursorPosition.withRelativeColumn(-1)).character)) {
				terminal.setCursorPosition(cursorPosition.column - 1, cursorPosition.row)
			} else {
				terminal.setCursorPosition(cursorPosition.column, cursorPosition.row)
			}
		} else {
			terminal.setCursorVisible(false)
		}
		terminal.flush()
	}

	@Throws(IOException::class)
	private fun useScrollHint() {
		if (scrollHint == null) {
			return
		}

		try {
			if (scrollHint === ScrollHint.INVALID) {
				return
			}
			val term = terminal
			if (term is Scrollable) {
				// just try and see if it cares:
				scrollHint!!.applyTo(term as Scrollable)
				// if that didn't throw, then update front buffer:
				scrollHint!!.applyTo(frontBuffer)
			}
		} catch (uoe: UnsupportedOperationException) { /* ignore */
		} finally {
			scrollHint = null
		}
	}

	@Throws(IOException::class)
	private fun refreshByDelta() {
		val updateMap = TreeMap<TerminalPosition, TextCharacter>(ScreenPointComparator())
		val terminalSize = terminalSize

		useScrollHint()

		for (y in 0 until terminalSize!!.rows) {
			var x = 0
			while (x < terminalSize!!.columns) {
				val backBufferCharacter = backBuffer!!.getCharacterAt(x, y)
				if (backBufferCharacter != frontBuffer!!.getCharacterAt(x, y)) {
					updateMap.put(TerminalPosition(x, y), backBufferCharacter)
				}
				if (TerminalTextUtils.isCharCJK(backBufferCharacter!!.character)) {
					x++    //Skip the trailing padding
				}
				x++
			}
		}

		if (updateMap.isEmpty()) {
			return
		}
		var currentPosition = updateMap.keys.iterator().next()
		terminal.setCursorPosition(currentPosition.column, currentPosition.row)

		val firstScreenCharacterToUpdate = updateMap.values.iterator().next()
		val currentSGR = firstScreenCharacterToUpdate.getModifiers()
		terminal.resetColorAndSGR()
		for (sgr in currentSGR) {
			terminal.enableSGR(sgr)
		}
		var currentForegroundColor = firstScreenCharacterToUpdate.foregroundColor
		var currentBackgroundColor = firstScreenCharacterToUpdate.backgroundColor
		terminal.setForegroundColor(currentForegroundColor!!)
		terminal.setBackgroundColor(currentBackgroundColor!!)
		for (position in updateMap.keys) {
			if (position != currentPosition) {
				terminal.setCursorPosition(position.column, position.row)
				currentPosition = position
			}
			val newCharacter = updateMap[position]
			if (currentForegroundColor != newCharacter.foregroundColor) {
				terminal.setForegroundColor(newCharacter.foregroundColor!!)
				currentForegroundColor = newCharacter.foregroundColor
			}
			if (currentBackgroundColor != newCharacter.backgroundColor) {
				terminal.setBackgroundColor(newCharacter.backgroundColor!!)
				currentBackgroundColor = newCharacter.backgroundColor
			}
			for (sgr in SGR.values()) {
				if (currentSGR.contains(sgr) && !newCharacter.getModifiers().contains(sgr)) {
					terminal.disableSGR(sgr)
					currentSGR.remove(sgr)
				} else if (!currentSGR.contains(sgr) && newCharacter.getModifiers().contains(sgr)) {
					terminal.enableSGR(sgr)
					currentSGR.add(sgr)
				}
			}
			terminal.putCharacter(newCharacter.character)
			if (TerminalTextUtils.isCharCJK(newCharacter.character)) {
				//CJK characters advances two columns
				currentPosition = currentPosition.withRelativeColumn(2)
			} else {
				//Normal characters advances one column
				currentPosition = currentPosition.withRelativeColumn(1)
			}
		}
	}

	@Throws(IOException::class)
	private fun refreshFull() {
		terminal.setForegroundColor(TextColor.ANSI.DEFAULT)
		terminal.setBackgroundColor(TextColor.ANSI.DEFAULT)
		terminal.clearScreen()
		terminal.resetColorAndSGR()
		scrollHint = null // discard any scroll hint for full refresh

		val currentSGR = EnumSet.noneOf<SGR>(SGR::class.java)
		var currentForegroundColor: TextColor? = TextColor.ANSI.DEFAULT
		var currentBackgroundColor: TextColor? = TextColor.ANSI.DEFAULT
		for (y in 0 until terminalSize!!.rows) {
			terminal.setCursorPosition(0, y)
			var currentColumn = 0
			var x = 0
			while (x < terminalSize!!.columns) {
				val newCharacter = backBuffer!!.getCharacterAt(x, y)
				if (newCharacter == Screen.DEFAULT_CHARACTER) {
					x++
					continue
				}

				if (currentForegroundColor != newCharacter!!.foregroundColor) {
					terminal.setForegroundColor(newCharacter.foregroundColor!!)
					currentForegroundColor = newCharacter.foregroundColor
				}
				if (currentBackgroundColor != newCharacter.backgroundColor) {
					terminal.setBackgroundColor(newCharacter.backgroundColor!!)
					currentBackgroundColor = newCharacter.backgroundColor
				}
				for (sgr in SGR.values()) {
					if (currentSGR.contains(sgr) && !newCharacter.getModifiers().contains(sgr)) {
						terminal.disableSGR(sgr)
						currentSGR.remove(sgr)
					} else if (!currentSGR.contains(sgr) && newCharacter.getModifiers().contains(sgr)) {
						terminal.enableSGR(sgr)
						currentSGR.add(sgr)
					}
				}
				if (currentColumn != x) {
					terminal.setCursorPosition(x, y)
					currentColumn = x
				}
				terminal.putCharacter(newCharacter.character)
				if (TerminalTextUtils.isCharCJK(newCharacter.character)) {
					//CJK characters take up two columns
					currentColumn += 2
					x++
				} else {
					//Normal characters take up one column
					currentColumn += 1
				}
				x++
			}
		}
	}

	@Throws(IOException::class)
	override fun readInput() =
		terminal.readInput()

	@Throws(IOException::class)
	override fun pollInput() =
		terminal.pollInput()

	@Synchronized override fun clear() {
		super.clear()
		fullRedrawHint = true
		scrollHint = ScrollHint.INVALID
	}

	@Synchronized override fun doResizeIfNecessary(): TerminalSize? {
		val newSize = super.doResizeIfNecessary()
		if (newSize != null) {
			fullRedrawHint = true
		}
		return newSize
	}

	/**
	 * Perform the scrolling and save scroll-range and distance in order
	 * to be able to optimize Terminal-update later.
	 */
	override fun scrollLines(firstLine: Int, lastLine: Int, distance: Int) {
		// just ignore certain kinds of garbage:
		if (distance == 0 || firstLine > lastLine) {
			return
		}

		super.scrollLines(firstLine, lastLine, distance)

		// Save scroll hint for next refresh:
		val newHint = ScrollHint(firstLine, lastLine, distance)
		if (scrollHint == null) {
			// no scroll hint yet: use the new one:
			scrollHint = newHint
		} else
			if (scrollHint === ScrollHint.INVALID) {
				// scroll ranges already inconsistent since latest refresh!
				// leave at INVALID
			} else if (scrollHint!!.matches(newHint)) {
				// same range: just accumulate distance:
				scrollHint!!.distance += newHint.distance
			} else {
				// different scroll range: no scroll-optimization for next refresh
				this.scrollHint = ScrollHint.INVALID
			}
	}

	private inner class TerminalScreenResizeListener : TerminalResizeListener {
		override fun onResized(terminal: Terminal, newSize: TerminalSize) {
			addResizeRequest(newSize)
		}
	}

	private class ScreenPointComparator : Comparator<TerminalPosition> {
		override fun compare(o1: TerminalPosition, o2: TerminalPosition) =
			if (o1.row == o2.row) {
				if (o1.column == o2.column) {
					0
				} else {
					o1.column.compareTo(o2.column)
				}
			} else {
				o1.row.compareTo(o2.row)
			}
	}

	private class ScrollHint(val firstLine: Int, val lastLine: Int, var distance: Int) {

		fun matches(other: ScrollHint) =
			this.firstLine == other.firstLine && this.lastLine == other.lastLine

		@Throws(IOException::class)
		fun applyTo(scr: Scrollable?) {
			scr!!.scrollLines(firstLine, lastLine, distance)
		}

		companion object {
			val INVALID = ScrollHint(-1, -1, 0)
		}
	}

}
/**
 * Creates a new Screen on top of a supplied terminal, will query the terminal for its size. The screen is initially
 * blank. The default character used for unused space (the newly initialized state of the screen and new areas after
 * expanding the terminal size) will be a blank space in 'default' ANSI front- and background color.
 *
 *
 * Before you can display the content of this buffered screen to the real underlying terminal, you must call the
 * `startScreen()` method. This will ask the terminal to enter private mode (which is required for Screens to
 * work properly). Similarly, when you are done, you should call `stopScreen()` which will exit private mode.
 *
 * @param terminal Terminal object to create the DefaultScreen on top of
 * @throws java.io.IOException If there was an underlying I/O error when querying the size of the terminal
 */
