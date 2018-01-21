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
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.nio.charset.Charset
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections

/**
 * UnixLikeTerminal extends from ANSITerminal and defines functionality that is common to
 * `UnixTerminal` and `CygwinTerminal`, like setting tty modes; echo, cbreak
 * and minimum characters for reading as well as a shutdown hook to set the tty back to
 * original state at the end.
 *
 *
 * If requested, it handles Control-C input to terminate the program, and hooks
 * into Unix WINCH signal to detect when the user has resized the terminal,
 * if supported by the JVM.
 *
 * @author Andreas
 * @author Martin
 */
abstract class UnixLikeTTYTerminal
/**
 * Creates a UnixTerminal using a specified input stream, output stream and character set, with a custom size
 * querier instead of using the default one. This way you can override size detection (if you want to force the
 * terminal to a fixed size, for example). You also choose how you want ctrl+c key strokes to be handled.
 *
 * @param ttyDev TTY device file that is representing this terminal session, will be used when calling stty to make
 * it operate on this session
 * @param terminalInput Input stream to read terminal input from
 * @param terminalOutput Output stream to write terminal output to
 * @param terminalCharset Character set to use when converting characters to bytes
 * @param terminalCtrlCBehaviour Special settings on how the terminal will behave, see `UnixTerminalMode` for
 * more details
 * @throws IOException If there was an I/O error while setting up the terminal
 */
@Throws(IOException::class)
protected constructor(
	private val ttyDev: File?,
	terminalInput: InputStream,
	terminalOutput: OutputStream,
	terminalCharset: Charset,
	terminalCtrlCBehaviour: UnixLikeTerminal.CtrlCBehaviour) : UnixLikeTerminal(terminalInput, terminalOutput, terminalCharset, terminalCtrlCBehaviour) {
	private var sttyStatusToRestore: String? = null

	private val sttyCommand: String
		get() = "/bin/stty"

	init {

		// Take ownership of the terminal
		realAcquire()
	}

	@Throws(IOException::class)
	override fun acquire() {
		// Hack!
	}

	@Throws(IOException::class)
	private fun realAcquire() {
		super.acquire()
	}

	@Throws(IOException::class)
	override fun registerTerminalResizeListener(onResize: Runnable) {
		try {
			val signalClass = Class.forName("sun.misc.Signal")
			for (m in signalClass.declaredMethods) {
				if ("handle" == m.name) {
					val windowResizeHandler = Proxy.newProxyInstance(javaClass.getClassLoader(), arrayOf(Class.forName("sun.misc.SignalHandler"))) { proxy, method, args ->
						if ("handle" == method.name) {
							onResize.run()
						}
						null
					}
					m.invoke(null, signalClass.getConstructor(String::class.java).newInstance("WINCH"), windowResizeHandler)
				}
			}
		} catch (ignore: Throwable) {
			// We're probably running on a non-Sun JVM and there's no way to catch signals without resorting to native
			// code integration
		}

	}

	@Throws(IOException::class)
	override fun saveTerminalSettings() {
		sttyStatusToRestore = exec(sttyCommand, "-g").trim { it <= ' ' }
	}

	@Throws(IOException::class)
	override fun restoreTerminalSettings() {
		sttyStatusToRestore?.let { exec(sttyCommand, it) }
	}

	@Throws(IOException::class)
	override fun keyEchoEnabled(enabled: Boolean) {
		exec(sttyCommand, if (enabled) "echo" else "-echo")
	}

	@Throws(IOException::class)
	override fun canonicalMode(enabled: Boolean) {
		exec(sttyCommand, if (enabled) "icanon" else "-icanon")
		if (!enabled) {
			exec(sttyCommand, "min", "1")
		}
	}

	@Throws(IOException::class)
	override fun keyStrokeSignalsEnabled(enabled: Boolean) {
		if (enabled) {
			exec(sttyCommand, "intr", "^C")
		} else {
			exec(sttyCommand, "intr", "undef")
		}
	}

	@Throws(IOException::class)
	protected open fun runSTTYCommand(vararg parameters: String): String {
		val commandLine = ArrayList<String>(listOf<String>(sttyCommand))
		commandLine.addAll(Arrays.asList(*parameters))
		return exec(*commandLine.toTypedArray())
	}

	@Throws(IOException::class)
	protected fun exec(vararg cmd: String): String {
		var cmd = cmd
		if (ttyDev != null) {
			//Here's what we try to do, but that is Java 7+ only:
			// processBuilder.redirectInput(ProcessBuilder.Redirect.from(ttyDev));
			//instead, for Java 6, we join the cmd into a scriptlet with redirection
			//and replace cmd by a call to sh with the scriptlet:
			val sb = StringBuilder()
			for (arg in cmd) {
				sb.append(arg).append(' ')
			}
			sb.append("< ").append(ttyDev)
			cmd = arrayOf("sh", "-c", sb.toString())
		}
		val pb = ProcessBuilder(*cmd)
		val process = pb.start()
		val stdoutBuffer = ByteArrayOutputStream()
		val stdout = process.inputStream
		var readByte = stdout.read()
		while (readByte >= 0) {
			stdoutBuffer.write(readByte)
			readByte = stdout.read()
		}
		val stdoutBufferInputStream = ByteArrayInputStream(stdoutBuffer.toByteArray())
		val reader = BufferedReader(InputStreamReader(stdoutBufferInputStream))
		val builder = StringBuilder()
		var line: String
		while (true) {
			line = reader.readLine()
			if (line == null) {
				break
			}
			builder.append(line)
		}
		reader.close()
		return builder.toString()
	}
}