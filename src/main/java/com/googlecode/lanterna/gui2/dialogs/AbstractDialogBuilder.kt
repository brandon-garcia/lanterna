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

import java.util.Collections
import java.util.HashSet

/**
 * Abstract class for dialog building, containing much shared code between different kinds of dialogs
 * @param <B> The real type of the builder class
 * @param <T> Type of dialog this builder is building
 * @author Martin
</T></B> */
abstract class AbstractDialogBuilder<B, T : DialogWindow>
/**
 * Default constructor for a dialog builder
 * @param title Title to assign to the dialog
 */
(protected var title: String) {
	protected var description: String? = null
	protected var extraWindowHints: Set<Window.Hint>

	init {
		this.description = null
		this.extraWindowHints = setOf<Hint>(Window.Hint.CENTERED)
	}

	/**
	 * Changes the title of the dialog
	 * @param title New title
	 * @return Itself
	 */
	fun setTitle(title: String?): B {
		var title = title
		if (title == null) {
			title = ""
		}
		this.title = title
		return self()
	}

	/**
	 * Returns the title that the built dialog will have
	 * @return Title that the built dialog will have
	 */
	fun getTitle() =
		title

	/**
	 * Changes the description of the dialog
	 * @param description New description
	 * @return Itself
	 */
	fun setDescription(description: String): B {
		this.description = description
		return self()
	}

	/**
	 * Returns the description that the built dialog will have
	 * @return Description that the built dialog will have
	 */
	fun getDescription() =
		description

	/**
	 * Assigns a set of extra window hints that you want the built dialog to have
	 * @param extraWindowHints Window hints to assign to the window in addition to the ones the builder will put
	 * @return Itself
	 */
	fun setExtraWindowHints(extraWindowHints: Set<Window.Hint>): B {
		this.extraWindowHints = extraWindowHints
		return self()
	}

	/**
	 * Returns the list of extra window hints that will be assigned to the window when built
	 * @return List of extra window hints that will be assigned to the window when built
	 */
	fun getExtraWindowHints(): Set<Window.Hint> =
		extraWindowHints

	/**
	 * Helper method for casting this to `type` parameter `B`
	 * @return `this` as `B`
	 */
	protected abstract fun self(): B

	/**
	 * Builds the dialog according to the builder implementation
	 * @return New dialog object
	 */
	protected abstract fun buildDialog(): T

	/**
	 * Builds a new dialog following the specifications of this builder
	 * @return New dialog built following the specifications of this builder
	 */
	fun build(): T {
		val dialog = buildDialog()
		if (!extraWindowHints.isEmpty()) {
			val combinedHints = HashSet(dialog.getHints())
			combinedHints.addAll(extraWindowHints)
			dialog.setHints(combinedHints)
		}
		return dialog
	}
}
