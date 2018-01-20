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

import com.googlecode.lanterna.*
import com.googlecode.lanterna.graphics.Theme
import com.googlecode.lanterna.graphics.ThemeDefinition
import com.googlecode.lanterna.gui2.Direction
import com.googlecode.lanterna.gui2.ScrollBar
import com.googlecode.lanterna.gui2.TextGUIGraphics

import java.util.ArrayList

/**
 * Default implementation of `TableRenderer`
 * @param <V> Type of data stored in each table cell
 * @author Martin
</V> */
class DefaultTableRenderer<V> : TableRenderer<V> {

	private val verticalScrollBar: ScrollBar
	private val horizontalScrollBar: ScrollBar

	private var headerVerticalBorderStyle: TableCellBorderStyle? = null
	private var headerHorizontalBorderStyle: TableCellBorderStyle? = null
	private var cellVerticalBorderStyle: TableCellBorderStyle? = null
	private var cellHorizontalBorderStyle: TableCellBorderStyle? = null

	//So that we don't have to recalculate the size every time. This still isn't optimal but shouganai.
	private var cachedSize: TerminalSize? = null
	private val columnSizes: MutableList<Int>
	private val rowSizes: MutableList<Int>
	private var headerSizeInRows: Int = 0

	private val isHorizontallySpaced: Boolean
		get() = headerHorizontalBorderStyle != TableCellBorderStyle.None || cellHorizontalBorderStyle != TableCellBorderStyle.None

	/**
	 * Default constructor
	 */
	init {
		verticalScrollBar = ScrollBar(Direction.VERTICAL)
		horizontalScrollBar = ScrollBar(Direction.HORIZONTAL)

		headerVerticalBorderStyle = TableCellBorderStyle.None
		headerHorizontalBorderStyle = TableCellBorderStyle.EmptySpace
		cellVerticalBorderStyle = TableCellBorderStyle.None
		cellHorizontalBorderStyle = TableCellBorderStyle.EmptySpace

		cachedSize = null

		columnSizes = ArrayList()
		rowSizes = ArrayList()
		headerSizeInRows = 0
	}

	/**
	 * Sets the style to be used when separating the table header row from the actual "data" cells below. This will
	 * cause a new line to be added under the header labels, unless set to `TableCellBorderStyle.None`.
	 *
	 * @param headerVerticalBorderStyle Style to use to separate Table header from body
	 */
	fun setHeaderVerticalBorderStyle(headerVerticalBorderStyle: TableCellBorderStyle) {
		this.headerVerticalBorderStyle = headerVerticalBorderStyle
	}

	/**
	 * Sets the style to be used when separating the table header labels from each other. This will cause a new
	 * column to be added in between each label, unless set to `TableCellBorderStyle.None`.
	 *
	 * @param headerHorizontalBorderStyle Style to use when separating header columns horizontally
	 */
	fun setHeaderHorizontalBorderStyle(headerHorizontalBorderStyle: TableCellBorderStyle) {
		this.headerHorizontalBorderStyle = headerHorizontalBorderStyle
	}

	/**
	 * Sets the style to be used when vertically separating table cells from each other. This will cause a new line
	 * to be added between every row, unless set to `TableCellBorderStyle.None`.
	 *
	 * @param cellVerticalBorderStyle Style to use to separate table cells vertically
	 */
	fun setCellVerticalBorderStyle(cellVerticalBorderStyle: TableCellBorderStyle) {
		this.cellVerticalBorderStyle = cellVerticalBorderStyle
	}

	/**
	 * Sets the style to be used when horizontally separating table cells from each other. This will cause a new
	 * column to be added between every row, unless set to `TableCellBorderStyle.None`.
	 *
	 * @param cellHorizontalBorderStyle Style to use to separate table cells horizontally
	 */
	fun setCellHorizontalBorderStyle(cellHorizontalBorderStyle: TableCellBorderStyle) {
		this.cellHorizontalBorderStyle = cellHorizontalBorderStyle
	}

	override fun getPreferredSize(table: Table<V>): TerminalSize {
		//Quick bypass if the table hasn't changed
		if (!table.isInvalid && cachedSize != null) {
			return cachedSize
		}

		val tableModel = table.tableModel
		val viewLeftColumn = table.viewLeftColumn
		val viewTopRow = table.viewTopRow
		var visibleColumns = table.visibleColumns
		var visibleRows = table.visibleRows
		val rows = tableModel!!.rows
		val columnHeaders = tableModel.columnLabels
		val tableHeaderRenderer = table.tableHeaderRenderer
		val tableCellRenderer = table.tableCellRenderer

		if (visibleColumns == 0) {
			visibleColumns = tableModel.columnCount
		}
		if (visibleRows == 0) {
			visibleRows = tableModel.rowCount
		}

		columnSizes.clear()
		rowSizes.clear()

		if (tableModel.columnCount == 0) {
			return TerminalSize.ZERO
		}

		// If there are no rows, base the column sizes off of the column labels
		if (rows.size == 0) {
			for (columnIndex in viewLeftColumn until viewLeftColumn + visibleColumns) {
				val columnSize = tableHeaderRenderer!!.getPreferredSize(table, columnHeaders[columnIndex], columnIndex).columns
				val listOffset = columnIndex - viewLeftColumn
				if (columnSizes.size == listOffset) {
					columnSizes.add(columnSize)
				} else {
					if (columnSizes[listOffset] < columnSize) {
						columnSizes[listOffset] = columnSize
					}
				}
			}
		}

		for (rowIndex in rows.indices) {
			val row = rows[rowIndex]
			for (columnIndex in viewLeftColumn until Math.min(row.size, viewLeftColumn + visibleColumns)) {
				val cell = row[columnIndex]
				val columnSize = tableCellRenderer!!.getPreferredSize(table, cell, columnIndex, rowIndex).columns
				val listOffset = columnIndex - viewLeftColumn
				if (columnSizes.size == listOffset) {
					columnSizes.add(columnSize)
				} else {
					if (columnSizes[listOffset] < columnSize) {
						columnSizes[listOffset] = columnSize
					}
				}
			}

			//Do the headers too, on the first iteration
			if (rowIndex == 0) {
				for (columnIndex in viewLeftColumn until Math.min(row.size, viewLeftColumn + visibleColumns)) {
					val columnSize = tableHeaderRenderer!!.getPreferredSize(table, columnHeaders[columnIndex], columnIndex).columns
					val listOffset = columnIndex - viewLeftColumn
					if (columnSizes.size == listOffset) {
						columnSizes.add(columnSize)
					} else {
						if (columnSizes[listOffset] < columnSize) {
							columnSizes[listOffset] = columnSize
						}
					}
				}
			}
		}

		for (columnIndex in columnHeaders.indices) {
			for (rowIndex in viewTopRow until Math.min(rows.size, viewTopRow + visibleRows)) {
				val cell = rows[rowIndex][columnIndex]
				val rowSize = tableCellRenderer!!.getPreferredSize(table, cell, columnIndex, rowIndex).rows
				val listOffset = rowIndex - viewTopRow
				if (rowSizes.size == listOffset) {
					rowSizes.add(rowSize)
				} else {
					if (rowSizes[listOffset] < rowSize) {
						rowSizes[listOffset] = rowSize
					}
				}
			}
		}

		var preferredRowSize = 0
		var preferredColumnSize = 0
		for (size in columnSizes) {
			preferredColumnSize += size
		}
		for (size in rowSizes) {
			preferredRowSize += size
		}

		headerSizeInRows = 0
		for (columnIndex in columnHeaders.indices) {
			val headerRows = tableHeaderRenderer!!.getPreferredSize(table, columnHeaders[columnIndex], columnIndex).rows
			if (headerSizeInRows < headerRows) {
				headerSizeInRows = headerRows
			}
		}
		preferredRowSize += headerSizeInRows

		if (headerVerticalBorderStyle != TableCellBorderStyle.None) {
			preferredRowSize++    //Spacing between header and body
		}
		if (cellVerticalBorderStyle != TableCellBorderStyle.None) {
			if (!rows.isEmpty()) {
				preferredRowSize += Math.min(rows.size, visibleRows) - 1 //Vertical space between cells
			}
		}
		if (isHorizontallySpaced) {
			if (!columnHeaders.isEmpty()) {
				preferredColumnSize += Math.min(tableModel.columnCount, visibleColumns) - 1    //Spacing between the columns
			}
		}

		//Add on space taken by scrollbars (if needed)
		if (visibleRows < rows.size) {
			preferredColumnSize++
		}
		if (visibleColumns < tableModel.columnCount) {
			preferredRowSize++
		}

		cachedSize = TerminalSize(preferredColumnSize, preferredRowSize)
		return cachedSize
	}

	override fun getCursorLocation(component: Table<V>): TerminalPosition {
		return null
	}

	override fun drawComponent(graphics: TextGUIGraphics, table: Table<V>) {
		//Get the size
		val area = graphics.size

		//Don't even bother
		if (area.rows == 0 || area.columns == 0) {
			return
		}

		// Get preferred size if the table model has changed
		if (table.isInvalid) getPreferredSize(table)

		val topPosition = drawHeader(graphics, table)
		drawRows(graphics, table, topPosition)
	}

	private fun drawHeader(graphics: TextGUIGraphics, table: Table<V>): Int {
		val theme = table.theme
		val tableHeaderRenderer = table.tableHeaderRenderer
		val headers = table.tableModel!!.columnLabels
		val viewLeftColumn = table.viewLeftColumn
		var visibleColumns = table.visibleColumns
		if (visibleColumns == 0) {
			visibleColumns = table.tableModel!!.columnCount
		}
		var topPosition = 0
		var leftPosition = 0
		val endColumnIndex = Math.min(headers.size, viewLeftColumn + visibleColumns)
		for (index in viewLeftColumn until endColumnIndex) {
			val label = headers[index]
			val size = TerminalSize(columnSizes[index - viewLeftColumn], headerSizeInRows)
			tableHeaderRenderer!!.drawHeader(table, label, index, graphics.newTextGraphics(TerminalPosition(leftPosition, 0), size))
			leftPosition += size.columns
			if (headerHorizontalBorderStyle != TableCellBorderStyle.None && index < endColumnIndex - 1) {
				graphics.applyThemeStyle(theme.getDefinition(Table<*>::class.java!!).normal)
				graphics.setCharacter(leftPosition, 0, getVerticalCharacter(headerHorizontalBorderStyle))
				leftPosition++
			}
		}
		topPosition += headerSizeInRows

		if (headerVerticalBorderStyle != TableCellBorderStyle.None) {
			leftPosition = 0
			graphics.applyThemeStyle(theme.getDefinition(Table<*>::class.java!!).normal)
			for (i in columnSizes.indices) {
				if (i > 0) {
					graphics.setCharacter(
						leftPosition,
						topPosition,
						getJunctionCharacter(
							headerVerticalBorderStyle,
							headerHorizontalBorderStyle,
							cellHorizontalBorderStyle))
					leftPosition++
				}
				val columnWidth = columnSizes[i]
				graphics.drawLine(leftPosition, topPosition, leftPosition + columnWidth - 1, topPosition, getHorizontalCharacter(headerVerticalBorderStyle))
				leftPosition += columnWidth
			}
			//Expand out the line in case the area is bigger
			if (leftPosition < graphics.size.columns) {
				graphics.drawLine(leftPosition, topPosition, graphics.size.columns - 1, topPosition, getHorizontalCharacter(headerVerticalBorderStyle))
			}
			topPosition++
		}
		return topPosition
	}

	private fun drawRows(graphics: TextGUIGraphics, table: Table<V>, topPosition: Int) {
		var graphics = graphics
		var topPosition = topPosition
		val theme = table.theme
		val themeDefinition = theme.getDefinition(Table<*>::class.java!!)
		val area = graphics.size
		val tableCellRenderer = table.tableCellRenderer
		val tableModel = table.tableModel
		val rows = tableModel!!.rows
		val viewTopRow = table.viewTopRow
		val viewLeftColumn = table.viewLeftColumn
		var visibleRows = table.visibleRows
		var visibleColumns = table.visibleColumns
		if (visibleColumns == 0) {
			visibleColumns = tableModel.columnCount
		}
		if (visibleRows == 0) {
			visibleRows = tableModel.rowCount
		}

		//Draw scrollbars (if needed)
		if (visibleRows < rows.size) {
			val verticalScrollBarPreferredSize = verticalScrollBar.preferredSize
			var scrollBarHeight = graphics.size.rows - topPosition
			if (visibleColumns < tableModel.columnCount) {
				scrollBarHeight--
			}
			verticalScrollBar.setPosition(TerminalPosition(graphics.size.columns - verticalScrollBarPreferredSize.columns, topPosition))
			verticalScrollBar.setSize(verticalScrollBarPreferredSize.withRows(scrollBarHeight))
			verticalScrollBar.setScrollMaximum(rows.size)
			verticalScrollBar.setViewSize(visibleRows)
			verticalScrollBar.setScrollPosition(viewTopRow)

			// Ensure the parent is correct
			if (table.parent !== verticalScrollBar.parent) {
				if (verticalScrollBar.parent != null) {
					verticalScrollBar.onRemoved(verticalScrollBar.parent!!)
				}
				if (table.parent != null) {
					verticalScrollBar.onAdded(table.parent!!)
				}
			}

			// Finally draw the thing
			verticalScrollBar.draw(graphics.newTextGraphics(verticalScrollBar.position, verticalScrollBar.size!!))

			// Adjust graphics object to the remaining area when the vertical scrollbar is subtracted
			graphics = graphics.newTextGraphics(TerminalPosition.TOP_LEFT_CORNER, graphics.size.withRelativeColumns(-verticalScrollBarPreferredSize.columns))
		}
		if (visibleColumns < tableModel.columnCount) {
			val horizontalScrollBarPreferredSize = horizontalScrollBar.preferredSize
			val scrollBarWidth = graphics.size.columns
			horizontalScrollBar.setPosition(TerminalPosition(0, graphics.size.rows - horizontalScrollBarPreferredSize.rows))
			horizontalScrollBar.setSize(horizontalScrollBarPreferredSize.withColumns(scrollBarWidth))
			horizontalScrollBar.setScrollMaximum(tableModel.columnCount)
			horizontalScrollBar.setViewSize(visibleColumns)
			horizontalScrollBar.setScrollPosition(viewLeftColumn)

			// Ensure the parent is correct
			if (table.parent !== horizontalScrollBar.parent) {
				if (horizontalScrollBar.parent != null) {
					horizontalScrollBar.onRemoved(horizontalScrollBar.parent!!)
				}
				if (table.parent != null) {
					horizontalScrollBar.onAdded(table.parent!!)
				}
			}

			// Finally draw the thing
			horizontalScrollBar.draw(graphics.newTextGraphics(horizontalScrollBar.position, horizontalScrollBar.size!!))

			// Adjust graphics object to the remaining area when the horizontal scrollbar is subtracted
			graphics = graphics.newTextGraphics(TerminalPosition.TOP_LEFT_CORNER, graphics.size.withRelativeRows(-horizontalScrollBarPreferredSize.rows))
		}

		var leftPosition: Int
		for (rowIndex in viewTopRow until Math.min(viewTopRow + visibleRows, rows.size)) {
			leftPosition = 0
			val row = rows[rowIndex]
			for (columnIndex in viewLeftColumn until Math.min(viewLeftColumn + visibleColumns, row.size)) {
				if (columnIndex > viewLeftColumn) {
					if (table.selectedRow == rowIndex && !table.isCellSelection) {
						if (table.isFocused) {
							graphics.applyThemeStyle(themeDefinition.active)
						} else {
							graphics.applyThemeStyle(themeDefinition.selected)
						}
					} else {
						graphics.applyThemeStyle(themeDefinition.normal)
					}
					graphics.setCharacter(leftPosition, topPosition, getVerticalCharacter(cellHorizontalBorderStyle))
					leftPosition++
				}
				val cell = row[columnIndex]
				val cellPosition = TerminalPosition(leftPosition, topPosition)
				val cellArea = TerminalSize(columnSizes[columnIndex - viewLeftColumn], rowSizes[rowIndex - viewTopRow])
				tableCellRenderer!!.drawCell(table, cell, columnIndex, rowIndex, graphics.newTextGraphics(cellPosition, cellArea))
				leftPosition += cellArea.columns
				if (leftPosition > area.columns) {
					break
				}
			}
			topPosition += rowSizes[rowIndex - viewTopRow]
			if (cellVerticalBorderStyle != TableCellBorderStyle.None) {
				leftPosition = 0
				graphics.applyThemeStyle(themeDefinition.normal)
				for (i in columnSizes.indices) {
					if (i > 0) {
						graphics.setCharacter(
							leftPosition,
							topPosition,
							getJunctionCharacter(
								cellVerticalBorderStyle,
								cellHorizontalBorderStyle,
								cellHorizontalBorderStyle))
						leftPosition++
					}
					val columnWidth = columnSizes[i]
					graphics.drawLine(leftPosition, topPosition, leftPosition + columnWidth - 1, topPosition, getHorizontalCharacter(cellVerticalBorderStyle))
					leftPosition += columnWidth
				}
				topPosition += 1
			}
			if (topPosition > area.rows) {
				break
			}
		}
	}

	private fun getHorizontalCharacter(style: TableCellBorderStyle?): Char {
		when (style) {
			TableCellBorderStyle.SingleLine -> return Symbols.SINGLE_LINE_HORIZONTAL
			TableCellBorderStyle.DoubleLine -> return Symbols.DOUBLE_LINE_HORIZONTAL
			else -> return ' '
		}
	}

	private fun getVerticalCharacter(style: TableCellBorderStyle?): Char {
		when (style) {
			TableCellBorderStyle.SingleLine -> return Symbols.SINGLE_LINE_VERTICAL
			TableCellBorderStyle.DoubleLine -> return Symbols.DOUBLE_LINE_VERTICAL
			else -> return ' '
		}
	}

	private fun getJunctionCharacter(mainStyle: TableCellBorderStyle?, styleAbove: TableCellBorderStyle?, styleBelow: TableCellBorderStyle?): Char {
		return if (mainStyle == TableCellBorderStyle.SingleLine) {
			if (styleAbove == TableCellBorderStyle.SingleLine) {
				if (styleBelow == TableCellBorderStyle.SingleLine) {
					Symbols.SINGLE_LINE_CROSS
				} else if (styleBelow == TableCellBorderStyle.DoubleLine) {
					//There isn't any character for this, give upper side priority
					Symbols.SINGLE_LINE_T_UP
				} else {
					Symbols.SINGLE_LINE_T_UP
				}
			} else if (styleAbove == TableCellBorderStyle.DoubleLine) {
				if (styleBelow == TableCellBorderStyle.SingleLine) {
					//There isn't any character for this, give upper side priority
					Symbols.SINGLE_LINE_T_DOUBLE_UP
				} else if (styleBelow == TableCellBorderStyle.DoubleLine) {
					Symbols.DOUBLE_LINE_VERTICAL_SINGLE_LINE_CROSS
				} else {
					Symbols.SINGLE_LINE_T_DOUBLE_UP
				}
			} else {
				if (styleBelow == TableCellBorderStyle.SingleLine) {
					Symbols.SINGLE_LINE_T_DOWN
				} else if (styleBelow == TableCellBorderStyle.DoubleLine) {
					Symbols.SINGLE_LINE_T_DOUBLE_DOWN
				} else {
					Symbols.SINGLE_LINE_HORIZONTAL
				}
			}
		} else if (mainStyle == TableCellBorderStyle.DoubleLine) {
			if (styleAbove == TableCellBorderStyle.SingleLine) {
				if (styleBelow == TableCellBorderStyle.SingleLine) {
					Symbols.DOUBLE_LINE_HORIZONTAL_SINGLE_LINE_CROSS
				} else if (styleBelow == TableCellBorderStyle.DoubleLine) {
					//There isn't any character for this, give upper side priority
					Symbols.DOUBLE_LINE_T_SINGLE_UP
				} else {
					Symbols.DOUBLE_LINE_T_SINGLE_UP
				}
			} else if (styleAbove == TableCellBorderStyle.DoubleLine) {
				if (styleBelow == TableCellBorderStyle.SingleLine) {
					//There isn't any character for this, give upper side priority
					Symbols.DOUBLE_LINE_T_UP
				} else if (styleBelow == TableCellBorderStyle.DoubleLine) {
					Symbols.DOUBLE_LINE_CROSS
				} else {
					Symbols.DOUBLE_LINE_T_UP
				}
			} else {
				if (styleBelow == TableCellBorderStyle.SingleLine) {
					Symbols.DOUBLE_LINE_T_SINGLE_DOWN
				} else if (styleBelow == TableCellBorderStyle.DoubleLine) {
					Symbols.DOUBLE_LINE_T_DOWN
				} else {
					Symbols.DOUBLE_LINE_HORIZONTAL
				}
			}
		} else {
			' '
		}
	}
}
