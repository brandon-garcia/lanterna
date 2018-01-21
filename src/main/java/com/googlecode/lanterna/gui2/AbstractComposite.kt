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
import com.googlecode.lanterna.input.KeyStroke
import java.util.Collections
import javax.swing.text.StyleConstants.getComponent

/**
 * This abstract implementation contains common code for the different `Composite` implementations. A
 * `Composite` component is one that encapsulates a single component, like borders. Because of this, a
 * `Composite` can be seen as a special case of a `Container` and indeed this abstract class does in fact
 * implement the `Container` interface as well, to make the composites easier to work with internally.
 * @author martin
 * @param <T> Should always be itself, see `AbstractComponent`
</T> */
abstract class AbstractComposite<T : Container> : AbstractComponent<T>(), Composite, Container {

	private var component: Component? = null

	override val childCount: Int
		get() = if (component != null) 1 else 0

	override val children: Collection<Component>
		get() = if (component != null) {
			listOf<Component>(component)
		} else {
			emptyList<Component>()
		}

	override val isInvalid: Boolean
		get() = component != null && component!!.isInvalid

	/**
	 * Default constructor
	 */
	init {
		component = null
	}

	override fun setComponent(component: Component?) {
		val oldComponent = this.component
		if (oldComponent === component) {
			return
		}
		if (oldComponent != null) {
			removeComponent(oldComponent)
		}
		if (component != null) {
			this.component = component
			component.onAdded(this)
			component.position = TerminalPosition.TOP_LEFT_CORNER
			invalidate()
		}
	}

	override fun getComponent() =
		component

	override fun containsComponent(component: Component?) =
		component != null && component.hasParent(this)

	override fun removeComponent(component: Component): Boolean {
		if (this.component === component) {
			this.component = null
			component.onRemoved(this)
			invalidate()
			return true
		}
		return false
	}

	override fun invalidate() {
		super.invalidate()

		//Propagate
		if (component != null) {
			component!!.invalidate()
		}
	}

	override fun nextFocus(fromThis: Interactable?) =
		if (fromThis == null && getComponent() is Interactable) {
			getComponent() as Interactable?
		} else if (getComponent() is Container) {
			(getComponent() as Container).nextFocus(fromThis)
		} else {
			null
		}

	override fun previousFocus(fromThis: Interactable?) =
		if (fromThis == null && getComponent() is Interactable) {
			getComponent() as Interactable?
		} else if (getComponent() is Container) {
			(getComponent() as Container).previousFocus(fromThis)
		} else {
			null
		}

	override fun handleInput(key: KeyStroke) =
		false

	override fun updateLookupMap(interactableLookupMap: InteractableLookupMap) {
		if (getComponent() is Container) {
			(getComponent() as Container).updateLookupMap(interactableLookupMap)
		} else if (getComponent() is Interactable) {
			interactableLookupMap.add(getComponent() as Interactable?)
		}
	}
}
