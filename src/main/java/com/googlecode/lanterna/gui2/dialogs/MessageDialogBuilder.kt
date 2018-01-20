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

import com.googlecode.lanterna.gui2.Window

import java.util.ArrayList
import java.util.Collections

/**
 * Dialog builder for the `MessageDialog` class, use this to create instances of that class and to customize
 * them
 * @author Martin
 */
class MessageDialogBuilder {
	private var title: String? = null
	private var text: String? = null
	private val buttons: MutableList<MessageDialogButton>
	private val extraWindowHints: MutableSet<Window.Hint>

	/**
	 * Default constructor
	 */
	init {
		this.title = "MessageDialog"
		this.text = "Text"
		this.buttons = ArrayList()
		this.extraWindowHints = setOf<Hint>(Window.Hint.CENTERED)
	}

	/**
	 * Builds a new `MessageDialog` from the properties in the builder
	 * @return Newly build `MessageDialog`
	 */
	fun build(): MessageDialog {
		val messageDialog = MessageDialog(
			title,
			text,
			*buttons.toTypedArray<MessageDialogButton>())
		messageDialog.setHints(extraWindowHints)
		return messageDialog
	}

	/**
	 * Sets the title of the `MessageDialog`
	 * @param title New title of the message dialog
	 * @return Itself
	 */
	fun setTitle(title: String?): MessageDialogBuilder {
		var title = title
		if (title == null) {
			title = ""
		}
		this.title = title
		return this
	}

	/**
	 * Sets the main text of the `MessageDialog`
	 * @param text Main text of the `MessageDialog`
	 * @return Itself
	 */
	fun setText(text: String?): MessageDialogBuilder {
		var text = text
		if (text == null) {
			text = ""
		}
		this.text = text
		return this
	}

	/**
	 * Assigns a set of extra window hints that you want the built dialog to have
	 * @param extraWindowHints Window hints to assign to the window in addition to the ones the builder will put
	 * @return Itself
	 */
	fun setExtraWindowHints(extraWindowHints: Set<Window.Hint>): MessageDialogBuilder {
		this.extraWindowHints.clear()
		this.extraWindowHints.addAll(extraWindowHints)
		return this
	}

	/**
	 * Adds a button to the dialog
	 * @param button Button to add to the dialog
	 * @return Itself
	 */
	fun addButton(button: MessageDialogButton?): MessageDialogBuilder {
		if (button != null) {
			buttons.add(button)
		}
		return this
	}
}
