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
import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.TextColor
import com.googlecode.lanterna.graphics.TextGraphics
import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.terminal.IOSafeTerminal
import com.googlecode.lanterna.terminal.TerminalResizeListener

import javax.swing.*
import java.awt.*
import java.util.concurrent.TimeUnit

/**
 * This class provides an Swing implementation of the [com.googlecode.lanterna.terminal.Terminal] interface that
 * is an embeddable component you can put into a Swing container. The class has static helper methods for opening a new
 * frame with a [SwingTerminal] as its content, similar to how the SwingTerminal used to work in earlier versions
 * of lanterna. This version supports private mode and non-private mode with a scrollback history. You can customize
 * many of the properties by supplying device configuration, font configuration and color configuration when you
 * construct the object.
 * @author martin
 */
class SwingTerminal
/**
 * Creates a new SwingTerminal component using custom settings and a custom scroll controller. The scrolling
 * controller will be notified when the terminal's history size grows and will be called when this class needs to
 * figure out the current scrolling position.
 * @param initialTerminalSize Initial size of the terminal, which will be used when calculating the preferred size
 * of the component. If null, it will default to 80x25. If the AWT layout manager forces
 * the component to a different size, the value of this parameter won't have any meaning
 * @param deviceConfiguration Device configuration to use for this SwingTerminal
 * @param fontConfiguration Font configuration to use for this SwingTerminal
 * @param colorConfiguration Color configuration to use for this SwingTerminal
 * @param scrollController Controller to use for scrolling, the object passed in will be notified whenever the
 * scrollable area has changed
 */
@JvmOverloads constructor(
	initialTerminalSize: TerminalSize?,
	deviceConfiguration: TerminalEmulatorDeviceConfiguration?,
	fontConfiguration: SwingTerminalFontConfiguration?,
	colorConfiguration: TerminalEmulatorColorConfiguration?,
	scrollController: TerminalScrollController = TerminalScrollController.Null()) : JComponent(), IOSafeTerminal {

	private val terminalImplementation: SwingTerminalImplementation

	override var cursorPosition: TerminalPosition
		get() = terminalImplementation.cursorPosition
		set(position) {
			terminalImplementation.cursorPosition = position
		}

	override val terminalSize: TerminalSize
		get() = terminalImplementation.terminalSize

	/**
	 * Creates a new SwingTerminal with all the defaults set and no scroll controller connected.
	 */
	constructor() : this(TerminalScrollController.Null()) {}


	/**
	 * Creates a new SwingTerminal with a particular scrolling controller that will be notified when the terminals
	 * history size grows and will be called when this class needs to figure out the current scrolling position.
	 * @param scrollController Controller for scrolling the terminal history
	 */
	constructor(scrollController: TerminalScrollController) : this(TerminalEmulatorDeviceConfiguration.default,
		SwingTerminalFontConfiguration.default,
		TerminalEmulatorColorConfiguration.default,
		scrollController) {
	}

	/**
	 * Creates a new SwingTerminal component using custom settings and no scroll controller.
	 * @param deviceConfiguration Device configuration to use for this SwingTerminal
	 * @param fontConfiguration Font configuration to use for this SwingTerminal
	 * @param colorConfiguration Color configuration to use for this SwingTerminal
	 */
	constructor(
		deviceConfiguration: TerminalEmulatorDeviceConfiguration,
		fontConfiguration: SwingTerminalFontConfiguration,
		colorConfiguration: TerminalEmulatorColorConfiguration) : this(null, deviceConfiguration, fontConfiguration, colorConfiguration) {
	}

	/**
	 * Creates a new SwingTerminal component using custom settings and a custom scroll controller. The scrolling
	 * controller will be notified when the terminal's history size grows and will be called when this class needs to
	 * figure out the current scrolling position.
	 * @param deviceConfiguration Device configuration to use for this SwingTerminal
	 * @param fontConfiguration Font configuration to use for this SwingTerminal
	 * @param colorConfiguration Color configuration to use for this SwingTerminal
	 * @param scrollController Controller to use for scrolling, the object passed in will be notified whenever the
	 * scrollable area has changed
	 */
	constructor(
		deviceConfiguration: TerminalEmulatorDeviceConfiguration,
		fontConfiguration: SwingTerminalFontConfiguration,
		colorConfiguration: TerminalEmulatorColorConfiguration,
		scrollController: TerminalScrollController) : this(null, deviceConfiguration, fontConfiguration, colorConfiguration, scrollController) {
	}


	init {
		var deviceConfiguration = deviceConfiguration
		var fontConfiguration = fontConfiguration
		var colorConfiguration = colorConfiguration

		//Enforce valid values on the input parameters
		if (deviceConfiguration == null) {
			deviceConfiguration = TerminalEmulatorDeviceConfiguration.default
		}
		if (fontConfiguration == null) {
			fontConfiguration = SwingTerminalFontConfiguration.default
		}
		if (colorConfiguration == null) {
			colorConfiguration = TerminalEmulatorColorConfiguration.default
		}

		terminalImplementation = SwingTerminalImplementation(
			this,
			fontConfiguration,
			initialTerminalSize,
			deviceConfiguration,
			colorConfiguration,
			scrollController)
	}

	/**
	 * Overridden method from Swing's `JComponent` class that returns the preferred size of the terminal (in
	 * pixels)
	 * @return The terminal's preferred size in pixels
	 */
	@Synchronized override fun getPreferredSize(): Dimension {
		return terminalImplementation.preferredSize
	}

	/**
	 * Overridden method from Swing's `JComponent` class that is called by OS window system when the component
	 * needs to be redrawn
	 * @param componentGraphics `Graphics` object to use when drawing the component
	 */
	@Synchronized override fun paintComponent(componentGraphics: Graphics) {
		terminalImplementation.paintComponent(componentGraphics)
	}

	/**
	 * Takes a KeyStroke and puts it on the input queue of the terminal emulator. This way you can insert synthetic
	 * input events to be processed as if they came from the user typing on the keyboard.
	 * @param keyStroke Key stroke input event to put on the queue
	 */
	fun addInput(keyStroke: KeyStroke) {
		terminalImplementation.addInput(keyStroke)
	}

	////////////////////////////////////////////////////////////////////////////////
	// Terminal methods below here, just forward to the implementation

	override fun enterPrivateMode() {
		terminalImplementation.enterPrivateMode()
	}

	override fun exitPrivateMode() {
		terminalImplementation.exitPrivateMode()
	}

	override fun clearScreen() {
		terminalImplementation.clearScreen()
	}

	override fun setCursorPosition(x: Int, y: Int) {
		terminalImplementation.setCursorPosition(x, y)
	}

	override fun setCursorVisible(visible: Boolean) {
		terminalImplementation.setCursorVisible(visible)
	}

	override fun putCharacter(c: Char) {
		terminalImplementation.putCharacter(c)
	}

	override fun enableSGR(sgr: SGR) {
		terminalImplementation.enableSGR(sgr)
	}

	override fun disableSGR(sgr: SGR) {
		terminalImplementation.disableSGR(sgr)
	}

	override fun resetColorAndSGR() {
		terminalImplementation.resetColorAndSGR()
	}

	override fun setForegroundColor(color: TextColor) {
		terminalImplementation.setForegroundColor(color)
	}

	override fun setBackgroundColor(color: TextColor) {
		terminalImplementation.setBackgroundColor(color)
	}

	override fun enquireTerminal(timeout: Int, timeoutUnit: TimeUnit): ByteArray {
		return terminalImplementation.enquireTerminal(timeout, timeoutUnit)
	}

	override fun bell() {
		terminalImplementation.bell()
	}

	override fun flush() {
		terminalImplementation.flush()
	}

	override fun close() {
		terminalImplementation.close()
	}

	override fun pollInput(): KeyStroke {
		return terminalImplementation.pollInput()
	}

	override fun readInput(): KeyStroke {
		return terminalImplementation.readInput()
	}

	override fun newTextGraphics(): TextGraphics {
		return terminalImplementation.newTextGraphics()
	}

	override fun addResizeListener(listener: TerminalResizeListener) {
		terminalImplementation.addResizeListener(listener)
	}

	override fun removeResizeListener(listener: TerminalResizeListener) {
		terminalImplementation.removeResizeListener(listener)
	}
}
/**
 * Creates a new SwingTerminal component using custom settings and no scroll controller.
 * @param initialTerminalSize Initial size of the terminal, which will be used when calculating the preferred size
 * of the component. If null, it will default to 80x25. If the AWT layout manager forces
 * the component to a different size, the value of this parameter won't have any meaning
 * @param deviceConfiguration Device configuration to use for this SwingTerminal
 * @param fontConfiguration Font configuration to use for this SwingTerminal
 * @param colorConfiguration Color configuration to use for this SwingTerminal
 */
