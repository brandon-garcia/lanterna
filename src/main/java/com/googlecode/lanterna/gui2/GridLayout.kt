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

import com.googlecode.lanterna.TerminalPosition
import com.googlecode.lanterna.TerminalSize

import java.util.*

/**
 * This emulates the behaviour of the GridLayout in SWT (as opposed to the one in AWT/Swing). I originally ported the
 * SWT class itself but due to licensing concerns (the eclipse license is not compatible with LGPL) I was advised not to
 * do that. This is a partial implementation and some of the semantics have changed, but in general it works the same
 * way so the SWT documentation will generally match.
 *
 *
 * You use the `GridLayout` by specifying a number of columns you want your grid to have and then when you add
 * components, you assign `LayoutData` to these components using the different static methods in this class
 * (`createLayoutData(..)`). You can set components to span both rows and columns, as well as defining how to
 * distribute the available space.
 */
class GridLayout
/**
 * Creates a new `GridLayout` with the specified number of columns. Initially, this layout will have a
 * horizontal spacing of 1 and vertical spacing of 0, with a left and right margin of 1.
 * @param numberOfColumns Number of columns in this grid
 */
(private val numberOfColumns: Int) : LayoutManager {
	private var horizontalSpacing: Int = 0
	private var verticalSpacing: Int = 0
	private var topMarginSize: Int = 0
	private var bottomMarginSize: Int = 0
	private var leftMarginSize: Int = 0
	private var rightMarginSize: Int = 0

	private var changed: Boolean = false

	/**
	 * The enum is used to specify where in a grid cell a component should be placed, in the case that the preferred
	 * size of the component is smaller than the space in the cell. This class will generally use two alignments, one
	 * for horizontal and one for vertical.
	 */
	enum class Alignment {
		/**
		 * Place the component at the start of the cell (horizontally or vertically) and leave whatever space is left
		 * after the preferred size empty.
		 */
		BEGINNING,
		/**
		 * Place the component at the middle of the cell (horizontally or vertically) and leave the space before and
		 * after empty.
		 */
		CENTER,
		/**
		 * Place the component at the end of the cell (horizontally or vertically) and leave whatever space is left
		 * before the preferred size empty.
		 */
		END,
		/**
		 * Force the component to be the same size as the table cell
		 */
		FILL
	}

	internal class GridLayoutData private constructor(
		val horizontalAlignment: Alignment,
		val verticalAlignment: Alignment,
		val grabExtraHorizontalSpace: Boolean,
		val grabExtraVerticalSpace: Boolean,
		val horizontalSpan: Int,
		val verticalSpan: Int) : LayoutData {

		init {

			if (horizontalSpan < 1 || verticalSpan < 1) {
				throw IllegalArgumentException("Horizontal/Vertical span must be 1 or greater")
			}
		}
	}

	init {
		this.horizontalSpacing = 1
		this.verticalSpacing = 0
		this.topMarginSize = 0
		this.bottomMarginSize = 0
		this.leftMarginSize = 1
		this.rightMarginSize = 1
		this.changed = true
	}

	/**
	 * Returns the horizontal spacing, i.e. the number of empty columns between each cell
	 * @return Horizontal spacing
	 */
	fun getHorizontalSpacing(): Int {
		return horizontalSpacing
	}

	/**
	 * Sets the horizontal spacing, i.e. the number of empty columns between each cell
	 * @param horizontalSpacing New horizontal spacing
	 * @return Itself
	 */
	fun setHorizontalSpacing(horizontalSpacing: Int): GridLayout {
		if (horizontalSpacing < 0) {
			throw IllegalArgumentException("Horizontal spacing cannot be less than 0")
		}
		this.horizontalSpacing = horizontalSpacing
		this.changed = true
		return this
	}

	/**
	 * Returns the vertical spacing, i.e. the number of empty columns between each row
	 * @return Vertical spacing
	 */
	fun getVerticalSpacing(): Int {
		return verticalSpacing
	}

	/**
	 * Sets the vertical spacing, i.e. the number of empty columns between each row
	 * @param verticalSpacing New vertical spacing
	 * @return Itself
	 */
	fun setVerticalSpacing(verticalSpacing: Int): GridLayout {
		if (verticalSpacing < 0) {
			throw IllegalArgumentException("Vertical spacing cannot be less than 0")
		}
		this.verticalSpacing = verticalSpacing
		this.changed = true
		return this
	}

	/**
	 * Returns the top margin, i.e. number of empty rows above the first row in the grid
	 * @return Top margin, in number of rows
	 */
	fun getTopMarginSize(): Int {
		return topMarginSize
	}

	/**
	 * Sets the top margin, i.e. number of empty rows above the first row in the grid
	 * @param topMarginSize Top margin, in number of rows
	 * @return Itself
	 */
	fun setTopMarginSize(topMarginSize: Int): GridLayout {
		if (topMarginSize < 0) {
			throw IllegalArgumentException("Top margin size cannot be less than 0")
		}
		this.topMarginSize = topMarginSize
		this.changed = true
		return this
	}

	/**
	 * Returns the bottom margin, i.e. number of empty rows below the last row in the grid
	 * @return Bottom margin, in number of rows
	 */
	fun getBottomMarginSize(): Int {
		return bottomMarginSize
	}

	/**
	 * Sets the bottom margin, i.e. number of empty rows below the last row in the grid
	 * @param bottomMarginSize Bottom margin, in number of rows
	 * @return Itself
	 */
	fun setBottomMarginSize(bottomMarginSize: Int): GridLayout {
		if (bottomMarginSize < 0) {
			throw IllegalArgumentException("Bottom margin size cannot be less than 0")
		}
		this.bottomMarginSize = bottomMarginSize
		this.changed = true
		return this
	}

	/**
	 * Returns the left margin, i.e. number of empty columns left of the first column in the grid
	 * @return Left margin, in number of columns
	 */
	fun getLeftMarginSize(): Int {
		return leftMarginSize
	}

	/**
	 * Sets the left margin, i.e. number of empty columns left of the first column in the grid
	 * @param leftMarginSize Left margin, in number of columns
	 * @return Itself
	 */
	fun setLeftMarginSize(leftMarginSize: Int): GridLayout {
		if (leftMarginSize < 0) {
			throw IllegalArgumentException("Left margin size cannot be less than 0")
		}
		this.leftMarginSize = leftMarginSize
		this.changed = true
		return this
	}

	/**
	 * Returns the right margin, i.e. number of empty columns right of the last column in the grid
	 * @return Right margin, in number of columns
	 */
	fun getRightMarginSize(): Int {
		return rightMarginSize
	}

	/**
	 * Sets the right margin, i.e. number of empty columns right of the last column in the grid
	 * @param rightMarginSize Right margin, in number of columns
	 * @return Itself
	 */
	fun setRightMarginSize(rightMarginSize: Int): GridLayout {
		if (rightMarginSize < 0) {
			throw IllegalArgumentException("Right margin size cannot be less than 0")
		}
		this.rightMarginSize = rightMarginSize
		this.changed = true
		return this
	}

	override fun hasChanged(): Boolean {
		return this.changed
	}

	override fun getPreferredSize(components: List<Component>): TerminalSize {
		var preferredSize = TerminalSize.ZERO
		if (components.isEmpty()) {
			return preferredSize.withRelative(
				leftMarginSize + rightMarginSize,
				topMarginSize + bottomMarginSize)
		}

		var table = buildTable(components)
		table = eliminateUnusedRowsAndColumns(table)

		//Figure out each column first, this can be done independently of the row heights
		var preferredWidth = 0
		var preferredHeight = 0
		for (width in getPreferredColumnWidths(table)) {
			preferredWidth += width
		}
		for (height in getPreferredRowHeights(table)) {
			preferredHeight += height
		}
		preferredSize = preferredSize.withRelative(preferredWidth, preferredHeight)
		preferredSize = preferredSize.withRelativeColumns(leftMarginSize + rightMarginSize + (table[0].size - 1) * horizontalSpacing)
		preferredSize = preferredSize.withRelativeRows(topMarginSize + bottomMarginSize + (table.size - 1) * verticalSpacing)
		return preferredSize
	}

	override fun doLayout(area: TerminalSize, components: List<Component>) {
		var area = area
		//Sanity check, if the area is way too small, just return
		var table = buildTable(components)
		table = eliminateUnusedRowsAndColumns(table)

		if (area == TerminalSize.ZERO ||
			table.size == 0 ||
			area.columns <= leftMarginSize + rightMarginSize + (table[0].size - 1) * horizontalSpacing ||
			area.rows <= bottomMarginSize + topMarginSize + (table.size - 1) * verticalSpacing) {
			changed = false
			return
		}

		//Adjust area to the margins
		area = area.withRelative(-leftMarginSize - rightMarginSize, -topMarginSize - bottomMarginSize)

		val sizeMap = IdentityHashMap<Component, TerminalSize>()
		val positionMap = IdentityHashMap<Component, TerminalPosition>()

		//Figure out each column first, this can be done independently of the row heights
		val columnWidths = getPreferredColumnWidths(table)

		//Take notes of which columns we can expand if the usable area is larger than what the components want
		val expandableColumns = getExpandableColumns(table)

		//Next, start shrinking to make sure it fits the size of the area we are trying to lay out on.
		//Notice we subtract the horizontalSpacing to take the space between components into account
		val areaWithoutHorizontalSpacing = area.withRelativeColumns(-horizontalSpacing * (table[0].size - 1))
		var totalWidth = shrinkWidthToFitArea(areaWithoutHorizontalSpacing, columnWidths)

		//Finally, if there is extra space, make the expandable columns larger
		while (areaWithoutHorizontalSpacing.columns > totalWidth && !expandableColumns.isEmpty()) {
			totalWidth = grabExtraHorizontalSpace(areaWithoutHorizontalSpacing, columnWidths, expandableColumns, totalWidth)
		}

		//Now repeat for rows
		val rowHeights = getPreferredRowHeights(table)
		val expandableRows = getExpandableRows(table)
		val areaWithoutVerticalSpacing = area.withRelativeRows(-verticalSpacing * (table.size - 1))
		var totalHeight = shrinkHeightToFitArea(areaWithoutVerticalSpacing, rowHeights)
		while (areaWithoutVerticalSpacing.rows > totalHeight && !expandableRows.isEmpty()) {
			totalHeight = grabExtraVerticalSpace(areaWithoutVerticalSpacing, rowHeights, expandableRows, totalHeight)
		}

		//Ok, all constraints are in place, we can start placing out components. To simplify, do it horizontally first
		//and vertically after
		var tableCellTopLeft = TerminalPosition.TOP_LEFT_CORNER
		for (y in table.indices) {
			tableCellTopLeft = tableCellTopLeft.withColumn(0)
			for (x in 0 until table[y].size) {
				val component = table[y][x]
				if (component != null && !positionMap.containsKey(component)) {
					val layoutData = getLayoutData(component)
					var size = component.preferredSize
					var position = tableCellTopLeft

					var availableHorizontalSpace = 0
					var availableVerticalSpace = 0
					for (i in 0 until layoutData.horizontalSpan) {
						availableHorizontalSpace += columnWidths[x + i] + if (i > 0) horizontalSpacing else 0
					}
					for (i in 0 until layoutData.verticalSpan) {
						availableVerticalSpace += rowHeights[y + i] + if (i > 0) verticalSpacing else 0
					}

					//Make sure to obey the size restrictions
					size = size.withColumns(Math.min(size.columns, availableHorizontalSpace))
					size = size.withRows(Math.min(size.rows, availableVerticalSpace))

					when (layoutData.horizontalAlignment) {
						GridLayout.Alignment.CENTER -> position = position.withRelativeColumn((availableHorizontalSpace - size.columns) / 2)
						GridLayout.Alignment.END -> position = position.withRelativeColumn(availableHorizontalSpace - size.columns)
						GridLayout.Alignment.FILL -> size = size.withColumns(availableHorizontalSpace)
						else -> {
						}
					}
					when (layoutData.verticalAlignment) {
						GridLayout.Alignment.CENTER -> position = position.withRelativeRow((availableVerticalSpace - size.rows) / 2)
						GridLayout.Alignment.END -> position = position.withRelativeRow(availableVerticalSpace - size.rows)
						GridLayout.Alignment.FILL -> size = size.withRows(availableVerticalSpace)
						else -> {
						}
					}

					sizeMap.put(component, size)
					positionMap.put(component, position)
				}
				tableCellTopLeft = tableCellTopLeft.withRelativeColumn(columnWidths[x] + horizontalSpacing)
			}
			tableCellTopLeft = tableCellTopLeft.withRelativeRow(rowHeights[y] + verticalSpacing)
		}

		//Apply the margins here
		for (component in components) {
			component.position = positionMap[component].withRelative(leftMarginSize, topMarginSize)
			component.size = sizeMap[component]
		}
		this.changed = false
	}

	private fun getPreferredColumnWidths(table: Array<Array<Component>>): IntArray {
		//actualNumberOfColumns may be different from this.numberOfColumns since some columns may have been eliminated
		val actualNumberOfColumns = table[0].size
		val columnWidths = IntArray(actualNumberOfColumns)

		//Start by letting all span = 1 columns take what they need
		for (row in table) {
			for (i in 0 until actualNumberOfColumns) {
				val component = row[i] ?: continue
				val layoutData = getLayoutData(component)
				if (layoutData.horizontalSpan == 1) {
					columnWidths[i] = Math.max(columnWidths[i], component.preferredSize.columns)
				}
			}
		}

		//Next, do span > 1 and enlarge if necessary
		for (row in table) {
			var i = 0
			while (i < actualNumberOfColumns) {
				val component = row[i]
				if (component == null) {
					i++
					continue
				}
				val layoutData = getLayoutData(component)
				if (layoutData.horizontalSpan > 1) {
					var accumWidth = 0
					for (j in i until i + layoutData.horizontalSpan) {
						accumWidth += columnWidths[j]
					}

					val preferredWidth = component.preferredSize.columns
					if (preferredWidth > accumWidth) {
						var columnOffset = 0
						do {
							columnWidths[i + columnOffset++]++
							accumWidth++
							if (columnOffset == layoutData.horizontalSpan) {
								columnOffset = 0
							}
						} while (preferredWidth > accumWidth)
					}
				}
				i += layoutData.horizontalSpan
			}
		}
		return columnWidths
	}

	private fun getPreferredRowHeights(table: Array<Array<Component>>): IntArray {
		val numberOfRows = table.size
		val rowHeights = IntArray(numberOfRows)

		//Start by letting all span = 1 rows take what they need
		var rowIndex = 0
		for (row in table) {
			for (component in row) {
				if (component == null) {
					continue
				}
				val layoutData = getLayoutData(component)
				if (layoutData.verticalSpan == 1) {
					rowHeights[rowIndex] = Math.max(rowHeights[rowIndex], component.preferredSize.rows)
				}
			}
			rowIndex++
		}

		//Next, do span > 1 and enlarge if necessary
		for (x in 0 until numberOfColumns) {
			var y = 0
			while (y < numberOfRows && y < table.size) {
				if (x >= table[y].size) {
					y++
					continue
				}
				val component = table[y][x]
				if (component == null) {
					y++
					continue
				}
				val layoutData = getLayoutData(component)
				if (layoutData.verticalSpan > 1) {
					var accumulatedHeight = 0
					for (i in y until y + layoutData.verticalSpan) {
						accumulatedHeight += rowHeights[i]
					}

					val preferredHeight = component.preferredSize.rows
					if (preferredHeight > accumulatedHeight) {
						var rowOffset = 0
						do {
							rowHeights[y + rowOffset++]++
							accumulatedHeight++
							if (rowOffset == layoutData.verticalSpan) {
								rowOffset = 0
							}
						} while (preferredHeight > accumulatedHeight)
					}
				}
				y += layoutData.verticalSpan
			}
		}
		return rowHeights
	}

	private fun getExpandableColumns(table: Array<Array<Component>>): Set<Int> {
		val expandableColumns = TreeSet<Int>()
		for (row in table) {
			for (i in row.indices) {
				if (row[i] == null) {
					continue
				}
				val layoutData = getLayoutData(row[i])
				if (layoutData.grabExtraHorizontalSpace) {
					expandableColumns.add(i)
				}
			}
		}
		return expandableColumns
	}

	private fun getExpandableRows(table: Array<Array<Component>>): Set<Int> {
		val expandableRows = TreeSet<Int>()
		for (rowIndex in table.indices) {
			val row = table[rowIndex]
			for (cell in row) {
				if (cell == null) {
					continue
				}
				val layoutData = getLayoutData(cell)
				if (layoutData.grabExtraVerticalSpace) {
					expandableRows.add(rowIndex)
				}
			}
		}
		return expandableRows
	}

	private fun shrinkWidthToFitArea(area: TerminalSize, columnWidths: IntArray): Int {
		var totalWidth = 0
		for (width in columnWidths) {
			totalWidth += width
		}
		if (totalWidth > area.columns) {
			var columnOffset = 0
			do {
				if (columnWidths[columnOffset] > 0) {
					columnWidths[columnOffset]--
					totalWidth--
				}
				if (++columnOffset == numberOfColumns) {
					columnOffset = 0
				}
			} while (totalWidth > area.columns)
		}
		return totalWidth
	}

	private fun shrinkHeightToFitArea(area: TerminalSize, rowHeights: IntArray): Int {
		var totalHeight = 0
		for (height in rowHeights) {
			totalHeight += height
		}
		if (totalHeight > area.rows) {
			var rowOffset = 0
			do {
				if (rowHeights[rowOffset] > 0) {
					rowHeights[rowOffset]--
					totalHeight--
				}
				if (++rowOffset == rowHeights.size) {
					rowOffset = 0
				}
			} while (totalHeight > area.rows)
		}
		return totalHeight
	}

	private fun grabExtraHorizontalSpace(area: TerminalSize, columnWidths: IntArray, expandableColumns: Set<Int>, totalWidth: Int): Int {
		var totalWidth = totalWidth
		for (columnIndex in expandableColumns) {
			columnWidths[columnIndex]++
			totalWidth++
			if (area.columns == totalWidth) {
				break
			}
		}
		return totalWidth
	}

	private fun grabExtraVerticalSpace(area: TerminalSize, rowHeights: IntArray, expandableRows: Set<Int>, totalHeight: Int): Int {
		var totalHeight = totalHeight
		for (rowIndex in expandableRows) {
			rowHeights[rowIndex]++
			totalHeight++
			if (area.columns == totalHeight) {
				break
			}
		}
		return totalHeight
	}

	private fun buildTable(components: List<Component>): Array<Array<Component>> {
		val rows = ArrayList<Array<Component>>()
		val hspans = ArrayList<IntArray>()
		val vspans = ArrayList<IntArray>()

		var rowCount = 0
		var rowsExtent = 1
		val toBePlaced = LinkedList(components)
		while (!toBePlaced.isEmpty() || rowCount < rowsExtent) {
			//Start new row
			val row = arrayOfNulls<Component>(numberOfColumns)
			val hspan = IntArray(numberOfColumns)
			val vspan = IntArray(numberOfColumns)

			for (i in 0 until numberOfColumns) {
				if (i > 0 && hspan[i - 1] > 1) {
					row[i] = row[i - 1]
					hspan[i] = hspan[i - 1] - 1
					vspan[i] = vspan[i - 1]
				} else if (rowCount > 0 && vspans[rowCount - 1][i] > 1) {
					row[i] = rows[rowCount - 1][i]
					hspan[i] = hspans[rowCount - 1][i]
					vspan[i] = vspans[rowCount - 1][i] - 1
				} else if (!toBePlaced.isEmpty()) {
					val component = toBePlaced.poll()
					val gridLayoutData = getLayoutData(component)

					row[i] = component
					hspan[i] = gridLayoutData.horizontalSpan
					vspan[i] = gridLayoutData.verticalSpan
					rowsExtent = Math.max(rowsExtent, rowCount + gridLayoutData.verticalSpan)
				} else {
					row[i] = null
					hspan[i] = 1
					vspan[i] = 1
				}
			}

			rows.add(row)
			hspans.add(hspan)
			vspans.add(vspan)
			rowCount++
		}
		return rows.toTypedArray<Array<Component>>()
	}

	private fun eliminateUnusedRowsAndColumns(table: Array<Array<Component>>): Array<Array<Component>> {
		if (table.size == 0) {
			return table
		}
		//Could make this into a Set, but I doubt there will be any real gain in performance as these are probably going
		//to be very small.
		val rowsToRemove = ArrayList<Int>()
		val columnsToRemove = ArrayList<Int>()

		val tableRows = table.size
		val tableColumns = table[0].size

		//Scan for unnecessary columns
		columnLoop@ for (column in tableColumns - 1 downTo 1) {
			for (row in table) {
				if (row[column] !== row[column - 1]) {
					continue@columnLoop
				}
			}
			columnsToRemove.add(column)
		}

		//Scan for unnecessary rows
		rowLoop@ for (row in tableRows - 1 downTo 1) {
			for (column in 0 until tableColumns) {
				if (table[row][column] !== table[row - 1][column]) {
					continue@rowLoop
				}
			}
			rowsToRemove.add(row)
		}

		//If there's nothing to remove, just return the same
		if (rowsToRemove.isEmpty() && columnsToRemove.isEmpty()) {
			return table
		}

		//Build a new table with rows & columns eliminated
		val newTable = arrayOfNulls<Array<Component>>(tableRows - rowsToRemove.size)
		var insertedRowCounter = 0
		for (row in table) {
			val newColumn = arrayOfNulls<Component>(tableColumns - columnsToRemove.size)
			var insertedColumnCounter = 0
			for (column in 0 until tableColumns) {
				if (columnsToRemove.contains(column)) {
					continue
				}
				newColumn[insertedColumnCounter++] = row[column]
			}
			newTable[insertedRowCounter++] = newColumn
		}
		return newTable
	}

	private fun getLayoutData(component: Component): GridLayoutData {
		val layoutData = component.layoutData
		return layoutData as? GridLayoutData ?: DEFAULT
	}

	companion object {

		private val DEFAULT = GridLayoutData(
			Alignment.BEGINNING,
			Alignment.BEGINNING,
			false,
			false,
			1,
			1)

		/**
		 * Creates a layout data object for `GridLayout`:s that specify the horizontal and vertical alignment for the
		 * component in case the cell space is larger than the preferred size of the component. This method also has fields
		 * for indicating that the component would like to take more space if available to the container. For example, if
		 * the container is assigned is assigned an area of 50x15, but all the child components in the grid together only
		 * asks for 40x10, the remaining 10 columns and 5 rows will be empty. If just a single component asks for extra
		 * space horizontally and/or vertically, the grid will expand out to fill the entire area and the text space will be
		 * assigned to the component that asked for it. It also puts in data on how many rows and/or columns the component
		 * should span.
		 *
		 * @param horizontalAlignment Horizontal alignment strategy
		 * @param verticalAlignment Vertical alignment strategy
		 * @param grabExtraHorizontalSpace If set to `true`, this component will ask to be assigned extra horizontal
		 * space if there is any to assign
		 * @param grabExtraVerticalSpace If set to `true`, this component will ask to be assigned extra vertical
		 * space if there is any to assign
		 * @param horizontalSpan How many "cells" this component wants to span horizontally
		 * @param verticalSpan How many "cells" this component wants to span vertically
		 * @return The layout data object containing the specified alignments, size requirements and cell spanning
		 */
		@JvmOverloads
		fun createLayoutData(
			horizontalAlignment: Alignment,
			verticalAlignment: Alignment,
			grabExtraHorizontalSpace: Boolean = false,
			grabExtraVerticalSpace: Boolean = false,
			horizontalSpan: Int = 1,
			verticalSpan: Int = 1): LayoutData {

			return GridLayoutData(
				horizontalAlignment,
				verticalAlignment,
				grabExtraHorizontalSpace,
				grabExtraVerticalSpace,
				horizontalSpan,
				verticalSpan)
		}

		/**
		 * This is a shortcut method that will create a grid layout data object that will expand its cell as much as is can
		 * horizontally and make the component occupy the whole area horizontally and center it vertically
		 * @param horizontalSpan How many cells to span horizontally
		 * @return Layout data object with the specified span and horizontally expanding as much as it can
		 */
		fun createHorizontallyFilledLayoutData(horizontalSpan: Int): LayoutData {
			return createLayoutData(
				Alignment.FILL,
				Alignment.CENTER,
				true,
				false,
				horizontalSpan,
				1)
		}

		/**
		 * This is a shortcut method that will create a grid layout data object that will expand its cell as much as is can
		 * vertically and make the component occupy the whole area vertically and center it horizontally
		 * @param horizontalSpan How many cells to span vertically
		 * @return Layout data object with the specified span and vertically expanding as much as it can
		 */
		fun createHorizontallyEndAlignedLayoutData(horizontalSpan: Int): LayoutData {
			return createLayoutData(
				Alignment.END,
				Alignment.CENTER,
				true,
				false,
				horizontalSpan,
				1)
		}
	}
}
/**
 * Creates a layout data object for `GridLayout`:s that specify the horizontal and vertical alignment for the
 * component in case the cell space is larger than the preferred size of the component
 * @param horizontalAlignment Horizontal alignment strategy
 * @param verticalAlignment Vertical alignment strategy
 * @return The layout data object containing the specified alignments
 */
/**
 * Creates a layout data object for `GridLayout`:s that specify the horizontal and vertical alignment for the
 * component in case the cell space is larger than the preferred size of the component. This method also has fields
 * for indicating that the component would like to take more space if available to the container. For example, if
 * the container is assigned is assigned an area of 50x15, but all the child components in the grid together only
 * asks for 40x10, the remaining 10 columns and 5 rows will be empty. If just a single component asks for extra
 * space horizontally and/or vertically, the grid will expand out to fill the entire area and the text space will be
 * assigned to the component that asked for it.
 *
 * @param horizontalAlignment Horizontal alignment strategy
 * @param verticalAlignment Vertical alignment strategy
 * @param grabExtraHorizontalSpace If set to `true`, this component will ask to be assigned extra horizontal
 * space if there is any to assign
 * @param grabExtraVerticalSpace If set to `true`, this component will ask to be assigned extra vertical
 * space if there is any to assign
 * @return The layout data object containing the specified alignments and size requirements
 */
