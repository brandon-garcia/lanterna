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

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Base listener interface having callback methods for events relating to [BasePane] (and [Window], which
 * extends [BasePane]) so that you can be notified by a callback when certain events happen. Assume it is the GUI
 * thread that will call these methods. You typically use this through [WindowListener] and calling
 * [Window.addWindowListener]
 */
interface BasePaneListener<T : BasePane> {
	/**
	 * Called when a user input is about to be delivered to the focused [Interactable] inside the
	 * [BasePane], but before it is actually delivered. You can catch it and prevent it from being passed into
	 * the component by using the `deliverEvent` parameter and setting it to `false`.
	 *
	 * @param basePane Base pane that got the input event
	 * @param keyStroke The actual input event
	 * @param deliverEvent Set to `true` automatically, if you change it to `false` it will prevent the GUI
	 * from passing the input event on to the focused [Interactable]
	 */
	fun onInput(basePane: T, keyStroke: KeyStroke, deliverEvent: AtomicBoolean)

	/**
	 * Called when a user entered some input which wasn't handled by the focused component. This allows you to catch it
	 * at a [BasePane] (or [Window]) level and prevent it from being reported to the [TextGUI] as an
	 * unhandled input event.
	 * @param basePane [BasePane] that got the input event
	 * @param keyStroke The unhandled input event
	 * @param hasBeenHandled Initially set to `false`, if you change it to `true` then the event
	 * will not be reported as an unhandled input to the [TextGUI]
	 */
	fun onUnhandledInput(basePane: T, keyStroke: KeyStroke, hasBeenHandled: AtomicBoolean)
}
