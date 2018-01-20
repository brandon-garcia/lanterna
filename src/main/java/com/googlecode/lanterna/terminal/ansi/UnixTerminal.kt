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
package com.googlecode.lanterna.terminal.ansi


import java.io.*
import java.nio.charset.Charset

import com.googlecode.lanterna.TerminalSize

/**
 * This class extends UnixLikeTerminal and implements the Unix-specific parts.
 *
 *
 * If you need to have Lanterna to call stty at a different location, you'll need to
 * subclass this and override `getSTTYCommand()`.
 *
 * @author Martin
 */
class UnixTerminal @Throws(IOException::class)
private constructor(
	terminalDevice: File,
	terminalInput: InputStream,
	terminalOutput: OutputStream,
	terminalCharset: Charset,
	terminalCtrlCBehaviour: UnixLikeTerminal.CtrlCBehaviour) : UnixLikeTTYTerminal(terminalDevice, terminalInput, terminalOutput, terminalCharset, terminalCtrlCBehaviour) {

	/**
	 * Creates a UnixTerminal using a specified input stream, output stream and character set. Ctrl+c signal will be
	 * trapped instead of killing the application.
	 *
	 * @param terminalInput Input stream to read terminal input from
	 * @param terminalOutput Output stream to write terminal output to
	 * @param terminalCharset Character set to use when converting characters to bytes
	 * @throws java.io.IOException If there was an I/O error initializing the terminal
	 */
	@Throws(IOException::class)
	@JvmOverloads constructor(
		terminalInput: InputStream = System.`in`,
		terminalOutput: OutputStream = System.out,
		terminalCharset: Charset = Charset.defaultCharset()) : this(terminalInput, terminalOutput, terminalCharset, UnixLikeTerminal.CtrlCBehaviour.CTRL_C_KILLS_APPLICATION) {
	}

	/**
	 * Creates a UnixTerminal using a specified input stream, output stream and character set, with a custom size
	 * querier instead of using the default one. This way you can override size detection (if you want to force the
	 * terminal to a fixed size, for example). You also choose how you want ctrl+c key strokes to be handled.
	 *
	 * @param terminalInput Input stream to read terminal input from
	 * @param terminalOutput Output stream to write terminal output to
	 * @param terminalCharset Character set to use when converting characters to bytes
	 * @param terminalCtrlCBehaviour Special settings on how the terminal will behave, see `UnixTerminalMode` for more
	 * details
	 * @throws java.io.IOException If there was an I/O error initializing the terminal
	 */
	@Throws(IOException::class)
	constructor(
		terminalInput: InputStream,
		terminalOutput: OutputStream,
		terminalCharset: Charset,
		terminalCtrlCBehaviour: UnixLikeTerminal.CtrlCBehaviour) : this(File("/dev/tty"), terminalInput, terminalOutput, terminalCharset, terminalCtrlCBehaviour) {
	}
}
/**
 * Creates a UnixTerminal with default settings, using System.in and System.out for input/output, using the default
 * character set on the system as the encoding and trap ctrl+c signal instead of killing the application.
 * @throws IOException If there was an I/O error initializing the terminal
 */
