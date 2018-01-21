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

import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.TextColor

/**
 * Simple component which draws a solid color over its area. The size this component will request is specified through
 * it's constructor.
 *
 * @author Martin
 */
open class EmptySpace
/**
 * Creates an EmptySpace with a specified color (null will make it use a color from the theme) and preferred size
 * @param color Color to use (null will make it use the theme)
 * @param size Preferred size
 */
@JvmOverloads constructor(
	/**
	 * Returns the color this component is drawn with, or `null` if this component uses whatever the default color
	 * the theme is set to use
	 * @return Color used when drawing or `null` if it's using the theme
	 */
	/**
	 * Changes the color this component will use when drawn
	 * @param color New color to draw the component with, if `null` then the component will use the theme's
	 * default color
	 */
	var color: TextColor? = null, private val size: TerminalSize = TerminalSize.ONE) : AbstractComponent<EmptySpace>() {

	/**
	 * Creates an EmptySpace with a specified preferred size (color will be chosen from the theme)
	 * @param size Preferred size
	 */
	constructor(size: TerminalSize) : this(null, size) {}

	override fun createDefaultRenderer(): ComponentRenderer<EmptySpace> =
		object : ComponentRenderer<EmptySpace> {

			override fun getPreferredSize(component: EmptySpace) =
				size

			override fun drawComponent(graphics: TextGUIGraphics, component: EmptySpace) {
				graphics.applyThemeStyle(component.themeDefinition.normal)
				if (color != null) {
					graphics.setBackgroundColor(color!!)
				}
				graphics.fill(' ')
			}
		}
}
/**
 * Creates an EmptySpace with size 1x1 and a default color chosen from the theme
 */
/**
 * Creates an EmptySpace with a specified color and preferred size of 1x1
 * @param color Color to use (null will make it use the theme)
 */
