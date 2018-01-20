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
package com.googlecode.lanterna.gui2

import java.io.EOFException
import java.io.IOException
import java.util.concurrent.CountDownLatch

/**
 * Default implementation of TextGUIThread, this class runs the GUI event processing on a dedicated thread. The GUI
 * needs to be explicitly started in order for the event processing loop to begin, so you must call `start()`
 * for this. The GUI thread will stop if `stop()` is called, the input stream returns EOF or an exception is
 * thrown from inside the event handling loop.
 *
 *
 * Here is an example of how to use this `TextGUIThread`:
 * <pre>
 * `MultiWindowTextGUI textGUI = new MultiWindowTextGUI(new SeparateTextGUIThread.Factory(), screen);
 * // ... add components ...
 * ((AsynchronousTextGUIThread)textGUI.getGUIThread()).start();
 * // ... this thread will continue while the GUI runs on a separate thread ...
` *
</pre> *
 * @see TextGUIThread
 *
 * @see SameTextGUIThread
 *
 * @author Martin
 */
class SeparateTextGUIThread private constructor(textGUI: TextGUI) : AbstractTextGUIThread(textGUI), AsynchronousTextGUIThread {
	@Volatile override var state: AsynchronousTextGUIThread.State? = null
		private set
	override val thread: Thread
	private val waitLatch: CountDownLatch

	init {
		this.waitLatch = CountDownLatch(1)
		this.thread = object : Thread("LanternaGUI") {
			override fun run() {
				mainGUILoop()
			}
		}
		state = AsynchronousTextGUIThread.State.CREATED
	}

	override fun start() {
		thread.start()
		state = AsynchronousTextGUIThread.State.STARTED
	}

	override fun stop() {
		if (state != AsynchronousTextGUIThread.State.STARTED) {
			return
		}

		state = AsynchronousTextGUIThread.State.STOPPING
	}

	@Throws(InterruptedException::class)
	override fun waitForStop() {
		waitLatch.await()
	}

	@Throws(IllegalStateException::class)
	override fun invokeLater(runnable: Runnable) {
		if (state != AsynchronousTextGUIThread.State.STARTED) {
			throw IllegalStateException("Cannot schedule " + runnable + " for execution on the TextGUIThread " +
				"because the thread is in " + state + " state")
		}
		super.invokeLater(runnable)
	}

	private fun mainGUILoop() {
		try {
			//Draw initial screen, after this only draw when the GUI is marked as invalid
			try {
				textGUI.updateScreen()
			} catch (e: IOException) {
				exceptionHandler.onIOException(e)
			} catch (e: RuntimeException) {
				exceptionHandler.onRuntimeException(e)
			}

			while (state == AsynchronousTextGUIThread.State.STARTED) {
				try {
					if (!processEventsAndUpdate()) {
						try {
							Thread.sleep(1)
						} catch (ignored: InterruptedException) {
						}

					}
				} catch (e: EOFException) {
					stop()
					break //Break out quickly from the main loop
				} catch (e: IOException) {
					if (exceptionHandler.onIOException(e)) {
						stop()
						break
					}
				} catch (e: RuntimeException) {
					if (exceptionHandler.onRuntimeException(e)) {
						stop()
						break
					}
				}

			}
		} finally {
			state = AsynchronousTextGUIThread.State.STOPPED
			waitLatch.countDown()
		}
	}


	/**
	 * Factory class for creating SeparateTextGUIThread objects
	 */
	class Factory : TextGUIThreadFactory {
		override fun createTextGUIThread(textGUI: TextGUI): TextGUIThread {
			return SeparateTextGUIThread(textGUI)
		}
	}
}
