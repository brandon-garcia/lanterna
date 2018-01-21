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

import com.googlecode.lanterna.TerminalSize

import java.io.*
import java.nio.charset.Charset
import java.util.ArrayList
import java.util.Arrays
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * This class extends UnixLikeTerminal and implements the Cygwin-specific implementations. This means, running a Java
 * application using Lanterna inside the Cygwin Terminal application. The standard Windows command prompt (cmd.exe) is
 * not supported by this class.
 *
 *
 * **NOTE:** This class is experimental and does not fully work! Some of the operations, like disabling echo and
 * changing cbreak seems to be impossible to do without resorting to native code. Running "stty raw" before starting the
 * JVM will improve compatibility.
 *
 * @author Martin
 * @author Andreas
 */
class CygwinTerminal
/**
 * Creates a new CygwinTerminal based off input and output streams and a character set to use
 * @param terminalInput Input stream to read input from
 * @param terminalOutput Output stream to write output to
 * @param terminalCharset Character set to use when writing to the output stream
 * @throws IOException If there was an I/O error when trying to initialize the class and setup the terminal
 */
@Throws(IOException::class)
constructor(
	terminalInput: InputStream,
	terminalOutput: OutputStream,
	terminalCharset: Charset) : UnixLikeTTYTerminal(null, terminalInput, terminalOutput, terminalCharset, UnixLikeTerminal.CtrlCBehaviour.TRAP) {

	private//This will only work if you only have one terminal window open, otherwise we'll need to figure out somehow
		//which pty to use, which could be very tricky...
	val pseudoTerminalDevice: String
		get() = "/dev/pty0"

	@Throws(IOException::class)
	override fun findTerminalSize(): TerminalSize {
		try {
			val stty = runSTTYCommand("-a")
			val matcher = STTY_SIZE_PATTERN.matcher(stty)
			return if (matcher.matches()) {
				TerminalSize(Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(1)))
			} else {
				TerminalSize(80, 24)
			}
		} catch (e: Throwable) {
			return TerminalSize(80, 24)
		}

	}

	@Throws(IOException::class)
	override fun runSTTYCommand(vararg parameters: String): String {
		val commandLine = ArrayList(Arrays.asList(
			findSTTY(),
			"-F",
			pseudoTerminalDevice))
		commandLine.addAll(Arrays.asList(*parameters))
		return exec(*commandLine.toTypedArray<String>())
	}

	private fun findSTTY() =
		STTY_LOCATION

	companion object {

		private val STTY_LOCATION = findProgram("stty.exe")
		private val STTY_SIZE_PATTERN = Pattern.compile(".*rows ([0-9]+);.*columns ([0-9]+);.*")

		private fun findProgram(programName: String): String {
			val paths = System.getProperty("java.library.path").split(";".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
			for (path in paths) {
				val shBin = File(path, programName)
				if (shBin.exists()) {
					return shBin.getAbsolutePath()
				}
			}
			return programName
		}
	}
}
