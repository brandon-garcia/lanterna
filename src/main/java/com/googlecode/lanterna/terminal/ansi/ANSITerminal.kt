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

import com.googlecode.lanterna.SGR
import com.googlecode.lanterna.TerminalPosition
import com.googlecode.lanterna.input.*
import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.TextColor
import com.googlecode.lanterna.terminal.ExtendedTerminal
import com.googlecode.lanterna.terminal.MouseCaptureMode

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset

/**
 * Class containing graphics code for ANSI compliant text terminals and terminal emulators. All the methods inside of
 * this class uses ANSI escape codes written to the underlying output stream.
 *
 * @see [Wikipedia](http://en.wikipedia.org/wiki/ANSI_escape_code)
 *
 * @author Martin
 */
abstract class ANSITerminal protected constructor(
	terminalInput: InputStream,
	terminalOutput: OutputStream,
	terminalCharset: Charset) : StreamBasedTerminal(terminalInput, terminalOutput, terminalCharset), ExtendedTerminal {

	private var requestedMouseCaptureMode: MouseCaptureMode? = null
	private var mouseCaptureMode: MouseCaptureMode? = null
	/**
	 * Method to test if the terminal (as far as the library knows) is in private mode.
	 *
	 * @return True if there has been a call to enterPrivateMode() but not yet exitPrivateMode()
	 */
	internal var isInPrivateMode: Boolean = false
		private set

	/**
	 * This method can be overridden in a custom terminal implementation to change the default key decoders.
	 * @return The KeyDecodingProfile used by the terminal when translating character sequences to keystrokes
	 */
	protected val defaultKeyDecodingProfile: KeyDecodingProfile
		get() = DefaultKeyDecodingProfile()

	// Final because we handle the onResized logic here; extending classes should override #findTerminalSize instead
	override val terminalSize: TerminalSize
		@Synchronized @Throws(IOException::class)
		get() {
			val size = findTerminalSize()
			onResized(size)
			return size
		}

	override// ANSI terminal positions are 1-indexed so top-left corner is 1x1 instead of 0x0, that's why we need to adjust it here
	var cursorPosition: TerminalPosition
		@Synchronized @Throws(IOException::class)
		get() {
			resetMemorizedCursorPosition()
			reportPosition()
			var terminalPosition = waitForCursorPositionReport()
			if (terminalPosition == null) {
				terminalPosition = TerminalPosition.OFFSET_1x1
			}
			return terminalPosition.withRelative(-1, -1)
		}
		@Throws(IOException::class)
		set(position) = setCursorPosition(position.column, position.row)

	init {
		this.isInPrivateMode = false
		this.requestedMouseCaptureMode = null
		this.mouseCaptureMode = null
		inputDecoder.addProfile(defaultKeyDecodingProfile)
	}

	@Throws(IOException::class)
	private fun writeCSISequenceToTerminal(vararg tail: Byte) {
		val completeSequence = ByteArray(tail.size + 2)
		completeSequence[0] = 0x1b.toByte()
		completeSequence[1] = '['.toByte()
		System.arraycopy(tail, 0, completeSequence, 2, tail.size)
		writeToTerminal(*completeSequence)
	}

	@Throws(IOException::class)
	private fun writeSGRSequenceToTerminal(vararg sgrParameters: Byte) {
		val completeSequence = ByteArray(sgrParameters.size + 3)
		completeSequence[0] = 0x1b.toByte()
		completeSequence[1] = '['.toByte()
		completeSequence[completeSequence.size - 1] = 'm'.toByte()
		System.arraycopy(sgrParameters, 0, completeSequence, 2, sgrParameters.size)
		writeToTerminal(*completeSequence)
	}

	@Throws(IOException::class)
	private fun writeOSCSequenceToTerminal(vararg tail: Byte) {
		val completeSequence = ByteArray(tail.size + 2)
		completeSequence[0] = 0x1b.toByte()
		completeSequence[1] = ']'.toByte()
		System.arraycopy(tail, 0, completeSequence, 2, tail.size)
		writeToTerminal(*completeSequence)
	}

	@Throws(IOException::class)
	protected open fun findTerminalSize(): TerminalSize {
		saveCursorPosition()
		setCursorPosition(5000, 5000)
		resetMemorizedCursorPosition()
		reportPosition()
		restoreCursorPosition()
		var terminalPosition = waitForCursorPositionReport()
		if (terminalPosition == null) {
			terminalPosition = TerminalPosition(80, 24)
		}
		return TerminalSize(terminalPosition.column, terminalPosition.row)
	}

	@Throws(IOException::class)
	override fun setTerminalSize(columns: Int, rows: Int) {
		writeCSISequenceToTerminal(*("8;" + rows + ";" + columns + "t").toByteArray())

		//We can't trust that the previous call was honoured by the terminal so force a re-query here, which will
		//trigger a resize event if one actually took place
		terminalSize
	}

	@Throws(IOException::class)
	override fun setTitle(title: String) {
		var title = title
		//The bell character is our 'null terminator', make sure there's none in the title
		title = title.replace("\u0007", "")
		writeOSCSequenceToTerminal(*("2;" + title + "\u0007").toByteArray())
	}

	@Throws(IOException::class)
	override fun setForegroundColor(color: TextColor) {
		writeSGRSequenceToTerminal(*color.foregroundSGRSequence)
	}

	@Throws(IOException::class)
	override fun setBackgroundColor(color: TextColor) {
		writeSGRSequenceToTerminal(*color.backgroundSGRSequence)
	}

	@Throws(IOException::class)
	override fun enableSGR(sgr: SGR) {
		when (sgr) {
			SGR.BLINK -> writeCSISequenceToTerminal('5'.toByte(), 'm'.toByte())
			SGR.BOLD -> writeCSISequenceToTerminal('1'.toByte(), 'm'.toByte())
			SGR.BORDERED -> writeCSISequenceToTerminal('5'.toByte(), '1'.toByte(), 'm'.toByte())
			SGR.CIRCLED -> writeCSISequenceToTerminal('5'.toByte(), '2'.toByte(), 'm'.toByte())
			SGR.CROSSED_OUT -> writeCSISequenceToTerminal('9'.toByte(), 'm'.toByte())
			SGR.FRAKTUR -> writeCSISequenceToTerminal('2'.toByte(), '0'.toByte(), 'm'.toByte())
			SGR.REVERSE -> writeCSISequenceToTerminal('7'.toByte(), 'm'.toByte())
			SGR.UNDERLINE -> writeCSISequenceToTerminal('4'.toByte(), 'm'.toByte())
			SGR.ITALIC -> writeCSISequenceToTerminal('3'.toByte(), 'm'.toByte())
		}
	}

	@Throws(IOException::class)
	override fun disableSGR(sgr: SGR) {
		when (sgr) {
			SGR.BLINK -> writeCSISequenceToTerminal('2'.toByte(), '5'.toByte(), 'm'.toByte())
			SGR.BOLD -> writeCSISequenceToTerminal('2'.toByte(), '2'.toByte(), 'm'.toByte())
			SGR.BORDERED -> writeCSISequenceToTerminal('5'.toByte(), '4'.toByte(), 'm'.toByte())
			SGR.CIRCLED -> writeCSISequenceToTerminal('5'.toByte(), '4'.toByte(), 'm'.toByte())
			SGR.CROSSED_OUT -> writeCSISequenceToTerminal('2'.toByte(), '9'.toByte(), 'm'.toByte())
			SGR.FRAKTUR -> writeCSISequenceToTerminal('2'.toByte(), '3'.toByte(), 'm'.toByte())
			SGR.REVERSE -> writeCSISequenceToTerminal('2'.toByte(), '7'.toByte(), 'm'.toByte())
			SGR.UNDERLINE -> writeCSISequenceToTerminal('2'.toByte(), '4'.toByte(), 'm'.toByte())
			SGR.ITALIC -> writeCSISequenceToTerminal('2'.toByte(), '3'.toByte(), 'm'.toByte())
		}
	}

	@Throws(IOException::class)
	override fun resetColorAndSGR() {
		writeCSISequenceToTerminal('0'.toByte(), 'm'.toByte())
	}

	@Throws(IOException::class)
	override fun clearScreen() {
		writeCSISequenceToTerminal('2'.toByte(), 'J'.toByte())
	}

	@Throws(IOException::class)
	override fun enterPrivateMode() {
		if (isInPrivateMode) {
			throw IllegalStateException("Cannot call enterPrivateMode() when already in private mode")
		}
		writeCSISequenceToTerminal('?'.toByte(), '1'.toByte(), '0'.toByte(), '4'.toByte(), '9'.toByte(), 'h'.toByte())
		if (requestedMouseCaptureMode != null) {
			this.mouseCaptureMode = requestedMouseCaptureMode
			updateMouseCaptureMode(this.mouseCaptureMode, 'h')
		}
		flush()
		isInPrivateMode = true
	}

	@Throws(IOException::class)
	override fun exitPrivateMode() {
		if (!isInPrivateMode) {
			throw IllegalStateException("Cannot call exitPrivateMode() when not in private mode")
		}
		resetColorAndSGR()
		setCursorVisible(true)
		writeCSISequenceToTerminal('?'.toByte(), '1'.toByte(), '0'.toByte(), '4'.toByte(), '9'.toByte(), 'l'.toByte())
		if (null != mouseCaptureMode) {
			updateMouseCaptureMode(this.mouseCaptureMode, 'l')
			this.mouseCaptureMode = null
		}
		flush()
		isInPrivateMode = false
	}

	@Throws(IOException::class)
	override fun close() {
		if (isInPrivateMode) {
			exitPrivateMode()
		}
		super.close()
	}

	@Throws(IOException::class)
	override fun setCursorPosition(x: Int, y: Int) {
		writeCSISequenceToTerminal(*((y + 1).toString() + ";" + (x + 1) + "H").toByteArray())
	}

	@Throws(IOException::class)
	override fun setCursorVisible(visible: Boolean) {
		writeCSISequenceToTerminal(*("?25" + if (visible) "h" else "l").toByteArray())
	}

	@Throws(IOException::class)
	override fun readInput(): KeyStroke {
		var keyStroke: KeyStroke?
		do {
			// KeyStroke may because null by filterMouseEvents, so that's why we have the while(true) loop here
			keyStroke = filterMouseEvents(super.readInput())
		} while (keyStroke == null)
		return keyStroke
	}

	@Throws(IOException::class)
	override fun pollInput() =
		filterMouseEvents(super.pollInput())

	private fun filterMouseEvents(keyStroke: KeyStroke?): KeyStroke? {
		//Remove bad input events from terminals that are not following the xterm protocol properly
		if (keyStroke == null || keyStroke.keyType !== KeyType.MouseEvent) {
			return keyStroke
		}

		val mouseAction = keyStroke as MouseAction?
		when (mouseAction!!.actionType) {
			MouseActionType.CLICK_RELEASE -> if (mouseCaptureMode === MouseCaptureMode.CLICK) {
				return null
			}
			MouseActionType.DRAG -> if (mouseCaptureMode === MouseCaptureMode.CLICK || mouseCaptureMode === MouseCaptureMode.CLICK_RELEASE) {
				return null
			}
			MouseActionType.MOVE -> if (mouseCaptureMode === MouseCaptureMode.CLICK ||
				mouseCaptureMode === MouseCaptureMode.CLICK_RELEASE ||
				mouseCaptureMode === MouseCaptureMode.CLICK_RELEASE_DRAG) {
				return null
			}
		}
		return mouseAction
	}

	@Throws(IOException::class)
	override fun pushTitle() {
		throw UnsupportedOperationException("Not implemented yet")
	}

	@Throws(IOException::class)
	override fun popTitle() {
		throw UnsupportedOperationException("Not implemented yet")
	}

	@Throws(IOException::class)
	override fun iconify() {
		writeCSISequenceToTerminal('2'.toByte(), 't'.toByte())
	}

	@Throws(IOException::class)
	override fun deiconify() {
		writeCSISequenceToTerminal('1'.toByte(), 't'.toByte())
	}

	@Throws(IOException::class)
	override fun maximize() {
		writeCSISequenceToTerminal('9'.toByte(), ';'.toByte(), '1'.toByte(), 't'.toByte())
	}

	@Throws(IOException::class)
	override fun unmaximize() {
		writeCSISequenceToTerminal('9'.toByte(), ';'.toByte(), '0'.toByte(), 't'.toByte())
	}

	@Throws(IOException::class)
	private fun updateMouseCaptureMode(mouseCaptureMode: MouseCaptureMode?, l_or_h: Char) {
		if (mouseCaptureMode == null) {
			return
		}

		when (mouseCaptureMode) {
			MouseCaptureMode.CLICK -> writeCSISequenceToTerminal('?'.toByte(), '9'.toByte(), l_or_h.toByte())
			MouseCaptureMode.CLICK_RELEASE -> writeCSISequenceToTerminal('?'.toByte(), '1'.toByte(), '0'.toByte(), '0'.toByte(), '0'.toByte(), l_or_h.toByte())
			MouseCaptureMode.CLICK_RELEASE_DRAG -> writeCSISequenceToTerminal('?'.toByte(), '1'.toByte(), '0'.toByte(), '0'.toByte(), '2'.toByte(), l_or_h.toByte())
			MouseCaptureMode.CLICK_RELEASE_DRAG_MOVE -> writeCSISequenceToTerminal('?'.toByte(), '1'.toByte(), '0'.toByte(), '0'.toByte(), '3'.toByte(), l_or_h.toByte())
		}
		if (charset == Charset.forName("UTF-8")) {
			writeCSISequenceToTerminal('?'.toByte(), '1'.toByte(), '0'.toByte(), '0'.toByte(), '5'.toByte(), l_or_h.toByte())
		}
	}

	@Throws(IOException::class)
	override fun setMouseCaptureMode(mouseCaptureMode: MouseCaptureMode) {
		requestedMouseCaptureMode = mouseCaptureMode
		if (isInPrivateMode && requestedMouseCaptureMode !== this.mouseCaptureMode) {
			updateMouseCaptureMode(this.mouseCaptureMode, 'l')
			this.mouseCaptureMode = requestedMouseCaptureMode
			updateMouseCaptureMode(this.mouseCaptureMode, 'h')
		}
	}

	@Throws(IOException::class)
	override fun scrollLines(firstLine: Int, lastLine: Int, distance: Int) {
		var firstLine = firstLine
		val CSI = "\u001b["

		// some sanity checks:
		if (distance == 0) {
			return
		}
		if (firstLine < 0) {
			firstLine = 0
		}
		if (lastLine < firstLine) {
			return
		}
		val sb = StringBuilder()

		// define range:
		sb.append(CSI).append(firstLine + 1)
			.append(';').append(lastLine + 1).append('r')

		// place cursor on line to scroll away from:
		val target = if (distance > 0) lastLine else firstLine
		sb.append(CSI).append(target + 1).append(";1H")

		// do scroll:
		if (distance > 0) {
			val num = Math.min(distance, lastLine - firstLine + 1)
			for (i in 0 until num) {
				sb.append('\n')
			}
		} else { // distance < 0
			val num = Math.min(-distance, lastLine - firstLine + 1)
			for (i in 0 until num) {
				sb.append("\u001bM")
			}
		}

		// reset range:
		sb.append(CSI).append('r')

		// off we go!
		writeToTerminal(*sb.toString().toByteArray())
	}

	@Throws(IOException::class)
	internal fun reportPosition() {
		writeCSISequenceToTerminal(*"6n".toByteArray())
	}

	@Throws(IOException::class)
	internal fun restoreCursorPosition() {
		writeCSISequenceToTerminal(*"u".toByteArray())
	}

	@Throws(IOException::class)
	internal fun saveCursorPosition() {
		writeCSISequenceToTerminal(*"s".toByteArray())
	}
}
