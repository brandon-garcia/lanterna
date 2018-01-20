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

import com.googlecode.lanterna.input.KeyStroke

/**
 * Container is a component that contains a collection of child components. The basic example of an implementation of
 * this is the `Panel` class which uses a layout manager to size and position the children over its area. Note
 * that there is no method for adding components to the container, since this depends on the implementation. In general,
 * composites that contains one one (or zero) children, the method for specifying the child is in `Composite`.
 * Multi-child containers are generally using the `Panel` implementation which has an `addComponent(..)`
 * method.
 * @author Martin
 */
interface Container : Component {

	/**
	 * Returns the number of children this container currently has
	 * @return Number of children currently in this container
	 */
	val childCount: Int

	/**
	 * Returns collection that is to be considered a copy of the list of children contained inside of this object.
	 * Modifying this list will not affect any internal state.
	 * @return Child-components inside of this Container
	 */
	val children: Collection<Component>

	/**
	 * Returns `true` if this container contains the supplied component either directly or indirectly through
	 * intermediate containers.
	 * @param component Component to check if it's part of this container
	 * @return `true` if the component is inside this Container, otherwise `false`
	 */
	fun containsComponent(component: Component): Boolean

	/**
	 * Removes the component from the container. This should remove the component from the Container's internal data
	 * structure as well as call the onRemoved(..) method on the component itself if it was found inside the container.
	 * @param component Component to remove from the Container
	 * @return `true` if the component existed inside the container and was removed, `false` otherwise
	 */
	fun removeComponent(component: Component): Boolean

	/**
	 * Given an interactable, find the next one in line to receive focus. If the interactable isn't inside this
	 * container, this method should return `null`.
	 *
	 * @param fromThis Component from which to get the next interactable, or if
	 * null, pick the first available interactable
	 * @return The next interactable component, or null if there are no more
	 * interactables in the list
	 */
	fun nextFocus(fromThis: Interactable): Interactable

	/**
	 * Given an interactable, find the previous one in line to receive focus. If the interactable isn't inside this
	 * container, this method should return `null`.
	 *
	 * @param fromThis Component from which to get the previous interactable,
	 * or if null, pick the last interactable in the list
	 * @return The previous interactable component, or null if there are no more
	 * interactables in the list
	 */
	fun previousFocus(fromThis: Interactable): Interactable

	/**
	 * If an interactable component inside this container received a keyboard event that wasn't handled, the GUI system
	 * will recursively send the event to each parent container to give each of them a chance to consume the event.
	 * Return `false` if the implementer doesn't care about this particular keystroke and it will be automatically
	 * sent up the hierarchy the to next container. If you return `true`, the event will stop here and won't be
	 * reported as unhandled.
	 * @param key Keystroke that was ignored by the interactable inside this container
	 * @return `true` if this event was handled by this container and shouldn't be processed anymore,
	 * `false` if the container didn't take any action on the event and want to pass it on
	 */
	fun handleInput(key: KeyStroke): Boolean

	/**
	 * Takes a lookup map and updates it with information about where all the interactables inside of this container
	 * are located.
	 * @param interactableLookupMap Interactable map to update
	 */
	fun updateLookupMap(interactableLookupMap: InteractableLookupMap)
}
