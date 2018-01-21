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

import com.googlecode.lanterna.*
import com.googlecode.lanterna.graphics.TextGraphics
import com.googlecode.lanterna.graphics.Theme
import com.googlecode.lanterna.graphics.ThemeDefinition

import java.util.Arrays

/**
 * This class containers a couple of border implementation and utility methods for instantiating them. It also contains
 * a utility method for joining border line graphics together with adjacent lines so they blend in together:
 * `joinLinesWithFrame(..)`.
 * @author Martin
 */
object Borders {

	//Different ways to draw the border
	private enum class BorderStyle {
		Solid,
		Bevel,
		ReverseBevel
	}

	/**
	 * Creates a `Border` that is drawn as a solid color single line surrounding the wrapped component with a
	 * title string normally drawn at the top-left side
	 * @param title The title to draw on the border
	 * @return New solid color single line `Border` with a title
	 */
	@JvmOverloads
	fun singleLine(title: String = ""): Border =
		SingleLine(title, BorderStyle.Solid)

	/**
	 * Creates a `Border` that is drawn as a bevel color single line surrounding the wrapped component with a
	 * title string normally drawn at the top-left side
	 * @param title The title to draw on the border
	 * @return New bevel color single line `Border` with a title
	 */
	@JvmOverloads
	fun singleLineBevel(title: String = ""): Border =
		SingleLine(title, BorderStyle.Bevel)

	/**
	 * Creates a `Border` that is drawn as a reverse bevel color single line surrounding the wrapped component
	 * with a title string normally drawn at the top-left side
	 * @param title The title to draw on the border
	 * @return New reverse bevel color single line `Border` with a title
	 */
	@JvmOverloads
	fun singleLineReverseBevel(title: String = ""): Border =
		SingleLine(title, BorderStyle.ReverseBevel)

	/**
	 * Creates a `Border` that is drawn as a solid color double line surrounding the wrapped component with a
	 * title string normally drawn at the top-left side
	 * @param title The title to draw on the border
	 * @return New solid color double line `Border` with a title
	 */
	@JvmOverloads
	fun doubleLine(title: String = ""): Border =
		DoubleLine(title, BorderStyle.Solid)

	/**
	 * Creates a `Border` that is drawn as a bevel color double line surrounding the wrapped component with a
	 * title string normally drawn at the top-left side
	 * @param title The title to draw on the border
	 * @return New bevel color double line `Border` with a title
	 */
	@JvmOverloads
	fun doubleLineBevel(title: String = ""): Border =
		DoubleLine(title, BorderStyle.Bevel)

	/**
	 * Creates a `Border` that is drawn as a reverse bevel color double line surrounding the wrapped component
	 * with a title string normally drawn at the top-left side
	 * @param title The title to draw on the border
	 * @return New reverse bevel color double line `Border` with a title
	 */
	@JvmOverloads
	fun doubleLineReverseBevel(title: String = ""): Border =
		DoubleLine(title, BorderStyle.ReverseBevel)

	private abstract class StandardBorder protected constructor(val title: String?, protected val borderStyle: BorderStyle) : AbstractBorder() {

		init {
			if (title == null) {
				throw IllegalArgumentException("Cannot create a border with null title")
			}
		}

		override fun toString() =
			javaClass.getSimpleName() + "{" + title + "}"
	}

	private abstract class AbstractBorderRenderer protected constructor(private val borderStyle: BorderStyle) : Border.BorderRenderer {

		override val wrappedComponentTopLeftOffset: TerminalPosition
			get() = TerminalPosition.OFFSET_1x1

		override fun getPreferredSize(component: Border): TerminalSize {
			val border = component as StandardBorder
			val wrappedComponent = border.component
			var preferredSize: TerminalSize
			if (wrappedComponent == null) {
				preferredSize = TerminalSize.ZERO
			} else {
				preferredSize = wrappedComponent.preferredSize
			}
			preferredSize = preferredSize.withRelativeColumns(2).withRelativeRows(2)
			val borderTitle = border.title
			return preferredSize.max(TerminalSize(if (borderTitle.isEmpty()) 2 else TerminalTextUtils.getColumnWidth(borderTitle) + 4, 2))
		}

		override fun getWrappedComponentSize(borderSize: TerminalSize) =
			borderSize
				.withRelativeColumns(-Math.min(2, borderSize.columns))
				.withRelativeRows(-Math.min(2, borderSize.rows))

		override fun drawComponent(graphics: TextGUIGraphics, component: Border) {
			val border = component as StandardBorder
			val wrappedComponent = border.component ?: return
			val drawableArea = graphics.size

			val horizontalLine = getHorizontalLine(component.theme)
			val verticalLine = getVerticalLine(component.theme)
			val bottomLeftCorner = getBottomLeftCorner(component.theme)
			val topLeftCorner = getTopLeftCorner(component.theme)
			val bottomRightCorner = getBottomRightCorner(component.theme)
			val topRightCorner = getTopRightCorner(component.theme)
			val titleLeft = getTitleLeft(component.theme)
			val titleRight = getTitleRight(component.theme)

			val themeDefinition = component.theme.getDefinition(AbstractBorder::class.java!!)
			if (borderStyle == BorderStyle.Bevel) {
				graphics.applyThemeStyle(themeDefinition.preLight)
			} else {
				graphics.applyThemeStyle(themeDefinition.normal)
			}
			graphics.setCharacter(0, drawableArea.rows - 1, bottomLeftCorner)
			if (drawableArea.rows > 2) {
				graphics.drawLine(TerminalPosition(0, drawableArea.rows - 2), TerminalPosition(0, 1), verticalLine)
			}
			graphics.setCharacter(0, 0, topLeftCorner)
			if (drawableArea.columns > 2) {
				graphics.drawLine(TerminalPosition(1, 0), TerminalPosition(drawableArea.columns - 2, 0), horizontalLine)
			}

			if (borderStyle == BorderStyle.ReverseBevel) {
				graphics.applyThemeStyle(themeDefinition.preLight)
			} else {
				graphics.applyThemeStyle(themeDefinition.normal)
			}
			graphics.setCharacter(drawableArea.columns - 1, 0, topRightCorner)
			if (drawableArea.rows > 2) {
				graphics.drawLine(TerminalPosition(drawableArea.columns - 1, 1),
					TerminalPosition(drawableArea.columns - 1, drawableArea.rows - 2),
					verticalLine)
			}
			graphics.setCharacter(drawableArea.columns - 1, drawableArea.rows - 1, bottomRightCorner)
			if (drawableArea.columns > 2) {
				graphics.drawLine(TerminalPosition(1, drawableArea.rows - 1),
					TerminalPosition(drawableArea.columns - 2, drawableArea.rows - 1),
					horizontalLine)
			}


			if (border.title != null && !border.title.isEmpty() &&
				drawableArea.columns >= TerminalTextUtils.getColumnWidth(border.title) + 4) {
				graphics.applyThemeStyle(themeDefinition.active)
				graphics.putString(2, 0, border.title)

				if (borderStyle == BorderStyle.Bevel) {
					graphics.applyThemeStyle(themeDefinition.preLight)
				} else {
					graphics.applyThemeStyle(themeDefinition.normal)
				}
				graphics.setCharacter(1, 0, titleLeft)
				graphics.setCharacter(2 + TerminalTextUtils.getColumnWidth(border.title), 0, titleRight)
			}

			wrappedComponent.draw(graphics.newTextGraphics(wrappedComponentTopLeftOffset, getWrappedComponentSize(drawableArea)))


			joinLinesWithFrame(graphics)
		}

		protected abstract fun getHorizontalLine(theme: Theme): Char
		protected abstract fun getVerticalLine(theme: Theme): Char
		protected abstract fun getBottomLeftCorner(theme: Theme): Char
		protected abstract fun getTopLeftCorner(theme: Theme): Char
		protected abstract fun getBottomRightCorner(theme: Theme): Char
		protected abstract fun getTopRightCorner(theme: Theme): Char
		protected abstract fun getTitleLeft(theme: Theme): Char
		protected abstract fun getTitleRight(theme: Theme): Char
	}

	/**
	 * This method will attempt to join line drawing characters with the outermost bottom and top rows and left and
	 * right columns. For example, if a vertical left border character is ║ and the character immediately to the right
	 * of it is ─, then the border character will be updated to ╟ to join the two together. Please note that this method
	 * will **only** join the outer border columns and rows.
	 * @param graphics Graphics to use when inspecting and joining characters
	 */
	fun joinLinesWithFrame(graphics: TextGraphics) {
		val drawableArea = graphics.size
		if (drawableArea.rows <= 2 || drawableArea.columns <= 2) {
			//Too small
			return
		}

		val upperRow = 0
		val lowerRow = drawableArea.rows - 1
		val leftRow = 0
		val rightRow = drawableArea.columns - 1

		val junctionFromBelowSingle = Arrays.asList(
			Symbols.SINGLE_LINE_VERTICAL,
			Symbols.BOLD_FROM_NORMAL_SINGLE_LINE_VERTICAL,
			Symbols.SINGLE_LINE_CROSS,
			Symbols.DOUBLE_LINE_HORIZONTAL_SINGLE_LINE_CROSS,
			Symbols.SINGLE_LINE_BOTTOM_LEFT_CORNER,
			Symbols.SINGLE_LINE_BOTTOM_RIGHT_CORNER,
			Symbols.SINGLE_LINE_T_LEFT,
			Symbols.SINGLE_LINE_T_RIGHT,
			Symbols.SINGLE_LINE_T_UP,
			Symbols.SINGLE_LINE_T_DOUBLE_LEFT,
			Symbols.SINGLE_LINE_T_DOUBLE_RIGHT,
			Symbols.DOUBLE_LINE_T_SINGLE_UP)
		val junctionFromBelowDouble = Arrays.asList(
			Symbols.DOUBLE_LINE_VERTICAL,
			Symbols.DOUBLE_LINE_CROSS,
			Symbols.DOUBLE_LINE_VERTICAL_SINGLE_LINE_CROSS,
			Symbols.DOUBLE_LINE_BOTTOM_LEFT_CORNER,
			Symbols.DOUBLE_LINE_BOTTOM_RIGHT_CORNER,
			Symbols.DOUBLE_LINE_T_LEFT,
			Symbols.DOUBLE_LINE_T_RIGHT,
			Symbols.DOUBLE_LINE_T_UP,
			Symbols.DOUBLE_LINE_T_SINGLE_LEFT,
			Symbols.DOUBLE_LINE_T_SINGLE_RIGHT,
			Symbols.SINGLE_LINE_T_DOUBLE_UP)
		val junctionFromAboveSingle = Arrays.asList(
			Symbols.SINGLE_LINE_VERTICAL,
			Symbols.BOLD_TO_NORMAL_SINGLE_LINE_VERTICAL,
			Symbols.SINGLE_LINE_CROSS,
			Symbols.DOUBLE_LINE_HORIZONTAL_SINGLE_LINE_CROSS,
			Symbols.SINGLE_LINE_TOP_LEFT_CORNER,
			Symbols.SINGLE_LINE_TOP_RIGHT_CORNER,
			Symbols.SINGLE_LINE_T_LEFT,
			Symbols.SINGLE_LINE_T_RIGHT,
			Symbols.SINGLE_LINE_T_DOWN,
			Symbols.SINGLE_LINE_T_DOUBLE_LEFT,
			Symbols.SINGLE_LINE_T_DOUBLE_RIGHT,
			Symbols.DOUBLE_LINE_T_SINGLE_DOWN)
		val junctionFromAboveDouble = Arrays.asList(
			Symbols.DOUBLE_LINE_VERTICAL,
			Symbols.DOUBLE_LINE_CROSS,
			Symbols.DOUBLE_LINE_VERTICAL_SINGLE_LINE_CROSS,
			Symbols.DOUBLE_LINE_TOP_LEFT_CORNER,
			Symbols.DOUBLE_LINE_TOP_RIGHT_CORNER,
			Symbols.DOUBLE_LINE_T_LEFT,
			Symbols.DOUBLE_LINE_T_RIGHT,
			Symbols.DOUBLE_LINE_T_DOWN,
			Symbols.DOUBLE_LINE_T_SINGLE_LEFT,
			Symbols.DOUBLE_LINE_T_SINGLE_RIGHT,
			Symbols.SINGLE_LINE_T_DOUBLE_DOWN)
		val junctionFromLeftSingle = Arrays.asList(
			Symbols.SINGLE_LINE_HORIZONTAL,
			Symbols.BOLD_TO_NORMAL_SINGLE_LINE_HORIZONTAL,
			Symbols.SINGLE_LINE_CROSS,
			Symbols.DOUBLE_LINE_VERTICAL_SINGLE_LINE_CROSS,
			Symbols.SINGLE_LINE_BOTTOM_LEFT_CORNER,
			Symbols.SINGLE_LINE_TOP_LEFT_CORNER,
			Symbols.SINGLE_LINE_T_UP,
			Symbols.SINGLE_LINE_T_DOWN,
			Symbols.SINGLE_LINE_T_RIGHT,
			Symbols.SINGLE_LINE_T_DOUBLE_UP,
			Symbols.SINGLE_LINE_T_DOUBLE_DOWN,
			Symbols.DOUBLE_LINE_T_SINGLE_RIGHT)
		val junctionFromLeftDouble = Arrays.asList(
			Symbols.DOUBLE_LINE_HORIZONTAL,
			Symbols.DOUBLE_LINE_CROSS,
			Symbols.DOUBLE_LINE_HORIZONTAL_SINGLE_LINE_CROSS,
			Symbols.DOUBLE_LINE_BOTTOM_LEFT_CORNER,
			Symbols.DOUBLE_LINE_TOP_LEFT_CORNER,
			Symbols.DOUBLE_LINE_T_UP,
			Symbols.DOUBLE_LINE_T_DOWN,
			Symbols.DOUBLE_LINE_T_RIGHT,
			Symbols.DOUBLE_LINE_T_SINGLE_UP,
			Symbols.DOUBLE_LINE_T_SINGLE_DOWN,
			Symbols.SINGLE_LINE_T_DOUBLE_RIGHT)
		val junctionFromRightSingle = Arrays.asList(
			Symbols.SINGLE_LINE_HORIZONTAL,
			Symbols.BOLD_FROM_NORMAL_SINGLE_LINE_HORIZONTAL,
			Symbols.SINGLE_LINE_CROSS,
			Symbols.DOUBLE_LINE_VERTICAL_SINGLE_LINE_CROSS,
			Symbols.SINGLE_LINE_BOTTOM_RIGHT_CORNER,
			Symbols.SINGLE_LINE_TOP_RIGHT_CORNER,
			Symbols.SINGLE_LINE_T_UP,
			Symbols.SINGLE_LINE_T_DOWN,
			Symbols.SINGLE_LINE_T_LEFT,
			Symbols.SINGLE_LINE_T_DOUBLE_UP,
			Symbols.SINGLE_LINE_T_DOUBLE_DOWN,
			Symbols.DOUBLE_LINE_T_SINGLE_LEFT)
		val junctionFromRightDouble = Arrays.asList(
			Symbols.DOUBLE_LINE_HORIZONTAL,
			Symbols.DOUBLE_LINE_CROSS,
			Symbols.DOUBLE_LINE_HORIZONTAL_SINGLE_LINE_CROSS,
			Symbols.DOUBLE_LINE_BOTTOM_RIGHT_CORNER,
			Symbols.DOUBLE_LINE_TOP_RIGHT_CORNER,
			Symbols.DOUBLE_LINE_T_UP,
			Symbols.DOUBLE_LINE_T_DOWN,
			Symbols.DOUBLE_LINE_T_LEFT,
			Symbols.DOUBLE_LINE_T_SINGLE_UP,
			Symbols.DOUBLE_LINE_T_SINGLE_DOWN,
			Symbols.SINGLE_LINE_T_DOUBLE_LEFT)

		//Go horizontally and check vertical neighbours if it's possible to extend lines into the border
		for (column in 1 until drawableArea.columns - 1) {
			//Check first row
			var borderCharacter: TextCharacter? = graphics.getCharacter(column, upperRow) ?: continue
			var neighbourCharacter: TextCharacter? = graphics.getCharacter(column, upperRow + 1)
			if (neighbourCharacter != null) {
				val neighbour = neighbourCharacter.character
				if (borderCharacter!!.character == Symbols.SINGLE_LINE_HORIZONTAL) {
					if (junctionFromBelowSingle.contains(neighbour)) {
						graphics.setCharacter(column, upperRow, borderCharacter.withCharacter(Symbols.SINGLE_LINE_T_DOWN))
					} else if (junctionFromBelowDouble.contains(neighbour)) {
						graphics.setCharacter(column, upperRow, borderCharacter.withCharacter(Symbols.SINGLE_LINE_T_DOUBLE_DOWN))
					}
				} else if (borderCharacter.character == Symbols.DOUBLE_LINE_HORIZONTAL) {
					if (junctionFromBelowSingle.contains(neighbour)) {
						graphics.setCharacter(column, upperRow, borderCharacter.withCharacter(Symbols.DOUBLE_LINE_T_SINGLE_DOWN))
					} else if (junctionFromBelowDouble.contains(neighbour)) {
						graphics.setCharacter(column, upperRow, borderCharacter.withCharacter(Symbols.DOUBLE_LINE_T_DOWN))
					}
				}
			}

			//Check last row
			borderCharacter = graphics.getCharacter(column, lowerRow)
			if (borderCharacter == null) {
				continue
			}
			neighbourCharacter = graphics.getCharacter(column, lowerRow - 1)
			if (neighbourCharacter != null) {
				val neighbour = neighbourCharacter.character
				if (borderCharacter.character == Symbols.SINGLE_LINE_HORIZONTAL) {
					if (junctionFromAboveSingle.contains(neighbour)) {
						graphics.setCharacter(column, lowerRow, borderCharacter.withCharacter(Symbols.SINGLE_LINE_T_UP))
					} else if (junctionFromAboveDouble.contains(neighbour)) {
						graphics.setCharacter(column, lowerRow, borderCharacter.withCharacter(Symbols.SINGLE_LINE_T_DOUBLE_UP))
					}
				} else if (borderCharacter.character == Symbols.DOUBLE_LINE_HORIZONTAL) {
					if (junctionFromAboveSingle.contains(neighbour)) {
						graphics.setCharacter(column, lowerRow, borderCharacter.withCharacter(Symbols.DOUBLE_LINE_T_SINGLE_UP))
					} else if (junctionFromAboveDouble.contains(neighbour)) {
						graphics.setCharacter(column, lowerRow, borderCharacter.withCharacter(Symbols.DOUBLE_LINE_T_UP))
					}
				}
			}
		}

		//Go vertically and check horizontal neighbours if it's possible to extend lines into the border
		for (row in 1 until drawableArea.rows - 1) {
			//Check first column
			var borderCharacter: TextCharacter? = graphics.getCharacter(leftRow, row) ?: continue
			var neighbourCharacter: TextCharacter? = graphics.getCharacter(leftRow + 1, row)
			if (neighbourCharacter != null) {
				val neighbour = neighbourCharacter.character
				if (borderCharacter!!.character == Symbols.SINGLE_LINE_VERTICAL) {
					if (junctionFromRightSingle.contains(neighbour)) {
						graphics.setCharacter(leftRow, row, borderCharacter.withCharacter(Symbols.SINGLE_LINE_T_RIGHT))
					} else if (junctionFromRightDouble.contains(neighbour)) {
						graphics.setCharacter(leftRow, row, borderCharacter.withCharacter(Symbols.SINGLE_LINE_T_DOUBLE_RIGHT))
					}
				} else if (borderCharacter.character == Symbols.DOUBLE_LINE_VERTICAL) {
					if (junctionFromRightSingle.contains(neighbour)) {
						graphics.setCharacter(leftRow, row, borderCharacter.withCharacter(Symbols.DOUBLE_LINE_T_SINGLE_RIGHT))
					} else if (junctionFromRightDouble.contains(neighbour)) {
						graphics.setCharacter(leftRow, row, borderCharacter.withCharacter(Symbols.DOUBLE_LINE_T_RIGHT))
					}
				}
			}

			//Check last column
			borderCharacter = graphics.getCharacter(rightRow, row)
			if (borderCharacter == null) {
				continue
			}
			neighbourCharacter = graphics.getCharacter(rightRow - 1, row)
			if (neighbourCharacter != null) {
				val neighbour = neighbourCharacter.character
				if (borderCharacter.character == Symbols.SINGLE_LINE_VERTICAL) {
					if (junctionFromLeftSingle.contains(neighbour)) {
						graphics.setCharacter(rightRow, row, borderCharacter.withCharacter(Symbols.SINGLE_LINE_T_LEFT))
					} else if (junctionFromLeftDouble.contains(neighbour)) {
						graphics.setCharacter(rightRow, row, borderCharacter.withCharacter(Symbols.SINGLE_LINE_T_DOUBLE_LEFT))
					}
				} else if (borderCharacter.character == Symbols.DOUBLE_LINE_VERTICAL) {
					if (junctionFromLeftSingle.contains(neighbour)) {
						graphics.setCharacter(rightRow, row, borderCharacter.withCharacter(Symbols.DOUBLE_LINE_T_SINGLE_LEFT))
					} else if (junctionFromLeftDouble.contains(neighbour)) {
						graphics.setCharacter(rightRow, row, borderCharacter.withCharacter(Symbols.DOUBLE_LINE_T_LEFT))
					}
				}
			}
		}
	}

	private class SingleLine private constructor(title: String, borderStyle: BorderStyle) : StandardBorder(title, borderStyle) {

		override fun createDefaultRenderer(): Border.BorderRenderer =
			SingleLineRenderer(borderStyle)
	}

	private class SingleLineRenderer(borderStyle: BorderStyle) : AbstractBorderRenderer(borderStyle) {

		override fun getTopRightCorner(theme: Theme) =
			theme.getDefinition(SingleLine::class.java!!).getCharacter("TOP_RIGHT_CORNER", Symbols.SINGLE_LINE_TOP_RIGHT_CORNER)

		override fun getBottomRightCorner(theme: Theme) =
			theme.getDefinition(SingleLine::class.java!!).getCharacter("BOTTOM_RIGHT_CORNER", Symbols.SINGLE_LINE_BOTTOM_RIGHT_CORNER)

		override fun getTopLeftCorner(theme: Theme) =
			theme.getDefinition(SingleLine::class.java!!).getCharacter("TOP_LEFT_CORNER", Symbols.SINGLE_LINE_TOP_LEFT_CORNER)

		override fun getBottomLeftCorner(theme: Theme) =
			theme.getDefinition(SingleLine::class.java!!).getCharacter("BOTTOM_LEFT_CORNER", Symbols.SINGLE_LINE_BOTTOM_LEFT_CORNER)

		override fun getVerticalLine(theme: Theme) =
			theme.getDefinition(SingleLine::class.java!!).getCharacter("VERTICAL_LINE", Symbols.SINGLE_LINE_VERTICAL)

		override fun getHorizontalLine(theme: Theme) =
			theme.getDefinition(SingleLine::class.java!!).getCharacter("HORIZONTAL_LINE", Symbols.SINGLE_LINE_HORIZONTAL)

		override fun getTitleLeft(theme: Theme) =
			theme.getDefinition(SingleLine::class.java!!).getCharacter("TITLE_LEFT", Symbols.SINGLE_LINE_HORIZONTAL)

		override fun getTitleRight(theme: Theme) =
			theme.getDefinition(SingleLine::class.java!!).getCharacter("TITLE_RIGHT", Symbols.SINGLE_LINE_HORIZONTAL)
	}

	private class DoubleLine private constructor(title: String, borderStyle: BorderStyle) : StandardBorder(title, borderStyle) {

		override fun createDefaultRenderer(): Border.BorderRenderer =
			DoubleLineRenderer(borderStyle)
	}

	private class DoubleLineRenderer(borderStyle: BorderStyle) : AbstractBorderRenderer(borderStyle) {

		override fun getTopRightCorner(theme: Theme) =
			theme.getDefinition(DoubleLine::class.java!!).getCharacter("TOP_RIGHT_CORNER", Symbols.DOUBLE_LINE_TOP_RIGHT_CORNER)

		override fun getBottomRightCorner(theme: Theme) =
			theme.getDefinition(DoubleLine::class.java!!).getCharacter("BOTTOM_RIGHT_CORNER", Symbols.DOUBLE_LINE_BOTTOM_RIGHT_CORNER)

		override fun getTopLeftCorner(theme: Theme) =
			theme.getDefinition(DoubleLine::class.java!!).getCharacter("TOP_LEFT_CORNER", Symbols.DOUBLE_LINE_TOP_LEFT_CORNER)

		override fun getBottomLeftCorner(theme: Theme) =
			theme.getDefinition(DoubleLine::class.java!!).getCharacter("BOTTOM_LEFT_CORNER", Symbols.DOUBLE_LINE_BOTTOM_LEFT_CORNER)

		override fun getVerticalLine(theme: Theme) =
			theme.getDefinition(DoubleLine::class.java!!).getCharacter("VERTICAL_LINE", Symbols.DOUBLE_LINE_VERTICAL)

		override fun getHorizontalLine(theme: Theme) =
			theme.getDefinition(DoubleLine::class.java!!).getCharacter("HORIZONTAL_LINE", Symbols.DOUBLE_LINE_HORIZONTAL)

		override fun getTitleLeft(theme: Theme) =
			theme.getDefinition(DoubleLine::class.java!!).getCharacter("TITLE_LEFT", Symbols.DOUBLE_LINE_HORIZONTAL)

		override fun getTitleRight(theme: Theme) =
			theme.getDefinition(DoubleLine::class.java!!).getCharacter("TITLE_RIGHT", Symbols.DOUBLE_LINE_HORIZONTAL)
	}
}
/**
 * Creates a `Border` that is drawn as a solid color single line surrounding the wrapped component
 * @return New solid color single line `Border`
 */
/**
 * Creates a `Border` that is drawn as a bevel color single line surrounding the wrapped component
 * @return New bevel color single line `Border`
 */
/**
 * Creates a `Border` that is drawn as a reverse bevel color single line surrounding the wrapped component
 * @return New reverse bevel color single line `Border`
 */
/**
 * Creates a `Border` that is drawn as a solid color double line surrounding the wrapped component
 * @return New solid color double line `Border`
 */
/**
 * Creates a `Border` that is drawn as a bevel color double line surrounding the wrapped component
 * @return New bevel color double line `Border`
 */
/**
 * Creates a `Border` that is drawn as a reverse bevel color double line surrounding the wrapped component
 * @return New reverse bevel color double line `Border`
 */
