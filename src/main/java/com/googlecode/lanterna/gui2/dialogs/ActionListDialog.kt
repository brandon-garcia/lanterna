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
 * Dialog containing a multiple item action list box
 * @author Martin
 */
class ActionListDialog internal constructor(
	title: String,
	description: String?,
	actionListPreferredSize: TerminalSize,
	canCancel: Boolean,
	closeAutomatically: Boolean,
	actions: List<Runnable>) : DialogWindow(title) {

	init {
		if (actions.isEmpty()) {
			throw IllegalStateException("ActionListDialog needs at least one item")
		}

		val listBox = ActionListBox(actionListPreferredSize)
		for (action in actions) {
			listBox.addItem(action.toString(), Runnable {
				action.run()
				if (closeAutomatically) {
					close()
				}
			})
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

	private fun onCancel() {
		close()
	}

	companion object {

		/**
		 * Helper method for immediately displaying a `ActionListDialog`, the method will return when the dialog is
		 * closed
		 * @param textGUI Text GUI the dialog should be added to
		 * @param title Title of the dialog
		 * @param description Description of the dialog
		 * @param items Items in the `ActionListBox`, the label will be taken from each `Runnable` by calling
		 * `toString()` on each one
		 */
		fun showDialog(textGUI: WindowBasedTextGUI, title: String, description: String, vararg items: Runnable) {
			val actionListDialog = ActionListDialogBuilder()
				.setTitle(title)
				.setDescription(description)
				.addActions(*items)
				.build()
			actionListDialog.showDialog(textGUI)
		}
	}
}
