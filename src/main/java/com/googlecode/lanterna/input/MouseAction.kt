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
package com.googlecode.lanterna.input

import com.googlecode.lanterna.TerminalPosition

/**
 * MouseAction, a KeyStroke in disguise, this class contains the information of a single mouse action event.
 */
class MouseAction
/**
 * Constructs a MouseAction based on an action type, a button and a location on the screen
 * @param actionType The kind of mouse event
 * @param button Which button is involved (no button = 0, left button = 1, middle (wheel) button = 2,
 * right button = 3, scroll wheel up = 4, scroll wheel down = 5)
 * @param position Where in the terminal is the mouse cursor located
 */
(
	/**
	 * Returns the mouse action type so the caller can determine which kind of action was performed.
	 * @return The action type of the mouse event
	 */
	val actionType: MouseActionType,
	/**
	 * Which button was involved in this event. Please note that for CLICK_RELEASE events, there is no button
	 * information available (getButton() will return 0). The standard xterm mapping is:
	 *
	 *  * No button = 0
	 *  * Left button = 1
	 *  * Middle (wheel) button = 2
	 *  * Right button = 3
	 *  * Wheel up = 4
	 *  * Wheel down = 5
	 *
	 * @return The button which is clicked down when this event was generated
	 */
	val button: Int,
	/**
	 * The location of the mouse cursor when this event was generated.
	 * @return Location of the mouse cursor
	 */
	val position: TerminalPosition) : KeyStroke(KeyType.MouseEvent, false, false) {

	override fun toString(): String {
		return "MouseAction{actionType=$actionType, button=$button, position=$position}"
	}
}
