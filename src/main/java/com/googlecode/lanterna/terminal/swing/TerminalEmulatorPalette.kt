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
 * This class specifies the palette of colors the terminal will use for the normally available 8 + 1 ANSI colors but
 * also their 'bright' versions with are normally enabled through bold mode. There are several palettes available, all
 * based on popular terminal emulators. All colors are defined in the AWT format.
 * @author Martin
 */
class TerminalEmulatorPalette
/**
 * Creates a new palette with all colors specified up-front
 * @param defaultColor Default color which no specific color has been selected
 * @param defaultBrightColor Default color which no specific color has been selected but bold is enabled
 * @param defaultBackgroundColor Default color to use for the background when no specific color has been selected
 * @param normalBlack Color for normal black
 * @param brightBlack Color for bright black
 * @param normalRed Color for normal red
 * @param brightRed Color for bright red
 * @param normalGreen Color for normal green
 * @param brightGreen Color for bright green
 * @param normalYellow Color for normal yellow
 * @param brightYellow Color for bright yellow
 * @param normalBlue Color for normal blue
 * @param brightBlue Color for bright blue
 * @param normalMagenta Color for normal magenta
 * @param brightMagenta Color for bright magenta
 * @param normalCyan Color for normal cyan
 * @param brightCyan Color for bright cyan
 * @param normalWhite Color for normal white
 * @param brightWhite Color for bright white
 */
(
	private val defaultColor: Color?,
	private val defaultBrightColor: Color?,
	private val defaultBackgroundColor: Color?,
	private val normalBlack: Color?,
	private val brightBlack: Color?,
	private val normalRed: Color?,
	private val brightRed: Color?,
	private val normalGreen: Color?,
	private val brightGreen: Color?,
	private val normalYellow: Color?,
	private val brightYellow: Color?,
	private val normalBlue: Color?,
	private val brightBlue: Color?,
	private val normalMagenta: Color?,
	private val brightMagenta: Color?,
	private val normalCyan: Color?,
	private val brightCyan: Color?,
	private val normalWhite: Color?,
	private val brightWhite: Color?) {

	/**
	 * Returns the AWT color from this palette given an ANSI color and two hints for if we are looking for a background
	 * color and if we want to use the bright version.
	 * @param color Which ANSI color we want to extract
	 * @param isForeground Is this color we extract going to be used as a background color?
	 * @param useBrightTones If true, we should return the bright version of the color
	 * @return AWT color extracted from this palette for the input parameters
	 */
	operator fun get(color: TextColor.ANSI, isForeground: Boolean, useBrightTones: Boolean) =
		if (useBrightTones) {
			when (color) {
				TextColor.ANSI.BLACK -> brightBlack
				TextColor.ANSI.BLUE -> brightBlue
				TextColor.ANSI.CYAN -> brightCyan
				TextColor.ANSI.DEFAULT -> if (isForeground) defaultBrightColor else defaultBackgroundColor
				TextColor.ANSI.GREEN -> brightGreen
				TextColor.ANSI.MAGENTA -> brightMagenta
				TextColor.ANSI.RED -> brightRed
				TextColor.ANSI.WHITE -> brightWhite
				TextColor.ANSI.YELLOW -> brightYellow
			}
		} else {
			when (color) {
				TextColor.ANSI.BLACK -> normalBlack
				TextColor.ANSI.BLUE -> normalBlue
				TextColor.ANSI.CYAN -> normalCyan
				TextColor.ANSI.DEFAULT -> if (isForeground) defaultColor else defaultBackgroundColor
				TextColor.ANSI.GREEN -> normalGreen
				TextColor.ANSI.MAGENTA -> normalMagenta
				TextColor.ANSI.RED -> normalRed
				TextColor.ANSI.WHITE -> normalWhite
				TextColor.ANSI.YELLOW -> normalYellow
				else -> throw IllegalArgumentException("Unknown text color " + color)
			}
		}

	override fun equals(obj: Any?): Boolean {
		if (obj == null) {
			return false
		}
		if (javaClass != obj.javaClass) {
			return false
		}
		val other = obj as TerminalEmulatorPalette?
		if (this.defaultColor !== other!!.defaultColor && (this.defaultColor == null || this.defaultColor != other!!.defaultColor)) {
			return false
		}
		if (this.defaultBrightColor !== other!!.defaultBrightColor && (this.defaultBrightColor == null || this.defaultBrightColor != other!!.defaultBrightColor)) {
			return false
		}
		if (this.defaultBackgroundColor !== other!!.defaultBackgroundColor && (this.defaultBackgroundColor == null || this.defaultBackgroundColor != other!!.defaultBackgroundColor)) {
			return false
		}
		if (this.normalBlack !== other!!.normalBlack && (this.normalBlack == null || this.normalBlack != other!!.normalBlack)) {
			return false
		}
		if (this.brightBlack !== other!!.brightBlack && (this.brightBlack == null || this.brightBlack != other!!.brightBlack)) {
			return false
		}
		if (this.normalRed !== other!!.normalRed && (this.normalRed == null || this.normalRed != other!!.normalRed)) {
			return false
		}
		if (this.brightRed !== other!!.brightRed && (this.brightRed == null || this.brightRed != other!!.brightRed)) {
			return false
		}
		if (this.normalGreen !== other!!.normalGreen && (this.normalGreen == null || this.normalGreen != other!!.normalGreen)) {
			return false
		}
		if (this.brightGreen !== other!!.brightGreen && (this.brightGreen == null || this.brightGreen != other!!.brightGreen)) {
			return false
		}
		if (this.normalYellow !== other!!.normalYellow && (this.normalYellow == null || this.normalYellow != other!!.normalYellow)) {
			return false
		}
		if (this.brightYellow !== other!!.brightYellow && (this.brightYellow == null || this.brightYellow != other!!.brightYellow)) {
			return false
		}
		if (this.normalBlue !== other!!.normalBlue && (this.normalBlue == null || this.normalBlue != other!!.normalBlue)) {
			return false
		}
		if (this.brightBlue !== other!!.brightBlue && (this.brightBlue == null || this.brightBlue != other!!.brightBlue)) {
			return false
		}
		if (this.normalMagenta !== other!!.normalMagenta && (this.normalMagenta == null || this.normalMagenta != other!!.normalMagenta)) {
			return false
		}
		if (this.brightMagenta !== other!!.brightMagenta && (this.brightMagenta == null || this.brightMagenta != other!!.brightMagenta)) {
			return false
		}
		if (this.normalCyan !== other!!.normalCyan && (this.normalCyan == null || this.normalCyan != other!!.normalCyan)) {
			return false
		}
		if (this.brightCyan !== other!!.brightCyan && (this.brightCyan == null || this.brightCyan != other!!.brightCyan)) {
			return false
		}
		return if (this.normalWhite !== other!!.normalWhite && (this.normalWhite == null || this.normalWhite != other!!.normalWhite)) {
			false
		} else !(this.brightWhite !== other!!.brightWhite && (this.brightWhite == null || this.brightWhite != other!!.brightWhite))
	}

	override fun hashCode(): Int {
		var hash = 5
		hash = 47 * hash + if (this.defaultColor != null) this.defaultColor.hashCode() else 0
		hash = 47 * hash + if (this.defaultBrightColor != null) this.defaultBrightColor.hashCode() else 0
		hash = 47 * hash + if (this.defaultBackgroundColor != null) this.defaultBackgroundColor.hashCode() else 0
		hash = 47 * hash + if (this.normalBlack != null) this.normalBlack.hashCode() else 0
		hash = 47 * hash + if (this.brightBlack != null) this.brightBlack.hashCode() else 0
		hash = 47 * hash + if (this.normalRed != null) this.normalRed.hashCode() else 0
		hash = 47 * hash + if (this.brightRed != null) this.brightRed.hashCode() else 0
		hash = 47 * hash + if (this.normalGreen != null) this.normalGreen.hashCode() else 0
		hash = 47 * hash + if (this.brightGreen != null) this.brightGreen.hashCode() else 0
		hash = 47 * hash + if (this.normalYellow != null) this.normalYellow.hashCode() else 0
		hash = 47 * hash + if (this.brightYellow != null) this.brightYellow.hashCode() else 0
		hash = 47 * hash + if (this.normalBlue != null) this.normalBlue.hashCode() else 0
		hash = 47 * hash + if (this.brightBlue != null) this.brightBlue.hashCode() else 0
		hash = 47 * hash + if (this.normalMagenta != null) this.normalMagenta.hashCode() else 0
		hash = 47 * hash + if (this.brightMagenta != null) this.brightMagenta.hashCode() else 0
		hash = 47 * hash + if (this.normalCyan != null) this.normalCyan.hashCode() else 0
		hash = 47 * hash + if (this.brightCyan != null) this.brightCyan.hashCode() else 0
		hash = 47 * hash + if (this.normalWhite != null) this.normalWhite.hashCode() else 0
		hash = 47 * hash + if (this.brightWhite != null) this.brightWhite.hashCode() else 0
		return hash
	}

	override fun toString() =
		"SwingTerminalPalette{" +
			"defaultColor=" + defaultColor +
			", defaultBrightColor=" + defaultBrightColor +
			", defaultBackgroundColor=" + defaultBackgroundColor +
			", normalBlack=" + normalBlack +
			", brightBlack=" + brightBlack +
			", normalRed=" + normalRed +
			", brightRed=" + brightRed +
			", normalGreen=" + normalGreen +
			", brightGreen=" + brightGreen +
			", normalYellow=" + normalYellow +
			", brightYellow=" + brightYellow +
			", normalBlue=" + normalBlue +
			", brightBlue=" + brightBlue +
			", normalMagenta=" + normalMagenta +
			", brightMagenta=" + brightMagenta +
			", normalCyan=" + normalCyan +
			", brightCyan=" + brightCyan +
			", normalWhite=" + normalWhite +
			", brightWhite=" + brightWhite + '}'

	companion object {
		/**
		 * Values taken from gnome-terminal on Ubuntu
		 */
		val GNOME_TERMINAL = TerminalEmulatorPalette(
			java.awt.Color(211, 215, 207),
			java.awt.Color(238, 238, 236),
			java.awt.Color(46, 52, 54),
			java.awt.Color(46, 52, 54),
			java.awt.Color(85, 87, 83),
			java.awt.Color(204, 0, 0),
			java.awt.Color(239, 41, 41),
			java.awt.Color(78, 154, 6),
			java.awt.Color(138, 226, 52),
			java.awt.Color(196, 160, 0),
			java.awt.Color(252, 233, 79),
			java.awt.Color(52, 101, 164),
			java.awt.Color(114, 159, 207),
			java.awt.Color(117, 80, 123),
			java.awt.Color(173, 127, 168),
			java.awt.Color(6, 152, 154),
			java.awt.Color(52, 226, 226),
			java.awt.Color(211, 215, 207),
			java.awt.Color(238, 238, 236))

		/**
		 * Values taken from [
 * wikipedia](http://en.wikipedia.org/wiki/ANSI_escape_code), these are supposed to be the standard VGA palette.
		 */
		val STANDARD_VGA = TerminalEmulatorPalette(
			java.awt.Color(170, 170, 170),
			java.awt.Color(255, 255, 255),
			java.awt.Color(0, 0, 0),
			java.awt.Color(0, 0, 0),
			java.awt.Color(85, 85, 85),
			java.awt.Color(170, 0, 0),
			java.awt.Color(255, 85, 85),
			java.awt.Color(0, 170, 0),
			java.awt.Color(85, 255, 85),
			java.awt.Color(170, 85, 0),
			java.awt.Color(255, 255, 85),
			java.awt.Color(0, 0, 170),
			java.awt.Color(85, 85, 255),
			java.awt.Color(170, 0, 170),
			java.awt.Color(255, 85, 255),
			java.awt.Color(0, 170, 170),
			java.awt.Color(85, 255, 255),
			java.awt.Color(170, 170, 170),
			java.awt.Color(255, 255, 255))

		/**
		 * Values taken from [
 * wikipedia](http://en.wikipedia.org/wiki/ANSI_escape_code), these are supposed to be what Windows XP cmd is using.
		 */
		val WINDOWS_XP_COMMAND_PROMPT = TerminalEmulatorPalette(
			java.awt.Color(192, 192, 192),
			java.awt.Color(255, 255, 255),
			java.awt.Color(0, 0, 0),
			java.awt.Color(0, 0, 0),
			java.awt.Color(128, 128, 128),
			java.awt.Color(128, 0, 0),
			java.awt.Color(255, 0, 0),
			java.awt.Color(0, 128, 0),
			java.awt.Color(0, 255, 0),
			java.awt.Color(128, 128, 0),
			java.awt.Color(255, 255, 0),
			java.awt.Color(0, 0, 128),
			java.awt.Color(0, 0, 255),
			java.awt.Color(128, 0, 128),
			java.awt.Color(255, 0, 255),
			java.awt.Color(0, 128, 128),
			java.awt.Color(0, 255, 255),
			java.awt.Color(192, 192, 192),
			java.awt.Color(255, 255, 255))

		/**
		 * Values taken from [
 * wikipedia](http://en.wikipedia.org/wiki/ANSI_escape_code), these are supposed to be what terminal.app on MacOSX is using.
		 */
		val MAC_OS_X_TERMINAL_APP = TerminalEmulatorPalette(
			java.awt.Color(203, 204, 205),
			java.awt.Color(233, 235, 235),
			java.awt.Color(0, 0, 0),
			java.awt.Color(0, 0, 0),
			java.awt.Color(129, 131, 131),
			java.awt.Color(194, 54, 33),
			java.awt.Color(252, 57, 31),
			java.awt.Color(37, 188, 36),
			java.awt.Color(49, 231, 34),
			java.awt.Color(173, 173, 39),
			java.awt.Color(234, 236, 35),
			java.awt.Color(73, 46, 225),
			java.awt.Color(88, 51, 255),
			java.awt.Color(211, 56, 211),
			java.awt.Color(249, 53, 248),
			java.awt.Color(51, 187, 200),
			java.awt.Color(20, 240, 240),
			java.awt.Color(203, 204, 205),
			java.awt.Color(233, 235, 235))

		/**
		 * Values taken from [
 * wikipedia](http://en.wikipedia.org/wiki/ANSI_escape_code), these are supposed to be what putty is using.
		 */
		val PUTTY = TerminalEmulatorPalette(
			java.awt.Color(187, 187, 187),
			java.awt.Color(255, 255, 255),
			java.awt.Color(0, 0, 0),
			java.awt.Color(0, 0, 0),
			java.awt.Color(85, 85, 85),
			java.awt.Color(187, 0, 0),
			java.awt.Color(255, 85, 85),
			java.awt.Color(0, 187, 0),
			java.awt.Color(85, 255, 85),
			java.awt.Color(187, 187, 0),
			java.awt.Color(255, 255, 85),
			java.awt.Color(0, 0, 187),
			java.awt.Color(85, 85, 255),
			java.awt.Color(187, 0, 187),
			java.awt.Color(255, 85, 255),
			java.awt.Color(0, 187, 187),
			java.awt.Color(85, 255, 255),
			java.awt.Color(187, 187, 187),
			java.awt.Color(255, 255, 255))

		/**
		 * Values taken from [
 * wikipedia](http://en.wikipedia.org/wiki/ANSI_escape_code), these are supposed to be what xterm is using.
		 */
		val XTERM = TerminalEmulatorPalette(
			java.awt.Color(229, 229, 229),
			java.awt.Color(255, 255, 255),
			java.awt.Color(0, 0, 0),
			java.awt.Color(0, 0, 0),
			java.awt.Color(127, 127, 127),
			java.awt.Color(205, 0, 0),
			java.awt.Color(255, 0, 0),
			java.awt.Color(0, 205, 0),
			java.awt.Color(0, 255, 0),
			java.awt.Color(205, 205, 0),
			java.awt.Color(255, 255, 0),
			java.awt.Color(0, 0, 238),
			java.awt.Color(92, 92, 255),
			java.awt.Color(205, 0, 205),
			java.awt.Color(255, 0, 255),
			java.awt.Color(0, 205, 205),
			java.awt.Color(0, 255, 255),
			java.awt.Color(229, 229, 229),
			java.awt.Color(255, 255, 255))

		/**
		 * Default colors the SwingTerminal is using if you don't specify anything
		 */
		val DEFAULT = GNOME_TERMINAL
	}
}
