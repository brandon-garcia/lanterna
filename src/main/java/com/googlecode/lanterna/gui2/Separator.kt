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
import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.graphics.ThemeDefinition

/**
 * Static non-interactive component that is typically rendered as a single line. Normally this component is used to
 * separate component from each other in situations where a bordered panel isn't ideal. By default the separator will
 * ask for a size of 1x1 so you'll need to make it bigger, either through the layout manager or by overriding the
 * preferred size.
 * @author Martin
 */
class Separator
/**
 * Creates a new `Separator` for a specific direction, which will decide whether to draw a horizontal line or
 * a vertical line
 *
 * @param direction Direction of the line to draw within the separator
 */
(
	/**
	 * Returns the direction of the line drawn for this separator
	 * @return Direction of the line drawn for this separator
	 */
	val direction: Direction?) : AbstractComponent<Separator>() {

	init {
		if (direction == null) {
			throw IllegalArgumentException("Cannot create a separator with a null direction")
		}
	}

	override fun createDefaultRenderer() =
		DefaultSeparatorRenderer()

	/**
	 * Helper interface that doesn't add any new methods but makes coding new button renderers a little bit more clear
	 */
	abstract class SeparatorRenderer : ComponentRenderer<Separator>

	/**
	 * This is the default separator renderer that is used if you don't override anything. With this renderer, the
	 * separator has a preferred size of one but will take up the whole area it is given and fill that space with either
	 * horizontal or vertical lines, depending on the direction of the `Separator`
	 */
	class DefaultSeparatorRenderer : SeparatorRenderer() {
		override fun getPreferredSize(component: Separator) =
			TerminalSize.ONE

		override fun drawComponent(graphics: TextGUIGraphics, component: Separator) {
			val themeDefinition = component.themeDefinition
			graphics.applyThemeStyle(themeDefinition.normal)
			val character = themeDefinition.getCharacter(component.direction.name.toUpperCase(),
				if (component.direction == Direction.HORIZONTAL) Symbols.SINGLE_LINE_HORIZONTAL else Symbols.SINGLE_LINE_VERTICAL)
			graphics.fill(character)
		}
	}
}
