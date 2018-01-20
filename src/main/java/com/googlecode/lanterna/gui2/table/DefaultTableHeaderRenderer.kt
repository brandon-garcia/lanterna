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
package com.googlecode.lanterna.gui2.table

import com.googlecode.lanterna.TerminalTextUtils
import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.graphics.ThemeDefinition
import com.googlecode.lanterna.gui2.TextGUIGraphics

/**
 * Default implementation of `TableHeaderRenderer`
 * @author Martin
 */
class DefaultTableHeaderRenderer<V> : TableHeaderRenderer<V> {
	override fun getPreferredSize(table: Table<V>, label: String?, columnIndex: Int): TerminalSize {
		return if (label == null) {
			TerminalSize.ZERO
		} else TerminalSize(TerminalTextUtils.getColumnWidth(label), 1)
	}

	override fun drawHeader(table: Table<V>, label: String, index: Int, textGUIGraphics: TextGUIGraphics) {
		val themeDefinition = table.themeDefinition
		textGUIGraphics.applyThemeStyle(themeDefinition.getCustom("HEADER", themeDefinition.normal))
		textGUIGraphics.putString(0, 0, label)
	}
}
