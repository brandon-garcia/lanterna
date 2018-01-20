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

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Dialog builder for the `TextInputDialog` class, use this to create instances of that class and to customize
 * them
 * @author Martin
 */
class TextInputDialogBuilder : AbstractDialogBuilder<TextInputDialogBuilder, TextInputDialog>("TextInputDialog") {
	private var initialContent: String? = null
	private var textBoxSize: TerminalSize? = null
	private var validator: TextInputDialogResultValidator? = null
	private var passwordInput: Boolean = false

	/**
	 * Default constructor
	 */
	init {
		this.initialContent = ""
		this.textBoxSize = null
		this.validator = null
		this.passwordInput = false
	}

	override fun self(): TextInputDialogBuilder {
		return this
	}

	override fun buildDialog(): TextInputDialog {
		var size = textBoxSize
		if ((initialContent == null || initialContent!!.trim { it <= ' ' } == "") && size == null) {
			size = TerminalSize(40, 1)
		}
		return TextInputDialog(
			title,
			description,
			size,
			initialContent,
			validator,
			passwordInput)
	}

	/**
	 * Sets the initial content the dialog will have
	 * @param initialContent Initial content the dialog will have
	 * @return Itself
	 */
	fun setInitialContent(initialContent: String): TextInputDialogBuilder {
		this.initialContent = initialContent
		return this
	}

	/**
	 * Returns the initial content the dialog will have
	 * @return Initial content the dialog will have
	 */
	fun getInitialContent(): String? {
		return initialContent
	}

	/**
	 * Sets the size of the text box the dialog will have
	 * @param textBoxSize Size of the text box the dialog will have
	 * @return Itself
	 */
	fun setTextBoxSize(textBoxSize: TerminalSize): TextInputDialogBuilder {
		this.textBoxSize = textBoxSize
		return this
	}

	/**
	 * Returns the size of the text box the dialog will have
	 * @return Size of the text box the dialog will have
	 */
	fun getTextBoxSize(): TerminalSize? {
		return textBoxSize
	}

	/**
	 * Sets the validator that will be attached to the text box in the dialog
	 * @param validator Validator that will be attached to the text box in the dialog
	 * @return Itself
	 */
	fun setValidator(validator: TextInputDialogResultValidator): TextInputDialogBuilder {
		this.validator = validator
		return this
	}

	/**
	 * Returns the validator that will be attached to the text box in the dialog
	 * @return validator that will be attached to the text box in the dialog
	 */
	fun getValidator(): TextInputDialogResultValidator? {
		return validator
	}

	/**
	 * Helper method that assigned a validator to the text box the dialog will have which matches the pattern supplied
	 * @param pattern Pattern to validate the text box
	 * @param errorMessage Error message to show when the pattern doesn't match
	 * @return Itself
	 */
	fun setValidationPattern(pattern: Pattern, errorMessage: String?): TextInputDialogBuilder {
		return setValidator(TextInputDialogResultValidator { content ->
			val matcher = pattern.matcher(content)
			if (!matcher.matches()) {
				return@TextInputDialogResultValidator errorMessage ?: "Invalid input"
			}
			null
		})
	}

	/**
	 * Sets if the text box the dialog will have contains a password and should be masked (default: `false`)
	 * @param passwordInput `true` if the text box should be password masked, `false` otherwise
	 * @return Itself
	 */
	fun setPasswordInput(passwordInput: Boolean): TextInputDialogBuilder {
		this.passwordInput = passwordInput
		return this
	}

	/**
	 * Returns `true` if the text box the dialog will have contains a password and should be masked
	 * @return `true` if the text box the dialog will have contains a password and should be masked
	 */
	fun isPasswordInput(): Boolean {
		return passwordInput
	}
}
