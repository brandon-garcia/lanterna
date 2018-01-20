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
 * Dialog builder for the `ListSelectDialog` class, use this to create instances of that class and to customize
 * them
 * @author Martin
 */
class ListSelectDialogBuilder<T> : AbstractDialogBuilder<ListSelectDialogBuilder<T>, ListSelectDialog<T>>("ListSelectDialog") {
	private val content: MutableList<T>
	private var listBoxSize: TerminalSize? = null
	private var canCancel: Boolean = false

	/**
	 * Returns a copy of the list of items in the list box
	 * @return Copy of the list of items in the list box
	 */
	val listItems: List<T>
		get() = ArrayList(content)

	/**
	 * Default constructor
	 */
	init {
		this.listBoxSize = null
		this.canCancel = true
		this.content = ArrayList()
	}

	override fun self(): ListSelectDialogBuilder<T> {
		return this
	}

	override fun buildDialog(): ListSelectDialog<T> {
		return ListSelectDialog(
			title,
			description,
			listBoxSize,
			canCancel,
			content)
	}

	/**
	 * Sets the size of the list box in the dialog, scrollbars will be used if there is not enough space to draw all
	 * items. If set to `null`, the dialog will ask for enough space to be able to draw all items.
	 * @param listBoxSize Size of the list box in the dialog
	 * @return Itself
	 */
	fun setListBoxSize(listBoxSize: TerminalSize): ListSelectDialogBuilder<T> {
		this.listBoxSize = listBoxSize
		return this
	}

	/**
	 * Size of the list box in the dialog or `null` if the dialog will ask for enough space to draw all items
	 * @return Size of the list box in the dialog or `null` if the dialog will ask for enough space to draw all items
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
	fun setCanCancel(canCancel: Boolean): ListSelectDialogBuilder<T> {
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
	 * Adds an item to the list box at the end
	 * @param item Item to add to the list box
	 * @return Itself
	 */
	fun addListItem(item: T): ListSelectDialogBuilder<T> {
		this.content.add(item)
		return this
	}

	/**
	 * Adds a list of items to the list box at the end, in the order they are passed in
	 * @param items Items to add to the list box
	 * @return Itself
	 */
	fun addListItems(vararg items: T): ListSelectDialogBuilder<T> {
		this.content.addAll(Arrays.asList(*items))
		return this
	}
}
