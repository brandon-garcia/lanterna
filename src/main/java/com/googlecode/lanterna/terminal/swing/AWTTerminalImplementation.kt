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
import com.googlecode.lanterna.input.KeyStroke

import java.awt.*
import java.awt.event.*
import java.util.Collections

/**
 * AWT implementation of [GraphicalTerminalImplementation] that contains all the overrides for AWT
 * Created by martin on 08/02/16.
 */
internal class AWTTerminalImplementation
/**
 * Creates a new `AWTTerminalImplementation`
 * @param component Component that is the AWT terminal surface
 * @param fontConfiguration Font configuration to use
 * @param initialTerminalSize Initial size of the terminal
 * @param deviceConfiguration Device configuration
 * @param colorConfiguration Color configuration
 * @param scrollController Controller to be used when inspecting scroll status
 */
(
	private val component: Component,
	private val fontConfiguration: AWTTerminalFontConfiguration,
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

		component.addKeyListener(GraphicalTerminalImplementation.TerminalInputListener())
		component.addMouseListener(object : GraphicalTerminalImplementation.TerminalMouseListener() {
			override fun mouseClicked(e: MouseEvent?) {
				super.mouseClicked(e)
				this@AWTTerminalImplementation.component.requestFocusInWindow()
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

	override fun getFontForCharacter(character: TextCharacter): Font {
		return fontConfiguration.getFontForCharacter(character)
	}

	override fun repaint() {
		if (EventQueue.isDispatchThread()) {
			component.repaint()
		} else {
			EventQueue.invokeLater { component.repaint() }
		}
	}

	override fun readInput(): KeyStroke {
		if (EventQueue.isDispatchThread()) {
			throw UnsupportedOperationException("Cannot call SwingTerminal.readInput() on the AWT thread")
		}
		return super.readInput()
	}
}
