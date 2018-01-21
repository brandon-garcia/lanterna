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

import java.io.IOException
import java.util.Queue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue

/**
 * Abstract implementation of [TextGUIThread] with common logic for both available concrete implementations.
 */
abstract class AbstractTextGUIThread
/**
 * Sets up this [AbstractTextGUIThread] for operations on the supplies [TextGUI]
 * @param textGUI Text GUI this [TextGUIThread] implementations will be operating on
 */
(protected val textGUI: TextGUI) : TextGUIThread {
	protected val customTasks: Queue<Runnable>
	protected var exceptionHandler: TextGUIThread.ExceptionHandler

	init {
		this.exceptionHandler = object : TextGUIThread.ExceptionHandler {
			override fun onIOException(e: IOException): Boolean {
				e.printStackTrace()
				return true
			}

			override fun onRuntimeException(e: RuntimeException): Boolean {
				e.printStackTrace()
				return true
			}
		}
		this.customTasks = LinkedBlockingQueue()
	}

	@Throws(IllegalStateException::class)
	override fun invokeLater(runnable: Runnable) {
		customTasks.add(runnable)
	}

	override fun setExceptionHandler(exceptionHandler: TextGUIThread.ExceptionHandler) {
		this.exceptionHandler = exceptionHandler
	}

	@Synchronized
	@Throws(IOException::class)
	override fun processEventsAndUpdate(): Boolean {
		if (thread !== Thread.currentThread()) {
			throw IllegalStateException("Calling processEventAndUpdate outside of GUI thread")
		}
		textGUI.processInput()
		while (!customTasks.isEmpty()) {
			val r = customTasks.poll()
			r?.run()
		}
		if (textGUI.isPendingUpdate) {
			textGUI.updateScreen()
			return true
		}
		return false
	}

	@Throws(IllegalStateException::class, InterruptedException::class)
	override fun invokeAndWait(runnable: Runnable) {
		val guiThread = thread
		if (guiThread == null || Thread.currentThread() === guiThread) {
			runnable.run()
		} else {
			val countDownLatch = CountDownLatch(1)
			invokeLater {
				try {
					runnable.run()
				} finally {
					countDownLatch.countDown()
				}
			}
			countDownLatch.await()
		}
	}
}
