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

import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.nio.charset.Charset

import com.googlecode.lanterna.Symbols
import com.googlecode.lanterna.TerminalTextUtils
import com.googlecode.lanterna.input.InputDecoder
import com.googlecode.lanterna.input.KeyDecodingProfile
import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.input.ScreenInfoAction
import com.googlecode.lanterna.input.ScreenInfoCharacterPattern
import com.googlecode.lanterna.terminal.AbstractTerminal
import com.googlecode.lanterna.TerminalPosition

import java.io.ByteArrayOutputStream
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

/**
 * An abstract terminal implementing functionality for terminals using OutputStream/InputStream. You can extend from
 * this class if your terminal implementation is using standard input and standard output but not ANSI escape codes (in
 * which case you should extend ANSITerminal). This class also contains some automatic UTF-8 to VT100 character
 * conversion when the terminal is not set to read UTF-8.
 *
 * @author Martin
 */
abstract class StreamBasedTerminal(private val terminalInput: InputStream, private val terminalOutput: OutputStream, terminalCharset: Charset?) : AbstractTerminal() {
	protected val charset: Charset

	/**
	 * Returns the `InputDecoder` attached to this `StreamBasedTerminal`. Can be used to add additional
	 * character patterns to recognize and tune the way input is turned in `KeyStroke`:s.
	 * @return `InputDecoder` attached to this `StreamBasedTerminal`
	 */
	val inputDecoder: InputDecoder
	private val keyQueue: Queue<KeyStroke>
	private val readLock: Lock

	@Volatile private var lastReportedCursorPosition: TerminalPosition? = null

	init {
		if (terminalCharset == null) {
			this.charset = Charset.defaultCharset()
		} else {
			this.charset = terminalCharset
		}
		this.inputDecoder = InputDecoder(InputStreamReader(this.terminalInput, this.charset))
		this.keyQueue = LinkedList()
		this.readLock = ReentrantLock()
		this.lastReportedCursorPosition = null

	}

	/**
	 * {@inheritDoc}
	 *
	 * The `StreamBasedTerminal` class will attempt to translate some unicode characters to VT100 if the encoding
	 * attached to this `Terminal` isn't UTF-8.
	 */
	@Throws(IOException::class)
	override fun putCharacter(c: Char) {
		if (TerminalTextUtils.isPrintableCharacter(c)) {
			writeToTerminal(*translateCharacter(c))
		}
	}

	/**
	 * This method will write a list of bytes directly to the output stream of the terminal.
	 * @param bytes Bytes to write to the terminal (synchronized)
	 * @throws java.io.IOException If there was an underlying I/O error
	 */
	@Throws(IOException::class)
	protected fun writeToTerminal(vararg bytes: Byte) {
		synchronized(terminalOutput) {
			terminalOutput.write(bytes)
		}
	}

	@Throws(IOException::class)
	override fun enquireTerminal(timeout: Int, timeoutTimeUnit: TimeUnit): ByteArray {
		synchronized(terminalOutput) {
			terminalOutput.write(5)    //ENQ
			flush()
		}

		//Wait for input
		val startTime = System.currentTimeMillis()
		while (terminalInput.available() == 0) {
			if (System.currentTimeMillis() - startTime > timeoutTimeUnit.toMillis(timeout.toLong())) {
				return ByteArray(0)
			}
			try {
				Thread.sleep(1)
			} catch (e: InterruptedException) {
				return ByteArray(0)
			}

		}

		//We have at least one character, read as far as we can and return
		val buffer = ByteArrayOutputStream()
		while (terminalInput.available() > 0) {
			buffer.write(terminalInput.read())
		}
		return buffer.toByteArray()
	}

	@Throws(IOException::class)
	override fun bell() {
		terminalOutput.write(7.toByte().toInt())
		terminalOutput.flush()
	}

	/**
	 * Used by the cursor reporting methods to reset any previous position memorized, so we're guaranteed to return the
	 * next reported position
	 */
	internal fun resetMemorizedCursorPosition() {
		lastReportedCursorPosition = null
	}

	/**
	 * Waits for up to 5 seconds for a terminal cursor position report to appear in the input stream. If the timeout
	 * expires, it will return null. You should have sent the cursor position query already before
	 * calling this method.
	 * @return Current position of the cursor, or null if the terminal didn't report it in time.
	 * @throws IOException If there was an I/O error
	 */
	@Synchronized
	@Throws(IOException::class)
	internal fun waitForCursorPositionReport(): TerminalPosition? {
		val startTime = System.currentTimeMillis()
		var cursorPosition = lastReportedCursorPosition
		while (cursorPosition == null) {
			if (System.currentTimeMillis() - startTime > 5000) {
				//throw new IllegalStateException("Terminal didn't send any position report for 5 seconds, please file a bug with a reproduce!");
				return null
			}
			val keyStroke = readInput(false, false)
			if (keyStroke != null) {
				keyQueue.add(keyStroke)
			} else {
				try {
					Thread.sleep(1)
				} catch (ignored: InterruptedException) {
				}

			}
			cursorPosition = lastReportedCursorPosition
		}
		return cursorPosition
	}

	@Throws(IOException::class)
	override fun pollInput() =
		readInput(false, true)

	@Throws(IOException::class)
	override fun readInput() =
		readInput(true, true)

	@Throws(IOException::class)
	private fun readInput(blocking: Boolean, useKeyQueue: Boolean): KeyStroke? {
		while (true) {
			if (useKeyQueue) {
				val previouslyReadKey = keyQueue.poll()
				if (previouslyReadKey != null) {
					return previouslyReadKey
				}
			}
			if (blocking) {
				readLock.lock()
			} else {
				// If we are in non-blocking readInput(), don't wait for the lock, just return null right away
				if (!readLock.tryLock()) {
					return null
				}
			}
			try {
				val key = inputDecoder.getNextCharacter(blocking)
				val report = ScreenInfoCharacterPattern.tryToAdopt(key)
				if (lastReportedCursorPosition == null && report != null) {
					lastReportedCursorPosition = report.position
				} else {
					return key
				}
			} finally {
				readLock.unlock()
			}
		}
	}

	@Throws(IOException::class)
	override fun flush() {
		synchronized(terminalOutput) {
			terminalOutput.flush()
		}
	}

	@Throws(IOException::class)
	override fun close() {
		// Should we close the input/output streams here?
		// If someone uses lanterna just temporarily and want to switch back to using System.out/System.in manually,
		// they won't be too happy if we closed the streams
	}

	protected fun translateCharacter(input: Char) =
		if (UTF8_REFERENCE != null && UTF8_REFERENCE === charset) {
			convertToCharset(input)
		} else {
			//Convert ACS to ordinary terminal codes
			when (input) {
				Symbols.ARROW_DOWN -> convertToVT100('v')
				Symbols.ARROW_LEFT -> convertToVT100('<')
				Symbols.ARROW_RIGHT -> convertToVT100('>')
				Symbols.ARROW_UP -> convertToVT100('^')
				Symbols.BLOCK_DENSE, Symbols.BLOCK_MIDDLE, Symbols.BLOCK_SOLID, Symbols.BLOCK_SPARSE -> convertToVT100(97.toChar())
				Symbols.HEART, Symbols.CLUB, Symbols.SPADES -> convertToVT100('?')
				Symbols.FACE_BLACK, Symbols.FACE_WHITE, Symbols.DIAMOND -> convertToVT100(96.toChar())
				Symbols.BULLET -> convertToVT100(102.toChar())
				Symbols.DOUBLE_LINE_CROSS, Symbols.SINGLE_LINE_CROSS -> convertToVT100(110.toChar())
				Symbols.DOUBLE_LINE_HORIZONTAL, Symbols.SINGLE_LINE_HORIZONTAL -> convertToVT100(113.toChar())
				Symbols.DOUBLE_LINE_BOTTOM_LEFT_CORNER, Symbols.SINGLE_LINE_BOTTOM_LEFT_CORNER -> convertToVT100(109.toChar())
				Symbols.DOUBLE_LINE_BOTTOM_RIGHT_CORNER, Symbols.SINGLE_LINE_BOTTOM_RIGHT_CORNER -> convertToVT100(106.toChar())
				Symbols.DOUBLE_LINE_T_DOWN, Symbols.SINGLE_LINE_T_DOWN, Symbols.DOUBLE_LINE_T_SINGLE_DOWN, Symbols.SINGLE_LINE_T_DOUBLE_DOWN -> convertToVT100(119.toChar())
				Symbols.DOUBLE_LINE_T_LEFT, Symbols.SINGLE_LINE_T_LEFT, Symbols.DOUBLE_LINE_T_SINGLE_LEFT, Symbols.SINGLE_LINE_T_DOUBLE_LEFT -> convertToVT100(117.toChar())
				Symbols.DOUBLE_LINE_T_RIGHT, Symbols.SINGLE_LINE_T_RIGHT, Symbols.DOUBLE_LINE_T_SINGLE_RIGHT, Symbols.SINGLE_LINE_T_DOUBLE_RIGHT -> convertToVT100(116.toChar())
				Symbols.DOUBLE_LINE_T_UP, Symbols.SINGLE_LINE_T_UP, Symbols.DOUBLE_LINE_T_SINGLE_UP, Symbols.SINGLE_LINE_T_DOUBLE_UP -> convertToVT100(118.toChar())
				Symbols.DOUBLE_LINE_TOP_LEFT_CORNER, Symbols.SINGLE_LINE_TOP_LEFT_CORNER -> convertToVT100(108.toChar())
				Symbols.DOUBLE_LINE_TOP_RIGHT_CORNER, Symbols.SINGLE_LINE_TOP_RIGHT_CORNER -> convertToVT100(107.toChar())
				Symbols.DOUBLE_LINE_VERTICAL, Symbols.SINGLE_LINE_VERTICAL -> convertToVT100(120.toChar())
				else -> convertToCharset(input)
			}
		}

	private fun convertToVT100(code: Char) =
		//Warning! This might be terminal type specific!!!!
		//So far it's worked everywhere I've tried it (xterm, gnome-terminal, putty)
		byteArrayOf(27, 40, 48, code.toByte(), 27, 40, 66)

	private fun convertToCharset(input: Char) =
		charset.encode(Character.toString(input)).array()

	companion object {

		private val UTF8_REFERENCE = Charset.forName("UTF-8")
	}
}
