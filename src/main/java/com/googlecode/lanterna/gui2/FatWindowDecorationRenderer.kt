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

import com.googlecode.lanterna.Symbols
import com.googlecode.lanterna.TerminalPosition
import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.TerminalTextUtils
import com.googlecode.lanterna.graphics.ThemeDefinition

/**
 *
 */
class FatWindowDecorationRenderer : WindowDecorationRenderer {
	override fun draw(textGUI: WindowBasedTextGUI, graphics: TextGUIGraphics, window: Window): TextGUIGraphics {
		var title: String? = window.title
		if (title == null) {
			title = ""
		}
		val hasTitle = !title.trim { it <= ' ' }.isEmpty()
		if (hasTitle) {
			title = " " + title.trim { it <= ' ' } + " "
		}

		val themeDefinition = window.theme.getDefinition(FatWindowDecorationRenderer::class.java!!)
		val horizontalLine = themeDefinition.getCharacter("HORIZONTAL_LINE", Symbols.SINGLE_LINE_HORIZONTAL)
		val verticalLine = themeDefinition.getCharacter("VERTICAL_LINE", Symbols.SINGLE_LINE_VERTICAL)
		val bottomLeftCorner = themeDefinition.getCharacter("BOTTOM_LEFT_CORNER", Symbols.SINGLE_LINE_BOTTOM_LEFT_CORNER)
		val topLeftCorner = themeDefinition.getCharacter("TOP_LEFT_CORNER", Symbols.SINGLE_LINE_TOP_LEFT_CORNER)
		val bottomRightCorner = themeDefinition.getCharacter("BOTTOM_RIGHT_CORNER", Symbols.SINGLE_LINE_BOTTOM_RIGHT_CORNER)
		val topRightCorner = themeDefinition.getCharacter("TOP_RIGHT_CORNER", Symbols.SINGLE_LINE_TOP_RIGHT_CORNER)
		val leftJunction = themeDefinition.getCharacter("LEFT_JUNCTION", Symbols.SINGLE_LINE_T_RIGHT)
		val rightJunction = themeDefinition.getCharacter("RIGHT_JUNCTION", Symbols.SINGLE_LINE_T_LEFT)
		val drawableArea = graphics.size

		if (hasTitle) {
			graphics.applyThemeStyle(themeDefinition.preLight)
			graphics.drawLine(0, drawableArea.rows - 2, 0, 1, verticalLine)
			graphics.drawLine(1, 0, drawableArea.columns - 2, 0, horizontalLine)
			graphics.drawLine(1, 2, drawableArea.columns - 2, 2, horizontalLine)
			graphics.setCharacter(0, 0, topLeftCorner)
			graphics.setCharacter(0, 2, leftJunction)
			graphics.setCharacter(0, drawableArea.rows - 1, bottomLeftCorner)

			graphics.applyThemeStyle(themeDefinition.normal)
			graphics.drawLine(
				drawableArea.columns - 1, 1,
				drawableArea.columns - 1, drawableArea.rows - 2,
				verticalLine)
			graphics.drawLine(
				1, drawableArea.rows - 1,
				drawableArea.columns - 2, drawableArea.rows - 1,
				horizontalLine)

			graphics.setCharacter(drawableArea.columns - 1, 0, topRightCorner)
			graphics.setCharacter(drawableArea.columns - 1, 2, rightJunction)
			graphics.setCharacter(drawableArea.columns - 1, drawableArea.rows - 1, bottomRightCorner)

			graphics.applyThemeStyle(themeDefinition.active)
			graphics.drawLine(1, 1, drawableArea.columns - 2, 1, ' ')
			graphics.putString(1, 1, TerminalTextUtils.fitString(title, drawableArea.columns - 3))

			return graphics.newTextGraphics(OFFSET_WITH_TITLE, graphics.size.withRelativeColumns(-2).withRelativeRows(-4))
		} else {
			graphics.applyThemeStyle(themeDefinition.preLight)
			graphics.drawLine(0, drawableArea.rows - 2, 0, 1, verticalLine)
			graphics.drawLine(1, 0, drawableArea.columns - 2, 0, horizontalLine)
			graphics.setCharacter(0, 0, topLeftCorner)
			graphics.setCharacter(0, drawableArea.rows - 1, bottomLeftCorner)

			graphics.applyThemeStyle(themeDefinition.normal)
			graphics.drawLine(
				drawableArea.columns - 1, 1,
				drawableArea.columns - 1, drawableArea.rows - 2,
				verticalLine)
			graphics.drawLine(
				1, drawableArea.rows - 1,
				drawableArea.columns - 2, drawableArea.rows - 1,
				horizontalLine)

			graphics.setCharacter(drawableArea.columns - 1, 0, topRightCorner)
			graphics.setCharacter(drawableArea.columns - 1, drawableArea.rows - 1, bottomRightCorner)

			return graphics.newTextGraphics(OFFSET_WITHOUT_TITLE, graphics.size.withRelativeColumns(-2).withRelativeRows(-2))
		}
	}

	override fun getDecoratedSize(window: Window, contentAreaSize: TerminalSize) =
		if (hasTitle(window)) {
			contentAreaSize
				.withRelativeColumns(2)
				.withRelativeRows(4)
				.max(TerminalSize(TerminalTextUtils.getColumnWidth(window.title) + 4, 1))  //Make sure the title fits!
		} else {
			contentAreaSize
				.withRelativeColumns(2)
				.withRelativeRows(2)
				.max(TerminalSize(3, 1))
		}

	override fun getOffset(window: Window) =
		if (hasTitle(window)) {
			OFFSET_WITH_TITLE
		} else {
			OFFSET_WITHOUT_TITLE
		}

	private fun hasTitle(window: Window) =
		!(window.title == null || window.title.trim { it <= ' ' }.isEmpty())

	companion object {

		private val OFFSET_WITH_TITLE = TerminalPosition(1, 3)
		private val OFFSET_WITHOUT_TITLE = TerminalPosition(1, 1)
	}
}
