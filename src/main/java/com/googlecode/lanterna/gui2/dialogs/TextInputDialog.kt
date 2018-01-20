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

import java.math.BigInteger
import java.util.regex.Pattern

/**
 * `TextInputDialog` is a modal text input dialog that prompts the user to enter a text string. The class supports
 * validation and password masking. The builder class to help setup `TextInputDialog`s is
 * `TextInputDialogBuilder`.
 */
class TextInputDialog internal constructor(
	title: String,
	description: String?,
	textBoxPreferredSize: TerminalSize,
	initialContent: String,
	private val validator: TextInputDialogResultValidator?,
	password: Boolean) : DialogWindow(title) {

	private val textBox: TextBox
	private var result: String? = null

	init {
		this.result = null
		this.textBox = TextBox(textBoxPreferredSize, initialContent)

		if (password) {
			textBox.setMask('*')
		}

		val buttonPanel = Panel()
		buttonPanel.setLayoutManager(GridLayout(2).setHorizontalSpacing(1))
		buttonPanel.addComponent(Button(LocalizedString.OK.toString()!!, Runnable { onOK() }).setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.CENTER, GridLayout.Alignment.CENTER, true, false)))
		buttonPanel.addComponent(Button(LocalizedString.Cancel.toString()!!, Runnable { onCancel() }))

		val mainPanel = Panel()
		mainPanel.setLayoutManager(
			GridLayout(1)
				.setLeftMarginSize(1)
				.setRightMarginSize(1))
		if (description != null) {
			mainPanel.addComponent(Label(description))
		}
		mainPanel.addComponent(EmptySpace(TerminalSize.ONE))
		textBox.setLayoutData(
			GridLayout.createLayoutData(
				GridLayout.Alignment.FILL,
				GridLayout.Alignment.CENTER,
				true,
				false))
			.addTo(mainPanel)
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

	private fun onOK() {
		val text = textBox.text
		if (validator != null) {
			val errorMessage = validator.validate(text)
			if (errorMessage != null) {
				MessageDialog.showMessageDialog(getTextGUI(), getTitle(), errorMessage, MessageDialogButton.OK)
				return
			}
		}
		result = text
		close()
	}

	private fun onCancel() {
		close()
	}

	override fun showDialog(textGUI: WindowBasedTextGUI): String? {
		result = null
		super.showDialog(textGUI)
		return result
	}

	companion object {

		/**
		 * Shortcut for quickly showing a `TextInputDialog`
		 * @param textGUI GUI to show the dialog on
		 * @param title Title of the dialog
		 * @param description Description of the dialog
		 * @param initialContent What content to place in the text box initially
		 * @return The string the user typed into the text box, or `null` if the dialog was cancelled
		 */
		fun showDialog(textGUI: WindowBasedTextGUI, title: String, description: String, initialContent: String): String? {
			val textInputDialog = TextInputDialogBuilder()
				.setTitle(title)
				.setDescription(description)
				.setInitialContent(initialContent)
				.build()
			return textInputDialog.showDialog(textGUI)
		}

		/**
		 * Shortcut for quickly showing a `TextInputDialog` that only accepts numbers
		 * @param textGUI GUI to show the dialog on
		 * @param title Title of the dialog
		 * @param description Description of the dialog
		 * @param initialContent What content to place in the text box initially
		 * @return The number the user typed into the text box, or `null` if the dialog was cancelled
		 */
		fun showNumberDialog(textGUI: WindowBasedTextGUI, title: String, description: String, initialContent: String): BigInteger? {
			val textInputDialog = TextInputDialogBuilder()
				.setTitle(title)
				.setDescription(description)
				.setInitialContent(initialContent)
				.setValidationPattern(Pattern.compile("[0-9]+"), "Not a number")
				.build()
			val numberString = textInputDialog.showDialog(textGUI)
			return if (numberString != null) BigInteger(numberString) else null
		}

		/**
		 * Shortcut for quickly showing a `TextInputDialog` with password masking
		 * @param textGUI GUI to show the dialog on
		 * @param title Title of the dialog
		 * @param description Description of the dialog
		 * @param initialContent What content to place in the text box initially
		 * @return The string the user typed into the text box, or `null` if the dialog was cancelled
		 */
		fun showPasswordDialog(textGUI: WindowBasedTextGUI, title: String, description: String, initialContent: String): String? {
			val textInputDialog = TextInputDialogBuilder()
				.setTitle(title)
				.setDescription(description)
				.setInitialContent(initialContent)
				.setPasswordInput(true)
				.build()
			return textInputDialog.showDialog(textGUI)
		}
	}
}
