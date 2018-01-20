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
 * Default implementation of `TableCellRenderer`
 * @param <V> Type of data stored in each table cell
 * @author Martin
</V> */
class DefaultTableCellRenderer<V> : TableCellRenderer<V> {
	override fun getPreferredSize(table: Table<V>, cell: V, columnIndex: Int, rowIndex: Int): TerminalSize {
		val lines = getContent(cell)
		var maxWidth = 0
		for (line in lines) {
			val length = TerminalTextUtils.getColumnWidth(line)
			if (maxWidth < length) {
				maxWidth = length
			}
		}
		return TerminalSize(maxWidth, lines.size)
	}

	override fun drawCell(table: Table<V>, cell: V, columnIndex: Int, rowIndex: Int, textGUIGraphics: TextGUIGraphics) {
		val themeDefinition = table.themeDefinition
		if (table.selectedColumn == columnIndex && table.selectedRow == rowIndex || table.selectedRow == rowIndex && !table.isCellSelection) {
			if (table.isFocused) {
				textGUIGraphics.applyThemeStyle(themeDefinition.active)
			} else {
				textGUIGraphics.applyThemeStyle(themeDefinition.selected)
			}
			textGUIGraphics.fill(' ')  //Make sure to fill the whole cell first
		} else {
			textGUIGraphics.applyThemeStyle(themeDefinition.normal)
		}
		val lines = getContent(cell)
		var rowCount = 0
		for (line in lines) {
			textGUIGraphics.putString(0, rowCount++, line)
		}
	}

	private fun getContent(cell: V?): Array<String> {
		val lines: Array<String>
		if (cell == null) {
			lines = arrayOf("")
		} else {
			lines = cell.toString().split("\r?\n".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
		}
		return lines
	}
}
