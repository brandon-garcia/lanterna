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
package com.googlecode.lanterna.terminal.swing

import com.googlecode.lanterna.TextColor

/**
 * Object that encapsulates the configuration parameters for the terminal 'device' that a SwingTerminal is emulating.
 * This includes properties such as the shape of the cursor, the color of the cursor, how large scrollback is available
 * and if the cursor should blink or not.
 * @author martin
 */
class TerminalEmulatorDeviceConfiguration
/**
 * Creates a new terminal device configuration object with all configurable values specified.
 * @param lineBufferScrollbackSize How many lines of scrollback buffer should the terminal save?
 * @param blinkLengthInMilliSeconds How many milliseconds does a 'blink' last
 * @param cursorStyle Style of the terminal text cursor
 * @param cursorColor Color of the terminal text cursor
 * @param cursorBlinking Should the terminal text cursor blink?
 * @param clipboardAvailable Should the terminal support pasting text from the clipboard?
 */
@JvmOverloads constructor(
	/**
	 * How many lines of history should be saved so the user can scroll back to them?
	 * @return Number of lines in the scrollback buffer
	 */
	val lineBufferScrollbackSize: Int = 2000,
	/**
	 * Returns the length of a 'blink', which is the interval time a character with the blink SGR enabled with be drawn
	 * with foreground color and background color set to the same.
	 * @return Milliseconds of a blink interval
	 */
	val blinkLengthInMilliSeconds: Int = 500,
	/**
	 * Style the text cursor should take
	 * @return Text cursor style
	 * @see TerminalEmulatorDeviceConfiguration.CursorStyle
	 */
	val cursorStyle: CursorStyle = CursorStyle.REVERSED,
	/**
	 * What color to draw the text cursor color in
	 * @return Color of the text cursor
	 */
	val cursorColor: TextColor = TextColor.RGB(255, 255, 255),
	/**
	 * Should the text cursor be blinking
	 * @return `true` if the text cursor should be blinking
	 */
	val isCursorBlinking: Boolean = false,
	val isClipboardAvailable: Boolean = true) {

	/**
	 * Copies the current configuration. The new object has the given value.
	 * @param blinkLengthInMilliSeconds How many milliseconds does a 'blink' last
	 * @return A copy of the current configuration with the changed value.
	 */
	fun withBlinkLengthInMilliSeconds(blinkLengthInMilliSeconds: Int): TerminalEmulatorDeviceConfiguration {
		return if (this.blinkLengthInMilliSeconds == blinkLengthInMilliSeconds) {
			this
		} else {
			TerminalEmulatorDeviceConfiguration(
				this.lineBufferScrollbackSize,
				blinkLengthInMilliSeconds,
				this.cursorStyle,
				this.cursorColor,
				this.isCursorBlinking,
				this.isClipboardAvailable)
		}
	}

	/**
	 * Copies the current configuration. The new object has the given value.
	 * @param lineBufferScrollbackSize How many lines of scrollback buffer should the terminal save?
	 * @return  A copy of the current configuration with the changed value.
	 */
	fun withLineBufferScrollbackSize(lineBufferScrollbackSize: Int): TerminalEmulatorDeviceConfiguration {
		return if (this.lineBufferScrollbackSize == lineBufferScrollbackSize) {
			this
		} else {
			TerminalEmulatorDeviceConfiguration(
				lineBufferScrollbackSize,
				this.blinkLengthInMilliSeconds,
				this.cursorStyle,
				this.cursorColor,
				this.isCursorBlinking,
				this.isClipboardAvailable)
		}
	}

	/**
	 * Copies the current configuration. The new object has the given value.
	 * @param cursorStyle Style of the terminal text cursor
	 * @return A copy of the current configuration with the changed value.
	 */
	fun withCursorStyle(cursorStyle: CursorStyle): TerminalEmulatorDeviceConfiguration {
		return if (this.cursorStyle == cursorStyle) {
			this
		} else {
			TerminalEmulatorDeviceConfiguration(
				this.lineBufferScrollbackSize,
				this.blinkLengthInMilliSeconds,
				cursorStyle,
				this.cursorColor,
				this.isCursorBlinking,
				this.isClipboardAvailable)
		}
	}

	/**
	 * Copies the current configuration. The new object has the given value.
	 * @param cursorColor Color of the terminal text cursor
	 * @return A copy of the current configuration with the changed value.
	 */
	fun withCursorColor(cursorColor: TextColor): TerminalEmulatorDeviceConfiguration {
		return if (this.cursorColor === cursorColor) {
			this
		} else {
			TerminalEmulatorDeviceConfiguration(
				this.lineBufferScrollbackSize,
				this.blinkLengthInMilliSeconds,
				this.cursorStyle,
				cursorColor,
				this.isCursorBlinking,
				this.isClipboardAvailable)
		}
	}

	/**
	 * Copies the current configuration. The new object has the given value.
	 * @param cursorBlinking Should the terminal text cursor blink?
	 * @return A copy of the current configuration with the changed value.
	 */
	fun withCursorBlinking(cursorBlinking: Boolean): TerminalEmulatorDeviceConfiguration {
		return if (this.isCursorBlinking == cursorBlinking) {
			this
		} else {
			TerminalEmulatorDeviceConfiguration(
				this.lineBufferScrollbackSize,
				this.blinkLengthInMilliSeconds,
				this.cursorStyle,
				this.cursorColor,
				cursorBlinking,
				this.isClipboardAvailable)
		}
	}

	/**
	 * Copies the current configuration. The new object has the given value.
	 * @param clipboardAvailable Should the terminal support pasting text from the clipboard?
	 * @return A copy of the current configuration with the changed value.
	 */
	fun withClipboardAvailable(clipboardAvailable: Boolean): TerminalEmulatorDeviceConfiguration {
		return if (this.isClipboardAvailable == clipboardAvailable) {
			this
		} else {
			TerminalEmulatorDeviceConfiguration(
				this.lineBufferScrollbackSize,
				this.blinkLengthInMilliSeconds,
				this.cursorStyle,
				this.cursorColor,
				this.isCursorBlinking,
				clipboardAvailable)
		}
	}

	/**
	 * Different cursor styles supported by SwingTerminal
	 */
	enum class CursorStyle {

		/**
		 * The cursor is drawn by inverting the front- and background colors of the cursor position
		 */
		REVERSED,

		/**
		 * The cursor is drawn by using the cursor color as the background color for the character at the cursor position
		 */
		FIXED_BACKGROUND,

		/**
		 * The cursor is rendered as a thick horizontal line at the bottom of the character
		 */
		UNDER_BAR,

		/**
		 * The cursor is rendered as a left-side aligned vertical line
		 */
		VERTICAL_BAR

	}

	companion object {

		/**
		 * This is a static reference to the default terminal device configuration. Use this one if you are unsure.
		 * @return A terminal device configuration object with all settings set to default
		 */
		val default: TerminalEmulatorDeviceConfiguration
			get() = TerminalEmulatorDeviceConfiguration()
	}

}
/**
 * Creates a new terminal device configuration object with all the defaults set
 */
/**
 * Creates a new terminal device configuration object with all configurable values specified.
 * @param lineBufferScrollbackSize How many lines of scrollback buffer should the terminal save?
 * @param blinkLengthInMilliSeconds How many milliseconds does a 'blink' last
 * @param cursorStyle Style of the terminal text cursor
 * @param cursorColor Color of the terminal text cursor
 * @param cursorBlinking Should the terminal text cursor blink?
 */
