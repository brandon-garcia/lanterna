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

/**
 * Simple layout manager the puts all components on a single line, either horizontally or vertically.
 */
class LinearLayout
/**
 * Standard constructor that creates a `LinearLayout` with a specified direction to position the components on
 * @param direction Direction for this `Direction`
 */
@JvmOverloads constructor(private val direction: Direction = Direction.VERTICAL) : LayoutManager {
	private var spacing: Int = 0
	private var changed: Boolean = false

	/**
	 * This enum type will decide the alignment of a component on the counter-axis, meaning the horizontal alignment on
	 * vertical `LinearLayout`s and vertical alignment on horizontal `LinearLayout`s.
	 */
	enum class Alignment {
		/**
		 * The component will be placed to the left (for vertical layouts) or top (for horizontal layouts)
		 */
		Beginning,
		/**
		 * The component will be placed horizontally centered (for vertical layouts) or vertically centered (for
		 * horizontal layouts)
		 */
		Center,
		/**
		 * The component will be placed to the right (for vertical layouts) or bottom (for horizontal layouts)
		 */
		End,
		/**
		 * The component will be forced to take up all the horizontal space (for vertical layouts) or vertical space
		 * (for horizontal layouts)
		 */
		Fill
	}

	private class LinearLayoutData(private val alignment: Alignment) : LayoutData

	init {
		this.spacing = if (direction == Direction.HORIZONTAL) 1 else 0
		this.changed = true
	}

	/**
	 * Sets the amount of empty space to put in between components. For horizontal layouts, this is number of columns
	 * (by default 1) and for vertical layouts this is number of rows (by default 0).
	 * @param spacing Spacing between components, either in number of columns or rows depending on the direction
	 * @return Itself
	 */
	fun setSpacing(spacing: Int): LinearLayout {
		this.spacing = spacing
		this.changed = true
		return this
	}

	/**
	 * Returns the amount of empty space to put in between components. For horizontal layouts, this is number of columns
	 * (by default 1) and for vertical layouts this is number of rows (by default 0).
	 * @return Spacing between components, either in number of columns or rows depending on the direction
	 */
	fun getSpacing(): Int {
		return spacing
	}

	override fun getPreferredSize(components: List<Component>): TerminalSize {
		return if (direction == Direction.VERTICAL) {
			getPreferredSizeVertically(components)
		} else {
			getPreferredSizeHorizontally(components)
		}
	}

	private fun getPreferredSizeVertically(components: List<Component>): TerminalSize {
		var maxWidth = 0
		var height = 0
		for (component in components) {
			val preferredSize = component.preferredSize
			if (maxWidth < preferredSize.columns) {
				maxWidth = preferredSize.columns
			}
			height += preferredSize.rows
		}
		height += spacing * (components.size - 1)
		return TerminalSize(maxWidth, Math.max(0, height))
	}

	private fun getPreferredSizeHorizontally(components: List<Component>): TerminalSize {
		var maxHeight = 0
		var width = 0
		for (component in components) {
			val preferredSize = component.preferredSize
			if (maxHeight < preferredSize.rows) {
				maxHeight = preferredSize.rows
			}
			width += preferredSize.columns
		}
		width += spacing * (components.size - 1)
		return TerminalSize(Math.max(0, width), maxHeight)
	}

	override fun hasChanged(): Boolean {
		return changed
	}

	override fun doLayout(area: TerminalSize, components: List<Component>) {
		if (direction == Direction.VERTICAL) {
			doVerticalLayout(area, components)
		} else {
			doHorizontalLayout(area, components)
		}
		this.changed = false
	}

	private fun doVerticalLayout(area: TerminalSize, components: List<Component>) {
		var remainingVerticalSpace = area.rows
		val availableHorizontalSpace = area.columns
		for (component in components) {
			if (remainingVerticalSpace <= 0) {
				component.position = TerminalPosition.TOP_LEFT_CORNER
				component.size = TerminalSize.ZERO
			} else {
				var alignment = Alignment.Beginning
				val layoutData = component.layoutData
				if (layoutData is LinearLayoutData) {
					alignment = layoutData.alignment
				}

				val preferredSize = component.preferredSize
				var decidedSize = TerminalSize(
					Math.min(availableHorizontalSpace, preferredSize.columns),
					Math.min(remainingVerticalSpace, preferredSize.rows))
				if (alignment == Alignment.Fill) {
					decidedSize = decidedSize.withColumns(availableHorizontalSpace)
					alignment = Alignment.Beginning
				}

				var position = component.position
				position = position.withRow(area.rows - remainingVerticalSpace)
				when (alignment) {
					LinearLayout.Alignment.End -> position = position.withColumn(availableHorizontalSpace - decidedSize.columns)
					LinearLayout.Alignment.Center -> position = position.withColumn((availableHorizontalSpace - decidedSize.columns) / 2)
					LinearLayout.Alignment.Beginning -> position = position.withColumn(0)
					else -> position = position.withColumn(0)
				}
				component.position = position
				component.size = component.size.with(decidedSize)
				remainingVerticalSpace -= decidedSize.rows + spacing
			}
		}
	}

	private fun doHorizontalLayout(area: TerminalSize, components: List<Component>) {
		var remainingHorizontalSpace = area.columns
		val availableVerticalSpace = area.rows
		for (component in components) {
			if (remainingHorizontalSpace <= 0) {
				component.position = TerminalPosition.TOP_LEFT_CORNER
				component.size = TerminalSize.ZERO
			} else {
				var alignment = Alignment.Beginning
				val layoutData = component.layoutData
				if (layoutData is LinearLayoutData) {
					alignment = layoutData.alignment
				}

				val preferredSize = component.preferredSize
				var decidedSize = TerminalSize(
					Math.min(remainingHorizontalSpace, preferredSize.columns),
					Math.min(availableVerticalSpace, preferredSize.rows))
				if (alignment == Alignment.Fill) {
					decidedSize = decidedSize.withRows(availableVerticalSpace)
					alignment = Alignment.Beginning
				}

				var position = component.position
				position = position.withColumn(area.columns - remainingHorizontalSpace)
				when (alignment) {
					LinearLayout.Alignment.End -> position = position.withRow(availableVerticalSpace - decidedSize.rows)
					LinearLayout.Alignment.Center -> position = position.withRow((availableVerticalSpace - decidedSize.rows) / 2)
					LinearLayout.Alignment.Beginning -> position = position.withRow(0)
					else -> position = position.withRow(0)
				}
				component.position = position
				component.size = component.size.with(decidedSize)
				remainingHorizontalSpace -= decidedSize.columns + spacing
			}
		}
	}

	companion object {

		/**
		 * Creates a `LayoutData` for `LinearLayout` that assigns a component to a particular alignment on its
		 * counter-axis, meaning the horizontal alignment on vertical `LinearLayout`s and vertical alignment on
		 * horizontal `LinearLayout`s.
		 * @param alignment Alignment to store in the `LayoutData` object
		 * @return `LayoutData` object created for `LinearLayout`s with the specified alignment
		 * @see Alignment
		 */
		fun createLayoutData(alignment: Alignment): LayoutData {
			return LinearLayoutData(alignment)
		}
	}
}
/**
 * Default constructor, creates a vertical `LinearLayout`
 */
