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
import com.googlecode.lanterna.input.KeyType
import com.googlecode.lanterna.terminal.IOSafeTerminal
import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.TextColor
import com.googlecode.lanterna.terminal.TerminalResizeListener

import java.awt.*
import java.util.Arrays
import java.util.EnumSet
import java.util.concurrent.TimeUnit
import javax.swing.*

/**
 * This class is similar to what SwingTerminal used to be before Lanterna 3.0; a JFrame that contains a terminal
 * emulator. In Lanterna 3, this class is just a JFrame containing a SwingTerminal component, but it also implements
 * the Terminal interface and delegates all calls to the internal SwingTerminal. You can tweak the class a bit to have
 * special behaviours when exiting private mode or when the user presses ESC key.
 * @author martin
 */
class SwingTerminalFrame private constructor(title: String?, private val swingTerminal: SwingTerminal, vararg autoCloseTriggers: TerminalEmulatorAutoCloseTrigger) : JFrame(title ?: "SwingTerminalFrame"), IOSafeTerminal {
	private val autoCloseTriggers: EnumSet<TerminalEmulatorAutoCloseTrigger>
	private var disposed: Boolean = false

	/**
	 * Returns the auto-close triggers used by the SwingTerminalFrame
	 * @return Current auto-close trigger
	 */
	val autoCloseTrigger: Set<TerminalEmulatorAutoCloseTrigger>
		get() = EnumSet.copyOf(autoCloseTriggers)

	override var cursorPosition: TerminalPosition
		get() = swingTerminal.cursorPosition
		set(position) {
			swingTerminal.cursorPosition = position
		}

	override val terminalSize: TerminalSize
		get() = swingTerminal.terminalSize

	/**
	 * Creates a new SwingTerminalFrame with an optional list of auto-close triggers
	 * @param autoCloseTriggers What to trigger automatic disposal of the JFrame
	 */
	constructor(vararg autoCloseTriggers: TerminalEmulatorAutoCloseTrigger) : this("SwingTerminalFrame", *autoCloseTriggers) {}

	/**
	 * Creates a new SwingTerminalFrame with a specific title and an optional list of auto-close triggers
	 * @param title Title to use for the window
	 * @param autoCloseTriggers What to trigger automatic disposal of the JFrame
	 */
	@Throws(HeadlessException::class)
	constructor(title: String, vararg autoCloseTriggers: TerminalEmulatorAutoCloseTrigger) : this(title, SwingTerminal(), *autoCloseTriggers) {
	}

	/**
	 * Creates a new SwingTerminalFrame using a specified title and a series of swing terminal configuration objects
	 * @param title What title to use for the window
	 * @param deviceConfiguration Device configuration for the embedded SwingTerminal
	 * @param fontConfiguration Font configuration for the embedded SwingTerminal
	 * @param colorConfiguration Color configuration for the embedded SwingTerminal
	 * @param autoCloseTriggers What to trigger automatic disposal of the JFrame
	 */
	constructor(title: String,
				deviceConfiguration: TerminalEmulatorDeviceConfiguration,
				fontConfiguration: SwingTerminalFontConfiguration,
				colorConfiguration: TerminalEmulatorColorConfiguration,
				vararg autoCloseTriggers: TerminalEmulatorAutoCloseTrigger) : this(title, null, deviceConfiguration, fontConfiguration, colorConfiguration, *autoCloseTriggers) {
	}

	/**
	 * Creates a new SwingTerminalFrame using a specified title and a series of swing terminal configuration objects
	 * @param title What title to use for the window
	 * @param terminalSize Initial size of the terminal, in rows and columns. If null, it will default to 80x25.
	 * @param deviceConfiguration Device configuration for the embedded SwingTerminal
	 * @param fontConfiguration Font configuration for the embedded SwingTerminal
	 * @param colorConfiguration Color configuration for the embedded SwingTerminal
	 * @param autoCloseTriggers What to trigger automatic disposal of the JFrame
	 */
	constructor(title: String,
				terminalSize: TerminalSize?,
				deviceConfiguration: TerminalEmulatorDeviceConfiguration,
				fontConfiguration: SwingTerminalFontConfiguration,
				colorConfiguration: TerminalEmulatorColorConfiguration,
				vararg autoCloseTriggers: TerminalEmulatorAutoCloseTrigger) : this(title,
		SwingTerminal(terminalSize, deviceConfiguration, fontConfiguration, colorConfiguration),
		*autoCloseTriggers) {
	}

	init {
		this.autoCloseTriggers = EnumSet.copyOf(Arrays.asList(*autoCloseTriggers))
		this.disposed = false

		contentPane.layout = BorderLayout()
		contentPane.add(swingTerminal, BorderLayout.CENTER)
		defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
		background = Color.BLACK //This will reduce white flicker when resizing the window
		pack()

		//Put input focus on the terminal component by default
		swingTerminal.requestFocusInWindow()
	}

	/**
	 * Sets the auto-close trigger to use on this terminal. This will reset any previous triggers. If called with
	 * `null`, all triggers are cleared.
	 * @param autoCloseTrigger Auto-close trigger to use on this terminal, or `null` to clear all existing triggers
	 * @return Itself
	 */
	fun setAutoCloseTrigger(autoCloseTrigger: TerminalEmulatorAutoCloseTrigger?): SwingTerminalFrame {
		this.autoCloseTriggers.clear()
		if (autoCloseTrigger != null) {
			this.autoCloseTriggers.add(autoCloseTrigger)
		}
		return this
	}

	/**
	 * Adds an auto-close trigger to use on this terminal.
	 * @param autoCloseTrigger Auto-close trigger to add to this terminal
	 * @return Itself
	 */
	fun addAutoCloseTrigger(autoCloseTrigger: TerminalEmulatorAutoCloseTrigger?): SwingTerminalFrame {
		if (autoCloseTrigger != null) {
			this.autoCloseTriggers.add(autoCloseTrigger)
		}
		return this
	}

	override fun dispose() {
		super.dispose()
		disposed = true
	}

	override fun close() {
		dispose()
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
		if (disposed) {
			return KeyStroke(KeyType.EOF)
		}
		val keyStroke = swingTerminal.pollInput()
		if (autoCloseTriggers.contains(TerminalEmulatorAutoCloseTrigger.CloseOnEscape) &&
			keyStroke != null &&
			keyStroke.keyType === KeyType.Escape) {
			dispose()
		}
		return keyStroke
	}

	override fun readInput(): KeyStroke {
		return swingTerminal.readInput()
	}

	override fun enterPrivateMode() {
		swingTerminal.enterPrivateMode()
	}

	override fun exitPrivateMode() {
		swingTerminal.exitPrivateMode()
		if (autoCloseTriggers.contains(TerminalEmulatorAutoCloseTrigger.CloseOnExitPrivateMode)) {
			dispose()
		}
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

	override fun addResizeListener(listener: TerminalResizeListener) {
		swingTerminal.addResizeListener(listener)
	}

	override fun removeResizeListener(listener: TerminalResizeListener) {
		swingTerminal.removeResizeListener(listener)
	}
}
