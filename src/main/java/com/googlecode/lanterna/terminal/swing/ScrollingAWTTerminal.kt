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
package com.googlecode.lanterna.terminal.swing

import com.googlecode.lanterna.SGR
import com.googlecode.lanterna.TerminalPosition
import com.googlecode.lanterna.graphics.TextGraphics
import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.terminal.IOSafeTerminal
import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.TextColor
import com.googlecode.lanterna.terminal.TerminalResizeListener

import java.awt.*
import java.awt.event.AdjustmentEvent
import java.awt.event.AdjustmentListener
import java.util.concurrent.TimeUnit

/**
 * This is a AWT Container that carries an [AWTTerminal] with a scrollbar, effectively implementing a
 * pseudo-terminal with scrollback history. You can choose the same parameters are for [AWTTerminal], they are
 * forwarded, this class mostly deals with linking the [AWTTerminal] with the scrollbar and having them update
 * each other.
 * @author Martin
 */
class ScrollingAWTTerminal
/**
 * Creates a new `ScrollingAWTTerminal` with customizable settings.
 * @param deviceConfiguration How to configure the terminal virtual device
 * @param fontConfiguration What kind of fonts to use
 * @param colorConfiguration Which color schema to use for ANSI colors
 */
(
	deviceConfiguration: TerminalEmulatorDeviceConfiguration,
	fontConfiguration: SwingTerminalFontConfiguration,
	colorConfiguration: TerminalEmulatorColorConfiguration) : Container(), IOSafeTerminal {

	private val awtTerminal: AWTTerminal
	private val scrollBar: Scrollbar

	// Used to prevent unnecessary repaints (the component is re-adjusting the scrollbar as part of the repaint
	// operation, we don't need the scrollbar listener to trigger another repaint of the terminal when that happens
	@Volatile private var scrollModelUpdateBySystem: Boolean = false

	override var cursorPosition: TerminalPosition
		get() = awtTerminal.cursorPosition
		set(position) {
			awtTerminal.cursorPosition = position
		}

	override val terminalSize: TerminalSize
		get() = awtTerminal.terminalSize

	/**
	 * Creates a new `ScrollingAWTTerminal` with all default options
	 */
	constructor() : this(TerminalEmulatorDeviceConfiguration.default,
		SwingTerminalFontConfiguration.default,
		TerminalEmulatorColorConfiguration.default) {
	}

	init {

		this.scrollBar = Scrollbar(Scrollbar.VERTICAL)
		this.awtTerminal = AWTTerminal(
			deviceConfiguration,
			fontConfiguration,
			colorConfiguration,
			ScrollController())

		layout = BorderLayout()
		add(awtTerminal, BorderLayout.CENTER)
		add(scrollBar, BorderLayout.EAST)
		this.scrollBar.minimum = 0
		this.scrollBar.maximum = 20
		this.scrollBar.value = 0
		this.scrollBar.visibleAmount = 20
		this.scrollBar.addAdjustmentListener(ScrollbarListener())
		this.scrollModelUpdateBySystem = false
	}

	private inner class ScrollController : TerminalScrollController {
		override var scrollingOffset: Int = 0
			private set

		override fun updateModel(totalSize: Int, screenHeight: Int) {
			if (!EventQueue.isDispatchThread()) {
				EventQueue.invokeLater { updateModel(totalSize, screenHeight) }
				return
			}
			try {
				scrollModelUpdateBySystem = true
				var value = scrollBar.value
				var maximum = scrollBar.maximum
				var visibleAmount = scrollBar.visibleAmount

				if (maximum != totalSize) {
					val lastMaximum = maximum
					maximum = if (totalSize > screenHeight) totalSize else screenHeight
					if (lastMaximum < maximum && lastMaximum - visibleAmount - value == 0) {
						value = scrollBar.value + (maximum - lastMaximum)
					}
				}
				if (value + screenHeight > maximum) {
					value = maximum - screenHeight
				}
				if (visibleAmount != screenHeight) {
					if (visibleAmount > screenHeight) {
						value += visibleAmount - screenHeight
					}
					visibleAmount = screenHeight
				}
				if (value > maximum - visibleAmount) {
					value = maximum - visibleAmount
				}
				if (value < 0) {
					value = 0
				}

				this.scrollingOffset = value

				if (scrollBar.maximum != maximum) {
					scrollBar.maximum = maximum
				}
				if (scrollBar.visibleAmount != visibleAmount) {
					scrollBar.visibleAmount = visibleAmount
				}
				if (scrollBar.value != value) {
					scrollBar.value = value
				}
			} finally {
				scrollModelUpdateBySystem = false
			}
		}
	}

	private inner class ScrollbarListener : AdjustmentListener {
		@Synchronized override fun adjustmentValueChanged(e: AdjustmentEvent) {
			if (!scrollModelUpdateBySystem) {
				// Only repaint if this was the user adjusting the scrollbar
				awtTerminal.repaint()
			}
		}
	}

	/**
	 * Takes a KeyStroke and puts it on the input queue of the terminal emulator. This way you can insert synthetic
	 * input events to be processed as if they came from the user typing on the keyboard.
	 * @param keyStroke Key stroke input event to put on the queue
	 */
	fun addInput(keyStroke: KeyStroke) {
		awtTerminal.addInput(keyStroke)
	}

	///////////
	// Delegate all Terminal interface implementations to SwingTerminal
	///////////
	override fun pollInput() =
		awtTerminal.pollInput()

	override fun readInput() =
		awtTerminal.readInput()

	override fun enterPrivateMode() {
		awtTerminal.enterPrivateMode()
	}

	override fun exitPrivateMode() {
		awtTerminal.exitPrivateMode()
	}

	override fun clearScreen() {
		awtTerminal.clearScreen()
	}

	override fun setCursorPosition(x: Int, y: Int) {
		awtTerminal.setCursorPosition(x, y)
	}

	override fun setCursorVisible(visible: Boolean) {
		awtTerminal.setCursorVisible(visible)
	}

	override fun putCharacter(c: Char) {
		awtTerminal.putCharacter(c)
	}

	override fun newTextGraphics() =
		awtTerminal.newTextGraphics()

	override fun enableSGR(sgr: SGR) {
		awtTerminal.enableSGR(sgr)
	}

	override fun disableSGR(sgr: SGR) {
		awtTerminal.disableSGR(sgr)
	}

	override fun resetColorAndSGR() {
		awtTerminal.resetColorAndSGR()
	}

	override fun setForegroundColor(color: TextColor) {
		awtTerminal.setForegroundColor(color)
	}

	override fun setBackgroundColor(color: TextColor) {
		awtTerminal.setBackgroundColor(color)
	}

	override fun enquireTerminal(timeout: Int, timeoutUnit: TimeUnit) =
		awtTerminal.enquireTerminal(timeout, timeoutUnit)

	override fun bell() {
		awtTerminal.bell()
	}

	override fun flush() {
		awtTerminal.flush()
	}

	override fun close() {
		awtTerminal.close()
	}

	override fun addResizeListener(listener: TerminalResizeListener) {
		awtTerminal.addResizeListener(listener)
	}

	override fun removeResizeListener(listener: TerminalResizeListener) {
		awtTerminal.removeResizeListener(listener)
	}
}
