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
import com.googlecode.lanterna.input.KeyType

/**
 * Default implementation of Interactable that extends from AbstractComponent. If you want to write your own component
 * that is interactable, i.e. can receive keyboard (and mouse) input, you probably want to extend from this class as
 * it contains some common implementations of the methods from `Interactable` interface
 * @param <T> Should always be itself, see `AbstractComponent`
 * @author Martin
</T> */
abstract class AbstractInteractableComponent<T : AbstractInteractableComponent<T>>
/**
 * Default constructor
 */
protected constructor() : AbstractComponent<T>(), Interactable {

	override var inputFilter: InputFilter? = null
		private set
	override var isFocused: Boolean = false
		private set
	override var isEnabled: Boolean = false
		private set

	override val renderer: InteractableRenderer<T>
		get() = super.renderer as InteractableRenderer<T>

	override val isFocusable: Boolean
		get() = true

	override val cursorLocation: TerminalPosition
		get() = renderer.getCursorLocation(self())

	init {
		inputFilter = null
		isFocused = false
		isEnabled = true
	}

	override fun takeFocus(): T {
		if (!isEnabled) {
			return self()
		}
		val basePane = basePane
		if (basePane != null) {
			basePane.focusedInteractable = this
		}
		return self()
	}

	/**
	 * {@inheritDoc}
	 *
	 *
	 * This method is final in `AbstractInteractableComponent`, please override `afterEnterFocus` instead
	 */
	override fun onEnterFocus(direction: Interactable.FocusChangeDirection, previouslyInFocus: Interactable) {
		isFocused = true
		afterEnterFocus(direction, previouslyInFocus)
	}

	/**
	 * Called by `AbstractInteractableComponent` automatically after this component has received input focus. You
	 * can override this method if you need to trigger some action based on this.
	 * @param direction How focus was transferred, keep in mind this is from the previous component's point of view so
	 * if this parameter has value DOWN, focus came in from above
	 * @param previouslyInFocus Which interactable component had focus previously
	 */
	protected open fun afterEnterFocus(direction: Interactable.FocusChangeDirection, previouslyInFocus: Interactable) {
		//By default no action
	}

	/**
	 * {@inheritDoc}
	 *
	 *
	 * This method is final in `AbstractInteractableComponent`, please override `afterLeaveFocus` instead
	 */
	override fun onLeaveFocus(direction: Interactable.FocusChangeDirection, nextInFocus: Interactable) {
		isFocused = false
		afterLeaveFocus(direction, nextInFocus)
	}

	/**
	 * Called by `AbstractInteractableComponent` automatically after this component has lost input focus. You
	 * can override this method if you need to trigger some action based on this.
	 * @param direction How focus was transferred, keep in mind this is from the this component's point of view so
	 * if this parameter has value DOWN, focus is moving down to a component below
	 * @param nextInFocus Which interactable component is going to receive focus
	 */
	protected open fun afterLeaveFocus(direction: Interactable.FocusChangeDirection, nextInFocus: Interactable) {
		//By default no action
	}

	abstract override fun createDefaultRenderer(): InteractableRenderer<T>

	@Synchronized override fun setEnabled(enabled: Boolean): T {
		this.isEnabled = enabled
		if (!enabled && isFocused) {
			val basePane = basePane
			if (basePane != null) {
				basePane.focusedInteractable = null
			}
		}
		return self()
	}

	@Synchronized override fun handleInput(keyStroke: KeyStroke): Interactable.Result =
		if (inputFilter == null || inputFilter!!.onInput(this, keyStroke)) {
			handleKeyStroke(keyStroke)
		} else {
			Interactable.Result.UNHANDLED
		}

	/**
	 * This method can be overridden to handle various user input (mostly from the keyboard) when this component is in
	 * focus. The input method from the interface, `handleInput(..)` is final in
	 * `AbstractInteractableComponent` to ensure the input filter is properly handled. If the filter decides that
	 * this event should be processed, it will call this method.
	 * @param keyStroke What input was entered by the user
	 * @return Result of processing the key-stroke
	 */
	open fun handleKeyStroke(keyStroke: KeyStroke): Interactable.Result {
		// Skip the keystroke if ctrl, alt or shift was down
		if (!keyStroke.isAltDown && !keyStroke.isCtrlDown && !keyStroke.isShiftDown) {
			when (keyStroke.keyType) {
				KeyType.ArrowDown -> return Interactable.Result.MOVE_FOCUS_DOWN
				KeyType.ArrowLeft -> return Interactable.Result.MOVE_FOCUS_LEFT
				KeyType.ArrowRight -> return Interactable.Result.MOVE_FOCUS_RIGHT
				KeyType.ArrowUp -> return Interactable.Result.MOVE_FOCUS_UP
				KeyType.Tab -> return Interactable.Result.MOVE_FOCUS_NEXT
				KeyType.ReverseTab -> return Interactable.Result.MOVE_FOCUS_PREVIOUS
				KeyType.MouseEvent -> {
					basePane!!.focusedInteractable = this
					return Interactable.Result.HANDLED
				}
			}
		} else {
			return Interactable.Result.UNHANDLED
		}
	}

	@Synchronized override fun setInputFilter(inputFilter: InputFilter): T {
		this.inputFilter = inputFilter
		return self()
	}
}
