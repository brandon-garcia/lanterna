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
import java.awt.Color

/**
 * Color configuration settings to be using with SwingTerminal. This class contains color-related settings that is used
 * by SwingTerminal when it renders the component.
 * @author martin
 */
class TerminalEmulatorColorConfiguration private constructor(private val colorPalette: TerminalEmulatorPalette, private val useBrightColorsOnBold: Boolean) {

	/**
	 * Given a TextColor and a hint as to if the color is to be used as foreground or not and if we currently have
	 * bold text enabled or not, it returns the closest AWT color that matches this.
	 * @param color What text color to convert
	 * @param isForeground Is the color intended to be used as foreground color
	 * @param inBoldContext Is the color intended to be used for on a character this is bold
	 * @return The AWT color that represents this text color
	 */
	fun toAWTColor(color: TextColor, isForeground: Boolean, inBoldContext: Boolean) =
		if (color is TextColor.ANSI) {
			colorPalette.get(color, isForeground, inBoldContext && useBrightColorsOnBold)
		} else color.toColor()

	companion object {

		/**
		 * This is the default settings that is used when you create a new SwingTerminal without specifying any color
		 * configuration. It will use classic VGA colors for the ANSI palette and bright colors on bold text.
		 * @return A terminal emulator color configuration object with values set to classic VGA palette
		 */
		val default: TerminalEmulatorColorConfiguration
			get() = newInstance(TerminalEmulatorPalette.STANDARD_VGA)

		/**
		 * Creates a new color configuration based on a particular palette and with using brighter colors on bold text.
		 * @param colorPalette Palette to use for this color configuration
		 * @return The resulting color configuration
		 */
		fun newInstance(colorPalette: TerminalEmulatorPalette) =
			TerminalEmulatorColorConfiguration(colorPalette, true)
	}
}
