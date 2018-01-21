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

import com.googlecode.lanterna.TerminalPosition
import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.input.KeyType

import java.util.*

/**
 * Abstract Window has most of the code requiring for a window to function, all concrete window implementations extends
 * from this in one way or another. You can define your own window by extending from this, as an alternative to building
 * up the GUI externally by constructing a `BasicWindow` and adding components to it.
 * @author Martin
 */
abstract class AbstractWindow
/**
 * Creates a window with a specific title that will (probably) be drawn in the window decorations
 * @param title Title of this window
 */
@JvmOverloads constructor(private var title: String? = "") : AbstractBasePane<Window>(), Window {
	private var textGUI: WindowBasedTextGUI? = null
	override var isVisible: Boolean = false
	private var lastKnownSize: TerminalSize? = null
	override var decoratedSize: TerminalSize? = null
	private var lastKnownPosition: TerminalPosition? = null
	private var contentOffset: TerminalPosition? = null
	private var hints: Set<Window.Hint>? = null
	override var postRenderer: WindowPostRenderer? = null
		private set
	private var closeWindowWithEscape: Boolean = false

	override val preferredSize: TerminalSize
		get() = contentHolder.preferredSize

	override// Fire listeners
	var position: TerminalPosition?
		get() = lastKnownPosition
		set(topLeft) {
			val oldPosition = this.lastKnownPosition
			this.lastKnownPosition = topLeft
			for (listener in basePaneListeners) {
				(listener as? WindowListener)?.onMoved(this, oldPosition, topLeft)
			}
		}

	override var size: TerminalSize?
		get() = lastKnownSize
		set(size) = setSize(size, true)

	init {
		this.textGUI = null
		this.isVisible = true
		this.contentOffset = TerminalPosition.TOP_LEFT_CORNER
		this.lastKnownPosition = null
		this.lastKnownSize = null
		this.decoratedSize = null
		this.closeWindowWithEscape = false

		this.hints = HashSet()
	}

	/**
	 * Setting this property to `true` will cause pressing the ESC key to close the window. This used to be the
	 * default behaviour of lanterna 3 during the development cycle but is not longer the case. You are encouraged to
	 * put proper buttons or other kind of components to clearly mark to the user how to close the window instead of
	 * magically taking ESC, but sometimes it can be useful (when doing testing, for example) to enable this mode.
	 * @param closeWindowWithEscape If `true`, this window will self-close if you press ESC key
	 */
	fun setCloseWindowWithEscape(closeWindowWithEscape: Boolean) {
		this.closeWindowWithEscape = closeWindowWithEscape
	}

	override fun setTextGUI(textGUI: WindowBasedTextGUI?) {
		//This is kind of stupid check, but might cause it to blow up on people using the library incorrectly instead of
		//just causing weird behaviour
		if (this.textGUI != null && textGUI != null) {
			throw UnsupportedOperationException("Are you calling setTextGUI yourself? Please read the documentation"
				+ " in that case (this could also be a bug in Lanterna, please report it if you are sure you are "
				+ "not calling Window.setTextGUI(..) from your code)")
		}
		this.textGUI = textGUI
	}

	override fun getTextGUI(): WindowBasedTextGUI? =
		textGUI

	/**
	 * Alters the title of the window to the supplied string
	 * @param title New title of the window
	 */
	fun setTitle(title: String) {
		this.title = title
		invalidate()
	}

	override fun getTitle() =
		title

	override fun draw(graphics: TextGUIGraphics) {
		if (graphics.size != lastKnownSize) {
			component!!.invalidate()
		}
		setSize(graphics.size, false)
		super.draw(graphics)
	}

	override fun handleInput(key: KeyStroke): Boolean {
		val handled = super.handleInput(key)
		if (!handled && closeWindowWithEscape && key.keyType === KeyType.Escape) {
			close()
			return true
		}
		return handled
	}

	override fun toGlobal(localPosition: TerminalPosition?) =
		if (localPosition == null) {
			null
		} else lastKnownPosition!!.withRelative(contentOffset!!.withRelative(localPosition))

	override fun fromGlobal(globalPosition: TerminalPosition?) =
		if (globalPosition == null || lastKnownPosition == null) {
			null
		} else globalPosition.withRelative(
			-lastKnownPosition!!.column - contentOffset!!.column,
			-lastKnownPosition!!.row - contentOffset!!.row)

	override fun setHints(hints: Collection<Window.Hint>) {
		this.hints = HashSet(hints)
		invalidate()
	}

	override fun getHints(): Set<Window.Hint> =
		Collections.unmodifiableSet<Hint>(hints!!)

	override fun addWindowListener(windowListener: WindowListener) {
		addBasePaneListener(windowListener)
	}

	override fun removeWindowListener(windowListener: WindowListener) {
		removeBasePaneListener(windowListener)
	}

	/**
	 * Sets the post-renderer to use for this window. This will override the default from the GUI system (if there is
	 * one set, otherwise from the theme).
	 * @param windowPostRenderer Window post-renderer to assign to this window
	 */
	fun setWindowPostRenderer(windowPostRenderer: WindowPostRenderer) {
		this.postRenderer = windowPostRenderer
	}

	private fun setSize(size: TerminalSize, invalidate: Boolean) {
		val oldSize = this.lastKnownSize
		this.lastKnownSize = size
		if (invalidate) {
			invalidate()
		}

		// Fire listeners
		for (listener in basePaneListeners) {
			(listener as? WindowListener)?.onResized(this, oldSize, size)
		}
	}

	override fun setContentOffset(offset: TerminalPosition) {
		this.contentOffset = offset
	}

	override fun close() {
		if (textGUI != null) {
			textGUI!!.removeWindow(this)
		}
		component = null
	}

	override fun waitUntilClosed() {
		val textGUI = getTextGUI()
		textGUI?.waitForWindowToClose(this)
	}

	internal override fun self(): Window =
		this
}
/**
 * Default constructor, this creates a window with no title
 */
