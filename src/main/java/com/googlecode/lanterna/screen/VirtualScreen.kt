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
import com.googlecode.lanterna.graphics.TextGraphics
import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.input.KeyType

import java.io.IOException

/**
 * VirtualScreen wraps a normal screen and presents it as a screen that has a configurable minimum size; if the real
 * screen is smaller than this size, the presented screen will add scrolling to get around it. To anyone using this
 * class, it will appear and behave just as a normal screen. Scrolling is done by using CTRL + arrow keys.
 *
 *
 * The use case for this class is to allow you to set a minimum size that you can count on be honored, no matter how
 * small the user makes the terminal. This should make programming GUIs easier.
 * @author Martin
 */
class VirtualScreen
/**
 * Creates a new VirtualScreen that wraps a supplied Screen. The screen passed in here should be the real screen
 * that is created on top of the real `Terminal`, it will have the correct size and content for what's
 * actually displayed to the user, but this class will present everything as one view with a fixed minimum size,
 * no matter what size the real terminal has.
 *
 *
 * The initial minimum size will be the current size of the screen.
 * @param screen Real screen that will be used when drawing the whole or partial virtual screen
 */
(private val realScreen: Screen) : AbstractScreen(realScreen.terminalSize) {
	private val frameRenderer: FrameRenderer
	private var minimumSize: TerminalSize? = null
	private var viewportTopLeft: TerminalPosition? = null
	/**
	 * Returns the current size of the viewport. This will generally match the dimensions of the underlying terminal.
	 * @return Viewport size for this [VirtualScreen]
	 */
	var viewportSize: TerminalSize? = null
		private set

	override var cursorPosition: TerminalPosition?
		get
		set(position) {
			var position = position
			super.setCursorPosition(position)
			if (position == null) {
				realScreen.cursorPosition = null
				return
			}
			position = position.withRelativeColumn(-viewportTopLeft!!.column).withRelativeRow(-viewportTopLeft!!.row)
			if (position.column >= 0 && position.column < viewportSize!!.columns &&
				position.row >= 0 && position.row < viewportSize!!.rows) {
				realScreen.cursorPosition = position
			} else {
				realScreen.cursorPosition = null
			}
		}

	init {
		this.frameRenderer = DefaultFrameRenderer()
		this.minimumSize = realScreen.terminalSize
		this.viewportTopLeft = TerminalPosition.TOP_LEFT_CORNER
		this.viewportSize = minimumSize
	}

	/**
	 * Sets the minimum size we want the virtual screen to have. If the user resizes the real terminal to something
	 * smaller than this, the virtual screen will refuse to make it smaller and add scrollbars to the view.
	 * @param minimumSize Minimum size we want the screen to have
	 */
	fun setMinimumSize(minimumSize: TerminalSize) {
		this.minimumSize = minimumSize
		val virtualSize = minimumSize.max(realScreen.terminalSize)
		if (minimumSize != virtualSize) {
			addResizeRequest(virtualSize)
			super.doResizeIfNecessary()
		}
		calculateViewport(realScreen.terminalSize)
	}

	/**
	 * Returns the minimum size this virtual screen can have. If the real terminal is made smaller than this, the
	 * virtual screen will draw scrollbars and implement scrolling
	 * @return Minimum size configured for this virtual screen
	 */
	fun getMinimumSize(): TerminalSize? {
		return minimumSize
	}

	@Throws(IOException::class)
	override fun startScreen() {
		realScreen.startScreen()
	}

	@Throws(IOException::class)
	override fun stopScreen() {
		realScreen.stopScreen()
	}

	override fun getFrontCharacter(position: TerminalPosition): TextCharacter? {
		return null
	}

	@Synchronized override fun doResizeIfNecessary(): TerminalSize? {
		val underlyingSize = realScreen.doResizeIfNecessary() ?: return null

		val newVirtualSize = calculateViewport(underlyingSize)
		if (terminalSize != newVirtualSize) {
			addResizeRequest(newVirtualSize)
			return super.doResizeIfNecessary()
		}
		return newVirtualSize
	}

	private fun calculateViewport(realTerminalSize: TerminalSize): TerminalSize {
		val newVirtualSize = minimumSize!!.max(realTerminalSize)
		if (newVirtualSize == realTerminalSize) {
			viewportSize = realTerminalSize
			viewportTopLeft = TerminalPosition.TOP_LEFT_CORNER
		} else {
			val newViewportSize = frameRenderer.getViewportSize(realTerminalSize, newVirtualSize)
			if (newViewportSize.rows > viewportSize!!.rows) {
				viewportTopLeft = viewportTopLeft!!.withRow(Math.max(0, viewportTopLeft!!.row - (newViewportSize.rows - viewportSize!!.rows)))
			}
			if (newViewportSize.columns > viewportSize!!.columns) {
				viewportTopLeft = viewportTopLeft!!.withColumn(Math.max(0, viewportTopLeft!!.column - (newViewportSize.columns - viewportSize!!.columns)))
			}
			viewportSize = newViewportSize
		}
		return newVirtualSize
	}

	@Throws(IOException::class)
	override fun refresh(refreshType: Screen.RefreshType) {
		cursorPosition = getCursorPosition() //Make sure the cursor is at the correct position
		if (viewportSize != realScreen.terminalSize) {
			frameRenderer.drawFrame(
				realScreen.newTextGraphics(),
				realScreen.terminalSize,
				terminalSize,
				viewportTopLeft)
		}

		//Copy the rows
		val viewportOffset = frameRenderer.viewportOffset
		if (realScreen is AbstractScreen) {
			backBuffer!!.copyTo(
				realScreen.backBuffer,
				viewportTopLeft!!.row,
				viewportSize!!.rows,
				viewportTopLeft!!.column,
				viewportSize!!.columns,
				viewportOffset.row,
				viewportOffset.column)
		} else {
			for (y in 0 until viewportSize!!.rows) {
				for (x in 0 until viewportSize!!.columns) {
					realScreen.setCharacter(
						x + viewportOffset.column,
						y + viewportOffset.row,
						backBuffer!!.getCharacterAt(
							x + viewportTopLeft!!.column,
							y + viewportTopLeft!!.row))
				}
			}
		}
		realScreen.refresh(refreshType)
	}

	@Throws(IOException::class)
	override fun pollInput(): KeyStroke {
		return filter(realScreen.pollInput())
	}

	@Throws(IOException::class)
	override fun readInput(): KeyStroke {
		return filter(realScreen.readInput())
	}

	@Throws(IOException::class)
	private fun filter(keyStroke: KeyStroke?): KeyStroke? {
		if (keyStroke == null) {
			return null
		} else if (keyStroke.isAltDown && keyStroke.keyType === KeyType.ArrowLeft) {
			if (viewportTopLeft!!.column > 0) {
				viewportTopLeft = viewportTopLeft!!.withRelativeColumn(-1)
				refresh()
				return null
			}
		} else if (keyStroke.isAltDown && keyStroke.keyType === KeyType.ArrowRight) {
			if (viewportTopLeft!!.column + viewportSize!!.columns < terminalSize!!.columns) {
				viewportTopLeft = viewportTopLeft!!.withRelativeColumn(1)
				refresh()
				return null
			}
		} else if (keyStroke.isAltDown && keyStroke.keyType === KeyType.ArrowUp) {
			if (viewportTopLeft!!.row > 0) {
				viewportTopLeft = viewportTopLeft!!.withRelativeRow(-1)
				realScreen.scrollLines(0, viewportSize!!.rows - 1, -1)
				refresh()
				return null
			}
		} else if (keyStroke.isAltDown && keyStroke.keyType === KeyType.ArrowDown) {
			if (viewportTopLeft!!.row + viewportSize!!.rows < terminalSize!!.rows) {
				viewportTopLeft = viewportTopLeft!!.withRelativeRow(1)
				realScreen.scrollLines(0, viewportSize!!.rows - 1, 1)
				refresh()
				return null
			}
		}
		return keyStroke
	}

	override fun scrollLines(firstLine: Int, lastLine: Int, distance: Int) {
		var firstLine = firstLine
		var lastLine = lastLine
		// do base class stuff (scroll own back buffer)
		super.scrollLines(firstLine, lastLine, distance)
		// vertical range visible in realScreen:
		val vpFirst = viewportTopLeft!!.row
		val vpRows = viewportSize!!.rows
		// adapt to realScreen range:
		firstLine = Math.max(0, firstLine - vpFirst)
		lastLine = Math.min(vpRows - 1, lastLine - vpFirst)
		// if resulting range non-empty: scroll that range in realScreen:
		if (firstLine <= lastLine) {
			realScreen.scrollLines(firstLine, lastLine, distance)
		}
	}

	/**
	 * Interface for rendering the virtual screen's frame when the real terminal is too small for the virtual screen
	 */
	interface FrameRenderer {

		/**
		 * Where in the virtual screen should the top-left position of the viewport be? To draw the viewport from the
		 * top-left position of the screen, return 0x0 (or TerminalPosition.TOP_LEFT_CORNER) here.
		 * @return Position of the top-left corner of the viewport inside the screen
		 */
		val viewportOffset: TerminalPosition

		/**
		 * Given the size of the real terminal and the current size of the virtual screen, how large should the viewport
		 * where the screen content is drawn be?
		 * @param realSize Size of the real terminal
		 * @param virtualSize Size of the virtual screen
		 * @return Size of the viewport, according to this FrameRenderer
		 */
		fun getViewportSize(realSize: TerminalSize, virtualSize: TerminalSize): TerminalSize

		/**
		 * Drawn the 'frame', meaning anything that is outside the viewport (title, scrollbar, etc)
		 * @param graphics Graphics to use to text drawing operations
		 * @param realSize Size of the real terminal
		 * @param virtualSize Size of the virtual screen
		 * @param virtualScrollPosition If the virtual screen is larger than the real terminal, this is the current
		 * scroll offset the VirtualScreen is using
		 */
		fun drawFrame(
			graphics: TextGraphics,
			realSize: TerminalSize,
			virtualSize: TerminalSize?,
			virtualScrollPosition: TerminalPosition?)
	}

	private class DefaultFrameRenderer : FrameRenderer {

		override val viewportOffset: TerminalPosition
			get() = TerminalPosition.TOP_LEFT_CORNER

		override fun getViewportSize(realSize: TerminalSize, virtualSize: TerminalSize?): TerminalSize {
			return if (realSize.columns > 1 && realSize.rows > 2) {
				realSize.withRelativeColumns(-1).withRelativeRows(-2)
			} else {
				realSize
			}
		}

		override fun drawFrame(
			graphics: TextGraphics,
			realSize: TerminalSize,
			virtualSize: TerminalSize?,
			virtualScrollPosition: TerminalPosition?) {

			if (realSize.columns == 1 || realSize.rows <= 2) {
				return
			}
			val viewportSize = getViewportSize(realSize, virtualSize)

			graphics.foregroundColor = TextColor.ANSI.WHITE
			graphics.backgroundColor = TextColor.ANSI.BLACK
			graphics.fill(' ')
			graphics.putString(0, graphics.size.rows - 1, "Terminal too small, use ALT+arrows to scroll")

			val horizontalSize = (viewportSize.columns.toDouble() / virtualSize!!.columns.toDouble() * viewportSize.columns).toInt()
			var scrollable = viewportSize.columns - horizontalSize - 1
			val horizontalPosition = (scrollable.toDouble() * (virtualScrollPosition!!.column.toDouble() / (virtualSize.columns - viewportSize.columns).toDouble())).toInt()
			graphics.drawLine(
				TerminalPosition(horizontalPosition, graphics.size.rows - 2),
				TerminalPosition(horizontalPosition + horizontalSize, graphics.size.rows - 2),
				Symbols.BLOCK_MIDDLE)

			val verticalSize = (viewportSize.rows.toDouble() / virtualSize.rows.toDouble() * viewportSize.rows).toInt()
			scrollable = viewportSize.rows - verticalSize - 1
			val verticalPosition = (scrollable.toDouble() * (virtualScrollPosition.row.toDouble() / (virtualSize.rows - viewportSize.rows).toDouble())).toInt()
			graphics.drawLine(
				TerminalPosition(graphics.size.columns - 1, verticalPosition),
				TerminalPosition(graphics.size.columns - 1, verticalPosition + verticalSize),
				Symbols.BLOCK_MIDDLE)
		}
	}
}
