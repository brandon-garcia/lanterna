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

import com.googlecode.lanterna.TerminalTextUtils
import com.googlecode.lanterna.SGR
import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.TextColor
import com.googlecode.lanterna.graphics.ThemeDefinition

import java.util.EnumSet

/**
 * Label is a simple read-only text display component. It supports customized colors and multi-line text.
 * @author Martin
 */
open class Label
/**
 * Main constructor, creates a new Label displaying a specific text.
 * @param text Text the label will display
 */
(text: String) : AbstractComponent<Label>() {
	private var lines: Array<String>? = null
	private var labelWidth: Int? = null
	private var labelSize: TerminalSize? = null
	private var foregroundColor: TextColor? = null
	private var backgroundColor: TextColor? = null
	private val additionalStyles: EnumSet<SGR>

	/**
	 * Returns the text this label is displaying. Multi-line labels will have their text concatenated with \n, even if
	 * they were originally set using multi-line text having \r\n as line terminators.
	 * @return String of the text this label is displaying
	 */
	/**
	 * Updates the text this label is displaying
	 * @param text New text to display
	 */
	var text: String
		@Synchronized get() {
			if (lines!!.size == 0) {
				return ""
			}
			val bob = StringBuilder(lines!![0])
			for (i in 1 until lines!!.size) {
				bob.append("\n").append(lines!![i])
			}
			return bob.toString()
		}
		@Synchronized set(text) {
			setLines(splitIntoMultipleLines(text))
			this.labelSize = getBounds(lines, labelSize)
			invalidate()
		}

	init {
		this.lines = null
		this.labelSize = TerminalSize.ZERO
		this.labelWidth = 0
		this.foregroundColor = null
		this.backgroundColor = null
		this.additionalStyles = EnumSet.noneOf(SGR::class.java)
		text = text
	}

	/**
	 * Protected access to set the internal representation of the text in this label, to be used by sub-classes of label
	 * in certain cases where `setText(..)` doesn't work. In general, you probably want to stick to
	 * `setText(..)` instead of this method unless you have a good reason not to.
	 * @param lines New lines this label will display
	 */
	protected fun setLines(lines: Array<String>) {
		this.lines = lines
	}

	/**
	 * Utility method for taking a string and turning it into an array of lines. This method is used in order to deal
	 * with line endings consistently.
	 * @param text Text to split
	 * @return Array of strings that forms the lines of the original string
	 */
	protected fun splitIntoMultipleLines(text: String): Array<String> {
		return text.replace("\r", "").split("\n".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
	}

	/**
	 * Returns the area, in terminal columns and rows, required to fully draw the lines passed in.
	 * @param lines Lines to measure the size of
	 * @param currentBounds Optional (can pass `null`) terminal size to use for storing the output values. If the
	 * method is called many times and always returning the same value, passing in an external
	 * reference of this size will avoid creating new `TerminalSize` objects every time
	 * @return Size that is required to draw the lines
	 */
	protected fun getBounds(lines: Array<String>?, currentBounds: TerminalSize?): TerminalSize {
		var currentBounds = currentBounds
		if (currentBounds == null) {
			currentBounds = TerminalSize.ZERO
		}
		currentBounds = currentBounds.withRows(lines!!.size)
		if (labelWidth == null || labelWidth == 0) {
			var preferredWidth = 0
			for (line in lines) {
				val lineWidth = TerminalTextUtils.getColumnWidth(line)
				if (preferredWidth < lineWidth) {
					preferredWidth = lineWidth
				}
			}
			currentBounds = currentBounds.withColumns(preferredWidth)
		} else {
			val wordWrapped = TerminalTextUtils.getWordWrappedText(labelWidth!!, *lines)
			currentBounds = currentBounds.withColumns(labelWidth!!).withRows(wordWrapped.size)
		}
		return currentBounds
	}

	/**
	 * Overrides the current theme's foreground color and use the one specified. If called with `null`, the
	 * override is cleared and the theme is used again.
	 * @param foregroundColor Foreground color to use when drawing the label, if `null` then use the theme's
	 * default
	 * @return Itself
	 */
	@Synchronized
	fun setForegroundColor(foregroundColor: TextColor): Label {
		this.foregroundColor = foregroundColor
		return this
	}

	/**
	 * Returns the foreground color used when drawing the label, or `null` if the color is read from the current
	 * theme.
	 * @return Foreground color used when drawing the label, or `null` if the color is read from the current
	 * theme.
	 */
	fun getForegroundColor(): TextColor? {
		return foregroundColor
	}

	/**
	 * Overrides the current theme's background color and use the one specified. If called with `null`, the
	 * override is cleared and the theme is used again.
	 * @param backgroundColor Background color to use when drawing the label, if `null` then use the theme's
	 * default
	 * @return Itself
	 */
	@Synchronized
	fun setBackgroundColor(backgroundColor: TextColor): Label {
		this.backgroundColor = backgroundColor
		return this
	}

	/**
	 * Returns the background color used when drawing the label, or `null` if the color is read from the current
	 * theme.
	 * @return Background color used when drawing the label, or `null` if the color is read from the current
	 * theme.
	 */
	fun getBackgroundColor(): TextColor? {
		return backgroundColor
	}

	/**
	 * Adds an additional SGR style to use when drawing the label, in case it wasn't enabled by the theme
	 * @param sgr SGR style to enable for this label
	 * @return Itself
	 */
	@Synchronized
	fun addStyle(sgr: SGR): Label {
		additionalStyles.add(sgr)
		return this
	}

	/**
	 * Removes an additional SGR style used when drawing the label, previously added by `addStyle(..)`. If the
	 * style you are trying to remove is specified by the theme, calling this method will have no effect.
	 * @param sgr SGR style to remove
	 * @return Itself
	 */
	@Synchronized
	fun removeStyle(sgr: SGR): Label {
		additionalStyles.remove(sgr)
		return this
	}

	/**
	 * Use this method to limit how wide the label can grow. If set to `null` there is no limit but if set to a
	 * positive integer then the preferred size will be calculated using word wrapping for lines that are longer than
	 * this label width. This may make the label increase in height as new rows may be requested. Please note that some
	 * layout managers might assign more space to the label and because of this the wrapping might not be as you expect
	 * it. If set to 0, the label will request the same space as if set to `null`, but when drawing it will apply
	 * word wrapping instead of truncation in order to fit the label inside the designated area if it's smaller than
	 * what was requested. By default this is set to 0.
	 *
	 * @param labelWidth Either `null` or 0 for no limit on how wide the label can be, where 0 indicates word
	 * wrapping should be used if the assigned area is smaller than the requested size, or a positive
	 * integer setting the requested maximum width at what point word wrapping will begin
	 * @return Itself
	 */
	@Synchronized
	fun setLabelWidth(labelWidth: Int?): Label {
		this.labelWidth = labelWidth
		return this
	}

	/**
	 * Returns the limit how wide the label can grow. If set to `null` or 0 there is no limit but if set to a
	 * positive integer then the preferred size will be calculated using word wrapping for lines that are longer than
	 * the label width. This may make the label increase in height as new rows may be requested. Please note that some
	 * layout managers might assign more space to the label and because of this the wrapping might not be as you expect
	 * it. If set to 0, the label will request the same space as if set to `null`, but when drawing it will apply
	 * word wrapping instead of truncation in order to fit the label inside the designated area if it's smaller than
	 * what was requested.
	 * @return Either `null` or 0 for no limit on how wide the label can be, where 0 indicates word
	 * wrapping should be used if the assigned area is smaller than the requested size, or a positive
	 * integer setting the requested maximum width at what point word wrapping will begin
	 */
	fun getLabelWidth(): Int? {
		return labelWidth
	}

	override fun createDefaultRenderer(): ComponentRenderer<Label> {
		return object : ComponentRenderer<Label> {
			override fun getPreferredSize(Label: Label): TerminalSize? {
				return labelSize
			}

			override fun drawComponent(graphics: TextGUIGraphics, component: Label) {
				val themeDefinition = component.themeDefinition
				graphics.applyThemeStyle(themeDefinition.normal)
				if (foregroundColor != null) {
					graphics.setForegroundColor(foregroundColor!!)
				}
				if (backgroundColor != null) {
					graphics.setBackgroundColor(backgroundColor!!)
				}
				for (sgr in additionalStyles) {
					graphics.enableModifiers(sgr)
				}

				val linesToDraw: Array<String>?
				if (component.getLabelWidth() == null) {
					linesToDraw = component.lines
				} else {
					linesToDraw = TerminalTextUtils.getWordWrappedText(graphics.size.columns, *component.lines!!).toTypedArray<String>()
				}

				for (row in 0 until Math.min(graphics.size.rows, linesToDraw!!.size)) {
					val line = linesToDraw!![row]
					if (graphics.size.columns >= labelSize!!.columns) {
						graphics.putString(0, row, line)
					} else {
						val availableColumns = graphics.size.columns
						val fitString = TerminalTextUtils.fitString(line, availableColumns)
						graphics.putString(0, row, fitString)
					}
				}
			}
		}
	}
}
