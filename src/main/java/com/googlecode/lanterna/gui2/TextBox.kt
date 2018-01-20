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
import com.googlecode.lanterna.TerminalPosition
import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.graphics.ThemeDefinition
import com.googlecode.lanterna.input.KeyStroke

import java.util.ArrayList
import java.util.regex.Pattern

/**
 * This component keeps a text content that is editable by the user. A TextBox can be single line or multiline and lets
 * the user navigate the cursor in the text area by using the arrow keys, page up, page down, home and end. For
 * multi-line `TextBox`:es, scrollbars will be automatically displayed if needed.
 *
 *
 * Size-wise, a `TextBox` should be hard-coded to a particular size, it's not good at guessing how large it should
 * be. You can do this through the constructor.
 */
open class TextBox
/**
 * Main constructor of the `TextBox` which decides size, initial content and style
 * @param preferredSize Size of the `TextBox`
 * @param initialContent Initial content of the `TextBox`
 * @param style Style to use for this `TextBox`, instead of auto-detecting
 */
@JvmOverloads constructor(preferredSize: TerminalSize?, initialContent: String, private val style: Style = if (preferredSize != null && preferredSize!!.rows > 1 || initialContent.contains("\n")) Style.MULTI_LINE else Style.SINGLE_LINE) : AbstractInteractableComponent<TextBox>() {

	private val lines: MutableList<String>

	/**
	 * Returns the position of the caret, as a `TerminalPosition` where the row and columns equals the coordinates
	 * in a multi-line `TextBox` and for single-line `TextBox` you can ignore the `row` component.
	 * @return Position of the text input caret
	 */
	var caretPosition: TerminalPosition? = null
		private set
	private var caretWarp: Boolean = false
	private var readOnly: Boolean = false
	private var horizontalFocusSwitching: Boolean = false
	private var verticalFocusSwitching: Boolean = false
	private val maxLineLength: Int
	private var longestRow: Int = 0
	private var mask: Char? = null
	private var validationPattern: Pattern? = null

	override val renderer: TextBoxRenderer
		get() = super.renderer as TextBoxRenderer

	/**
	 * Returns the text in this `TextBox`, for multi-line mode all lines will be concatenated together with \n as
	 * separator.
	 * @return The text inside this `TextBox`
	 */
	val text: String
		@Synchronized get() {
			val bob = StringBuilder(lines[0])
			for (i in 1 until lines.size) {
				bob.append("\n").append(lines[i])
			}
			return bob.toString()
		}

	/**
	 * Returns the number of lines currently in this TextBox. For single-line TextBox:es, this will always return 1.
	 * @return Number of lines of text currently in this TextBox
	 */
	val lineCount: Int
		@Synchronized get() = lines.size

	/**
	 * Enum value to force a `TextBox` to be either single line or multi line. This is usually auto-detected if
	 * the text box has some initial content by scanning that content for \n characters.
	 */
	enum class Style {
		/**
		 * The `TextBox` contains a single line of text and is typically drawn on one row
		 */
		SINGLE_LINE,
		/**
		 * The `TextBox` contains a none, one or many lines of text and is normally drawn over multiple lines
		 */
		MULTI_LINE
	}

	/**
	 * Default constructor, this creates a single-line `TextBox` of size 10 which is initially empty
	 */
	constructor() : this(TerminalSize(10, 1), "", Style.SINGLE_LINE) {}

	/**
	 * Constructor that creates a `TextBox` with an initial content and attempting to be big enough to display
	 * the whole text at once without scrollbars
	 * @param initialContent Initial content of the `TextBox`
	 */
	constructor(initialContent: String) : this(null, initialContent, if (initialContent.contains("\n")) Style.MULTI_LINE else Style.SINGLE_LINE) {}

	/**
	 * Creates a `TextBox` that has an initial content and attempting to be big enough to display the whole text
	 * at once without scrollbars.
	 *
	 * @param initialContent Initial content of the `TextBox`
	 * @param style Forced style instead of auto-detecting
	 */
	constructor(initialContent: String, style: Style) : this(null, initialContent, style) {}

	/**
	 * Creates a new empty `TextBox` with a specific size and style
	 * @param preferredSize Size of the `TextBox`
	 * @param style Style to use
	 */
	@JvmOverloads constructor(preferredSize: TerminalSize?, style: Style = if (preferredSize != null && preferredSize.rows > 1) Style.MULTI_LINE else Style.SINGLE_LINE) : this(preferredSize, "", style) {}

	init {
		var preferredSize = preferredSize
		this.lines = ArrayList()
		this.readOnly = false
		this.caretWarp = false
		this.verticalFocusSwitching = true
		this.horizontalFocusSwitching = style == Style.SINGLE_LINE
		this.caretPosition = TerminalPosition.TOP_LEFT_CORNER
		this.maxLineLength = -1
		this.longestRow = 1    //To fit the cursor
		this.mask = null
		this.validationPattern = null
		setText(initialContent)

		// Re-adjust caret position
		this.caretPosition = TerminalPosition.TOP_LEFT_CORNER.withColumn(getLine(0).length)

		if (preferredSize == null) {
			preferredSize = TerminalSize(Math.max(10, longestRow), lines.size)
		}
		setPreferredSize(preferredSize)
	}

	/**
	 * Sets a pattern on which the content of the text box is to be validated. For multi-line TextBox:s, the pattern is
	 * checked against each line individually, not the content as a whole. Partial matchings will not be allowed, the
	 * whole pattern must match, however, empty lines will always be allowed. When the user tried to modify the content
	 * of the TextBox in a way that does not match the pattern, the operation will be silently ignored. If you set this
	 * pattern to `null`, all validation is turned off.
	 * @param validationPattern Pattern to validate the lines in this TextBox against, or `null` to disable
	 * @return itself
	 */
	@Synchronized
	fun setValidationPattern(validationPattern: Pattern?): TextBox {
		if (validationPattern != null) {
			for (line in lines) {
				if (!validated(line)) {
					throw IllegalStateException("TextBox validation pattern $validationPattern does not match existing content")
				}
			}
		}
		this.validationPattern = validationPattern
		return this
	}

	/**
	 * Updates the text content of the `TextBox` to the supplied string.
	 * @param text New text to assign to the `TextBox`
	 * @return Itself
	 */
	@Synchronized
	fun setText(text: String): TextBox {
		val split = text.split("\n".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
		lines.clear()
		longestRow = 1
		for (line in split) {
			addLine(line)
		}
		if (caretPosition!!.row > lines.size - 1) {
			caretPosition = caretPosition!!.withRow(lines.size - 1)
		}
		if (caretPosition!!.column > lines[caretPosition!!.row].length) {
			caretPosition = caretPosition!!.withColumn(lines[caretPosition!!.row].length)
		}
		invalidate()
		return this
	}

	/**
	 * Adds a single line to the `TextBox` at the end, this only works when in multi-line mode
	 * @param line Line to add at the end of the content in this `TextBox`
	 * @return Itself
	 */
	@Synchronized
	fun addLine(line: String): TextBox {
		val bob = StringBuilder()
		for (i in 0 until line.length) {
			val c = line[i]
			if (c == '\n' && style == Style.MULTI_LINE) {
				val string = bob.toString()
				val lineWidth = TerminalTextUtils.getColumnWidth(string)
				lines.add(string)
				if (longestRow < lineWidth + 1) {
					longestRow = lineWidth + 1
				}
				addLine(line.substring(i + 1))
				return this
			} else if (Character.isISOControl(c)) {
				continue
			}

			bob.append(c)
		}
		val string = bob.toString()
		if (!validated(string)) {
			throw IllegalStateException("TextBox validation pattern $validationPattern does not match the supplied text")
		}
		val lineWidth = TerminalTextUtils.getColumnWidth(string)
		lines.add(string)
		if (longestRow < lineWidth + 1) {
			longestRow = lineWidth + 1
		}
		invalidate()
		return this
	}

	/**
	 * Sets if the caret should jump to the beginning of the next line if right arrow is pressed while at the end of a
	 * line. Similarly, pressing left arrow at the beginning of a line will make the caret jump to the end of the
	 * previous line. This only makes sense for multi-line TextBox:es; for single-line ones it has no effect. By default
	 * this is `false`.
	 * @param caretWarp Whether the caret will warp at the beginning/end of lines
	 * @return Itself
	 */
	fun setCaretWarp(caretWarp: Boolean): TextBox {
		this.caretWarp = caretWarp
		return this
	}

	/**
	 * Checks whether caret warp mode is enabled or not. See `setCaretWarp` for more details.
	 * @return `true` if caret warp mode is enabled
	 */
	fun isCaretWarp(): Boolean {
		return caretWarp
	}

	/**
	 * Moves the text caret position horizontally to a new position in the [TextBox]. For multi-line
	 * [TextBox]:es, this will move the cursor within the current line. If the position is out of bounds, it is
	 * automatically set back into range.
	 * @param column Position, in characters, within the [TextBox] (on the current line for multi-line
	 * [TextBox]:es) to where the text cursor should be moved
	 * @return Itself
	 */
	@Synchronized
	fun setCaretPosition(column: Int): TextBox {
		return setCaretPosition(caretPosition!!.row, column)
	}

	/**
	 * Moves the text caret position to a new position in the [TextBox]. For single-line [TextBox]:es, the
	 * line component is not used. If one of the positions are out of bounds, it is automatically set back into range.
	 * @param line Which line inside the [TextBox] to move the caret to (0 being the first line), ignored if the
	 * [TextBox] is single-line
	 * @param column  What column on the specified line to move the text caret to (0 being the first column)
	 * @return Itself
	 */
	@Synchronized
	fun setCaretPosition(line: Int, column: Int): TextBox {
		var line = line
		var column = column
		if (line < 0) {
			line = 0
		} else if (line >= lines.size) {
			line = lines.size - 1
		}
		if (column < 0) {
			column = 0
		} else if (column > lines[line].length) {
			column = lines[line].length
		}
		caretPosition = caretPosition!!.withRow(line).withColumn(column)
		return this
	}

	/**
	 * Helper method, it will return the content of the `TextBox` unless it's empty in which case it will return
	 * the supplied default value
	 * @param defaultValueIfEmpty Value to return if the `TextBox` is empty
	 * @return Text in the `TextBox` or `defaultValueIfEmpty` is the `TextBox` is empty
	 */
	fun getTextOrDefault(defaultValueIfEmpty: String): String {
		val text = text
		return if (text.isEmpty()) {
			defaultValueIfEmpty
		} else text
	}

	/**
	 * Returns the current text mask, meaning the substitute to draw instead of the text inside the `TextBox`.
	 * This is normally used for password input fields so the password isn't shown
	 * @return Current text mask or `null` if there is no mask
	 */
	fun getMask(): Char? {
		return mask
	}

	/**
	 * Sets the current text mask, meaning the substitute to draw instead of the text inside the `TextBox`.
	 * This is normally used for password input fields so the password isn't shown
	 * @param mask New text mask or `null` if there is no mask
	 * @return Itself
	 */
	fun setMask(mask: Char?): TextBox {
		if (mask != null && TerminalTextUtils.isCharCJK(mask)) {
			throw IllegalArgumentException("Cannot use a CJK character as a mask")
		}
		this.mask = mask
		invalidate()
		return this
	}

	/**
	 * Returns `true` if this `TextBox` is in read-only mode, meaning text input from the user through the
	 * keyboard is prevented
	 * @return `true` if this `TextBox` is in read-only mode
	 */
	fun isReadOnly(): Boolean {
		return readOnly
	}

	/**
	 * Sets the read-only mode of the `TextBox`, meaning text input from the user through the keyboard is
	 * prevented. The user can still focus and scroll through the text in this mode.
	 * @param readOnly If `true` then the `TextBox` will switch to read-only mode
	 * @return Itself
	 */
	fun setReadOnly(readOnly: Boolean): TextBox {
		this.readOnly = readOnly
		invalidate()
		return this
	}

	/**
	 * If `true`, the component will switch to the next available component above if the cursor is at the top of
	 * the TextBox and the user presses the 'up' array key, or switch to the next available component below if the
	 * cursor is at the bottom of the TextBox and the user presses the 'down' array key. The means that for single-line
	 * TextBox:es, pressing up and down will always switch focus.
	 * @return `true` if vertical focus switching is enabled
	 */
	fun isVerticalFocusSwitching(): Boolean {
		return verticalFocusSwitching
	}

	/**
	 * If set to `true`, the component will switch to the next available component above if the cursor is at the
	 * top of the TextBox and the user presses the 'up' array key, or switch to the next available component below if
	 * the cursor is at the bottom of the TextBox and the user presses the 'down' array key. The means that for
	 * single-line TextBox:es, pressing up and down will always switch focus with this mode enabled.
	 * @param verticalFocusSwitching If called with true, vertical focus switching will be enabled
	 * @return Itself
	 */
	fun setVerticalFocusSwitching(verticalFocusSwitching: Boolean): TextBox {
		this.verticalFocusSwitching = verticalFocusSwitching
		return this
	}

	/**
	 * If `true`, the TextBox will switch focus to the next available component to the left if the cursor in the
	 * TextBox is at the left-most position (index 0) on the row and the user pressed the 'left' arrow key, or vice
	 * versa for pressing the 'right' arrow key when the cursor in at the right-most position of the current row.
	 * @return `true` if horizontal focus switching is enabled
	 */
	fun isHorizontalFocusSwitching(): Boolean {
		return horizontalFocusSwitching
	}

	/**
	 * If set to `true`, the TextBox will switch focus to the next available component to the left if the cursor
	 * in the TextBox is at the left-most position (index 0) on the row and the user pressed the 'left' arrow key, or
	 * vice versa for pressing the 'right' arrow key when the cursor in at the right-most position of the current row.
	 * @param horizontalFocusSwitching If called with true, horizontal focus switching will be enabled
	 * @return Itself
	 */
	fun setHorizontalFocusSwitching(horizontalFocusSwitching: Boolean): TextBox {
		this.horizontalFocusSwitching = horizontalFocusSwitching
		return this
	}

	/**
	 * Returns the line on the specific row. For non-multiline TextBox:es, calling this with index set to 0 will return
	 * the same as calling `getText()`. If the row index is invalid (less than zero or equals or larger than the
	 * number of rows), this method will throw IndexOutOfBoundsException.
	 * @param index Index of the row to return the contents from
	 * @return The line at the specified index, as a String
	 * @throws IndexOutOfBoundsException if the row index is less than zero or too large
	 */
	@Synchronized
	fun getLine(index: Int): String {
		return lines[index]
	}

	override fun createDefaultRenderer(): TextBoxRenderer {
		return DefaultTextBoxRenderer()
	}

	@Synchronized public override fun handleKeyStroke(keyStroke: KeyStroke): Interactable.Result {
		if (readOnly) {
			return handleKeyStrokeReadOnly(keyStroke)
		}
		var line = lines[caretPosition!!.row]
		when (keyStroke.keyType) {
			KeyType.Character -> {
				if (maxLineLength == -1 || maxLineLength > line.length + 1) {
					line = line.substring(0, caretPosition!!.column) + keyStroke.character + line.substring(caretPosition!!.column)
					if (validated(line)) {
						lines[caretPosition!!.row] = line
						caretPosition = caretPosition!!.withRelativeColumn(1)
					}
				}
				return Interactable.Result.HANDLED
			}
			KeyType.Backspace -> {
				if (caretPosition!!.column > 0) {
					line = line.substring(0, caretPosition!!.column - 1) + line.substring(caretPosition!!.column)
					if (validated(line)) {
						lines[caretPosition!!.row] = line
						caretPosition = caretPosition!!.withRelativeColumn(-1)
					}
				} else if (style == Style.MULTI_LINE && caretPosition!!.row > 0) {
					val concatenatedLines = lines[caretPosition!!.row - 1] + line
					if (validated(concatenatedLines)) {
						lines.removeAt(caretPosition!!.row)
						caretPosition = caretPosition!!.withRelativeRow(-1)
						caretPosition = caretPosition!!.withColumn(lines[caretPosition!!.row].length)
						lines[caretPosition!!.row] = concatenatedLines
					}
				}
				return Interactable.Result.HANDLED
			}
			KeyType.Delete -> {
				if (caretPosition!!.column < line.length) {
					line = line.substring(0, caretPosition!!.column) + line.substring(caretPosition!!.column + 1)
					if (validated(line)) {
						lines[caretPosition!!.row] = line
					}
				} else if (style == Style.MULTI_LINE && caretPosition!!.row < lines.size - 1) {
					val concatenatedLines = line + lines[caretPosition!!.row + 1]
					if (validated(concatenatedLines)) {
						lines[caretPosition!!.row] = concatenatedLines
						lines.removeAt(caretPosition!!.row + 1)
					}
				}
				return Interactable.Result.HANDLED
			}
			KeyType.ArrowLeft -> {
				if (caretPosition!!.column > 0) {
					caretPosition = caretPosition!!.withRelativeColumn(-1)
				} else if (style == Style.MULTI_LINE && caretWarp && caretPosition!!.row > 0) {
					caretPosition = caretPosition!!.withRelativeRow(-1)
					caretPosition = caretPosition!!.withColumn(lines[caretPosition!!.row].length)
				} else if (horizontalFocusSwitching) {
					return Interactable.Result.MOVE_FOCUS_LEFT
				}
				return Interactable.Result.HANDLED
			}
			KeyType.ArrowRight -> {
				if (caretPosition!!.column < lines[caretPosition!!.row].length) {
					caretPosition = caretPosition!!.withRelativeColumn(1)
				} else if (style == Style.MULTI_LINE && caretWarp && caretPosition!!.row < lines.size - 1) {
					caretPosition = caretPosition!!.withRelativeRow(1)
					caretPosition = caretPosition!!.withColumn(0)
				} else if (horizontalFocusSwitching) {
					return Interactable.Result.MOVE_FOCUS_RIGHT
				}
				return Interactable.Result.HANDLED
			}
			KeyType.ArrowUp -> {
				if (caretPosition!!.row > 0) {
					val trueColumnPosition = TerminalTextUtils.getColumnIndex(lines[caretPosition!!.row], caretPosition!!.column)
					caretPosition = caretPosition!!.withRelativeRow(-1)
					line = lines[caretPosition!!.row]
					if (trueColumnPosition > TerminalTextUtils.getColumnWidth(line)) {
						caretPosition = caretPosition!!.withColumn(line.length)
					} else {
						caretPosition = caretPosition!!.withColumn(TerminalTextUtils.getStringCharacterIndex(line, trueColumnPosition))
					}
				} else if (verticalFocusSwitching) {
					return Interactable.Result.MOVE_FOCUS_UP
				}
				return Interactable.Result.HANDLED
			}
			KeyType.ArrowDown -> {
				if (caretPosition!!.row < lines.size - 1) {
					val trueColumnPosition = TerminalTextUtils.getColumnIndex(lines[caretPosition!!.row], caretPosition!!.column)
					caretPosition = caretPosition!!.withRelativeRow(1)
					line = lines[caretPosition!!.row]
					if (trueColumnPosition > TerminalTextUtils.getColumnWidth(line)) {
						caretPosition = caretPosition!!.withColumn(line.length)
					} else {
						caretPosition = caretPosition!!.withColumn(TerminalTextUtils.getStringCharacterIndex(line, trueColumnPosition))
					}
				} else if (verticalFocusSwitching) {
					return Interactable.Result.MOVE_FOCUS_DOWN
				}
				return Interactable.Result.HANDLED
			}
			KeyType.End -> {
				caretPosition = caretPosition!!.withColumn(line.length)
				return Interactable.Result.HANDLED
			}
			KeyType.Enter -> {
				if (style == Style.SINGLE_LINE) {
					return Interactable.Result.MOVE_FOCUS_NEXT
				}
				val newLine = line.substring(caretPosition!!.column)
				val oldLine = line.substring(0, caretPosition!!.column)
				if (validated(newLine) && validated(oldLine)) {
					lines[caretPosition!!.row] = oldLine
					lines.add(caretPosition!!.row + 1, newLine)
					caretPosition = caretPosition!!.withColumn(0).withRelativeRow(1)
				}
				return Interactable.Result.HANDLED
			}
			KeyType.Home -> {
				caretPosition = caretPosition!!.withColumn(0)
				return Interactable.Result.HANDLED
			}
			KeyType.PageDown -> {
				caretPosition = caretPosition!!.withRelativeRow(size!!.rows)
				if (caretPosition!!.row > lines.size - 1) {
					caretPosition = caretPosition!!.withRow(lines.size - 1)
				}
				if (lines[caretPosition!!.row].length < caretPosition!!.column) {
					caretPosition = caretPosition!!.withColumn(lines[caretPosition!!.row].length)
				}
				return Interactable.Result.HANDLED
			}
			KeyType.PageUp -> {
				caretPosition = caretPosition!!.withRelativeRow(-size!!.rows)
				if (caretPosition!!.row < 0) {
					caretPosition = caretPosition!!.withRow(0)
				}
				if (lines[caretPosition!!.row].length < caretPosition!!.column) {
					caretPosition = caretPosition!!.withColumn(lines[caretPosition!!.row].length)
				}
				return Interactable.Result.HANDLED
			}
		}
		return super.handleKeyStroke(keyStroke)
	}

	private fun validated(line: String): Boolean {
		return validationPattern == null || line.isEmpty() || validationPattern!!.matcher(line).matches()
	}

	private fun handleKeyStrokeReadOnly(keyStroke: KeyStroke): Interactable.Result {
		when (keyStroke.keyType) {
			KeyType.ArrowLeft -> {
				if (renderer.viewTopLeft.column == 0 && horizontalFocusSwitching) {
					return Interactable.Result.MOVE_FOCUS_LEFT
				}
				renderer.viewTopLeft = renderer.viewTopLeft.withRelativeColumn(-1)
				return Interactable.Result.HANDLED
			}
			KeyType.ArrowRight -> {
				if (renderer.viewTopLeft.column + size!!.columns == longestRow && horizontalFocusSwitching) {
					return Interactable.Result.MOVE_FOCUS_RIGHT
				}
				renderer.viewTopLeft = renderer.viewTopLeft.withRelativeColumn(1)
				return Interactable.Result.HANDLED
			}
			KeyType.ArrowUp -> {
				if (renderer.viewTopLeft.row == 0 && verticalFocusSwitching) {
					return Interactable.Result.MOVE_FOCUS_UP
				}
				renderer.viewTopLeft = renderer.viewTopLeft.withRelativeRow(-1)
				return Interactable.Result.HANDLED
			}
			KeyType.ArrowDown -> {
				if (renderer.viewTopLeft.row + size!!.rows == lines.size && verticalFocusSwitching) {
					return Interactable.Result.MOVE_FOCUS_DOWN
				}
				renderer.viewTopLeft = renderer.viewTopLeft.withRelativeRow(1)
				return Interactable.Result.HANDLED
			}
			KeyType.Home -> {
				renderer.viewTopLeft = TerminalPosition.TOP_LEFT_CORNER
				return Interactable.Result.HANDLED
			}
			KeyType.End -> {
				renderer.viewTopLeft = TerminalPosition.TOP_LEFT_CORNER.withRow(lineCount - size!!.rows)
				return Interactable.Result.HANDLED
			}
			KeyType.PageDown -> {
				renderer.viewTopLeft = renderer.viewTopLeft.withRelativeRow(size!!.rows)
				return Interactable.Result.HANDLED
			}
			KeyType.PageUp -> {
				renderer.viewTopLeft = renderer.viewTopLeft.withRelativeRow(-size!!.rows)
				return Interactable.Result.HANDLED
			}
		}
		return super.handleKeyStroke(keyStroke)
	}

	/**
	 * Helper interface that doesn't add any new methods but makes coding new text box renderers a little bit more clear
	 */
	interface TextBoxRenderer : InteractableRenderer<TextBox> {
		var viewTopLeft: TerminalPosition
	}

	/**
	 * This is the default text box renderer that is used if you don't override anything. With this renderer, the text
	 * box is filled with a solid background color and the text is drawn on top of it. Scrollbars are added for
	 * multi-line text whenever the text inside the `TextBox` does not fit in the available area.
	 */
	class DefaultTextBoxRenderer : TextBoxRenderer {
		private var viewTopLeft: TerminalPosition? = null
		private val verticalScrollBar: ScrollBar
		private val horizontalScrollBar: ScrollBar
		private var hideScrollBars: Boolean = false
		private var unusedSpaceCharacter: Char? = null

		/**
		 * Default constructor
		 */
		init {
			viewTopLeft = TerminalPosition.TOP_LEFT_CORNER
			verticalScrollBar = ScrollBar(Direction.VERTICAL)
			horizontalScrollBar = ScrollBar(Direction.HORIZONTAL)
			hideScrollBars = false
			unusedSpaceCharacter = null
		}

		/**
		 * Sets the character to represent an empty untyped space in the text box. This will be an empty space by
		 * default but you can override it to anything that isn't double-width.
		 * @param unusedSpaceCharacter Character to draw in unused space of the [TextBox]
		 * @throws IllegalArgumentException If unusedSpaceCharacter is a double-width character
		 */
		fun setUnusedSpaceCharacter(unusedSpaceCharacter: Char) {
			if (TerminalTextUtils.isCharDoubleWidth(unusedSpaceCharacter)) {
				throw IllegalArgumentException("Cannot use a double-width character as the unused space character in a TextBox")
			}
			this.unusedSpaceCharacter = unusedSpaceCharacter
		}

		override fun getViewTopLeft(): TerminalPosition? {
			return viewTopLeft
		}

		override fun setViewTopLeft(position: TerminalPosition) {
			var position = position
			if (position.column < 0) {
				position = position.withColumn(0)
			}
			if (position.row < 0) {
				position = position.withRow(0)
			}
			viewTopLeft = position
		}

		override fun getCursorLocation(component: TextBox): TerminalPosition? {
			if (component.isReadOnly()) {
				return null
			}

			//Adjust caret position if necessary
			var caretPosition = component.caretPosition
			val line = component.getLine(caretPosition!!.row)
			caretPosition = caretPosition.withColumn(Math.min(caretPosition.column, line.length))

			return caretPosition
				.withColumn(TerminalTextUtils.getColumnIndex(line, caretPosition.column))
				.withRelativeColumn(-viewTopLeft!!.column)
				.withRelativeRow(-viewTopLeft!!.row)
		}

		override fun getPreferredSize(component: TextBox): TerminalSize {
			return TerminalSize(component.longestRow, component.lines.size)
		}

		/**
		 * Controls whether scrollbars should be visible or not when a multi-line `TextBox` has more content than
		 * it can draw in the area it was assigned (default: false)
		 * @param hideScrollBars If `true`, don't show scrollbars if the multi-line content is bigger than the
		 * area
		 */
		fun setHideScrollBars(hideScrollBars: Boolean) {
			this.hideScrollBars = hideScrollBars
		}

		override fun drawComponent(graphics: TextGUIGraphics, component: TextBox) {
			var realTextArea = graphics.size
			if (realTextArea.rows == 0 || realTextArea.columns == 0) {
				return
			}
			var drawVerticalScrollBar = false
			var drawHorizontalScrollBar = false
			val textBoxLineCount = component.lineCount
			if (!hideScrollBars && textBoxLineCount > realTextArea.rows && realTextArea.columns > 1) {
				realTextArea = realTextArea.withRelativeColumns(-1)
				drawVerticalScrollBar = true
			}
			if (!hideScrollBars && component.longestRow > realTextArea.columns && realTextArea.rows > 1) {
				realTextArea = realTextArea.withRelativeRows(-1)
				drawHorizontalScrollBar = true
				if (textBoxLineCount > realTextArea.rows && !drawVerticalScrollBar) {
					realTextArea = realTextArea.withRelativeColumns(-1)
					drawVerticalScrollBar = true
				}
			}

			drawTextArea(graphics.newTextGraphics(TerminalPosition.TOP_LEFT_CORNER, realTextArea), component)

			//Draw scrollbars, if any
			if (drawVerticalScrollBar) {
				verticalScrollBar.onAdded(component.parent)
				verticalScrollBar.viewSize = realTextArea.rows
				verticalScrollBar.scrollMaximum = textBoxLineCount
				verticalScrollBar.scrollPosition = viewTopLeft!!.row
				verticalScrollBar.draw(graphics.newTextGraphics(
					TerminalPosition(graphics.size.columns - 1, 0),
					TerminalSize(1, graphics.size.rows - if (drawHorizontalScrollBar) 1 else 0)))
			}
			if (drawHorizontalScrollBar) {
				horizontalScrollBar.onAdded(component.parent)
				horizontalScrollBar.viewSize = realTextArea.columns
				horizontalScrollBar.scrollMaximum = component.longestRow - 1
				horizontalScrollBar.scrollPosition = viewTopLeft!!.column
				horizontalScrollBar.draw(graphics.newTextGraphics(
					TerminalPosition(0, graphics.size.rows - 1),
					TerminalSize(graphics.size.columns - if (drawVerticalScrollBar) 1 else 0, 1)))
			}
		}

		private fun drawTextArea(graphics: TextGUIGraphics, component: TextBox) {
			val textAreaSize = graphics.size
			if (viewTopLeft!!.column + textAreaSize.columns > component.longestRow) {
				viewTopLeft = viewTopLeft!!.withColumn(component.longestRow - textAreaSize.columns)
				if (viewTopLeft!!.column < 0) {
					viewTopLeft = viewTopLeft!!.withColumn(0)
				}
			}
			if (viewTopLeft!!.row + textAreaSize.rows > component.lineCount) {
				viewTopLeft = viewTopLeft!!.withRow(component.lineCount - textAreaSize.rows)
				if (viewTopLeft!!.row < 0) {
					viewTopLeft = viewTopLeft!!.withRow(0)
				}
			}
			val themeDefinition = component.themeDefinition
			if (component.isFocused) {
				if (component.isReadOnly()) {
					graphics.applyThemeStyle(themeDefinition.selected)
				} else {
					graphics.applyThemeStyle(themeDefinition.active)
				}
			} else {
				if (component.isReadOnly()) {
					graphics.applyThemeStyle(themeDefinition.insensitive)
				} else {
					graphics.applyThemeStyle(themeDefinition.normal)
				}
			}

			var fillCharacter = unusedSpaceCharacter
			if (fillCharacter == null) {
				fillCharacter = themeDefinition.getCharacter("FILL", ' ')
			}
			graphics.fill(fillCharacter)

			if (!component.isReadOnly()) {
				//Adjust caret position if necessary
				var caretPosition = component.caretPosition
				val caretLine = component.getLine(caretPosition!!.row)
				caretPosition = caretPosition.withColumn(Math.min(caretPosition.column, caretLine.length))

				//Adjust the view if necessary
				val trueColumnPosition = TerminalTextUtils.getColumnIndex(caretLine, caretPosition.column)
				if (trueColumnPosition < viewTopLeft!!.column) {
					viewTopLeft = viewTopLeft!!.withColumn(trueColumnPosition)
				} else if (trueColumnPosition >= textAreaSize.columns + viewTopLeft!!.column) {
					viewTopLeft = viewTopLeft!!.withColumn(trueColumnPosition - textAreaSize.columns + 1)
				}
				if (caretPosition.row < viewTopLeft!!.row) {
					viewTopLeft = viewTopLeft!!.withRow(caretPosition.row)
				} else if (caretPosition.row >= textAreaSize.rows + viewTopLeft!!.row) {
					viewTopLeft = viewTopLeft!!.withRow(caretPosition.row - textAreaSize.rows + 1)
				}

				//Additional corner-case for CJK characters
				if (trueColumnPosition - viewTopLeft!!.column == graphics.size.columns - 1) {
					if (caretLine.length > caretPosition.column && TerminalTextUtils.isCharCJK(caretLine[caretPosition.column])) {
						viewTopLeft = viewTopLeft!!.withRelativeColumn(1)
					}
				}
			}

			for (row in 0 until textAreaSize.rows) {
				val rowIndex = row + viewTopLeft!!.row
				if (rowIndex >= component.lines.size) {
					continue
				}
				var line = component.lines[rowIndex]
				if (component.getMask() != null) {
					val builder = StringBuilder()
					for (i in 0 until line.length) {
						builder.append(component.getMask())
					}
					line = builder.toString()
				}
				graphics.putString(0, row, TerminalTextUtils.fitString(line, viewTopLeft!!.column, textAreaSize.columns))
			}
		}
	}
}
/**
 * Creates a new empty `TextBox` with a specific size
 * @param preferredSize Size of the `TextBox`
 */
/**
 * Creates a new empty `TextBox` with a specific size and initial content
 * @param preferredSize Size of the `TextBox`
 * @param initialContent Initial content of the `TextBox`
 */
