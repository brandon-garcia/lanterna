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
import com.googlecode.lanterna.graphics.ThemeDefinition
import com.googlecode.lanterna.graphics.ThemedTextGraphics

/**
 * This WindowPostRenderer implementation draws a shadow under the window
 *
 * @author Martin
 */
class WindowShadowRenderer : WindowPostRenderer {
	override fun postRender(
		textGraphics: ThemedTextGraphics,
		textGUI: TextGUI,
		window: Window) {

		val windowPosition = window.position
		val decoratedWindowSize = window.decoratedSize
		val themeDefinition = window.theme.getDefinition(WindowShadowRenderer::class.java!!)
		textGraphics.applyThemeStyle(themeDefinition.normal)
		val filler = themeDefinition.getCharacter("FILLER", Symbols.BLOCK_SOLID)
		val useDoubleWidth = themeDefinition.getBooleanProperty("DOUBLE_WIDTH", true)
		val useTransparency = themeDefinition.getBooleanProperty("TRANSPARENT", false)

		val lowerLeft = windowPosition.withRelativeColumn(if (useDoubleWidth) 2 else 1).withRelativeRow(decoratedWindowSize.rows)
		var lowerRight = lowerLeft.withRelativeColumn(decoratedWindowSize.columns - if (useDoubleWidth) 3 else 2)
		for (column in lowerLeft.column..lowerRight.column) {
			var characterToDraw = filler
			if (useTransparency) {
				val tc = textGraphics.getCharacter(column, lowerLeft.row)
				if (tc != null) {
					characterToDraw = tc.character
				}
			}
			textGraphics.setCharacter(column, lowerLeft.row, characterToDraw)
		}

		lowerRight = lowerRight.withRelativeColumn(1)
		var upperRight = lowerRight.withRelativeRow(-decoratedWindowSize.rows + 1)
		for (row in upperRight.row..lowerRight.row) {
			var characterToDraw = filler
			if (useTransparency) {
				val tc = textGraphics.getCharacter(upperRight.column, row)
				if (tc != null) {
					characterToDraw = tc.character
				}
			}
			textGraphics.setCharacter(upperRight.column, row, characterToDraw)
		}

		if (useDoubleWidth) {
			//Fill the remaining hole
			upperRight = upperRight.withRelativeColumn(1)
			for (row in upperRight.row..lowerRight.row) {
				var characterToDraw = filler
				if (useTransparency) {
					val tc = textGraphics.getCharacter(upperRight.column, row)
					if (tc != null) {
						characterToDraw = tc.character
					}
				}
				textGraphics.setCharacter(upperRight.column, row, characterToDraw)
			}
		}
	}
}
