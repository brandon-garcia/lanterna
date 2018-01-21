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

import com.googlecode.lanterna.TerminalTextUtils
import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.gui2.*

/**
 * Dialog that allows the user to select an item from a list
 *
 * @param <T> Type of elements in the list
 * @author Martin
</T> */
class ListSelectDialog<T> internal constructor(
	title: String,
	description: String?,
	listBoxPreferredSize: TerminalSize,
	canCancel: Boolean,
	content: List<T>) : DialogWindow(title) {
	private var result: T? = null

	init {
		this.result = null
		if (content.isEmpty()) {
			throw IllegalStateException("ListSelectDialog needs at least one item")
		}

		val listBox = ActionListBox(listBoxPreferredSize)
		for (item in content) {
			listBox.addItem(item.toString(), Runnable { onSelect(item) })
		}

		val mainPanel = Panel()
		mainPanel.setLayoutManager(
			GridLayout(1)
				.setLeftMarginSize(1)
				.setRightMarginSize(1))
		if (description != null) {
			mainPanel.addComponent(Label(description))
			mainPanel.addComponent(EmptySpace(TerminalSize.ONE))
		}
		listBox.setLayoutData(
			GridLayout.createLayoutData(
				GridLayout.Alignment.FILL,
				GridLayout.Alignment.CENTER,
				true,
				false))
			.addTo(mainPanel)
		mainPanel.addComponent(EmptySpace(TerminalSize.ONE))

		if (canCancel) {
			val buttonPanel = Panel()
			buttonPanel.setLayoutManager(GridLayout(2).setHorizontalSpacing(1))
			buttonPanel.addComponent(Button(LocalizedString.Cancel.toString()!!, Runnable { onCancel() }).setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.CENTER, GridLayout.Alignment.CENTER, true, false)))
			buttonPanel.setLayoutData(
				GridLayout.createLayoutData(
					GridLayout.Alignment.END,
					GridLayout.Alignment.CENTER,
					false,
					false))
				.addTo(mainPanel)
		}
		component = mainPanel
	}

	private fun onSelect(item: T) {
		result = item
		close()
	}

	private fun onCancel() {
		close()
	}

	/**
	 * {@inheritDoc}
	 *
	 * @param textGUI Text GUI to add the dialog to
	 * @return The item in the list that was selected or `null` if the dialog was cancelled
	 */
	override fun showDialog(textGUI: WindowBasedTextGUI): T? {
		result = null
		super.showDialog(textGUI)
		return result
	}

	companion object {

		/**
		 * Shortcut for quickly creating a new dialog
		 * @param textGUI Text GUI to add the dialog to
		 * @param title Title of the dialog
		 * @param description Description of the dialog
		 * @param items Items in the dialog
		 * @param <T> Type of items in the dialog
		 * @return The selected item or `null` if cancelled
		</T> */
		fun <T> showDialog(textGUI: WindowBasedTextGUI, title: String, description: String, vararg items: T): T? =
			showDialog<T>(textGUI, title, description, null, *items)

		/**
		 * Shortcut for quickly creating a new dialog
		 * @param textGUI Text GUI to add the dialog to
		 * @param title Title of the dialog
		 * @param description Description of the dialog
		 * @param listBoxHeight Maximum height of the list box, scrollbars will be used if there are more items
		 * @param items Items in the dialog
		 * @param <T> Type of items in the dialog
		 * @return The selected item or `null` if cancelled
		</T> */
		fun <T> showDialog(textGUI: WindowBasedTextGUI, title: String, description: String, listBoxHeight: Int, vararg items: T): T? {
			var width = 0
			for (item in items) {
				width = Math.max(width, TerminalTextUtils.getColumnWidth(item.toString()))
			}
			width += 2
			return showDialog(textGUI, title, description, TerminalSize(width, listBoxHeight), *items)
		}

		/**
		 * Shortcut for quickly creating a new dialog
		 * @param textGUI Text GUI to add the dialog to
		 * @param title Title of the dialog
		 * @param description Description of the dialog
		 * @param listBoxSize Maximum size of the list box, scrollbars will be used if the items cannot fit
		 * @param items Items in the dialog
		 * @param <T> Type of items in the dialog
		 * @return The selected item or `null` if cancelled
		</T> */
		fun <T> showDialog(textGUI: WindowBasedTextGUI, title: String, description: String, listBoxSize: TerminalSize?, vararg items: T): T? =
			ListSelectDialogBuilder<T>()
				.setTitle(title)
				.setDescription(description)
				.setListBoxSize(listBoxSize)
				.addListItems(*items)
				.build()
				.showDialog(textGUI)
	}
}
