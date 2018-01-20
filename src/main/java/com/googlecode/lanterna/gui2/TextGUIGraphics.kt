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
package com.googlecode.lanterna.gui2

import com.googlecode.lanterna.*
import com.googlecode.lanterna.graphics.*
import com.googlecode.lanterna.screen.TabBehaviour
import java.util.EnumSet

/**
 * TextGraphics implementation used by TextGUI when doing any drawing operation.
 * @author Martin
 */
interface TextGUIGraphics : ThemedTextGraphics, TextGraphics {
	/**
	 * Returns the `TextGUI` this `TextGUIGraphics` belongs to
	 * @return `TextGUI` this `TextGUIGraphics` belongs to
	 */
	val textGUI: TextGUI

	@Throws(IllegalArgumentException::class)
	override fun newTextGraphics(topLeftCorner: TerminalPosition, size: TerminalSize): TextGUIGraphics

	override fun applyThemeStyle(themeStyle: ThemeStyle): TextGUIGraphics

	override fun setBackgroundColor(backgroundColor: TextColor): TextGUIGraphics

	override fun setForegroundColor(foregroundColor: TextColor): TextGUIGraphics

	override fun enableModifiers(vararg modifiers: SGR): TextGUIGraphics

	override fun disableModifiers(vararg modifiers: SGR): TextGUIGraphics

	override fun setModifiers(modifiers: EnumSet<SGR>): TextGUIGraphics

	override fun clearModifiers(): TextGUIGraphics

	override fun setTabBehaviour(tabBehaviour: TabBehaviour): TextGUIGraphics

	override fun fill(c: Char): TextGUIGraphics

	override fun fillRectangle(topLeft: TerminalPosition, size: TerminalSize, character: Char): TextGUIGraphics

	override fun fillRectangle(topLeft: TerminalPosition, size: TerminalSize, character: TextCharacter): TextGUIGraphics

	override fun drawRectangle(topLeft: TerminalPosition, size: TerminalSize, character: Char): TextGUIGraphics

	override fun drawRectangle(topLeft: TerminalPosition, size: TerminalSize, character: TextCharacter): TextGUIGraphics

	override fun fillTriangle(p1: TerminalPosition, p2: TerminalPosition, p3: TerminalPosition, character: Char): TextGUIGraphics

	override fun fillTriangle(p1: TerminalPosition, p2: TerminalPosition, p3: TerminalPosition, character: TextCharacter): TextGUIGraphics

	override fun drawTriangle(p1: TerminalPosition, p2: TerminalPosition, p3: TerminalPosition, character: Char): TextGUIGraphics

	override fun drawTriangle(p1: TerminalPosition, p2: TerminalPosition, p3: TerminalPosition, character: TextCharacter): TextGUIGraphics

	override fun drawLine(fromPoint: TerminalPosition, toPoint: TerminalPosition, character: Char): TextGUIGraphics

	override fun drawLine(fromPoint: TerminalPosition, toPoint: TerminalPosition, character: TextCharacter): TextGUIGraphics

	override fun drawLine(fromX: Int, fromY: Int, toX: Int, toY: Int, character: Char): TextGUIGraphics

	override fun drawLine(fromX: Int, fromY: Int, toX: Int, toY: Int, character: TextCharacter): TextGUIGraphics

	override fun drawImage(topLeft: TerminalPosition, image: TextImage): TextGUIGraphics

	override fun drawImage(topLeft: TerminalPosition, image: TextImage, sourceImageTopLeft: TerminalPosition, sourceImageSize: TerminalSize): TextGUIGraphics

	override fun setCharacter(position: TerminalPosition, character: Char): TextGUIGraphics

	override fun setCharacter(position: TerminalPosition, character: TextCharacter): TextGUIGraphics

	override fun setCharacter(column: Int, row: Int, character: Char): TextGUIGraphics

	override fun setCharacter(column: Int, row: Int, character: TextCharacter): TextGUIGraphics

	override fun putString(column: Int, row: Int, string: String): TextGUIGraphics

	override fun putString(position: TerminalPosition, string: String): TextGUIGraphics

	override fun putString(column: Int, row: Int, string: String, extraModifier: SGR, vararg optionalExtraModifiers: SGR): TextGUIGraphics

	override fun putString(position: TerminalPosition, string: String, extraModifier: SGR, vararg optionalExtraModifiers: SGR): TextGUIGraphics

	fun putString(column: Int, row: Int, string: String, extraModifiers: Collection<SGR>): TextGUIGraphics

	override fun putCSIStyledString(column: Int, row: Int, string: String): TextGUIGraphics

	override fun putCSIStyledString(position: TerminalPosition, string: String): TextGUIGraphics

	override fun setStyleFrom(source: StyleSet<*>): TextGUIGraphics

}
