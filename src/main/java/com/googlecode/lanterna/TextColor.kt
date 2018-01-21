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


import java.awt.*
import java.util.regex.Pattern
import kotlin.experimental.and

/**
 * This is an abstract base class for terminal color definitions. Since there are different ways of specifying terminal
 * colors, all with a different range of adoptions, this makes it possible to program an API against an implementation-
 * agnostic color definition. Please remember when using colors that not all terminals and terminal emulators supports
 * them. The 24-bit color mode is very unsupported, for example, and even the default Linux terminal doesn't support
 * the 256-color indexed mode.
 *
 * @author Martin
 */
interface TextColor {
	/**
	 * Returns the byte sequence in between CSI and character 'm' that is used to enable this color as the foreground
	 * color on an ANSI-compatible terminal.
	 * @return Byte array out data to output in between of CSI and 'm'
	 */
	val foregroundSGRSequence: ByteArray

	/**
	 * Returns the byte sequence in between CSI and character 'm' that is used to enable this color as the background
	 * color on an ANSI-compatible terminal.
	 * @return Byte array out data to output in between of CSI and 'm'
	 */
	val backgroundSGRSequence: ByteArray

	/**
	 * Converts this color to an AWT color object, assuming a standard VGA palette.
	 * @return TextColor as an AWT Color
	 */
	fun toColor(): Color

	/**
	 * This class represent classic ANSI colors that are likely to be very compatible with most terminal
	 * implementations. It is limited to 8 colors (plus the 'default' color) but as a norm, using bold mode (SGR code)
	 * will slightly alter the color, giving it a bit brighter tone, so in total this will give you 16 (+1) colors.
	 *
	 *
	 * For more information, see http://en.wikipedia.org/wiki/File:Ansi.png
	 */
	enum class ANSI private constructor(private val index: Byte, red: Int, green: Int, blue: Int) : TextColor {
		BLACK(0.toByte(), 0, 0, 0),
		RED(1.toByte(), 170, 0, 0),
		GREEN(2.toByte(), 0, 170, 0),
		YELLOW(3.toByte(), 170, 85, 0),
		BLUE(4.toByte(), 0, 0, 170),
		MAGENTA(5.toByte(), 170, 0, 170),
		CYAN(6.toByte(), 0, 170, 170),
		WHITE(7.toByte(), 170, 170, 170),
		DEFAULT(9.toByte(), 0, 0, 0);

		private val color: Color

		override//48 is ascii code for '0'
		val foregroundSGRSequence: ByteArray
			get() = byteArrayOf('3'.toByte(), (48 + index).toByte())

		override//48 is ascii code for '0'
		val backgroundSGRSequence: ByteArray
			get() = byteArrayOf('4'.toByte(), (48 + index).toByte())

		init {
			this.color = Color(red, green, blue)
		}

		override fun toColor(): Color {
			return color
		}
	}

	/**
	 * This class represents a color expressed in the indexed XTerm 256 color extension, where each color is defined in a
	 * lookup-table. All in all, there are 256 codes, but in order to know which one to know you either need to have the
	 * table at hand, or you can use the two static helper methods which can help you convert from three 8-bit
	 * RGB values to the closest approximate indexed color number. If you are interested, the 256 index values are
	 * actually divided like this:<br></br>
	 * 0 .. 15 - System colors, same as ANSI, but the actual rendered color depends on the terminal emulators color scheme<br></br>
	 * 16 .. 231 - Forms a 6x6x6 RGB color cube<br></br>
	 * 232 .. 255 - A gray scale ramp (without black and white endpoints)<br></br>
	 *
	 *
	 * Support for indexed colors is somewhat widely adopted, not as much as the ANSI colors (TextColor.ANSI) but more
	 * than the RGB (TextColor.RGB).
	 *
	 *
	 * For more details on this, please see [
 * this](https://github.com/robertknight/konsole/blob/master/user-doc/README.moreColors) commit message to Konsole.
	 */
	class Indexed
	/**
	 * Creates a new TextColor using the XTerm 256 color indexed mode, with the specified index value. You must
	 * choose a value between 0 and 255.
	 * @param colorIndex Index value to use for this color.
	 */
	(private val colorIndex: Int) : TextColor {
		private val awtColor: Color

		override val foregroundSGRSequence: ByteArray
			get() = ("38;5;" + colorIndex).toByteArray()

		override val backgroundSGRSequence: ByteArray
			get() = ("48;5;" + colorIndex).toByteArray()

		init {
			if (colorIndex > 255 || colorIndex < 0) {
				throw IllegalArgumentException("Cannot create a Color.Indexed with a color index of " + colorIndex +
					", must be in the range of 0-255")
			}
			this.awtColor = Color(
				COLOR_TABLE[colorIndex][0].and(0x000000ff.toByte()).toInt(),
				COLOR_TABLE[colorIndex][1].and(0x000000ff.toByte()).toInt(),
				COLOR_TABLE[colorIndex][2].and(0x000000ff.toByte()).toInt()
			)
		}

		override fun toColor(): Color {
			return awtColor
		}

		override fun toString(): String {
			return "{IndexedColor:$colorIndex}"
		}

		override fun hashCode(): Int {
			var hash = 3
			hash = 43 * hash + this.colorIndex
			return hash
		}

		override fun equals(obj: Any?): Boolean {
			if (obj == null) {
				return false
			}
			if (javaClass != obj.javaClass) {
				return false
			}
			val other = obj as Indexed?
			return this.colorIndex == other!!.colorIndex
		}

		companion object {
			private val COLOR_TABLE = arrayOf(
				//These are the standard 16-color VGA palette entries
				byteArrayOf(0.toByte(), 0.toByte(), 0.toByte()), byteArrayOf(170.toByte(), 0.toByte(), 0.toByte()), byteArrayOf(0.toByte(), 170.toByte(), 0.toByte()), byteArrayOf(170.toByte(), 85.toByte(), 0.toByte()), byteArrayOf(0.toByte(), 0.toByte(), 170.toByte()), byteArrayOf(170.toByte(), 0.toByte(), 170.toByte()), byteArrayOf(0.toByte(), 170.toByte(), 170.toByte()), byteArrayOf(170.toByte(), 170.toByte(), 170.toByte()), byteArrayOf(85.toByte(), 85.toByte(), 85.toByte()), byteArrayOf(255.toByte(), 85.toByte(), 85.toByte()), byteArrayOf(85.toByte(), 255.toByte(), 85.toByte()), byteArrayOf(255.toByte(), 255.toByte(), 85.toByte()), byteArrayOf(85.toByte(), 85.toByte(), 255.toByte()), byteArrayOf(255.toByte(), 85.toByte(), 255.toByte()), byteArrayOf(85.toByte(), 255.toByte(), 255.toByte()), byteArrayOf(255.toByte(), 255.toByte(), 255.toByte()),

				//Starting 6x6x6 RGB color cube from 16
				byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte()), byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x5f.toByte()), byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x87.toByte()), byteArrayOf(0x00.toByte(), 0x00.toByte(), 0xaf.toByte()), byteArrayOf(0x00.toByte(), 0x00.toByte(), 0xd7.toByte()), byteArrayOf(0x00.toByte(), 0x00.toByte(), 0xff.toByte()), byteArrayOf(0x00.toByte(), 0x5f.toByte(), 0x00.toByte()), byteArrayOf(0x00.toByte(), 0x5f.toByte(), 0x5f.toByte()), byteArrayOf(0x00.toByte(), 0x5f.toByte(), 0x87.toByte()), byteArrayOf(0x00.toByte(), 0x5f.toByte(), 0xaf.toByte()), byteArrayOf(0x00.toByte(), 0x5f.toByte(), 0xd7.toByte()), byteArrayOf(0x00.toByte(), 0x5f.toByte(), 0xff.toByte()), byteArrayOf(0x00.toByte(), 0x87.toByte(), 0x00.toByte()), byteArrayOf(0x00.toByte(), 0x87.toByte(), 0x5f.toByte()), byteArrayOf(0x00.toByte(), 0x87.toByte(), 0x87.toByte()), byteArrayOf(0x00.toByte(), 0x87.toByte(), 0xaf.toByte()), byteArrayOf(0x00.toByte(), 0x87.toByte(), 0xd7.toByte()), byteArrayOf(0x00.toByte(), 0x87.toByte(), 0xff.toByte()), byteArrayOf(0x00.toByte(), 0xaf.toByte(), 0x00.toByte()), byteArrayOf(0x00.toByte(), 0xaf.toByte(), 0x5f.toByte()), byteArrayOf(0x00.toByte(), 0xaf.toByte(), 0x87.toByte()), byteArrayOf(0x00.toByte(), 0xaf.toByte(), 0xaf.toByte()), byteArrayOf(0x00.toByte(), 0xaf.toByte(), 0xd7.toByte()), byteArrayOf(0x00.toByte(), 0xaf.toByte(), 0xff.toByte()), byteArrayOf(0x00.toByte(), 0xd7.toByte(), 0x00.toByte()), byteArrayOf(0x00.toByte(), 0xd7.toByte(), 0x5f.toByte()), byteArrayOf(0x00.toByte(), 0xd7.toByte(), 0x87.toByte()), byteArrayOf(0x00.toByte(), 0xd7.toByte(), 0xaf.toByte()), byteArrayOf(0x00.toByte(), 0xd7.toByte(), 0xd7.toByte()), byteArrayOf(0x00.toByte(), 0xd7.toByte(), 0xff.toByte()), byteArrayOf(0x00.toByte(), 0xff.toByte(), 0x00.toByte()), byteArrayOf(0x00.toByte(), 0xff.toByte(), 0x5f.toByte()), byteArrayOf(0x00.toByte(), 0xff.toByte(), 0x87.toByte()), byteArrayOf(0x00.toByte(), 0xff.toByte(), 0xaf.toByte()), byteArrayOf(0x00.toByte(), 0xff.toByte(), 0xd7.toByte()), byteArrayOf(0x00.toByte(), 0xff.toByte(), 0xff.toByte()), byteArrayOf(0x5f.toByte(), 0x00.toByte(), 0x00.toByte()), byteArrayOf(0x5f.toByte(), 0x00.toByte(), 0x5f.toByte()), byteArrayOf(0x5f.toByte(), 0x00.toByte(), 0x87.toByte()), byteArrayOf(0x5f.toByte(), 0x00.toByte(), 0xaf.toByte()), byteArrayOf(0x5f.toByte(), 0x00.toByte(), 0xd7.toByte()), byteArrayOf(0x5f.toByte(), 0x00.toByte(), 0xff.toByte()), byteArrayOf(0x5f.toByte(), 0x5f.toByte(), 0x00.toByte()), byteArrayOf(0x5f.toByte(), 0x5f.toByte(), 0x5f.toByte()), byteArrayOf(0x5f.toByte(), 0x5f.toByte(), 0x87.toByte()), byteArrayOf(0x5f.toByte(), 0x5f.toByte(), 0xaf.toByte()), byteArrayOf(0x5f.toByte(), 0x5f.toByte(), 0xd7.toByte()), byteArrayOf(0x5f.toByte(), 0x5f.toByte(), 0xff.toByte()), byteArrayOf(0x5f.toByte(), 0x87.toByte(), 0x00.toByte()), byteArrayOf(0x5f.toByte(), 0x87.toByte(), 0x5f.toByte()), byteArrayOf(0x5f.toByte(), 0x87.toByte(), 0x87.toByte()), byteArrayOf(0x5f.toByte(), 0x87.toByte(), 0xaf.toByte()), byteArrayOf(0x5f.toByte(), 0x87.toByte(), 0xd7.toByte()), byteArrayOf(0x5f.toByte(), 0x87.toByte(), 0xff.toByte()), byteArrayOf(0x5f.toByte(), 0xaf.toByte(), 0x00.toByte()), byteArrayOf(0x5f.toByte(), 0xaf.toByte(), 0x5f.toByte()), byteArrayOf(0x5f.toByte(), 0xaf.toByte(), 0x87.toByte()), byteArrayOf(0x5f.toByte(), 0xaf.toByte(), 0xaf.toByte()), byteArrayOf(0x5f.toByte(), 0xaf.toByte(), 0xd7.toByte()), byteArrayOf(0x5f.toByte(), 0xaf.toByte(), 0xff.toByte()), byteArrayOf(0x5f.toByte(), 0xd7.toByte(), 0x00.toByte()), byteArrayOf(0x5f.toByte(), 0xd7.toByte(), 0x5f.toByte()), byteArrayOf(0x5f.toByte(), 0xd7.toByte(), 0x87.toByte()), byteArrayOf(0x5f.toByte(), 0xd7.toByte(), 0xaf.toByte()), byteArrayOf(0x5f.toByte(), 0xd7.toByte(), 0xd7.toByte()), byteArrayOf(0x5f.toByte(), 0xd7.toByte(), 0xff.toByte()), byteArrayOf(0x5f.toByte(), 0xff.toByte(), 0x00.toByte()), byteArrayOf(0x5f.toByte(), 0xff.toByte(), 0x5f.toByte()), byteArrayOf(0x5f.toByte(), 0xff.toByte(), 0x87.toByte()), byteArrayOf(0x5f.toByte(), 0xff.toByte(), 0xaf.toByte()), byteArrayOf(0x5f.toByte(), 0xff.toByte(), 0xd7.toByte()), byteArrayOf(0x5f.toByte(), 0xff.toByte(), 0xff.toByte()), byteArrayOf(0x87.toByte(), 0x00.toByte(), 0x00.toByte()), byteArrayOf(0x87.toByte(), 0x00.toByte(), 0x5f.toByte()), byteArrayOf(0x87.toByte(), 0x00.toByte(), 0x87.toByte()), byteArrayOf(0x87.toByte(), 0x00.toByte(), 0xaf.toByte()), byteArrayOf(0x87.toByte(), 0x00.toByte(), 0xd7.toByte()), byteArrayOf(0x87.toByte(), 0x00.toByte(), 0xff.toByte()), byteArrayOf(0x87.toByte(), 0x5f.toByte(), 0x00.toByte()), byteArrayOf(0x87.toByte(), 0x5f.toByte(), 0x5f.toByte()), byteArrayOf(0x87.toByte(), 0x5f.toByte(), 0x87.toByte()), byteArrayOf(0x87.toByte(), 0x5f.toByte(), 0xaf.toByte()), byteArrayOf(0x87.toByte(), 0x5f.toByte(), 0xd7.toByte()), byteArrayOf(0x87.toByte(), 0x5f.toByte(), 0xff.toByte()), byteArrayOf(0x87.toByte(), 0x87.toByte(), 0x00.toByte()), byteArrayOf(0x87.toByte(), 0x87.toByte(), 0x5f.toByte()), byteArrayOf(0x87.toByte(), 0x87.toByte(), 0x87.toByte()), byteArrayOf(0x87.toByte(), 0x87.toByte(), 0xaf.toByte()), byteArrayOf(0x87.toByte(), 0x87.toByte(), 0xd7.toByte()), byteArrayOf(0x87.toByte(), 0x87.toByte(), 0xff.toByte()), byteArrayOf(0x87.toByte(), 0xaf.toByte(), 0x00.toByte()), byteArrayOf(0x87.toByte(), 0xaf.toByte(), 0x5f.toByte()), byteArrayOf(0x87.toByte(), 0xaf.toByte(), 0x87.toByte()), byteArrayOf(0x87.toByte(), 0xaf.toByte(), 0xaf.toByte()), byteArrayOf(0x87.toByte(), 0xaf.toByte(), 0xd7.toByte()), byteArrayOf(0x87.toByte(), 0xaf.toByte(), 0xff.toByte()), byteArrayOf(0x87.toByte(), 0xd7.toByte(), 0x00.toByte()), byteArrayOf(0x87.toByte(), 0xd7.toByte(), 0x5f.toByte()), byteArrayOf(0x87.toByte(), 0xd7.toByte(), 0x87.toByte()), byteArrayOf(0x87.toByte(), 0xd7.toByte(), 0xaf.toByte()), byteArrayOf(0x87.toByte(), 0xd7.toByte(), 0xd7.toByte()), byteArrayOf(0x87.toByte(), 0xd7.toByte(), 0xff.toByte()), byteArrayOf(0x87.toByte(), 0xff.toByte(), 0x00.toByte()), byteArrayOf(0x87.toByte(), 0xff.toByte(), 0x5f.toByte()), byteArrayOf(0x87.toByte(), 0xff.toByte(), 0x87.toByte()), byteArrayOf(0x87.toByte(), 0xff.toByte(), 0xaf.toByte()), byteArrayOf(0x87.toByte(), 0xff.toByte(), 0xd7.toByte()), byteArrayOf(0x87.toByte(), 0xff.toByte(), 0xff.toByte()), byteArrayOf(0xaf.toByte(), 0x00.toByte(), 0x00.toByte()), byteArrayOf(0xaf.toByte(), 0x00.toByte(), 0x5f.toByte()), byteArrayOf(0xaf.toByte(), 0x00.toByte(), 0x87.toByte()), byteArrayOf(0xaf.toByte(), 0x00.toByte(), 0xaf.toByte()), byteArrayOf(0xaf.toByte(), 0x00.toByte(), 0xd7.toByte()), byteArrayOf(0xaf.toByte(), 0x00.toByte(), 0xff.toByte()), byteArrayOf(0xaf.toByte(), 0x5f.toByte(), 0x00.toByte()), byteArrayOf(0xaf.toByte(), 0x5f.toByte(), 0x5f.toByte()), byteArrayOf(0xaf.toByte(), 0x5f.toByte(), 0x87.toByte()), byteArrayOf(0xaf.toByte(), 0x5f.toByte(), 0xaf.toByte()), byteArrayOf(0xaf.toByte(), 0x5f.toByte(), 0xd7.toByte()), byteArrayOf(0xaf.toByte(), 0x5f.toByte(), 0xff.toByte()), byteArrayOf(0xaf.toByte(), 0x87.toByte(), 0x00.toByte()), byteArrayOf(0xaf.toByte(), 0x87.toByte(), 0x5f.toByte()), byteArrayOf(0xaf.toByte(), 0x87.toByte(), 0x87.toByte()), byteArrayOf(0xaf.toByte(), 0x87.toByte(), 0xaf.toByte()), byteArrayOf(0xaf.toByte(), 0x87.toByte(), 0xd7.toByte()), byteArrayOf(0xaf.toByte(), 0x87.toByte(), 0xff.toByte()), byteArrayOf(0xaf.toByte(), 0xaf.toByte(), 0x00.toByte()), byteArrayOf(0xaf.toByte(), 0xaf.toByte(), 0x5f.toByte()), byteArrayOf(0xaf.toByte(), 0xaf.toByte(), 0x87.toByte()), byteArrayOf(0xaf.toByte(), 0xaf.toByte(), 0xaf.toByte()), byteArrayOf(0xaf.toByte(), 0xaf.toByte(), 0xd7.toByte()), byteArrayOf(0xaf.toByte(), 0xaf.toByte(), 0xff.toByte()), byteArrayOf(0xaf.toByte(), 0xd7.toByte(), 0x00.toByte()), byteArrayOf(0xaf.toByte(), 0xd7.toByte(), 0x5f.toByte()), byteArrayOf(0xaf.toByte(), 0xd7.toByte(), 0x87.toByte()), byteArrayOf(0xaf.toByte(), 0xd7.toByte(), 0xaf.toByte()), byteArrayOf(0xaf.toByte(), 0xd7.toByte(), 0xd7.toByte()), byteArrayOf(0xaf.toByte(), 0xd7.toByte(), 0xff.toByte()), byteArrayOf(0xaf.toByte(), 0xff.toByte(), 0x00.toByte()), byteArrayOf(0xaf.toByte(), 0xff.toByte(), 0x5f.toByte()), byteArrayOf(0xaf.toByte(), 0xff.toByte(), 0x87.toByte()), byteArrayOf(0xaf.toByte(), 0xff.toByte(), 0xaf.toByte()), byteArrayOf(0xaf.toByte(), 0xff.toByte(), 0xd7.toByte()), byteArrayOf(0xaf.toByte(), 0xff.toByte(), 0xff.toByte()), byteArrayOf(0xd7.toByte(), 0x00.toByte(), 0x00.toByte()), byteArrayOf(0xd7.toByte(), 0x00.toByte(), 0x5f.toByte()), byteArrayOf(0xd7.toByte(), 0x00.toByte(), 0x87.toByte()), byteArrayOf(0xd7.toByte(), 0x00.toByte(), 0xaf.toByte()), byteArrayOf(0xd7.toByte(), 0x00.toByte(), 0xd7.toByte()), byteArrayOf(0xd7.toByte(), 0x00.toByte(), 0xff.toByte()), byteArrayOf(0xd7.toByte(), 0x5f.toByte(), 0x00.toByte()), byteArrayOf(0xd7.toByte(), 0x5f.toByte(), 0x5f.toByte()), byteArrayOf(0xd7.toByte(), 0x5f.toByte(), 0x87.toByte()), byteArrayOf(0xd7.toByte(), 0x5f.toByte(), 0xaf.toByte()), byteArrayOf(0xd7.toByte(), 0x5f.toByte(), 0xd7.toByte()), byteArrayOf(0xd7.toByte(), 0x5f.toByte(), 0xff.toByte()), byteArrayOf(0xd7.toByte(), 0x87.toByte(), 0x00.toByte()), byteArrayOf(0xd7.toByte(), 0x87.toByte(), 0x5f.toByte()), byteArrayOf(0xd7.toByte(), 0x87.toByte(), 0x87.toByte()), byteArrayOf(0xd7.toByte(), 0x87.toByte(), 0xaf.toByte()), byteArrayOf(0xd7.toByte(), 0x87.toByte(), 0xd7.toByte()), byteArrayOf(0xd7.toByte(), 0x87.toByte(), 0xff.toByte()), byteArrayOf(0xd7.toByte(), 0xaf.toByte(), 0x00.toByte()), byteArrayOf(0xd7.toByte(), 0xaf.toByte(), 0x5f.toByte()), byteArrayOf(0xd7.toByte(), 0xaf.toByte(), 0x87.toByte()), byteArrayOf(0xd7.toByte(), 0xaf.toByte(), 0xaf.toByte()), byteArrayOf(0xd7.toByte(), 0xaf.toByte(), 0xd7.toByte()), byteArrayOf(0xd7.toByte(), 0xaf.toByte(), 0xff.toByte()), byteArrayOf(0xd7.toByte(), 0xd7.toByte(), 0x00.toByte()), byteArrayOf(0xd7.toByte(), 0xd7.toByte(), 0x5f.toByte()), byteArrayOf(0xd7.toByte(), 0xd7.toByte(), 0x87.toByte()), byteArrayOf(0xd7.toByte(), 0xd7.toByte(), 0xaf.toByte()), byteArrayOf(0xd7.toByte(), 0xd7.toByte(), 0xd7.toByte()), byteArrayOf(0xd7.toByte(), 0xd7.toByte(), 0xff.toByte()), byteArrayOf(0xd7.toByte(), 0xff.toByte(), 0x00.toByte()), byteArrayOf(0xd7.toByte(), 0xff.toByte(), 0x5f.toByte()), byteArrayOf(0xd7.toByte(), 0xff.toByte(), 0x87.toByte()), byteArrayOf(0xd7.toByte(), 0xff.toByte(), 0xaf.toByte()), byteArrayOf(0xd7.toByte(), 0xff.toByte(), 0xd7.toByte()), byteArrayOf(0xd7.toByte(), 0xff.toByte(), 0xff.toByte()), byteArrayOf(0xff.toByte(), 0x00.toByte(), 0x00.toByte()), byteArrayOf(0xff.toByte(), 0x00.toByte(), 0x5f.toByte()), byteArrayOf(0xff.toByte(), 0x00.toByte(), 0x87.toByte()), byteArrayOf(0xff.toByte(), 0x00.toByte(), 0xaf.toByte()), byteArrayOf(0xff.toByte(), 0x00.toByte(), 0xd7.toByte()), byteArrayOf(0xff.toByte(), 0x00.toByte(), 0xff.toByte()), byteArrayOf(0xff.toByte(), 0x5f.toByte(), 0x00.toByte()), byteArrayOf(0xff.toByte(), 0x5f.toByte(), 0x5f.toByte()), byteArrayOf(0xff.toByte(), 0x5f.toByte(), 0x87.toByte()), byteArrayOf(0xff.toByte(), 0x5f.toByte(), 0xaf.toByte()), byteArrayOf(0xff.toByte(), 0x5f.toByte(), 0xd7.toByte()), byteArrayOf(0xff.toByte(), 0x5f.toByte(), 0xff.toByte()), byteArrayOf(0xff.toByte(), 0x87.toByte(), 0x00.toByte()), byteArrayOf(0xff.toByte(), 0x87.toByte(), 0x5f.toByte()), byteArrayOf(0xff.toByte(), 0x87.toByte(), 0x87.toByte()), byteArrayOf(0xff.toByte(), 0x87.toByte(), 0xaf.toByte()), byteArrayOf(0xff.toByte(), 0x87.toByte(), 0xd7.toByte()), byteArrayOf(0xff.toByte(), 0x87.toByte(), 0xff.toByte()), byteArrayOf(0xff.toByte(), 0xaf.toByte(), 0x00.toByte()), byteArrayOf(0xff.toByte(), 0xaf.toByte(), 0x5f.toByte()), byteArrayOf(0xff.toByte(), 0xaf.toByte(), 0x87.toByte()), byteArrayOf(0xff.toByte(), 0xaf.toByte(), 0xaf.toByte()), byteArrayOf(0xff.toByte(), 0xaf.toByte(), 0xd7.toByte()), byteArrayOf(0xff.toByte(), 0xaf.toByte(), 0xff.toByte()), byteArrayOf(0xff.toByte(), 0xd7.toByte(), 0x00.toByte()), byteArrayOf(0xff.toByte(), 0xd7.toByte(), 0x5f.toByte()), byteArrayOf(0xff.toByte(), 0xd7.toByte(), 0x87.toByte()), byteArrayOf(0xff.toByte(), 0xd7.toByte(), 0xaf.toByte()), byteArrayOf(0xff.toByte(), 0xd7.toByte(), 0xd7.toByte()), byteArrayOf(0xff.toByte(), 0xd7.toByte(), 0xff.toByte()), byteArrayOf(0xff.toByte(), 0xff.toByte(), 0x00.toByte()), byteArrayOf(0xff.toByte(), 0xff.toByte(), 0x5f.toByte()), byteArrayOf(0xff.toByte(), 0xff.toByte(), 0x87.toByte()), byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xaf.toByte()), byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xd7.toByte()), byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte()),

				//Grey-scale ramp from 232
				byteArrayOf(0x08.toByte(), 0x08.toByte(), 0x08.toByte()), byteArrayOf(0x12.toByte(), 0x12.toByte(), 0x12.toByte()), byteArrayOf(0x1c.toByte(), 0x1c.toByte(), 0x1c.toByte()), byteArrayOf(0x26.toByte(), 0x26.toByte(), 0x26.toByte()), byteArrayOf(0x30.toByte(), 0x30.toByte(), 0x30.toByte()), byteArrayOf(0x3a.toByte(), 0x3a.toByte(), 0x3a.toByte()), byteArrayOf(0x44.toByte(), 0x44.toByte(), 0x44.toByte()), byteArrayOf(0x4e.toByte(), 0x4e.toByte(), 0x4e.toByte()), byteArrayOf(0x58.toByte(), 0x58.toByte(), 0x58.toByte()), byteArrayOf(0x62.toByte(), 0x62.toByte(), 0x62.toByte()), byteArrayOf(0x6c.toByte(), 0x6c.toByte(), 0x6c.toByte()), byteArrayOf(0x76.toByte(), 0x76.toByte(), 0x76.toByte()), byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x80.toByte()), byteArrayOf(0x8a.toByte(), 0x8a.toByte(), 0x8a.toByte()), byteArrayOf(0x94.toByte(), 0x94.toByte(), 0x94.toByte()), byteArrayOf(0x9e.toByte(), 0x9e.toByte(), 0x9e.toByte()), byteArrayOf(0xa8.toByte(), 0xa8.toByte(), 0xa8.toByte()), byteArrayOf(0xb2.toByte(), 0xb2.toByte(), 0xb2.toByte()), byteArrayOf(0xbc.toByte(), 0xbc.toByte(), 0xbc.toByte()), byteArrayOf(0xc6.toByte(), 0xc6.toByte(), 0xc6.toByte()), byteArrayOf(0xd0.toByte(), 0xd0.toByte(), 0xd0.toByte()), byteArrayOf(0xda.toByte(), 0xda.toByte(), 0xda.toByte()), byteArrayOf(0xe4.toByte(), 0xe4.toByte(), 0xe4.toByte()), byteArrayOf(0xee.toByte(), 0xee.toByte(), 0xee.toByte()))

			/**
			 * Picks out a color approximated from the supplied RGB components
			 * @param red Red intensity, from 0 to 255
			 * @param green Red intensity, from 0 to 255
			 * @param blue Red intensity, from 0 to 255
			 * @return Nearest color from the 6x6x6 RGB color cube or from the 24 entries grey-scale ramp (whichever is closest)
			 */
			fun fromRGB(red: Int, green: Int, blue: Int): Indexed {
				if (red < 0 || red > 255) {
					throw IllegalArgumentException("fromRGB: red is outside of valid range (0-255)")
				}
				if (green < 0 || green > 255) {
					throw IllegalArgumentException("fromRGB: green is outside of valid range (0-255)")
				}
				if (blue < 0 || blue > 255) {
					throw IllegalArgumentException("fromRGB: blue is outside of valid range (0-255)")
				}

				val rescaledRed = (red.toDouble() / 255.0 * 5.0).toInt()
				val rescaledGreen = (green.toDouble() / 255.0 * 5.0).toInt()
				val rescaledBlue = (blue.toDouble() / 255.0 * 5.0).toInt()

				val index = rescaledBlue + 6 * rescaledGreen + 36 * rescaledRed + 16
				val fromColorCube = Indexed(index)
				val fromGreyRamp = fromGreyRamp((red + green + blue) / 3)

				//Now figure out which one is closest
				val colored = fromColorCube.toColor()
				val grey = fromGreyRamp.toColor()
				val coloredDistance = (red - colored.red) * (red - colored.red) +
					(green - colored.green) * (green - colored.green) +
					(blue - colored.blue) * (blue - colored.blue)
				val greyDistance = (red - grey.red) * (red - grey.red) +
					(green - grey.green) * (green - grey.green) +
					(blue - grey.blue) * (blue - grey.blue)
				return if (coloredDistance < greyDistance) {
					fromColorCube
				} else {
					fromGreyRamp
				}
			}

			/**
			 * Picks out a color from the grey-scale ramp area of the color index.
			 * @param intensity Intensity, 0 - 255
			 * @return Indexed color from the grey-scale ramp which is the best match for the supplied intensity
			 */
			private fun fromGreyRamp(intensity: Int): Indexed {
				val rescaled = (intensity.toDouble() / 255.0 * 23.0).toInt() + 232
				return Indexed(rescaled)
			}
		}
	}

	/**
	 * This class can be used to specify a color in 24-bit color space (RGB with 8-bit resolution per color). Please be
	 * aware that only a few terminal support 24-bit color control codes, please avoid using this class unless you know
	 * all users will have compatible terminals. For details, please see
	 * [
 * this](https://github.com/robertknight/konsole/blob/master/user-doc/README.moreColors) commit log. Behavior on terminals that don't support these codes is undefined.
	 */
	class RGB
	/**
	 * This class can be used to specify a color in 24-bit color space (RGB with 8-bit resolution per color). Please be
	 * aware that only a few terminal support 24-bit color control codes, please avoid using this class unless you know
	 * all users will have compatible terminals. For details, please see
	 * [
 * this](https://github.com/robertknight/konsole/blob/master/user-doc/README.moreColors) commit log. Behavior on terminals that don't support these codes is undefined.
	 *
	 * @param r Red intensity, from 0 to 255
	 * @param g Green intensity, from 0 to 255
	 * @param b Blue intensity, from 0 to 255
	 */
	(r: Int, g: Int, b: Int) : TextColor {
		private val color: Color

		override val foregroundSGRSequence: ByteArray
			get() = "38;2;$red;$green;$blue".toByteArray()

		override val backgroundSGRSequence: ByteArray
			get() = "48;2;$red;$green;$blue".toByteArray()

		/**
		 * @return Red intensity of this color, from 0 to 255
		 */
		val red: Int
			get() = color.red

		/**
		 * @return Green intensity of this color, from 0 to 255
		 */
		val green: Int
			get() = color.green

		/**
		 * @return Blue intensity of this color, from 0 to 255
		 */
		val blue: Int
			get() = color.blue

		init {
			if (r < 0 || r > 255) {
				throw IllegalArgumentException("RGB: r is outside of valid range (0-255)")
			}
			if (g < 0 || g > 255) {
				throw IllegalArgumentException("RGB: g is outside of valid range (0-255)")
			}
			if (b < 0 || b > 255) {
				throw IllegalArgumentException("RGB: b is outside of valid range (0-255)")
			}
			this.color = Color(r, g, b)
		}

		override fun toColor(): Color {
			return color
		}

		override fun toString(): String {
			return "{RGB:$red,$green,$blue}"
		}

		override fun hashCode(): Int {
			var hash = 7
			hash = 29 * hash + color.hashCode()
			return hash
		}

		override fun equals(obj: Any?): Boolean {
			if (obj == null) {
				return false
			}
			if (javaClass != obj.javaClass) {
				return false
			}
			val other = obj as RGB?
			return color == other!!.color
		}
	}

	/**
	 * Utility class to instantiate colors from other types and definitions
	 */
	object Factory {
		private val INDEXED_COLOR = Pattern.compile("#[0-9]{1,3}")
		private val RGB_COLOR = Pattern.compile("#[0-9a-fA-F]{6}")

		/**
		 * Parses a string into a color. The string can have one of three formats:
		 *
		 *  * *blue* - Constant value from the [ANSI] enum
		 *  * *#17* - Hash character followed by one to three numbers; picks the color with that index from
		 * the 256 color palette
		 *  * *#1a1a1a* - Hash character followed by three hex-decimal tuples; creates an RGB color entry by
		 * parsing the tuples as Red, Green and Blue
		 *
		 * @param value The string value to parse
		 * @return A [TextColor] that is either an [ANSI], an [Indexed] or an [RGB] depending on
		 * the format of the string, or `null` if `value` is `null`.
		 */
		fun fromString(value: String?): TextColor? {
			var value: String? = value ?: return null
			value = value!!.trim { it <= ' ' }
			if (RGB_COLOR.matcher(value).matches()) {
				val r = Integer.parseInt(value.substring(1, 3), 16)
				val g = Integer.parseInt(value.substring(3, 5), 16)
				val b = Integer.parseInt(value.substring(5, 7), 16)
				return TextColor.RGB(r, g, b)
			} else if (INDEXED_COLOR.matcher(value).matches()) {
				val index = Integer.parseInt(value.substring(1))
				return TextColor.Indexed(index)
			}
			try {
				return TextColor.ANSI.valueOf(value.toUpperCase())
			} catch (e: IllegalArgumentException) {
				throw IllegalArgumentException("Unknown color definition \"" + value + "\"", e)
			}

		}
	}
}
