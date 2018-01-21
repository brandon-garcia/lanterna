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

import java.util.*

/**
 * A `TableModel` contains the data model behind a table, here is where all the action cell values and header
 * labels are stored.
 *
 * @author Martin
 */
class TableModel<V>
/**
 * Default constructor, creates a new model with same number of columns as labels supplied
 * @param columnLabels Labels for the column headers
 */
(vararg columnLabels: String) {

	private val columns: MutableList<String>
	private val rows: MutableList<List<V>>
	private val listeners: MutableList<Listener<V>>

	/**
	 * Returns the number of columns in the model
	 * @return Number of columns in the model
	 */
	val columnCount: Int
		@Synchronized get() = columns.size

	/**
	 * Returns number of rows in the model
	 * @return Number of rows in the model
	 */
	val rowCount: Int
		@Synchronized get() = rows.size

	/**
	 * Returns all column header label as a list of strings
	 * @return All column header label as a list of strings
	 */
	val columnLabels: List<String>
		@Synchronized get() = ArrayList(columns)

	/**
	 * Listener interface for the [TableModel] class which can be attached to a [TableModel] to be notified
	 * of changes to the table model.
	 * @param <V> Value type stored in the table
	</V> */
	interface Listener<V> {
		/**
		 * Called when a new row has been added to the model
		 * @param model Model the row was added to
		 * @param index Index of the new row
		 */
		fun onRowAdded(model: TableModel<V>, index: Int)

		/**
		 * Called when a row has been removed from the model
		 * @param model Model the row was removed from
		 * @param index Index of the removed row
		 * @param oldRow Content of the row that was removed
		 */
		fun onRowRemoved(model: TableModel<V>, index: Int, oldRow: List<V>)

		/**
		 * Called when a new column has been added to the model
		 * @param model Model the column was added to
		 * @param index Index of the new column
		 */
		fun onColumnAdded(model: TableModel<V>, index: Int)

		/**
		 * Called when a column has been removed from the model
		 * @param model Model the column was removed from
		 * @param index Index of the removed column
		 * @param oldHeader Header the removed column had
		 * @param oldColumn Values in the removed column
		 */
		fun onColumnRemoved(model: TableModel<V>, index: Int, oldHeader: String, oldColumn: List<V>)

		/**
		 * Called when an existing cell had its content updated
		 * @param model Model that was modified
		 * @param row Row index of the modified cell
		 * @param column Column index of the modified cell
		 * @param oldValue Previous value of the cell
		 * @param newValue New value of the cell
		 */
		fun onCellChanged(model: TableModel<V>, row: Int, column: Int, oldValue: V, newValue: V)
	}

	init {
		this.columns = ArrayList(Arrays.asList(*columnLabels))
		this.rows = ArrayList()
		this.listeners = ArrayList()
	}

	/**
	 * Returns all rows in the model as a list of lists containing the data as elements
	 * @return All rows in the model as a list of lists containing the data as elements
	 */
	@Synchronized
	fun getRows(): List<List<V>> {
		val copy = ArrayList<List<V>>()
		for (row in rows) {
			copy.add(ArrayList(row))
		}
		return copy
	}

	/**
	 * Returns a row from the table as a list of the cell data
	 * @param index Index of the row to return
	 * @return Row from the table as a list of the cell data
	 */
	@Synchronized
	fun getRow(index: Int): List<V> =
		ArrayList(rows[index])

	/**
	 * Adds a new row to the table model at the end
	 * @param values Data to associate with the new row, mapped column by column in order
	 * @return Itself
	 */
	@Synchronized
	fun addRow(vararg values: V): TableModel<V> {
		addRow(Arrays.asList(*values))
		return this
	}

	/**
	 * Adds a new row to the table model at the end
	 * @param values Data to associate with the new row, mapped column by column in order
	 * @return Itself
	 */
	@Synchronized
	fun addRow(values: Collection<V>): TableModel<V> {
		insertRow(rowCount, values)
		return this
	}

	/**
	 * Inserts a new row to the table model at a particular index
	 * @param index Index the new row should have, 0 means the first row and *row count* will append the row at the
	 * end
	 * @param values Data to associate with the new row, mapped column by column in order
	 * @return Itself
	 */
	@Synchronized
	fun insertRow(index: Int, values: Collection<V>): TableModel<V> {
		val list = ArrayList(values)
		rows.add(index, list)
		for (listener in listeners) {
			listener.onRowAdded(this, index)
		}
		return this
	}

	/**
	 * Removes a row at a particular index from the table model
	 * @param index Index of the row to remove
	 * @return Itself
	 */
	@Synchronized
	fun removeRow(index: Int): TableModel<V> {
		val removedRow = rows.removeAt(index)
		for (listener in listeners) {
			listener.onRowRemoved(this, index, removedRow)
		}
		return this
	}

	/**
	 * Returns the label of a column header
	 * @param index Index of the column to retrieve the header label for
	 * @return Label of the column selected
	 */
	@Synchronized
	fun getColumnLabel(index: Int) =
		columns[index]

	/**
	 * Updates the label of a column header
	 * @param index Index of the column to update the header label for
	 * @param newLabel New label to assign to the column header
	 * @return Itself
	 */
	@Synchronized
	fun setColumnLabel(index: Int, newLabel: String): TableModel<V> {
		columns[index] = newLabel
		return this
	}

	/**
	 * Adds a new column into the table model as the last column. You can optionally supply values for the existing rows
	 * through the `newColumnValues`.
	 * @param label Label for the header of the new column
	 * @param newColumnValues Optional values to assign to the existing rows, where the first element in the array will
	 * be the value of the first row and so on...
	 * @return Itself
	 */
	@Synchronized
	fun addColumn(label: String, newColumnValues: Array<V>): TableModel<V> =
		insertColumn(columnCount, label, newColumnValues)

	/**
	 * Adds a new column into the table model at a specified index. You can optionally supply values for the existing
	 * rows through the `newColumnValues`.
	 * @param index Index for the new column
	 * @param label Label for the header of the new column
	 * @param newColumnValues Optional values to assign to the existing rows, where the first element in the array will
	 * be the value of the first row and so on...
	 * @return Itself
	 */
	@Synchronized
	fun insertColumn(index: Int, label: String, newColumnValues: Array<V>?): TableModel<V> {
		columns.add(index, label)
		for (i in rows.indices) {
			val row = rows[i]

			//Pad row with null if necessary
			for (j in row.size until index) {
				row.add(null)
			}

			if (newColumnValues != null && i < newColumnValues.size && newColumnValues[i] != null) {
				row.add(index, newColumnValues[i])
			} else {
				row.add(index, null)
			}
		}

		for (listener in listeners) {
			listener.onColumnAdded(this, index)
		}
		return this
	}

	/**
	 * Removes a column from the table model
	 * @param index Index of the column to remove
	 * @return Itself
	 */
	@Synchronized
	fun removeColumn(index: Int): TableModel<V> {
		val removedColumnHeader = columns.removeAt(index)
		val removedColumn = ArrayList<V>()
		for (row in rows) {
			removedColumn.add(row.removeAt(index))
		}
		for (listener in listeners) {
			listener.onColumnRemoved(this, index, removedColumnHeader, removedColumn)
		}
		return this
	}

	/**
	 * Returns the cell value stored at a specific column/row coordinate.
	 * @param columnIndex Column index of the cell
	 * @param rowIndex Row index of the cell
	 * @return The data value stored in this cell
	 */
	@Synchronized
	fun getCell(columnIndex: Int, rowIndex: Int): V {
		if (rowIndex < 0 || columnIndex < 0) {
			throw IndexOutOfBoundsException("Invalid row or column index: $rowIndex $columnIndex")
		} else if (rowIndex >= rowCount) {
			throw IndexOutOfBoundsException("TableModel has $rowCount rows, invalid access at rowIndex $rowIndex")
		}
		if (columnIndex >= columnCount) {
			throw IndexOutOfBoundsException("TableModel has $columnIndex columns, invalid access at columnIndex $columnIndex")
		}
		return rows[rowIndex][columnIndex]
	}

	/**
	 * Updates the call value stored at a specific column/row coordinate.
	 * @param columnIndex Column index of the cell
	 * @param rowIndex Row index of the cell
	 * @param value New value to assign to the cell
	 * @return Itself
	 */
	@Synchronized
	fun setCell(columnIndex: Int, rowIndex: Int, value: V): TableModel<V> {
		getCell(columnIndex, rowIndex)
		val row = rows[rowIndex]

		//Pad row with null if necessary
		for (j in row.size until columnIndex) {
			row.add(null)
		}

		val existingValue = row[columnIndex]
		if (existingValue === value) {
			return this
		}
		row.set(columnIndex, value)
		for (listener in listeners) {
			listener.onCellChanged(this, rowIndex, columnIndex, existingValue, value)
		}
		return this
	}

	/**
	 * Adds a listener to this table model that will be notified whenever the model changes
	 * @param listener [Listener] to register with this model
	 * @return Itself
	 */
	fun addListener(listener: Listener<V>): TableModel<V> {
		listeners.add(listener)
		return this
	}

	/**
	 * Removes a listener from this model so that it will no longer receive any notifications when the model changes
	 * @param listener [Listener] to deregister from this model
	 * @return Itself
	 */
	fun removeListener(listener: Listener<V>): TableModel<V> {
		listeners.remove(listener)
		return this
	}
}
