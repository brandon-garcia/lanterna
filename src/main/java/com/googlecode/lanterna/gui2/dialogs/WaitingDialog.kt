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
package com.googlecode.lanterna.gui2.dialogs

import com.googlecode.lanterna.gui2.*

/**
 * Dialog that displays a text message, an optional spinning indicator and an optional progress bar. There is no buttons
 * in this dialog so it has to be explicitly closed through code.
 * @author martin
 */
class WaitingDialog private constructor(title: String, text: String) : DialogWindow(title) {
	init {

		val mainPanel = Panels.horizontal(
			Label(text),
			AnimatedLabel.createClassicSpinningLine())
		component = mainPanel
	}

	override fun showDialog(textGUI: WindowBasedTextGUI): Any? {
		showDialog(textGUI, true)
		return null
	}

	/**
	 * Displays the waiting dialog and optionally blocks until another thread closes it
	 * @param textGUI GUI to add the dialog to
	 * @param blockUntilClosed If `true`, the method call will block until another thread calls `close()` on
	 * the dialog, otherwise the method call returns immediately
	 */
	fun showDialog(textGUI: WindowBasedTextGUI, blockUntilClosed: Boolean) {
		textGUI.addWindow(this)

		if (blockUntilClosed) {
			//Wait for the window to close, in case the window manager doesn't honor the MODAL hint
			waitUntilClosed()
		}
	}

	companion object {

		/**
		 * Creates a new waiting dialog
		 * @param title Title of the waiting dialog
		 * @param text Text to display on the waiting dialog
		 * @return Created waiting dialog
		 */
		fun createDialog(title: String, text: String): WaitingDialog {
			return WaitingDialog(title, text)
		}

		/**
		 * Creates and displays a waiting dialog without blocking for it to finish
		 * @param textGUI GUI to add the dialog to
		 * @param title Title of the waiting dialog
		 * @param text Text to display on the waiting dialog
		 * @return Created waiting dialog
		 */
		fun showDialog(textGUI: WindowBasedTextGUI, title: String, text: String): WaitingDialog {
			val waitingDialog = createDialog(title, text)
			waitingDialog.showDialog(textGUI, false)
			return waitingDialog
		}
	}
}
