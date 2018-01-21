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
import com.googlecode.lanterna.graphics.ThemeDefinition

/**
 * Special component that is by default displayed as the background of a text gui unless you override it with something
 * else. Themes can control how this backdrop is drawn, the normal is one solid color.
 */
open class GUIBackdrop : EmptySpace() {
	override fun createDefaultRenderer() =
		object : ComponentRenderer<EmptySpace> {

			override fun getPreferredSize(component: EmptySpace) =
				TerminalSize.ONE

			override fun drawComponent(graphics: TextGUIGraphics, component: EmptySpace) {
				val themeDefinition = component.theme.getDefinition(GUIBackdrop::class.java!!)
				graphics.applyThemeStyle(themeDefinition.normal)
				graphics.fill(themeDefinition.getCharacter("BACKGROUND", ' '))
			}
		}
}
