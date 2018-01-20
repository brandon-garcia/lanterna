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
import com.googlecode.lanterna.TerminalTextUtils
import com.googlecode.lanterna.graphics.ThemeDefinition

/**
 * This GUI element gives a visual indication of how far a process of some sort has progressed at any given time. It's
 * a classic user interface component that most people are familiar with. It works based on a scale expressed as having
 * a *minimum*, a *maximum* and a current *value* somewhere along that range. When the current
 * *value* is the same as the *minimum*, the progress indication is empty, at 0%. If the *value* is the
 * same as the *maximum*, the progress indication is filled, at 100%. Any *value* in between the
 * *minimum* and the *maximum* will be indicated proportionally to where on this range between *minimum*
 * and *maximum* it is.
 *
 *
 * In order to add a label to the progress bar, for example to print the % completed, this class supports adding a
 * format specification. This label format, before drawing, will be passed in through a `String.format(..)` with
 * the current progress of *value* from *minimum* to *maximum* expressed as a `float` passed in as
 * a single vararg parameter. This parameter will be scaled from 0.0f to 100.0f. By default, the label format is set to
 * "%2.0f%%" which becomes a simple percentage string when formatted.
 * @author Martin
 */
class ProgressBar
/**
 * Creates a new progress bar with a defined range of minimum to maximum and also with a hint as to how wide the
 * progress bar should be drawn
 * @param min The minimum value of this progress bar
 * @param max The maximum value of this progress bar
 * @param preferredWidth Width size hint, in number of columns, for this progress bar. The renderer may choose to
 * not use this hint. 0 or less means that there is no hint.
 */
@JvmOverloads constructor(min: Int = 0, private var max: Int = 100, preferredWidth: Int = 0) : AbstractComponent<ProgressBar>() {

	private var min: Int = 0
	private var value: Int = 0
	/**
	 * Returns the preferred width of the progress bar component, in number of columns. If 0 or less, it should be
	 * interpreted as no preference on width and it's up to the renderer to decide.
	 * @return Preferred width this progress bar would like to have, or 0 (or less) if no preference
	 */
	/**
	 * Updated the preferred width hint, which tells the renderer how wide this progress bar would like to be. If called
	 * with 0 (or less), it means no preference on width and the renderer will have to figure out on its own how wide
	 * to make it.
	 * @param preferredWidth New preferred width in number of columns, or 0 if no preference
	 */
	var preferredWidth: Int = 0
	private var labelFormat: String? = null

	/**
	 * Returns the current progress of this progress bar's *value* from *minimum* to *maximum*, expressed
	 * as a float from 0.0f to 1.0f.
	 * @return current progress of this progress bar expressed as a float from 0.0f to 1.0f.
	 */
	val progress: Float
		@Synchronized get() = (value - min).toFloat() / max.toFloat()

	/**
	 * Returns the label of this progress bar formatted through `String.format(..)` with the current progress
	 * value.
	 * @return The progress bar label formatted with the current progress
	 */
	val formattedLabel: String
		@Synchronized get() = if (labelFormat == null) {
			""
		} else String.format(labelFormat!!, progress * 100.0f)

	init {
		var min = min
		var preferredWidth = preferredWidth
		if (min > max) {
			min = max
		}
		this.min = min
		this.value = min
		this.labelFormat = "%2.0f%%"

		if (preferredWidth < 1) {
			preferredWidth = 1
		}
		this.preferredWidth = preferredWidth
	}

	/**
	 * Returns the current *minimum* value for this progress bar
	 * @return The *minimum* value of this progress bar
	 */
	fun getMin(): Int {
		return min
	}

	/**
	 * Updates the *minimum* value of this progress bar. If the current *maximum* and/or *value* are
	 * smaller than this new *minimum*, they are automatically adjusted so that the range is still valid.
	 * @param min New *minimum* value to assign to this progress bar
	 * @return Itself
	 */
	@Synchronized
	fun setMin(min: Int): ProgressBar {
		if (min > max) {
			setMax(min)
		}
		if (min > value) {
			setValue(min)
		}
		if (this.min != min) {
			this.min = min
			invalidate()
		}
		return this
	}

	/**
	 * Returns the current *maximum* value for this progress bar
	 * @return The *maximum* value of this progress bar
	 */
	fun getMax(): Int {
		return max
	}

	/**
	 * Updates the *maximum* value of this progress bar. If the current *minimum* and/or *value* are
	 * greater than this new *maximum*, they are automatically adjusted so that the range is still valid.
	 * @param max New *maximum* value to assign to this progress bar
	 * @return Itself
	 */
	@Synchronized
	fun setMax(max: Int): ProgressBar {
		if (max < min) {
			setMin(max)
		}
		if (max < value) {
			setValue(max)
		}
		if (this.max != max) {
			this.max = max
			invalidate()
		}
		return this
	}

	/**
	 * Returns the current *value* of this progress bar, which represents how complete the progress indication is.
	 * @return The progress value of this progress bar
	 */
	fun getValue(): Int {
		return value
	}

	/**
	 * Updates the *value* of this progress bar, which will update the visual state. If the value passed in is
	 * outside the *minimum-maximum* range, it is automatically adjusted.
	 * @param value New value of the progress bar
	 * @return Itself
	 */
	@Synchronized
	fun setValue(value: Int): ProgressBar {
		var value = value
		if (value < min) {
			value = min
		}
		if (value > max) {
			value = max
		}
		if (this.value != value) {
			this.value = value
			invalidate()
		}
		return this
	}

	/**
	 * Returns the current label format string which is the template for what the progress bar would like to be the
	 * label printed. Exactly how this label is printed depends on the renderer, but the default renderer will print
	 * the label centered in the middle of the progress indication.
	 * @return The label format template string this progress bar is currently using
	 */
	fun getLabelFormat(): String? {
		return labelFormat
	}

	/**
	 * Sets the label format this progress bar should use when the component is drawn. The string would be compatible
	 * with `String.format(..)`, the class will pass the string through that method and pass in the current
	 * progress as a single vararg parameter (passed in as a `float` in the range of 0.0f to 100.0f). Setting this
	 * format string to null or empty string will turn off the label rendering.
	 * @param labelFormat Label format to use when drawing the progress bar, or `null` to disable
	 * @return Itself
	 */
	@Synchronized
	fun setLabelFormat(labelFormat: String): ProgressBar {
		this.labelFormat = labelFormat
		invalidate()
		return this
	}

	override fun createDefaultRenderer(): ComponentRenderer<ProgressBar> {
		return DefaultProgressBarRenderer()
	}

	/**
	 * Default implementation of the progress bar GUI component renderer. This renderer will draw the progress bar
	 * on a single line and gradually fill up the space with a different color as the progress is increasing.
	 */
	class DefaultProgressBarRenderer : ComponentRenderer<ProgressBar> {
		override fun getPreferredSize(component: ProgressBar): TerminalSize {
			val preferredWidth = component.preferredWidth
			return if (preferredWidth > 0) {
				TerminalSize(preferredWidth, 1)
			} else if (component.getLabelFormat() != null && !component.getLabelFormat()!!.trim { it <= ' ' }.isEmpty()) {
				TerminalSize(TerminalTextUtils.getColumnWidth(String.format(component.getLabelFormat()!!, 100.0f)) + 2, 1)
			} else {
				TerminalSize(10, 1)
			}
		}

		override fun drawComponent(graphics: TextGUIGraphics, component: ProgressBar) {
			val size = graphics.size
			if (size.rows == 0 || size.columns == 0) {
				return
			}
			val themeDefinition = component.themeDefinition
			val columnOfProgress = (component.progress * size.columns).toInt()
			var label = component.formattedLabel
			val labelRow = size.rows / 2

			// Adjust label so it fits inside the component
			var labelWidth = TerminalTextUtils.getColumnWidth(label)

			// Can't be too smart about this, because of CJK characters
			if (labelWidth > size.columns) {
				var tail = true
				while (labelWidth > size.columns) {
					if (tail) {
						label = label.substring(0, label.length - 1)
					} else {
						label = label.substring(1)
					}
					tail = !tail
					labelWidth = TerminalTextUtils.getColumnWidth(label)
				}
			}
			val labelStartPosition = (size.columns - labelWidth) / 2

			for (row in 0 until size.rows) {
				graphics.applyThemeStyle(themeDefinition.active)
				var column = 0
				while (column < size.columns) {
					if (column == columnOfProgress) {
						graphics.applyThemeStyle(themeDefinition.normal)
					}
					if (row == labelRow && column >= labelStartPosition && column < labelStartPosition + labelWidth) {
						val character = label[TerminalTextUtils.getStringCharacterIndex(label, column - labelStartPosition)]
						graphics.setCharacter(column, row, character)
						if (TerminalTextUtils.isCharDoubleWidth(character)) {
							column++
							if (column == columnOfProgress) {
								graphics.applyThemeStyle(themeDefinition.normal)
							}
						}
					} else {
						graphics.setCharacter(column, row, themeDefinition.getCharacter("FILLER", ' '))
					}
					column++
				}
			}
		}
	}

	/**
	 * This progress bar renderer implementation takes slightly more space (three rows) and draws a slightly more
	 * complicates progress bar with fixed measurers to mark 25%, 50% and 75%. Maybe you have seen this one before
	 * somewhere?
	 */
	class LargeProgressBarRenderer : ComponentRenderer<ProgressBar> {
		override fun getPreferredSize(component: ProgressBar): TerminalSize {
			val preferredWidth = component.preferredWidth
			return if (preferredWidth > 0) {
				TerminalSize(preferredWidth, 3)
			} else {
				TerminalSize(42, 3)
			}
		}

		override fun drawComponent(graphics: TextGUIGraphics, component: ProgressBar) {
			val size = graphics.size
			if (size.rows == 0 || size.columns == 0) {
				return
			}
			val themeDefinition = component.themeDefinition
			val columnOfProgress = (component.progress * (size.columns - 4)).toInt()
			var mark25 = -1
			var mark50 = -1
			var mark75 = -1

			if (size.columns > 9) {
				mark50 = (size.columns - 2) / 2
			}
			if (size.columns > 16) {
				mark25 = (size.columns - 2) / 4
				mark75 = mark50 + mark25
			}

			// Draw header, if there are at least 3 rows available
			var rowOffset = 0
			if (size.rows >= 3) {
				graphics.applyThemeStyle(themeDefinition.normal)
				graphics.drawLine(0, 0, size.columns, 0, ' ')
				if (size.columns > 1) {
					graphics.setCharacter(1, 0, '0')
				}
				if (mark25 != -1) {
					if (component.progress < 0.25f) {
						graphics.applyThemeStyle(themeDefinition.insensitive)
					}
					graphics.putString(1 + mark25, 0, "25")
				}
				if (mark50 != -1) {
					if (component.progress < 0.50f) {
						graphics.applyThemeStyle(themeDefinition.insensitive)
					}
					graphics.putString(1 + mark50, 0, "50")
				}
				if (mark75 != -1) {
					if (component.progress < 0.75f) {
						graphics.applyThemeStyle(themeDefinition.insensitive)
					}
					graphics.putString(1 + mark75, 0, "75")
				}
				if (size.columns >= 7) {
					if (component.progress < 1.0f) {
						graphics.applyThemeStyle(themeDefinition.insensitive)
					}
					graphics.putString(size.columns - 3, 0, "100")
				}
				rowOffset++
			}

			// Draw the main indicator
			for (i in 0 until Math.max(1, size.rows - 2)) {
				graphics.applyThemeStyle(themeDefinition.normal)
				graphics.drawLine(0, rowOffset, size.columns, rowOffset, ' ')
				if (size.columns > 2) {
					graphics.setCharacter(1, rowOffset, Symbols.SINGLE_LINE_VERTICAL)
				}
				if (size.columns > 3) {
					graphics.setCharacter(size.columns - 2, rowOffset, Symbols.SINGLE_LINE_VERTICAL)
				}
				if (size.columns > 4) {
					graphics.applyThemeStyle(themeDefinition.active)
					for (columnOffset in 2 until size.columns - 2) {
						if (columnOfProgress + 2 == columnOffset) {
							graphics.applyThemeStyle(themeDefinition.normal)
						}
						if (mark25 == columnOffset - 1) {
							graphics.setCharacter(columnOffset, rowOffset, Symbols.SINGLE_LINE_VERTICAL)
						} else if (mark50 == columnOffset - 1) {
							graphics.setCharacter(columnOffset, rowOffset, Symbols.SINGLE_LINE_VERTICAL)
						} else if (mark75 == columnOffset - 1) {
							graphics.setCharacter(columnOffset, rowOffset, Symbols.SINGLE_LINE_VERTICAL)
						} else {
							graphics.setCharacter(columnOffset, rowOffset, ' ')
						}
					}
				}

				if ((component.progress * ((size.columns - 4) * 2)).toInt() % 2 == 1) {
					graphics.applyThemeStyle(themeDefinition.preLight)
					graphics.setCharacter(columnOfProgress + 2, rowOffset, '|')
				}

				rowOffset++
			}

			// Draw footer if there are at least 2 rows, this one is easy
			if (size.rows >= 2) {
				graphics.applyThemeStyle(themeDefinition.normal)
				graphics.drawLine(0, rowOffset, size.columns, rowOffset, Symbols.SINGLE_LINE_T_UP)
				graphics.setCharacter(0, rowOffset, ' ')
				if (size.columns > 1) {
					graphics.setCharacter(size.columns - 1, rowOffset, ' ')
				}
				if (size.columns > 2) {
					graphics.setCharacter(1, rowOffset, Symbols.SINGLE_LINE_BOTTOM_LEFT_CORNER)
				}
				if (size.columns > 3) {
					graphics.setCharacter(size.columns - 2, rowOffset, Symbols.SINGLE_LINE_BOTTOM_RIGHT_CORNER)
				}
			}
		}
	}
}
/**
 * Creates a new progress bar initially defined with a range from 0 to 100. The
 */
/**
 * Creates a new progress bar with a defined range of minimum to maximum
 * @param min The minimum value of this progress bar
 * @param max The maximum value of this progress bar
 */
