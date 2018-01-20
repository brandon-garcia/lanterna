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
package com.googlecode.lanterna.input

import java.util.ArrayList
import java.util.Arrays

/**
 * This profile attempts to collect as many code combinations as possible without causing any collisions between
 * patterns. The patterns in here are tested with Linux terminal, XTerm, Gnome terminal, XFCE terminal, Cygwin and
 * Mac OS X terminal.
 *
 * @author Martin
 */
class DefaultKeyDecodingProfile : KeyDecodingProfile {

	override val patterns: Collection<CharacterPattern>
		get() = ArrayList(COMMON_PATTERNS)

	companion object {

		private val COMMON_PATTERNS = ArrayList(Arrays.asList(
			*arrayOf(BasicCharacterPattern(KeyStroke(KeyType.Escape), KeyDecodingProfile.ESC_CODE), BasicCharacterPattern(KeyStroke(KeyType.Tab), '\t'), BasicCharacterPattern(KeyStroke(KeyType.Enter), '\n'), BasicCharacterPattern(KeyStroke(KeyType.Enter), '\r', '\u0000'), //OS X
				BasicCharacterPattern(KeyStroke(KeyType.Backspace), 0x7f.toChar()), BasicCharacterPattern(KeyStroke(KeyType.Backspace), 0x08.toChar()), BasicCharacterPattern(KeyStroke(KeyType.F1), KeyDecodingProfile.ESC_CODE, '[', '[', 'A'), //Linux
				BasicCharacterPattern(KeyStroke(KeyType.F2), KeyDecodingProfile.ESC_CODE, '[', '[', 'B'), //Linux
				BasicCharacterPattern(KeyStroke(KeyType.F3), KeyDecodingProfile.ESC_CODE, '[', '[', 'C'), //Linux
				BasicCharacterPattern(KeyStroke(KeyType.F4), KeyDecodingProfile.ESC_CODE, '[', '[', 'D'), //Linux
				BasicCharacterPattern(KeyStroke(KeyType.F5), KeyDecodingProfile.ESC_CODE, '[', '[', 'E'), //Linux

				EscapeSequenceCharacterPattern(), NormalCharacterPattern(), AltAndCharacterPattern(), CtrlAndCharacterPattern(), CtrlAltAndCharacterPattern(), ScreenInfoCharacterPattern(), MouseCharacterPattern())))
	}

}
