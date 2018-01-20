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

import com.googlecode.lanterna.input.KeyStroke

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset

/**
 * Base class for all terminals that generally behave like Unix terminals. This class defined a number of abstract
 * methods that needs to be implemented which are all used to setup the terminal environment (turning off echo,
 * canonical mode, etc) and also a control variable for how to react to CTRL+c keystroke.
 */
abstract class UnixLikeTerminal @Throws(IOException::class)
protected constructor(terminalInput: InputStream,
					  terminalOutput: OutputStream,
					  terminalCharset: Charset,
					  protected val terminalCtrlCBehaviour: CtrlCBehaviour) : ANSITerminal(terminalInput, terminalOutput, terminalCharset) {
	private val catchSpecialCharacters: Boolean
	private val shutdownHook: Thread
	private var acquired: Boolean = false

	/**
	 * This enum lets you control how Lanterna will handle a ctrl+c keystroke from the user.
	 */
	enum class CtrlCBehaviour {
		/**
		 * Pressing ctrl+c doesn't kill the application, it will be added to the input queue as any other key stroke
		 */
		TRAP,
		/**
		 * Pressing ctrl+c will restore the terminal and kill the application as it normally does with terminal
		 * applications. Lanterna will restore the terminal and then call `System.exit(1)` for this.
		 */
		CTRL_C_KILLS_APPLICATION
	}

	init {
		this.acquired = false

		val catchSpecialCharactersPropValue = System.getProperty(
			"com.googlecode.lanterna.terminal.UnixTerminal.catchSpecialCharacters",
			"")
		this.catchSpecialCharacters = "false" != catchSpecialCharactersPropValue.trim { it <= ' ' }.toLowerCase()
		shutdownHook = object : Thread("Lanterna STTY restore") {
			override fun run() {
				exitPrivateModeAndRestoreState()
			}
		}
		acquire()
	}

	/**
	 * Effectively taking over the terminal and enabling it for Lanterna to use, by turning off echo and canonical mode,
	 * adding resize listeners and optionally trap unix signals. This should be called automatically by the constructor
	 * of any end-user class extending from [UnixLikeTerminal]
	 * @throws IOException If there was an I/O error
	 */
	@Throws(IOException::class)
	protected open fun acquire() {
		//Make sure to set an initial size
		onResized(80, 24)

		saveTerminalSettings()
		canonicalMode(false)
		keyEchoEnabled(false)
		if (catchSpecialCharacters) {
			keyStrokeSignalsEnabled(false)
		}
		registerTerminalResizeListener(Runnable {
			// This will trigger a resize notification as the size will be different than before
			try {
				terminalSize
			} catch (ignore: IOException) {
				// Not much to do here, we can't re-throw it
			}
		})
		Runtime.getRuntime().addShutdownHook(shutdownHook)
		acquired = true
	}

	@Throws(IOException::class)
	override fun close() {
		exitPrivateModeAndRestoreState()
		Runtime.getRuntime().removeShutdownHook(shutdownHook)
		acquired = false
		super.close()
	}

	@Throws(IOException::class)
	override fun pollInput(): KeyStroke {
		//Check if we have ctrl+c coming
		val key = super.pollInput()
		isCtrlC(key)
		return key
	}

	@Throws(IOException::class)
	override fun readInput(): KeyStroke {
		//Check if we have ctrl+c coming
		val key = super.readInput()
		isCtrlC(key)
		return key
	}

	@Throws(IOException::class)
	protected abstract fun registerTerminalResizeListener(onResize: Runnable)

	/**
	 * Stores the current terminal device settings (the ones that are modified through this interface) so that they can
	 * be restored later using [.restoreTerminalSettings]
	 * @throws IOException If there was an I/O error when altering the terminal environment
	 */
	@Throws(IOException::class)
	protected abstract fun saveTerminalSettings()

	/**
	 * Restores the terminal settings from last time [.saveTerminalSettings] was called
	 * @throws IOException If there was an I/O error when altering the terminal environment
	 */
	@Throws(IOException::class)
	protected abstract fun restoreTerminalSettings()

	@Throws(IOException::class)
	private fun restoreTerminalSettingsAndKeyStrokeSignals() {
		restoreTerminalSettings()
		if (catchSpecialCharacters) {
			keyStrokeSignalsEnabled(true)
		}
	}

	/**
	 * Enables or disable key echo mode, which means when the user press a key, the terminal will immediately print that
	 * key to the terminal. Normally for Lanterna, this should be turned off so the software can take the key as an
	 * input event, put it on the input queue and then depending on the code decide what to do with it.
	 * @param enabled `true` if key echo should be enabled, `false` otherwise
	 * @throws IOException If there was an I/O error when altering the terminal environment
	 */
	@Throws(IOException::class)
	protected abstract fun keyEchoEnabled(enabled: Boolean)

	/**
	 * In canonical mode, data are accumulated in a line editing buffer, and do not become "available for reading" until
	 * line editing has been terminated by the user sending a line delimiter character. This is usually the default mode
	 * for a terminal. Lanterna wants to read each character as they are typed, without waiting for the final newline,
	 * so it will attempt to turn canonical mode off on initialization.
	 * @param enabled `true` if canonical input mode should be enabled, `false` otherwise
	 * @throws IOException If there was an I/O error when altering the terminal environment
	 */
	@Throws(IOException::class)
	protected abstract fun canonicalMode(enabled: Boolean)

	/**
	 * This method causes certain keystrokes (at the moment only ctrl+c) to be passed in to the program as a regular
	 * [com.googlecode.lanterna.input.KeyStroke] instead of as a signal to the JVM process. For example,
	 * *ctrl+c* will normally send an interrupt that causes the JVM to shut down, but this method will make it pass
	 * in *ctrl+c* as a regular [com.googlecode.lanterna.input.KeyStroke] instead. You can of course still
	 * make *ctrl+c* kill the application through your own input handling if you like.
	 *
	 *
	 * Please note that this method is called automatically by lanterna to disable signals unless you define a system
	 * property "com.googlecode.lanterna.terminal.UnixTerminal.catchSpecialCharacters" and set it to the string "false".
	 * @param enabled Pass in `true` if you want keystrokes to generate system signals (like process interrupt),
	 * `false` if you want lanterna to catch and interpret these keystrokes are regular keystrokes
	 * @throws IOException If there was an I/O error when attempting to disable special characters
	 * @see UnixLikeTTYTerminal.CtrlCBehaviour
	 */
	@Throws(IOException::class)
	protected abstract fun keyStrokeSignalsEnabled(enabled: Boolean)

	@Throws(IOException::class)
	private fun isCtrlC(key: KeyStroke?) {
		if (key != null
			&& terminalCtrlCBehaviour == CtrlCBehaviour.CTRL_C_KILLS_APPLICATION
			&& key.character != null
			&& key.character == 'c'
			&& !key.isAltDown
			&& key.isCtrlDown) {

			if (isInPrivateMode) {
				exitPrivateMode()
			}
			System.exit(1)
		}
	}

	private fun exitPrivateModeAndRestoreState() {
		if (!acquired) {
			return
		}
		try {
			if (isInPrivateMode) {
				exitPrivateMode()
			}
		} catch (ignored: IOException) {
		} catch (ignored: IllegalStateException) {
		}
		// still possible!

		try {
			restoreTerminalSettingsAndKeyStrokeSignals()
		} catch (ignored: IOException) {
		}

	}
}
