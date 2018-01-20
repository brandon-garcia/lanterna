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
package com.googlecode.lanterna.terminal.virtual

import com.googlecode.lanterna.terminal.Terminal
import com.googlecode.lanterna.terminal.TerminalResizeListener

/**
 * Listener class for [VirtualTerminal] that allows you to receive callbacks on certain events. Please note that
 * while this extends [TerminalResizeListener] and can be attached to a [VirtualTerminal] through
 * [com.googlecode.lanterna.terminal.Terminal.addResizeListener], in that case only the
 * resize event will fire on the listener.
 */
interface VirtualTerminalListener : TerminalResizeListener {
	/**
	 * Called when the [Terminal.flush] method is invoked on the [VirtualTerminal]
	 */
	fun onFlush()

	/**
	 * Called when the [Terminal.bell] method is invoked on the [VirtualTerminal]
	 */
	fun onBell()

	/**
	 * Called when the [Terminal.close] method is invoked on the [VirtualTerminal]
	 */
	fun onClose()
}
