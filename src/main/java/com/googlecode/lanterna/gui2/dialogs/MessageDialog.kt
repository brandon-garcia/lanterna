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

import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.gui2.*

/**
 * Simple message dialog that displays a message and has optional selection/confirmation buttons
 *
 * @author Martin
 */
class MessageDialog internal constructor(
	title: String,
	text: String,
	vararg buttons: MessageDialogButton) : DialogWindow(title) {

	private var result: MessageDialogButton? = null

	init {
		var buttons = buttons
		this.result = null
		if (buttons == null || buttons.size == 0) {
			buttons = arrayOf(MessageDialogButton.OK)
		}

		val buttonPanel = Panel()
		buttonPanel.setLayoutManager(GridLayout(buttons.size).setHorizontalSpacing(1))
		for (button in buttons) {
			buttonPanel.addComponent(Button(button.toString(), Runnable {
				result = button
				close()
			}))
		}

		val mainPanel = Panel()
		mainPanel.setLayoutManager(
			GridLayout(1)
				.setLeftMarginSize(1)
				.setRightMarginSize(1))
		mainPanel.addComponent(Label(text))
		mainPanel.addComponent(EmptySpace(TerminalSize.ONE))
		buttonPanel.setLayoutData(
			GridLayout.createLayoutData(
				GridLayout.Alignment.END,
				GridLayout.Alignment.CENTER,
				false,
				false))
			.addTo(mainPanel)
		component = mainPanel
	}

	/**
	 * {@inheritDoc}
	 * @param textGUI Text GUI to add the dialog to
	 * @return The selected button's enum value
	 */
	override fun showDialog(textGUI: WindowBasedTextGUI): MessageDialogButton? {
		result = null
		super.showDialog(textGUI)
		return result
	}

	companion object {

		/**
		 * Shortcut for quickly displaying a message box
		 * @param textGUI The GUI to display the message box on
		 * @param title Title of the message box
		 * @param text Main message of the message box
		 * @param buttons Buttons that the user can confirm the message box with
		 * @return Which button the user selected
		 */
		fun showMessageDialog(
			textGUI: WindowBasedTextGUI,
			title: String,
			text: String,
			vararg buttons: MessageDialogButton): MessageDialogButton? {
			val builder = MessageDialogBuilder()
				.setTitle(title)
				.setText(text)
			if (buttons.size == 0) {
				builder.addButton(MessageDialogButton.OK)
			}
			for (button in buttons) {
				builder.addButton(button)
			}
			return builder.build().showDialog(textGUI)
		}
	}
}
