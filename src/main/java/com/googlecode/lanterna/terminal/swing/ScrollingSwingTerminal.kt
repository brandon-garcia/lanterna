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
import javax.swing.*

/**
 * This is a Swing JComponent that carries a [SwingTerminal] with a scrollbar, effectively implementing a
 * pseudo-terminal with scrollback history. You can choose the same parameters are for [SwingTerminal], they are
 * forwarded, this class mostly deals with linking the [SwingTerminal] with the scrollbar and having them update
 * each other.
 * @author Martin
 */
class ScrollingSwingTerminal
/**
 * Creates a new `ScrollingSwingTerminal` with customizable settings.
 * @param deviceConfiguration How to configure the terminal virtual device
 * @param fontConfiguration What kind of fonts to use
 * @param colorConfiguration Which color schema to use for ANSI colors
 */
(
	deviceConfiguration: TerminalEmulatorDeviceConfiguration,
	fontConfiguration: SwingTerminalFontConfiguration,
	colorConfiguration: TerminalEmulatorColorConfiguration) : JComponent(), IOSafeTerminal {

	private val swingTerminal: SwingTerminal
	private val scrollBar: JScrollBar

	// Used to prevent unnecessary repaints (the component is re-adjusting the scrollbar as part of the repaint
	// operation, we don't need the scrollbar listener to trigger another repaint of the terminal when that happens
	@Volatile private var scrollModelUpdateBySystem: Boolean = false

	override var cursorPosition: TerminalPosition
		get() = swingTerminal.cursorPosition
		set(position) {
			swingTerminal.cursorPosition = position
		}

	override val terminalSize: TerminalSize
		get() = swingTerminal.terminalSize

	/**
	 * Creates a new `ScrollingSwingTerminal` with all default options
	 */
	constructor() : this(TerminalEmulatorDeviceConfiguration.default,
		SwingTerminalFontConfiguration.default,
		TerminalEmulatorColorConfiguration.default) {
	}

	init {

		this.scrollBar = JScrollBar(JScrollBar.VERTICAL)
		this.swingTerminal = SwingTerminal(
			deviceConfiguration,
			fontConfiguration,
			colorConfiguration,
			ScrollController())

		layout = BorderLayout()
		add(swingTerminal, BorderLayout.CENTER)
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
			if (!SwingUtilities.isEventDispatchThread()) {
				SwingUtilities.invokeLater { updateModel(totalSize, screenHeight) }
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
				swingTerminal.repaint()
			}
		}
	}

	/**
	 * Takes a KeyStroke and puts it on the input queue of the terminal emulator. This way you can insert synthetic
	 * input events to be processed as if they came from the user typing on the keyboard.
	 * @param keyStroke Key stroke input event to put on the queue
	 */
	fun addInput(keyStroke: KeyStroke) {
		swingTerminal.addInput(keyStroke)
	}

	///////////
	// Delegate all Terminal interface implementations to SwingTerminal
	///////////
	override fun pollInput(): KeyStroke {
		return swingTerminal.pollInput()
	}

	override fun readInput(): KeyStroke {
		return swingTerminal.readInput()
	}

	override fun enterPrivateMode() {
		swingTerminal.enterPrivateMode()
	}

	override fun exitPrivateMode() {
		swingTerminal.exitPrivateMode()
	}

	override fun clearScreen() {
		swingTerminal.clearScreen()
	}

	override fun setCursorPosition(x: Int, y: Int) {
		swingTerminal.setCursorPosition(x, y)
	}

	override fun setCursorVisible(visible: Boolean) {
		swingTerminal.setCursorVisible(visible)
	}

	override fun putCharacter(c: Char) {
		swingTerminal.putCharacter(c)
	}

	override fun newTextGraphics(): TextGraphics {
		return swingTerminal.newTextGraphics()
	}

	override fun enableSGR(sgr: SGR) {
		swingTerminal.enableSGR(sgr)
	}

	override fun disableSGR(sgr: SGR) {
		swingTerminal.disableSGR(sgr)
	}

	override fun resetColorAndSGR() {
		swingTerminal.resetColorAndSGR()
	}

	override fun setForegroundColor(color: TextColor) {
		swingTerminal.setForegroundColor(color)
	}

	override fun setBackgroundColor(color: TextColor) {
		swingTerminal.setBackgroundColor(color)
	}

	override fun enquireTerminal(timeout: Int, timeoutUnit: TimeUnit): ByteArray {
		return swingTerminal.enquireTerminal(timeout, timeoutUnit)
	}

	override fun bell() {
		swingTerminal.bell()
	}

	override fun flush() {
		swingTerminal.flush()
	}

	override fun close() {
		swingTerminal.close()
	}

	override fun addResizeListener(listener: TerminalResizeListener) {
		swingTerminal.addResizeListener(listener)
	}

	override fun removeResizeListener(listener: TerminalResizeListener) {
		swingTerminal.removeResizeListener(listener)
	}
}
