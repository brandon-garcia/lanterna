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
package com.googlecode.lanterna

/**
 * Terminal dimensions in 2-d space, measured in number of rows and columns. This class is immutable and cannot change
 * its internal state after creation.
 *
 * @author Martin
 */
class TerminalSize
/**
 * Creates a new terminal size representation with a given width (columns) and height (rows)
 * @param columns Width, in number of columns
 * @param rows Height, in number of columns
 */
(
	/**
	 * @return Returns the width of this size representation, in number of columns
	 */
	val columns: Int,
	/**
	 * @return Returns the height of this size representation, in number of rows
	 */
	val rows: Int) {

	init {
		if (columns < 0) {
			throw IllegalArgumentException("TerminalSize.columns cannot be less than 0!")
		}
		if (rows < 0) {
			throw IllegalArgumentException("TerminalSize.rows cannot be less than 0!")
		}
	}

	/**
	 * Creates a new size based on this size, but with a different width
	 * @param columns Width of the new size, in columns
	 * @return New size based on this one, but with a new width
	 */
	fun withColumns(columns: Int) =
		if (this.columns == columns) {
			this
		} else {
			if (columns == 0 && this.rows == 0) {
				ZERO
			} else TerminalSize(columns, this.rows)
		}

	/**
	 * Creates a new size based on this size, but with a different height
	 * @param rows Height of the new size, in rows
	 * @return New size based on this one, but with a new height
	 */
	fun withRows(rows: Int) =
		if (this.rows == rows) {
			this
		} else {
			if (rows == 0 && this.columns == 0) {
				ZERO
			} else TerminalSize(this.columns, rows)
		}

	/**
	 * Creates a new TerminalSize object representing a size with the same number of rows, but with a column size offset by a
	 * supplied value. Calling this method with delta 0 will return this, calling it with a positive delta will return
	 * a terminal size *delta* number of columns wider and for negative numbers shorter.
	 * @param delta Column offset
	 * @return New terminal size based off this one but with an applied transformation
	 */
	fun withRelativeColumns(delta: Int) =
		if (delta == 0) {
			this
		} else withColumns(columns + delta)

	/**
	 * Creates a new TerminalSize object representing a size with the same number of columns, but with a row size offset by a
	 * supplied value. Calling this method with delta 0 will return this, calling it with a positive delta will return
	 * a terminal size *delta* number of rows longer and for negative numbers shorter.
	 * @param delta Row offset
	 * @return New terminal size based off this one but with an applied transformation
	 */
	fun withRelativeRows(delta: Int) =
		if (delta == 0) {
			this
		} else withRows(rows + delta)

	/**
	 * Creates a new TerminalSize object representing a size based on this object's size but with a delta applied.
	 * This is the same as calling
	 * `withRelativeColumns(delta.getColumns()).withRelativeRows(delta.getRows())`
	 * @param delta Column and row offset
	 * @return New terminal size based off this one but with an applied resize
	 */
	fun withRelative(delta: TerminalSize) =
		withRelative(delta.columns, delta.rows)

	/**
	 * Creates a new TerminalSize object representing a size based on this object's size but with a delta applied.
	 * This is the same as calling
	 * `withRelativeColumns(deltaColumns).withRelativeRows(deltaRows)`
	 * @param deltaColumns How many extra columns the new TerminalSize will have (negative values are allowed)
	 * @param deltaRows How many extra rows the new TerminalSize will have (negative values are allowed)
	 * @return New terminal size based off this one but with an applied resize
	 */
	fun withRelative(deltaColumns: Int, deltaRows: Int) =
		withRelativeRows(deltaRows).withRelativeColumns(deltaColumns)

	/**
	 * Takes a different TerminalSize and returns a new TerminalSize that has the largest dimensions of the two,
	 * measured separately. So calling 3x5 on a 5x3 will return 5x5.
	 * @param other Other TerminalSize to compare with
	 * @return TerminalSize that combines the maximum width between the two and the maximum height
	 */
	fun max(other: TerminalSize) =
		withColumns(Math.max(columns, other.columns))
			.withRows(Math.max(rows, other.rows))

	/**
	 * Takes a different TerminalSize and returns a new TerminalSize that has the smallest dimensions of the two,
	 * measured separately. So calling 3x5 on a 5x3 will return 3x3.
	 * @param other Other TerminalSize to compare with
	 * @return TerminalSize that combines the minimum width between the two and the minimum height
	 */
	fun min(other: TerminalSize) =
		withColumns(Math.min(columns, other.columns))
			.withRows(Math.min(rows, other.rows))

	/**
	 * Returns itself if it is equal to the supplied size, otherwise the supplied size. You can use this if you have a
	 * size field which is frequently recalculated but often resolves to the same size; it will keep the same object
	 * in memory instead of swapping it out every cycle.
	 * @param size Size you want to return
	 * @return Itself if this size equals the size passed in, otherwise the size passed in
	 */
	fun with(size: TerminalSize) =
		if (equals(size)) {
			this
		} else size

	override fun toString() =
		"{" + columns + "x" + rows + "}"

	override fun equals(obj: Any?): Boolean {
		if (this === obj) {
			return true
		}
		if (obj !is TerminalSize) {
			return false
		}

		val other = obj as TerminalSize?
		return columns == other!!.columns && rows == other.rows
	}

	override fun hashCode(): Int {
		var hash = 5
		hash = 53 * hash + this.columns
		hash = 53 * hash + this.rows
		return hash
	}

	companion object {
		val ZERO = TerminalSize(0, 0)
		val ONE = TerminalSize(1, 1)
	}
}
