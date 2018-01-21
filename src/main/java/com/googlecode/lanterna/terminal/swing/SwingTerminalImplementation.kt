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

import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.TextCharacter

import javax.swing.*
import java.awt.*
import java.awt.event.*
import java.util.Collections

/**
 * Concrete implementation of [GraphicalTerminalImplementation] that adapts it to Swing
 */
internal class SwingTerminalImplementation
/**
 * Creates a new `SwingTerminalImplementation`
 * @param component JComponent that is the Swing terminal surface
 * @param fontConfiguration Font configuration to use
 * @param initialTerminalSize Initial size of the terminal
 * @param deviceConfiguration Device configuration
 * @param colorConfiguration Color configuration
 * @param scrollController Controller to be used when inspecting scroll status
 */
(
	private val component: JComponent,
	/**
	 * Returns the current font configuration. Note that it is immutable and cannot be changed.
	 * @return This SwingTerminal's current font configuration
	 */
	val fontConfiguration: SwingTerminalFontConfiguration,
	initialTerminalSize: TerminalSize,
	deviceConfiguration: TerminalEmulatorDeviceConfiguration,
	colorConfiguration: TerminalEmulatorColorConfiguration,
	scrollController: TerminalScrollController) : GraphicalTerminalImplementation(initialTerminalSize, deviceConfiguration, colorConfiguration, scrollController) {

	protected override val fontHeight: Int
		get() = fontConfiguration.fontHeight

	protected override val fontWidth: Int
		get() = fontConfiguration.fontWidth

	protected override val height: Int
		get() = component.height

	protected override val width: Int
		get() = component.width

	protected override val isTextAntiAliased: Boolean
		get() = fontConfiguration.isAntiAliased

	init {

		//Prevent us from shrinking beyond one character
		component.minimumSize = Dimension(fontConfiguration.fontWidth, fontConfiguration.fontHeight)


		component.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, emptySet<AWTKeyStroke>())

		component.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, emptySet<AWTKeyStroke>())

		//Make sure the component is double-buffered to prevent flickering
		component.isDoubleBuffered = true

		component.addKeyListener(GraphicalTerminalImplementation.TerminalInputListener())
		component.addMouseListener(object : GraphicalTerminalImplementation.TerminalMouseListener() {
			override fun mouseClicked(e: MouseEvent?) {
				super.mouseClicked(e)
				this@SwingTerminalImplementation.component.requestFocusInWindow()
			}
		})
		component.addHierarchyListener { e ->
			if (e.changeFlags == HierarchyEvent.DISPLAYABILITY_CHANGED.toLong()) {
				if (e.changed.isDisplayable) {
					onCreated()
				} else {
					onDestroyed()
				}
			}
		}
	}

	override fun getFontForCharacter(character: TextCharacter) =
		fontConfiguration.getFontForCharacter(character)

	override fun repaint() {
		if (SwingUtilities.isEventDispatchThread()) {
			component.repaint()
		} else {
			SwingUtilities.invokeLater { component.repaint() }
		}
	}

	override fun readInput(): com.googlecode.lanterna.input.KeyStroke {
		if (SwingUtilities.isEventDispatchThread()) {
			throw UnsupportedOperationException("Cannot call SwingTerminal.readInput() on the AWT thread")
		}
		return super.readInput()
	}
}
