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

import com.googlecode.lanterna.Symbols
import com.googlecode.lanterna.TextCharacter

import java.awt.*
import java.awt.font.FontRenderContext
import java.awt.geom.Rectangle2D
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.*

/**
 * This class encapsulates the font information used by an [AWTTerminal]. By customizing this class, you can
 * choose which fonts are going to be used by an [AWTTerminal] component and some other related settings.
 * @author martin
 */
open class AWTTerminalFontConfiguration protected constructor(
	/**
	 * Returns `true` if anti-aliasing has been enabled, `false` otherwise
	 * @return `true` if anti-aliasing has been enabled, `false` otherwise
	 */
	internal val isAntiAliased: Boolean, private val boldMode: BoldMode, vararg fontsInOrderOfPriority: Font) {

	private val fontPriority: MutableList<Font>
	/**
	 * Returns the horizontal size in pixels of the fonts configured
	 * @return Horizontal size in pixels of the fonts configured
	 */
	internal val fontWidth: Int
	/**
	 * Returns the vertical size in pixels of the fonts configured
	 * @return Vertical size in pixels of the fonts configured
	 */
	internal val fontHeight: Int

	private val fontRenderContext: FontRenderContext
		get() = FontRenderContext(null,
			if (isAntiAliased)
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON
			else
				RenderingHints.VALUE_TEXT_ANTIALIAS_OFF,
			RenderingHints.VALUE_FRACTIONALMETRICS_DEFAULT)

	/**
	 * Controls how the SGR bold will take effect when enabled on a character. Mainly this is controlling if the
	 * character should be rendered with a bold font or not. The reason for this is that some characters, notably the
	 * lines and double-lines in defined in Symbol, usually doesn't look very good with bold font when you try to
	 * construct a GUI.
	 */
	enum class BoldMode {
		/**
		 * All characters with SGR Bold enabled will be rendered using a bold font
		 */
		EVERYTHING,
		/**
		 * All characters with SGR Bold enabled, except for the characters defined as constants in Symbols class, will
		 * be rendered using a bold font
		 */
		EVERYTHING_BUT_SYMBOLS,
		/**
		 * Bold font will not be used for characters with SGR bold enabled
		 */
		NOTHING
	}

	init {
		if (fontsInOrderOfPriority == null || fontsInOrderOfPriority.size == 0) {
			throw IllegalArgumentException("Must pass in a valid list of fonts to SwingTerminalFontConfiguration")
		}
		this.fontPriority = ArrayList(Arrays.asList(*fontsInOrderOfPriority))
		this.fontWidth = getFontWidth(fontPriority[0])
		this.fontHeight = getFontHeight(fontPriority[0])

		//Make sure all the fonts are monospace
		for (font in fontPriority) {
			if (!isFontMonospaced(font)) {
				throw IllegalArgumentException("Font $font isn't monospaced!")
			}
		}

		//Make sure all lower-priority fonts are less or equal in width and height, shrink if necessary
		for (i in 1 until fontPriority.size) {
			var font = fontPriority[i]
			while (getFontWidth(font) > fontWidth || getFontHeight(font) > fontHeight) {
				val newSize = font.size2D - 0.5f
				if (newSize < 0.01) {
					throw IllegalStateException("Unable to shrink font " + (i + 1) + " to fit the size of highest priority font " + fontPriority[0])
				}
				font = font.deriveFont(newSize)
				fontPriority[i] = font
			}
		}
	}

	/**
	 * Given a certain character, return the font to use for drawing it. The method will go through all fonts passed in
	 * to this [AWTTerminalFontConfiguration] in the order of priority specified and chose the first font which is
	 * capable of drawing `character`. If no such font is found, the normal fonts is returned (and probably won't
	 * be able to draw the character).
	 * @param character Character to find a font for
	 * @return Font which the `character` should be drawn using
	 */
	internal fun getFontForCharacter(character: TextCharacter): Font {
		var normalFont = getFontForCharacter(character.character)
		if (boldMode == BoldMode.EVERYTHING || boldMode == BoldMode.EVERYTHING_BUT_SYMBOLS && isNotASymbol(character.character)) {
			if (character.isBold) {
				normalFont = normalFont.deriveFont(Font.BOLD)
			}
		}
		if (character.isItalic) {
			normalFont = normalFont.deriveFont(Font.ITALIC)
		}
		return normalFont
	}

	private fun getFontForCharacter(c: Char): Font {
		for (font in fontPriority) {
			if (font.canDisplay(c)) {
				return font
			}
		}
		//No available font here, what to do...?
		return fontPriority[0]
	}

	private fun getFontWidth(font: Font) =
		font.getStringBounds("W", fontRenderContext).width.toInt()

	private fun getFontHeight(font: Font) =
		font.getStringBounds("W", fontRenderContext).height.toInt()

	private fun isNotASymbol(character: Char) =
		!SYMBOLS_CACHE.contains(character)

	companion object {

		private val MONOSPACE_CHECK_OVERRIDE = Collections.unmodifiableSet(HashSet(Arrays.asList(
			"VL Gothic Regular",
			"NanumGothic",
			"WenQuanYi Zen Hei Mono",
			"WenQuanYi Zen Hei",
			"AR PL UMing TW",
			"AR PL UMing HK",
			"AR PL UMing CN"
		)))

		private//Monospaced can look pretty bad on Windows, so let's override it
		val defaultWindowsFonts: List<Font>
			get() = Collections.unmodifiableList(Arrays.asList(
				Font("Courier New", Font.PLAIN, fontSize),
				Font("Monospaced", Font.PLAIN, fontSize)))

		private//Below, these should be redundant (Monospaced is supposed to catch-all)
			// but Java 6 seems to have issues with finding monospaced fonts sometimes
		val defaultLinuxFonts: List<Font>
			get() = Collections.unmodifiableList(Arrays.asList(
				Font("DejaVu Sans Mono", Font.PLAIN, fontSize),
				Font("Monospaced", Font.PLAIN, fontSize),
				Font("Ubuntu Mono", Font.PLAIN, fontSize),
				Font("FreeMono", Font.PLAIN, fontSize),
				Font("Liberation Mono", Font.PLAIN, fontSize),
				Font("VL Gothic Regular", Font.PLAIN, fontSize),
				Font("NanumGothic", Font.PLAIN, fontSize),
				Font("WenQuanYi Zen Hei Mono", Font.PLAIN, fontSize),
				Font("WenQuanYi Zen Hei", Font.PLAIN, fontSize),
				Font("AR PL UMing TW", Font.PLAIN, fontSize),
				Font("AR PL UMing HK", Font.PLAIN, fontSize),
				Font("AR PL UMing CN", Font.PLAIN, fontSize)))

		private val defaultFonts: List<Font>
			get() = Collections.unmodifiableList(listOf<Font>(Font("Monospaced", Font.PLAIN, fontSize)))

		// Here we check the screen resolution on the primary monitor and make a guess at if it's high-DPI or not
		private var CHOSEN_FONT_SIZE: Int? = null
		private// Source: http://stackoverflow.com/questions/3680221/how-can-i-get-the-monitor-size-in-java
			// Assume the first GraphicsDevice is the primary screen (this isn't always correct but what to do?)
			// Warning, there could be printers coming back here according to JavaDoc! Hopefully Java is reasonable and
			// passes them in after the real monitor(s).
			// If the width is wider than Full HD (1080p, or 1920x1080), then assume it's high-DPI
			// If no size was picked, default to 14
		val fontSize: Int
			@Synchronized get() {
				if (CHOSEN_FONT_SIZE != null) {
					return CHOSEN_FONT_SIZE!!
				}
				val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
				val gs = ge.screenDevices
				if (gs.size > 0) {
					val primaryMonitorWidth = gs[0].displayMode.width
					if (primaryMonitorWidth > 2000) {
						CHOSEN_FONT_SIZE = 28
					}
				}
				if (CHOSEN_FONT_SIZE == null) {
					CHOSEN_FONT_SIZE = 14
				}
				return CHOSEN_FONT_SIZE!!
			}

		/**
		 * Returns the default font to use depending on the platform
		 * @return Default font to use, system-dependent
		 */
		protected fun selectDefaultFont() =
			System.getProperty("os.name", "").toLowerCase().let {
				if (it.contains("win")) {
					defaultWindowsFonts.toTypedArray<Font>()
				} else if (it.contains("linux")) {
					defaultLinuxFonts.toTypedArray<Font>()
				} else {
					defaultFonts.toTypedArray<Font>()
				}
			}

		/**
		 * This is the default font settings that will be used if you don't specify anything
		 * @return An [AWTTerminal] font configuration object with default values set up
		 */
		val default: AWTTerminalFontConfiguration
			get() = newInstance(*filterMonospaced(*selectDefaultFont()))

		/**
		 * Given an array of fonts, returns another array with only the ones that are monospaced. The fonts in the result
		 * will have the same order as in which they came in. A font is considered monospaced if the width of 'i' and 'W' is
		 * the same.
		 * @param fonts Fonts to filter monospaced fonts from
		 * @return Array with the fonts from the input parameter that were monospaced
		 */
		fun filterMonospaced(vararg fonts: Font): Array<Font> {
			val result = ArrayList<Font>(fonts.size)
			for (font in fonts) {
				if (isFontMonospaced(font)) {
					result.add(font)
				}
			}
			return result.toTypedArray<Font>()
		}

		/**
		 * Creates a new font configuration from a list of fonts in order of priority. This works by having the terminal
		 * attempt to draw each character with the fonts in the order they are specified in and stop once we find a font
		 * that can actually draw the character. For ASCII characters, it's very likely that the first font will always be
		 * used.
		 * @param fontsInOrderOfPriority Fonts to use when drawing text, in order of priority
		 * @return Font configuration built from the font list
		 */
		fun newInstance(vararg fontsInOrderOfPriority: Font) =
			AWTTerminalFontConfiguration(true, BoldMode.EVERYTHING_BUT_SYMBOLS, *fontsInOrderOfPriority)

		private fun isFontMonospaced(font: Font): Boolean {
			if (MONOSPACE_CHECK_OVERRIDE.contains(font.name)) {
				return true
			}
			val frc = FontRenderContext(null,
				RenderingHints.VALUE_TEXT_ANTIALIAS_OFF,
				RenderingHints.VALUE_FRACTIONALMETRICS_DEFAULT)
			val iBounds = font.getStringBounds("i", frc)
			val mBounds = font.getStringBounds("W", frc)
			return iBounds.width == mBounds.width
		}


		private val SYMBOLS_CACHE = HashSet<Char>()

		init {
			for (field in Symbols::class.java!!.getFields()) {
				if (field.getType() == Char::class.javaPrimitiveType &&
					field.getModifiers() and Modifier.FINAL != 0 &&
					field.getModifiers() and Modifier.STATIC != 0) {
					try {
						SYMBOLS_CACHE.add(field.getChar(null))
					} catch (ignore: IllegalArgumentException) {
						//Should never happen!
					} catch (ignore: IllegalAccessException) {
						//Should never happen!
					}

				}
			}
		}
	}
}
