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

import com.googlecode.lanterna.graphics.BasicTextImage
import com.googlecode.lanterna.graphics.TextImage
import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.input.KeyType
import com.googlecode.lanterna.screen.Screen
import com.googlecode.lanterna.TerminalPosition
import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.TextColor
import com.googlecode.lanterna.screen.VirtualScreen

import java.io.EOFException
import java.io.IOException
import java.util.*

/**
 * This is the main Text GUI implementation built into Lanterna, supporting multiple tiled windows and a dynamic
 * background area that can be fully customized. If you want to create a text-based GUI with windows and controls,
 * it's very likely this is what you want to use.
 *
 * @author Martin
 */
class MultiWindowTextGUI private constructor(
	guiThreadFactory: TextGUIThreadFactory,
	private val virtualScreen: VirtualScreen,
	override val windowManager: WindowManager?,
	override val windowPostRenderer: WindowPostRenderer?,
	background: Component?) : AbstractTextGUI(guiThreadFactory, virtualScreen), WindowBasedTextGUI {
	override val backgroundPane: BasePane
	private val windows: MutableList<Window>
	private val windowRenderBufferCache: IdentityHashMap<Window, TextImage>

	@get:Synchronized override var activeWindow: Window? = null
		private set
	/**
	 * Returns whether the TextGUI should return EOF when you try to read input while there are no windows in the window
	 * manager. When this is true (true by default) will make the GUI automatically exit when the last window has been
	 * closed.
	 * @return Should the GUI return EOF when there are no windows left
	 */
	/**
	 * Sets whether the TextGUI should return EOF when you try to read input while there are no windows in the window
	 * manager. Setting this to true (on by default) will make the GUI automatically exit when the last window has been
	 * closed.
	 * @param eofWhenNoWindows Should the GUI return EOF when there are no windows left
	 */
	var isEOFWhenNoWindows: Boolean = false

	override val isPendingUpdate: Boolean
		@Synchronized get() {
			for (window in windows) {
				if (window.isVisible && window.isInvalid) {
					return true
				}
			}
			return super.isPendingUpdate || backgroundPane.isInvalid || windowManager.isInvalid
		}

	override val cursorPosition: TerminalPosition
		@Synchronized get() {
			val activeWindow = activeWindow
			return if (activeWindow != null) {
				activeWindow.toGlobal(activeWindow.cursorPosition)
			} else {
				backgroundPane.cursorPosition
			}
		}

	override val focusedInteractable: Interactable
		@Synchronized get() {
			val activeWindow = activeWindow
			return if (activeWindow != null) {
				activeWindow.focusedInteractable
			} else {
				backgroundPane.focusedInteractable
			}
		}

	override val screen: Screen
		get() = virtualScreen

	/**
	 * Creates a new `MultiWindowTextGUI` that uses the specified `Screen` as the backend for all drawing
	 * operations. The screen will be automatically wrapped in a `VirtualScreen` in order to deal with GUIs
	 * becoming too big to fit the terminal. The background area of the GUI is a solid color as decided by the
	 * `backgroundColor` parameter.
	 * @param screen Screen to use as the backend for drawing operations
	 * @param backgroundColor Color to use for the GUI background
	 */
	@JvmOverloads constructor(
		screen: Screen,
		backgroundColor: TextColor = TextColor.ANSI.BLUE) : this(screen, DefaultWindowManager(), EmptySpace(backgroundColor)) {
	}

	/**
	 * Creates a new `MultiWindowTextGUI` that uses the specified `Screen` as the backend for all drawing
	 * operations. The screen will be automatically wrapped in a `VirtualScreen` in order to deal with GUIs
	 * becoming too big to fit the terminal. The background area of the GUI is the component passed in as the
	 * `background` parameter, forced to full size.
	 * @param screen Screen to use as the backend for drawing operations
	 * @param windowManager Window manager implementation to use
	 * @param background Component to use as the background of the GUI, behind all the windows
	 */
	constructor(
		screen: Screen,
		windowManager: WindowManager,
		background: Component) : this(screen, windowManager, null, background) {
	}

	/**
	 * Creates a new `MultiWindowTextGUI` that uses the specified `Screen` as the backend for all drawing
	 * operations. The screen will be automatically wrapped in a `VirtualScreen` in order to deal with GUIs
	 * becoming too big to fit the terminal. The background area of the GUI is the component passed in as the
	 * `background` parameter, forced to full size.
	 * @param screen Screen to use as the backend for drawing operations
	 * @param windowManager Window manager implementation to use
	 * @param postRenderer `WindowPostRenderer` object to invoke after each window has been drawn
	 * @param background Component to use as the background of the GUI, behind all the windows
	 */
	constructor(
		screen: Screen,
		windowManager: WindowManager,
		postRenderer: WindowPostRenderer?,
		background: Component) : this(SameTextGUIThread.Factory(), screen, windowManager, postRenderer, background) {
	}

	/**
	 * Creates a new `MultiWindowTextGUI` that uses the specified `Screen` as the backend for all drawing
	 * operations. The screen will be automatically wrapped in a `VirtualScreen` in order to deal with GUIs
	 * becoming too big to fit the terminal. The background area of the GUI is the component passed in as the
	 * `background` parameter, forced to full size.
	 * @param guiThreadFactory Factory implementation to use when creating the `TextGUIThread`
	 * @param screen Screen to use as the backend for drawing operations
	 * @param windowManager Window manager implementation to use
	 * @param postRenderer `WindowPostRenderer` object to invoke after each window has been drawn
	 * @param background Component to use as the background of the GUI, behind all the windows
	 */
	@JvmOverloads constructor(
		guiThreadFactory: TextGUIThreadFactory,
		screen: Screen,
		windowManager: WindowManager = DefaultWindowManager(),
		postRenderer: WindowPostRenderer? = null,
		background: Component = GUIBackdrop()) : this(guiThreadFactory, VirtualScreen(screen), windowManager, postRenderer, background) {
	}

	init {
		var background = background
		if (windowManager == null) {
			throw IllegalArgumentException("Creating a window-based TextGUI requires a WindowManager")
		}
		if (background == null) {
			//Use a sensible default instead of throwing
			background = EmptySpace(TextColor.ANSI.BLUE)
		}
		this.backgroundPane = object : AbstractBasePane<BasePane>() {
			override val textGUI: TextGUI
				get() = this@MultiWindowTextGUI

			override fun toGlobal(localPosition: TerminalPosition): TerminalPosition {
				return localPosition
			}

			override fun fromGlobal(globalPosition: TerminalPosition): TerminalPosition {
				return globalPosition
			}

			internal override fun self(): BasePane {
				return this
			}
		}
		this.backgroundPane.component = background
		this.windows = LinkedList()
		this.windowRenderBufferCache = IdentityHashMap()
		this.isEOFWhenNoWindows = false
	}

	@Synchronized
	@Throws(IOException::class)
	override fun updateScreen() {
		var minimumTerminalSize = TerminalSize.ZERO
		for (window in windows) {
			if (window.isVisible) {
				if (window.hints.contains(Window.Hint.FULL_SCREEN) ||
					window.hints.contains(Window.Hint.FIT_TERMINAL_WINDOW) ||
					window.hints.contains(Window.Hint.EXPANDED)) {
					//Don't take full screen windows or auto-sized windows into account
					continue
				}
				val lastPosition = window.position
				minimumTerminalSize = minimumTerminalSize.max(
					//Add position to size to get the bottom-right corner of the window
					window.decoratedSize.withRelative(
						Math.max(lastPosition.column, 0),
						Math.max(lastPosition.row, 0)))
			}
		}
		virtualScreen.setMinimumSize(minimumTerminalSize)
		super.updateScreen()
	}

	@Synchronized
	@Throws(IOException::class)
	override fun readKeyStroke(): KeyStroke {
		val keyStroke = super.pollInput()
		return if (isEOFWhenNoWindows && keyStroke == null && windows.isEmpty()) {
			KeyStroke(KeyType.EOF)
		} else keyStroke ?: super.readKeyStroke()
	}

	@Synchronized override fun drawGUI(graphics: TextGUIGraphics) {
		drawBackgroundPane(graphics)
		windowManager.prepareWindows(this, Collections.unmodifiableList(windows), graphics.size)
		for (window in windows) {
			if (window.isVisible) {
				// First draw windows to a buffer, then copy it to the real destination. This is to make physical off-screen
				// drawing work better. Store the buffers in a cache so we don't have to re-create them every time.
				var textImage: TextImage? = windowRenderBufferCache[window]
				if (textImage == null || textImage.size != window.decoratedSize) {
					textImage = BasicTextImage(window.decoratedSize)
					windowRenderBufferCache.put(window, textImage)
				}
				var windowGraphics: TextGUIGraphics = DefaultTextGUIGraphics(this, textImage.newTextGraphics())
				var contentOffset = TerminalPosition.TOP_LEFT_CORNER
				if (!window.hints.contains(Window.Hint.NO_DECORATIONS)) {
					val decorationRenderer = windowManager.getWindowDecorationRenderer(window)
					windowGraphics = decorationRenderer.draw(this, windowGraphics, window)
					contentOffset = decorationRenderer.getOffset(window)
				}

				window.draw(windowGraphics)
				window.setContentOffset(contentOffset)
				Borders.joinLinesWithFrame(windowGraphics)

				graphics.drawImage(window.position, textImage)

				if (!window.hints.contains(Window.Hint.NO_POST_RENDERING)) {
					if (window.postRenderer != null) {
						window.postRenderer.postRender(graphics, this, window)
					} else if (windowPostRenderer != null) {
						windowPostRenderer.postRender(graphics, this, window)
					} else if (theme!!.windowPostRenderer != null) {
						theme!!.windowPostRenderer.postRender(graphics, this, window)
					}
				}
			}
		}

		// Purge the render buffer cache from windows that have been removed
		windowRenderBufferCache.keys.retainAll(windows)
	}

	private fun drawBackgroundPane(graphics: TextGUIGraphics) {
		backgroundPane.draw(DefaultTextGUIGraphics(this, graphics))
	}

	@Synchronized public override fun handleInput(keyStroke: KeyStroke): Boolean {
		val activeWindow = activeWindow
		return activeWindow?.handleInput(keyStroke) ?: backgroundPane.handleInput(keyStroke)
	}

	@Synchronized override fun addWindow(window: Window): WindowBasedTextGUI {
		//To protect against NPE if the user forgot to set a content component
		if (window.component == null) {
			window.component = EmptySpace(TerminalSize.ONE)
		}

		if (window.textGUI != null) {
			window.textGUI.removeWindow(window)
		}
		window.textGUI = this
		windowManager.onAdded(this, window, windows)
		if (!windows.contains(window)) {
			windows.add(window)
		}
		if (!window.hints.contains(Window.Hint.NO_FOCUS)) {
			setActiveWindow(window)
		}
		invalidate()
		return this
	}

	override fun addWindowAndWait(window: Window): WindowBasedTextGUI {
		addWindow(window)
		window.waitUntilClosed()
		return this
	}

	@Synchronized override fun removeWindow(window: Window): WindowBasedTextGUI {
		if (!windows.remove(window)) {
			//Didn't contain this window
			return this
		}
		window.textGUI = null
		windowManager.onRemoved(this, window, windows)
		changeWindow@ if (activeWindow === window) {
			//Go backward in reverse and find the first suitable window
			for (index in windows.indices.reversed()) {
				val candidate = windows[index]
				if (!candidate.hints.contains(Window.Hint.NO_FOCUS)) {
					setActiveWindow(candidate)
					break@changeWindow
				}
			}
			// No suitable window was found, so pass control back
			// to the background pane
			setActiveWindow(null)
		}
		invalidate()
		return this
	}

	override fun waitForWindowToClose(window: Window) {
		while (window.textGUI != null) {
			var sleep = true
			val guiThread = guiThread
			if (Thread.currentThread() === guiThread.thread) {
				try {
					sleep = !guiThread.processEventsAndUpdate()
				} catch (ignore: EOFException) {
					//The GUI has closed so allow exit
					break
				} catch (e: IOException) {
					throw RuntimeException("Unexpected IOException while waiting for window to close", e)
				}

			}
			if (sleep) {
				try {
					Thread.sleep(1)
				} catch (ignore: InterruptedException) {
				}

			}
		}
	}

	@Synchronized override fun getWindows(): Collection<Window> {
		return Collections.unmodifiableList(ArrayList(windows))
	}

	@Synchronized override fun setActiveWindow(activeWindow: Window?): MultiWindowTextGUI {
		this.activeWindow = activeWindow
		if (activeWindow != null) moveToTop(activeWindow)
		return this
	}

	@Synchronized override fun moveToTop(window: Window): WindowBasedTextGUI {
		if (!windows.contains(window)) {
			throw IllegalArgumentException("Window " + window + " isn't in MultiWindowTextGUI " + this)
		}
		windows.remove(window)
		windows.add(window)
		invalidate()
		return this
	}

	/**
	 * Switches the active window by cyclically shuffling the window list. If `reverse` parameter is `false`
	 * then the current top window is placed at the bottom of the stack and the window immediately behind it is the new
	 * top. If `reverse` is set to `true` then the window at the bottom of the stack is moved up to the
	 * front and the previous top window will be immediately below it
	 * @param reverse Direction to cycle through the windows
	 * @return Itself
	 */
	@Synchronized override fun cycleActiveWindow(reverse: Boolean): WindowBasedTextGUI {
		if (windows.isEmpty() || windows.size == 1 || activeWindow != null && activeWindow!!.hints.contains(Window.Hint.MODAL)) {
			return this
		}
		val originalActiveWindow = activeWindow
		var nextWindow: Window
		if (activeWindow == null) {
			// Cycling out of active background pane
			nextWindow = if (reverse) windows[windows.size - 1] else windows[0]
		} else {
			// Switch to the next window
			nextWindow = getNextWindow(reverse, activeWindow)
		}

		var noFocusWindows = 0
		while (nextWindow.hints.contains(Window.Hint.NO_FOCUS)) {
			++noFocusWindows
			if (noFocusWindows == windows.size) {
				// All windows are NO_FOCUS, so give up
				return this
			}
			nextWindow = getNextWindow(reverse, nextWindow)
			if (nextWindow === originalActiveWindow) {
				return this
			}
		}

		if (reverse) {
			moveToTop(nextWindow)
		} else if (originalActiveWindow != null) {
			windows.remove(originalActiveWindow)
			windows.add(0, originalActiveWindow)
		}
		setActiveWindow(nextWindow)
		return this
	}

	private fun getNextWindow(reverse: Boolean, window: Window): Window {
		var index = windows.indexOf(window)
		if (reverse) {
			if (++index >= windows.size) {
				index = 0
			}
		} else {
			if (--index < 0) {
				index = windows.size - 1
			}
		}
		return windows[index]
	}
}
/**
 * Creates a new `MultiWindowTextGUI` that uses the specified `Screen` as the backend for all drawing
 * operations. The screen will be automatically wrapped in a `VirtualScreen` in order to deal with GUIs
 * becoming too big to fit the terminal. The background area of the GUI will be solid blue.
 * @param screen Screen to use as the backend for drawing operations
 */
/**
 * Creates a new `MultiWindowTextGUI` that uses the specified `Screen` as the backend for all drawing
 * operations. The screen will be automatically wrapped in a `VirtualScreen` in order to deal with GUIs
 * becoming too big to fit the terminal. The background area of the GUI will be solid blue
 * @param guiThreadFactory Factory implementation to use when creating the `TextGUIThread`
 * @param screen Screen to use as the backend for drawing operations
 */
