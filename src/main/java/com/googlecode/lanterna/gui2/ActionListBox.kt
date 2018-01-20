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
import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.input.KeyType

/**
 * This class is a list box implementation that displays a number of items that has actions associated with them. You
 * can activate this action by pressing the Enter or Space keys on the keyboard and the action associated with the
 * currently selected item will fire.
 * @author Martin
 */
class ActionListBox
/**
 * Creates a new `ActionListBox` with a pre-set size. If the items don't fit in within this size, scrollbars
 * will be used to accommodate. Calling `new ActionListBox(null)` has the same effect as calling
 * `new ActionListBox()`.
 * @param preferredSize Preferred size of this [ActionListBox]
 */
@JvmOverloads constructor(preferredSize: TerminalSize? = null) : AbstractListBox<Runnable, ActionListBox>(preferredSize) {

	override val cursorLocation: TerminalPosition?
		get() = null

	/**
	 * {@inheritDoc}
	 *
	 * The label of the item in the list box will be the result of calling `.toString()` on the runnable, which
	 * might not be what you want to have unless you explicitly declare it. Consider using
	 * `addItem(String label, Runnable action` instead, if you want to just set the label easily without having
	 * to override `.toString()`.
	 *
	 * @param object Runnable to execute when the action was selected and fired in the list
	 * @return Itself
	 */
	override fun addItem(`object`: Runnable?): ActionListBox {
		return super.addItem(`object`)
	}

	/**
	 * Adds a new item to the list, which is displayed in the list using a supplied label.
	 * @param label Label to use in the list for the new item
	 * @param action Runnable to invoke when this action is selected and then triggered
	 * @return Itself
	 */
	fun addItem(label: String, action: Runnable): ActionListBox {
		return addItem(object : Runnable {
			override fun run() {
				action.run()
			}

			override fun toString(): String {
				return label
			}
		})
	}

	override fun handleKeyStroke(keyStroke: KeyStroke): Interactable.Result {
		val selectedItem = selectedItem
		if (selectedItem != null && (keyStroke.keyType === KeyType.Enter || keyStroke.keyType === KeyType.Character && keyStroke.character == ' ')) {

			(selectedItem as Runnable).run()
			return Interactable.Result.HANDLED
		}
		return super.handleKeyStroke(keyStroke)
	}
}
/**
 * Default constructor, creates an `ActionListBox` with no pre-defined size that will request to be big enough
 * to display all items
 */
