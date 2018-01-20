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

/**
 * Utility class for quickly bunching up components in a panel, arranged in a particular pattern
 * @author Martin
 */
object Panels {

	/**
	 * Creates a new `Panel` with a `LinearLayout` layout manager in horizontal mode and adds all the
	 * components passed in
	 * @param components Components to be added to the new `Panel`, in order
	 * @return The new `Panel`
	 */
	fun horizontal(vararg components: Component): Panel {
		val panel = Panel()
		panel.layoutManager = LinearLayout(Direction.HORIZONTAL)
		for (component in components) {
			panel.addComponent(component)
		}
		return panel
	}

	/**
	 * Creates a new `Panel` with a `LinearLayout` layout manager in vertical mode and adds all the
	 * components passed in
	 * @param components Components to be added to the new `Panel`, in order
	 * @return The new `Panel`
	 */
	fun vertical(vararg components: Component): Panel {
		val panel = Panel()
		panel.layoutManager = LinearLayout(Direction.VERTICAL)
		for (component in components) {
			panel.addComponent(component)
		}
		return panel
	}

	/**
	 * Creates a new `Panel` with a `GridLayout` layout manager and adds all the components passed in
	 * @param columns Number of columns in the grid
	 * @param components Components to be added to the new `Panel`, in order
	 * @return The new `Panel`
	 */
	fun grid(columns: Int, vararg components: Component): Panel {
		val panel = Panel()
		panel.layoutManager = GridLayout(columns)
		for (component in components) {
			panel.addComponent(component)
		}
		return panel
	}
}//Cannot instantiate
