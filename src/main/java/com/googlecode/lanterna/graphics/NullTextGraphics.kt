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
package com.googlecode.lanterna.graphics

import com.googlecode.lanterna.*
import com.googlecode.lanterna.screen.TabBehaviour
import java.util.Arrays
import java.util.EnumSet

/**
 * TextGraphics implementation that does nothing, but has a pre-defined size
 * @author martin
 */
internal class NullTextGraphics
/**
 * Creates a new `NullTextGraphics` that will return the specified size value if asked how big it is but other
 * than that ignore all other calls.
 * @param size The size to report
 */
(override val size: TerminalSize) : TextGraphics {
	override var foregroundColor: TextColor? = null
		private set
	override var backgroundColor: TextColor? = null
		private set
	override var tabBehaviour: TabBehaviour? = null
		private set
	private val activeModifiers: EnumSet<SGR>

	init {
		this.foregroundColor = TextColor.ANSI.DEFAULT
		this.backgroundColor = TextColor.ANSI.DEFAULT
		this.tabBehaviour = TabBehaviour.ALIGN_TO_COLUMN_4
		this.activeModifiers = EnumSet.noneOf(SGR::class.java)
	}

	@Throws(IllegalArgumentException::class)
	override fun newTextGraphics(topLeftCorner: TerminalPosition, size: TerminalSize) =
		this

	override fun setBackgroundColor(backgroundColor: TextColor): TextGraphics {
		this.backgroundColor = backgroundColor
		return this
	}

	override fun setForegroundColor(foregroundColor: TextColor): TextGraphics {
		this.foregroundColor = foregroundColor
		return this
	}

	override fun enableModifiers(vararg modifiers: SGR): TextGraphics {
		activeModifiers.addAll(Arrays.asList(*modifiers))
		return this
	}

	override fun disableModifiers(vararg modifiers: SGR): TextGraphics {
		activeModifiers.removeAll(Arrays.asList(*modifiers))
		return this
	}

	override fun setModifiers(modifiers: EnumSet<SGR>): TextGraphics {
		clearModifiers()
		activeModifiers.addAll(modifiers)
		return this
	}

	override fun clearModifiers(): TextGraphics {
		activeModifiers.clear()
		return this
	}

	override fun getActiveModifiers() =
		EnumSet.copyOf(activeModifiers)

	override fun setTabBehaviour(tabBehaviour: TabBehaviour): TextGraphics {
		this.tabBehaviour = tabBehaviour
		return this
	}

	override fun fill(c: Char) =
		this

	override fun setCharacter(column: Int, row: Int, character: Char) =
		this

	override fun setCharacter(column: Int, row: Int, character: TextCharacter) =
		this

	override fun setCharacter(position: TerminalPosition, character: Char) =
		this

	override fun setCharacter(position: TerminalPosition, character: TextCharacter) =
		this

	override fun drawLine(fromPoint: TerminalPosition, toPoint: TerminalPosition, character: Char) =
		this

	override fun drawLine(fromPoint: TerminalPosition, toPoint: TerminalPosition, character: TextCharacter) =
		this

	override fun drawLine(fromX: Int, fromY: Int, toX: Int, toY: Int, character: Char) =
		this

	override fun drawLine(fromX: Int, fromY: Int, toX: Int, toY: Int, character: TextCharacter) =
		this

	override fun drawTriangle(p1: TerminalPosition, p2: TerminalPosition, p3: TerminalPosition, character: Char) =
		this

	override fun drawTriangle(p1: TerminalPosition, p2: TerminalPosition, p3: TerminalPosition, character: TextCharacter) =
		this

	override fun fillTriangle(p1: TerminalPosition, p2: TerminalPosition, p3: TerminalPosition, character: Char) =
		this

	override fun fillTriangle(p1: TerminalPosition, p2: TerminalPosition, p3: TerminalPosition, character: TextCharacter) =
		this

	override fun drawRectangle(topLeft: TerminalPosition, size: TerminalSize, character: Char) =
		this

	override fun drawRectangle(topLeft: TerminalPosition, size: TerminalSize, character: TextCharacter) =
		this

	override fun fillRectangle(topLeft: TerminalPosition, size: TerminalSize, character: Char) =
		this

	override fun fillRectangle(topLeft: TerminalPosition, size: TerminalSize, character: TextCharacter) =
		this

	override fun drawImage(topLeft: TerminalPosition, image: TextImage) =
		this

	override fun drawImage(topLeft: TerminalPosition, image: TextImage, sourceImageTopLeft: TerminalPosition, sourceImageSize: TerminalSize) =
		this

	override fun putString(column: Int, row: Int, string: String) =
		this

	override fun putString(position: TerminalPosition, string: String) =
		this

	override fun putString(column: Int, row: Int, string: String, extraModifier: SGR, vararg optionalExtraModifiers: SGR) =
		this

	override fun putString(position: TerminalPosition, string: String, extraModifier: SGR, vararg optionalExtraModifiers: SGR) =
		this

	override fun putString(column: Int, row: Int, string: String, extraModifiers: Collection<SGR>) =
		this

	override fun putCSIStyledString(column: Int, row: Int, string: String) =
		this

	override fun putCSIStyledString(position: TerminalPosition, string: String) =
		this

	override fun getCharacter(column: Int, row: Int) =
		null

	override fun getCharacter(position: TerminalPosition) =
		null

	override fun setStyleFrom(source: StyleSet<*>): TextGraphics {
		setBackgroundColor(source.backgroundColor)
		setForegroundColor(source.foregroundColor)
		setModifiers(source.activeModifiers)
		return this
	}

}
