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
package com.googlecode.lanterna.terminal

import com.googlecode.lanterna.SGR
import com.googlecode.lanterna.TerminalPosition
import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.TextColor
import com.googlecode.lanterna.graphics.TextGraphics
import com.googlecode.lanterna.input.KeyStroke
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * This class exposes methods for converting a terminal into an IOSafeTerminal. There are two options available, either
 * one that will convert any IOException to a RuntimeException (and re-throw it) or one that will silently swallow any
 * IOException (and return null in those cases the method has a non-void return type).
 * @author Martin
 */
open class IOSafeTerminalAdapter(private val backend: Terminal, internal val exceptionHandler: ExceptionHandler) : IOSafeTerminal {

	override var cursorPosition: TerminalPosition?
		get() {
			try {
				return backend.cursorPosition
			} catch (e: IOException) {
				exceptionHandler.onException(e)
			}

			return null
		}
		set(position) = try {
			backend.cursorPosition = position
		} catch (e: IOException) {
			exceptionHandler.onException(e)
		}

	override val terminalSize: TerminalSize?
		get() {
			try {
				return backend.terminalSize
			} catch (e: IOException) {
				exceptionHandler.onException(e)
			}

			return null
		}

	private interface ExceptionHandler {
		fun onException(e: IOException)
	}

	private class ConvertToRuntimeException : ExceptionHandler {
		override fun onException(e: IOException) {
			throw RuntimeException(e)
		}
	}

	private class DoNothingAndOrReturnNull : ExceptionHandler {
		override fun onException(e: IOException) {}
	}

	override fun enterPrivateMode() {
		try {
			backend.enterPrivateMode()
		} catch (e: IOException) {
			exceptionHandler.onException(e)
		}

	}

	override fun exitPrivateMode() {
		try {
			backend.exitPrivateMode()
		} catch (e: IOException) {
			exceptionHandler.onException(e)
		}

	}

	override fun clearScreen() {
		try {
			backend.clearScreen()
		} catch (e: IOException) {
			exceptionHandler.onException(e)
		}

	}

	override fun setCursorPosition(x: Int, y: Int) {
		try {
			backend.setCursorPosition(x, y)
		} catch (e: IOException) {
			exceptionHandler.onException(e)
		}

	}

	override fun setCursorVisible(visible: Boolean) {
		try {
			backend.setCursorVisible(visible)
		} catch (e: IOException) {
			exceptionHandler.onException(e)
		}

	}

	override fun putCharacter(c: Char) {
		try {
			backend.putCharacter(c)
		} catch (e: IOException) {
			exceptionHandler.onException(e)
		}

	}

	override fun newTextGraphics(): TextGraphics? {
		try {
			return backend.newTextGraphics()
		} catch (e: IOException) {
			exceptionHandler.onException(e)
		}

		return null
	}

	override fun enableSGR(sgr: SGR) {
		try {
			backend.enableSGR(sgr)
		} catch (e: IOException) {
			exceptionHandler.onException(e)
		}

	}

	override fun disableSGR(sgr: SGR) {
		try {
			backend.disableSGR(sgr)
		} catch (e: IOException) {
			exceptionHandler.onException(e)
		}

	}

	override fun resetColorAndSGR() {
		try {
			backend.resetColorAndSGR()
		} catch (e: IOException) {
			exceptionHandler.onException(e)
		}

	}

	override fun setForegroundColor(color: TextColor) {
		try {
			backend.setForegroundColor(color)
		} catch (e: IOException) {
			exceptionHandler.onException(e)
		}

	}

	override fun setBackgroundColor(color: TextColor) {
		try {
			backend.setBackgroundColor(color)
		} catch (e: IOException) {
			exceptionHandler.onException(e)
		}

	}

	override fun addResizeListener(listener: TerminalResizeListener) {
		backend.addResizeListener(listener)
	}

	override fun removeResizeListener(listener: TerminalResizeListener) {
		backend.removeResizeListener(listener)
	}

	override fun enquireTerminal(timeout: Int, timeoutUnit: TimeUnit): ByteArray? {
		try {
			return backend.enquireTerminal(timeout, timeoutUnit)
		} catch (e: IOException) {
			exceptionHandler.onException(e)
		}

		return null
	}

	override fun bell() {
		try {
			backend.bell()
		} catch (e: IOException) {
			exceptionHandler.onException(e)
		}

	}

	override fun flush() {
		try {
			backend.flush()
		} catch (e: IOException) {
			exceptionHandler.onException(e)
		}

	}

	override fun close() {
		try {
			backend.close()
		} catch (e: IOException) {
			exceptionHandler.onException(e)
		}

	}

	override fun pollInput(): KeyStroke {
		try {
			return backend.pollInput()
		} catch (e: IOException) {
			exceptionHandler.onException(e)
		}

		return null
	}

	override fun readInput(): KeyStroke {
		try {
			return backend.readInput()
		} catch (e: IOException) {
			exceptionHandler.onException(e)
		}

		return null
	}

	/**
	 * This class exposes methods for converting an extended terminal into an IOSafeExtendedTerminal.
	 */
	class Extended(private val backend: ExtendedTerminal, exceptionHandler: ExceptionHandler) : IOSafeTerminalAdapter(backend, exceptionHandler), IOSafeExtendedTerminal {

		override fun setTerminalSize(columns: Int, rows: Int) {
			try {
				backend.setTerminalSize(columns, rows)
			} catch (e: IOException) {
				exceptionHandler.onException(e)
			}

		}

		override fun setTitle(title: String) {
			try {
				backend.setTitle(title)
			} catch (e: IOException) {
				exceptionHandler.onException(e)
			}

		}

		override fun pushTitle() {
			try {
				backend.pushTitle()
			} catch (e: IOException) {
				exceptionHandler.onException(e)
			}

		}

		override fun popTitle() {
			try {
				backend.popTitle()
			} catch (e: IOException) {
				exceptionHandler.onException(e)
			}

		}

		override fun iconify() {
			try {
				backend.iconify()
			} catch (e: IOException) {
				exceptionHandler.onException(e)
			}

		}

		override fun deiconify() {
			try {
				backend.deiconify()
			} catch (e: IOException) {
				exceptionHandler.onException(e)
			}

		}

		override fun maximize() {
			try {
				backend.maximize()
			} catch (e: IOException) {
				exceptionHandler.onException(e)
			}

		}

		override fun unmaximize() {
			try {
				backend.unmaximize()
			} catch (e: IOException) {
				exceptionHandler.onException(e)
			}

		}

		override fun setMouseCaptureMode(mouseCaptureMode: MouseCaptureMode) {
			try {
				backend.setMouseCaptureMode(mouseCaptureMode)
			} catch (e: IOException) {
				exceptionHandler.onException(e)
			}

		}

		override fun scrollLines(firstLine: Int, lastLine: Int, distance: Int) {
			try {
				backend.scrollLines(firstLine, lastLine, distance)
			} catch (e: IOException) {
				exceptionHandler.onException(e)
			}

		}

	}

	companion object {

		/**
		 * Creates a wrapper around a Terminal that exposes it as a IOSafeTerminal. If any IOExceptions occur, they will be
		 * wrapped by a RuntimeException and re-thrown.
		 * @param terminal Terminal to wrap
		 * @return IOSafeTerminal wrapping the supplied terminal
		 */
		fun createRuntimeExceptionConvertingAdapter(terminal: Terminal): IOSafeTerminal {
			return if (terminal is ExtendedTerminal) { // also handle Runtime-type:
				createRuntimeExceptionConvertingAdapter(terminal)
			} else {
				IOSafeTerminalAdapter(terminal, ConvertToRuntimeException())
			}
		}

		/**
		 * Creates a wrapper around an ExtendedTerminal that exposes it as a IOSafeExtendedTerminal.
		 * If any IOExceptions occur, they will be wrapped by a RuntimeException and re-thrown.
		 * @param terminal Terminal to wrap
		 * @return IOSafeTerminal wrapping the supplied terminal
		 */
		fun createRuntimeExceptionConvertingAdapter(terminal: ExtendedTerminal): IOSafeExtendedTerminal {
			return IOSafeTerminalAdapter.Extended(terminal, ConvertToRuntimeException())
		}

		/**
		 * Creates a wrapper around a Terminal that exposes it as a IOSafeTerminal. If any IOExceptions occur, they will be
		 * silently ignored and for those method with a non-void return type, null will be returned.
		 * @param terminal Terminal to wrap
		 * @return IOSafeTerminal wrapping the supplied terminal
		 */
		fun createDoNothingOnExceptionAdapter(terminal: Terminal): IOSafeTerminal {
			return if (terminal is ExtendedTerminal) { // also handle Runtime-type:
				createDoNothingOnExceptionAdapter(terminal)
			} else {
				IOSafeTerminalAdapter(terminal, DoNothingAndOrReturnNull())
			}
		}

		/**
		 * Creates a wrapper around an ExtendedTerminal that exposes it as a IOSafeExtendedTerminal.
		 * If any IOExceptions occur, they will be silently ignored and for those method with a
		 * non-void return type, null will be returned.
		 * @param terminal Terminal to wrap
		 * @return IOSafeTerminal wrapping the supplied terminal
		 */
		fun createDoNothingOnExceptionAdapter(terminal: ExtendedTerminal): IOSafeExtendedTerminal {
			return IOSafeTerminalAdapter.Extended(terminal, DoNothingAndOrReturnNull())
		}
	}
}
