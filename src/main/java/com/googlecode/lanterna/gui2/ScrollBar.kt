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
 * Classic scrollbar that can be used to display where inside a larger component a view is showing. This implementation
 * is not interactable and needs to be driven externally, meaning you can't focus on the scrollbar itself, you have to
 * update its state as part of another component being modified. `ScrollBar`s are either horizontal or vertical,
 * which affects the way they appear and how they are drawn.
 *
 *
 * This class works on two concepts, the min-position-max values and the view size. The minimum value is always 0 and
 * cannot be changed. The maximum value is 100 and can be adjusted programmatically. Position value is whever along the
 * axis of 0 to max the scrollbar's tracker currently is placed. The view size is an important concept, it determines
 * how big the tracker should be and limits the position so that it can only reach `maximum value - view size`.
 *
 *
 * The regular way to use the `ScrollBar` class is to tie it to the model-view of another component and set the
 * scrollbar's maximum to the total height (or width, if the scrollbar is horizontal) of the model-view. View size
 * should then be assigned based on the current size of the view, meaning as the terminal and/or the GUI changes and the
 * components visible space changes, the scrollbar's view size is updated along with it. Finally the position of the
 * scrollbar should be equal to the scroll offset in the component.
 *
 * @author Martin
 */
class ScrollBar
/**
 * Creates a new `ScrollBar` with a specified direction
 * @param direction Direction of the scrollbar
 */
(
	/**
	 * Returns the direction of this `ScrollBar`
	 * @return Direction of this `ScrollBar`
	 */
	val direction: Direction) : AbstractComponent<ScrollBar>() {
	private var maximum: Int = 0
	private var position: Int = 0
	private var viewSize: Int = 0

	init {
		this.maximum = 100
		this.position = 0
		this.viewSize = 0
	}

	/**
	 * Sets the maximum value the scrollbar's position (minus the view size) can have
	 * @param maximum Maximum value
	 * @return Itself
	 */
	fun setScrollMaximum(maximum: Int): ScrollBar {
		if (maximum < 0) {
			throw IllegalArgumentException("Cannot set ScrollBar maximum to " + maximum)
		}
		this.maximum = maximum
		invalidate()
		return this
	}

	/**
	 * Returns the maximum scroll value
	 * @return Maximum scroll value
	 */
	fun getScrollMaximum() =
		maximum


	/**
	 * Sets the scrollbar's position, should be a value between 0 and `maximum - view size`
	 * @param position Scrollbar's tracker's position
	 * @return Itself
	 */
	fun setScrollPosition(position: Int): ScrollBar {
		this.position = Math.min(position, this.maximum)
		invalidate()
		return this
	}

	/**
	 * Returns the position of the `ScrollBar`'s tracker
	 * @return Position of the `ScrollBar`'s tracker
	 */
	fun getScrollPosition() =
		position

	/**
	 * Sets the view size of the scrollbar, determining how big the scrollbar's tracker should be and also affecting the
	 * maximum value of tracker's position
	 * @param viewSize View size of the scrollbar
	 * @return Itself
	 */
	fun setViewSize(viewSize: Int): ScrollBar {
		this.viewSize = viewSize
		return this
	}

	/**
	 * Returns the view size of the scrollbar
	 * @return View size of the scrollbar
	 */
	fun getViewSize(): Int {
		if (viewSize > 0) {
			return viewSize
		}
		return if (direction == Direction.HORIZONTAL) {
			size!!.columns
		} else {
			size!!.rows
		}
	}

	override fun createDefaultRenderer(): ComponentRenderer<ScrollBar> =
		DefaultScrollBarRenderer()

	/**
	 * Helper class for making new `ScrollBar` renderers a little bit cleaner
	 */
	abstract class ScrollBarRenderer : ComponentRenderer<ScrollBar> {
		override fun getPreferredSize(component: ScrollBar) =
			TerminalSize.ONE
	}

	/**
	 * Default renderer for `ScrollBar` which will be used unless overridden. This will draw a scrollbar using
	 * arrows at each extreme end, a background color for spaces between those arrows and the tracker and then the
	 * tracker itself in three different styles depending on the size of the tracker. All characters and colors are
	 * customizable through whatever theme is currently in use.
	 */
	class DefaultScrollBarRenderer : ScrollBarRenderer() {

		private var growScrollTracker: Boolean = false

		/**
		 * Default constructor
		 */
		init {
			this.growScrollTracker = true
		}

		/**
		 * Should tracker automatically grow in size along with the `ScrollBar` (default: `true`)
		 * @param growScrollTracker Automatically grow tracker
		 */
		fun setGrowScrollTracker(growScrollTracker: Boolean) {
			this.growScrollTracker = growScrollTracker
		}

		override fun drawComponent(graphics: TextGUIGraphics, component: ScrollBar) {
			val size = graphics.size
			val direction = component.direction
			var position = component.getScrollPosition()
			val maximum = component.getScrollMaximum()
			val viewSize = component.getViewSize()

			if (size.rows == 0 || size.columns == 0) {
				return
			}

			//Adjust position if necessary
			if (position + viewSize >= maximum) {
				position = Math.max(0, maximum - viewSize)
				component.setScrollPosition(position)
			}

			val themeDefinition = component.themeDefinition
			graphics.applyThemeStyle(themeDefinition.normal)

			if (direction == Direction.VERTICAL) {
				if (size.rows == 1) {
					graphics.setCharacter(0, 0, themeDefinition.getCharacter("VERTICAL_BACKGROUND", Symbols.BLOCK_MIDDLE))
				} else if (size.rows == 2) {
					graphics.setCharacter(0, 0, themeDefinition.getCharacter("UP_ARROW", Symbols.TRIANGLE_UP_POINTING_BLACK))
					graphics.setCharacter(0, 1, themeDefinition.getCharacter("DOWN_ARROW", Symbols.TRIANGLE_DOWN_POINTING_BLACK))
				} else {
					val scrollableArea = size.rows - 2
					var scrollTrackerSize = 1
					if (growScrollTracker) {
						val ratio = clampRatio(viewSize.toFloat() / maximum.toFloat())
						scrollTrackerSize = Math.max(1, (ratio * scrollableArea.toFloat()).toInt())
					}

					val ratio = clampRatio(position.toFloat() / (maximum - viewSize).toFloat())
					val scrollTrackerPosition = (ratio * (scrollableArea - scrollTrackerSize).toFloat()).toInt() + 1

					graphics.setCharacter(0, 0, themeDefinition.getCharacter("UP_ARROW", Symbols.TRIANGLE_UP_POINTING_BLACK))
					graphics.drawLine(0, 1, 0, size.rows - 2, themeDefinition.getCharacter("VERTICAL_BACKGROUND", Symbols.BLOCK_MIDDLE))
					graphics.setCharacter(0, size.rows - 1, themeDefinition.getCharacter("DOWN_ARROW", Symbols.TRIANGLE_DOWN_POINTING_BLACK))
					if (scrollTrackerSize == 1) {
						graphics.setCharacter(0, scrollTrackerPosition, themeDefinition.getCharacter("VERTICAL_SMALL_TRACKER", Symbols.BLOCK_SOLID))
					} else if (scrollTrackerSize == 2) {
						graphics.setCharacter(0, scrollTrackerPosition, themeDefinition.getCharacter("VERTICAL_TRACKER_TOP", Symbols.BLOCK_SOLID))
						graphics.setCharacter(0, scrollTrackerPosition + 1, themeDefinition.getCharacter("VERTICAL_TRACKER_BOTTOM", Symbols.BLOCK_SOLID))
					} else {
						graphics.setCharacter(0, scrollTrackerPosition, themeDefinition.getCharacter("VERTICAL_TRACKER_TOP", Symbols.BLOCK_SOLID))
						graphics.drawLine(0, scrollTrackerPosition + 1, 0, scrollTrackerPosition + scrollTrackerSize - 2, themeDefinition.getCharacter("VERTICAL_TRACKER_BACKGROUND", Symbols.BLOCK_SOLID))
						graphics.setCharacter(0, scrollTrackerPosition + scrollTrackerSize / 2, themeDefinition.getCharacter("VERTICAL_SMALL_TRACKER", Symbols.BLOCK_SOLID))
						graphics.setCharacter(0, scrollTrackerPosition + scrollTrackerSize - 1, themeDefinition.getCharacter("VERTICAL_TRACKER_BOTTOM", Symbols.BLOCK_SOLID))
					}
				}
			} else {
				if (size.columns == 1) {
					graphics.setCharacter(0, 0, themeDefinition.getCharacter("HORIZONTAL_BACKGROUND", Symbols.BLOCK_MIDDLE))
				} else if (size.columns == 2) {
					graphics.setCharacter(0, 0, Symbols.TRIANGLE_LEFT_POINTING_BLACK)
					graphics.setCharacter(1, 0, Symbols.TRIANGLE_RIGHT_POINTING_BLACK)
				} else {
					val scrollableArea = size.columns - 2
					var scrollTrackerSize = 1
					if (growScrollTracker) {
						val ratio = clampRatio(viewSize.toFloat() / maximum.toFloat())
						scrollTrackerSize = Math.max(1, (ratio * scrollableArea.toFloat()).toInt())
					}

					val ratio = clampRatio(position.toFloat() / (maximum - viewSize).toFloat())
					val scrollTrackerPosition = (ratio * (scrollableArea - scrollTrackerSize).toFloat()).toInt() + 1

					graphics.setCharacter(0, 0, themeDefinition.getCharacter("LEFT_ARROW", Symbols.TRIANGLE_LEFT_POINTING_BLACK))
					graphics.drawLine(1, 0, size.columns - 2, 0, themeDefinition.getCharacter("HORIZONTAL_BACKGROUND", Symbols.BLOCK_MIDDLE))
					graphics.setCharacter(size.columns - 1, 0, themeDefinition.getCharacter("RIGHT_ARROW", Symbols.TRIANGLE_RIGHT_POINTING_BLACK))
					if (scrollTrackerSize == 1) {
						graphics.setCharacter(scrollTrackerPosition, 0, themeDefinition.getCharacter("HORIZONTAL_SMALL_TRACKER", Symbols.BLOCK_SOLID))
					} else if (scrollTrackerSize == 2) {
						graphics.setCharacter(scrollTrackerPosition, 0, themeDefinition.getCharacter("HORIZONTAL_TRACKER_LEFT", Symbols.BLOCK_SOLID))
						graphics.setCharacter(scrollTrackerPosition + 1, 0, themeDefinition.getCharacter("HORIZONTAL_TRACKER_RIGHT", Symbols.BLOCK_SOLID))
					} else {
						graphics.setCharacter(scrollTrackerPosition, 0, themeDefinition.getCharacter("HORIZONTAL_TRACKER_LEFT", Symbols.BLOCK_SOLID))
						graphics.drawLine(scrollTrackerPosition + 1, 0, scrollTrackerPosition + scrollTrackerSize - 2, 0, themeDefinition.getCharacter("HORIZONTAL_TRACKER_BACKGROUND", Symbols.BLOCK_SOLID))
						graphics.setCharacter(scrollTrackerPosition + scrollTrackerSize / 2, 0, themeDefinition.getCharacter("HORIZONTAL_SMALL_TRACKER", Symbols.BLOCK_SOLID))
						graphics.setCharacter(scrollTrackerPosition + scrollTrackerSize - 1, 0, themeDefinition.getCharacter("HORIZONTAL_TRACKER_RIGHT", Symbols.BLOCK_SOLID))
					}
				}
			}
		}

		private fun clampRatio(value: Float): Float =
			if (value < 0.0f) {
				0.0f
			} else if (value > 1.0f) {
				1.0f
			} else {
				value
			}
	}
}
