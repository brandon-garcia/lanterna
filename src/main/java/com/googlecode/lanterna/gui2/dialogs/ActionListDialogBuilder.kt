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

import java.util.ArrayList
import java.util.Arrays

/**
 * Dialog builder for the `ActionListDialog` class, use this to create instances of that class and to customize
 * them
 * @author Martin
 */
class ActionListDialogBuilder : AbstractDialogBuilder<ActionListDialogBuilder, ActionListDialog>("ActionListDialogBuilder") {

	private val actions: MutableList<Runnable>
	private var listBoxSize: TerminalSize? = null
	private var canCancel: Boolean = false
	private var closeAutomatically: Boolean = false

	/**
	 * Default constructor
	 */
	init {
		this.listBoxSize = null
		this.canCancel = true
		this.closeAutomatically = true
		this.actions = ArrayList()
	}

	override fun self(): ActionListDialogBuilder {
		return this
	}

	override fun buildDialog(): ActionListDialog {
		return ActionListDialog(
			title,
			description,
			listBoxSize,
			canCancel,
			closeAutomatically,
			actions)
	}

	/**
	 * Sets the size of the internal `ActionListBox` in columns and rows, forcing scrollbars to appear if the
	 * space isn't big enough to contain all the items
	 * @param listBoxSize Size of the `ActionListBox`
	 * @return Itself
	 */
	fun setListBoxSize(listBoxSize: TerminalSize): ActionListDialogBuilder {
		this.listBoxSize = listBoxSize
		return this
	}

	/**
	 * Returns the specified size of the internal `ActionListBox` or `null` if there is no size and the list
	 * box will attempt to take up enough size to draw all items
	 * @return Specified size of the internal `ActionListBox` or `null` if there is no size
	 */
	fun getListBoxSize(): TerminalSize? {
		return listBoxSize
	}

	/**
	 * Sets if the dialog can be cancelled or not (default: `true`)
	 * @param canCancel If `true`, the user has the option to cancel the dialog, if `false` there is no such
	 * button in the dialog
	 * @return Itself
	 */
	fun setCanCancel(canCancel: Boolean): ActionListDialogBuilder {
		this.canCancel = canCancel
		return this
	}

	/**
	 * Returns `true` if the dialog can be cancelled once it's opened
	 * @return `true` if the dialog can be cancelled once it's opened
	 */
	fun isCanCancel(): Boolean {
		return canCancel
	}

	/**
	 * Adds an additional action to the `ActionListBox` that is to be displayed when the dialog is opened
	 * @param label Label of the new action
	 * @param action Action to perform if the user selects this item
	 * @return Itself
	 */
	fun addAction(label: String, action: Runnable): ActionListDialogBuilder {
		return addAction(object : Runnable {
			override fun toString(): String {
				return label
			}

			override fun run() {
				action.run()
			}
		})
	}

	/**
	 * Adds an additional action to the `ActionListBox` that is to be displayed when the dialog is opened. The
	 * label of this item will be derived by calling `toString()` on the runnable
	 * @param action Action to perform if the user selects this item
	 * @return Itself
	 */
	fun addAction(action: Runnable): ActionListDialogBuilder {
		this.actions.add(action)
		return this
	}

	/**
	 * Adds additional actions to the `ActionListBox` that is to be displayed when the dialog is opened. The
	 * label of the items will be derived by calling `toString()` on each runnable
	 * @param actions Items to add to the `ActionListBox`
	 * @return Itself
	 */
	fun addActions(vararg actions: Runnable): ActionListDialogBuilder {
		this.actions.addAll(Arrays.asList(*actions))
		return this
	}

	/**
	 * Returns a copy of the internal list of actions currently inside this builder that will be assigned to the
	 * `ActionListBox` in the dialog when built
	 * @return Copy of the internal list of actions currently inside this builder
	 */
	fun getActions(): List<Runnable> {
		return ArrayList(actions)
	}

	/**
	 * Sets if clicking on an action automatically closes the dialog after the action is finished (default: `true`)
	 * @param closeAutomatically if `true` dialog will be automatically closed after choosing and finish any of the action
	 * @return Itself
	 */
	fun setCloseAutomaticallyOnAction(closeAutomatically: Boolean): ActionListDialogBuilder {
		this.closeAutomatically = closeAutomatically
		return this
	}
}
