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
import com.googlecode.lanterna.graphics.Theme
import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.input.KeyType
import com.googlecode.lanterna.input.MouseAction
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

/**
 * This abstract implementation of `BasePane` has the common code shared by all different concrete
 * implementations.
 */
abstract class AbstractBasePane<T : BasePane> protected constructor() : BasePane {
	protected val contentHolder: ContentHolder
	private val listeners: CopyOnWriteArrayList<BasePaneListener<T>>
	protected var interactableLookupMap: InteractableLookupMap
	private var focusedInteractable: Interactable? = null
	private var invalid: Boolean = false
	private var strictFocusChange: Boolean = false
	private var enableDirectionBasedMovements: Boolean = false
	@set:Synchronized override var theme: Theme? = null
		@Synchronized get() =
			if (field != null) {
				field
			} else if (textGUI != null) {
				textGUI.theme
			} else null

	override val isInvalid: Boolean
		get() = invalid || contentHolder.isInvalid

	override var component: Component?
		get() = contentHolder.getComponent()
		set(component) {
			contentHolder.component = component
		}

	override//Don't allow the component to set the cursor outside of its own boundaries
	val cursorPosition: TerminalPosition?
		get() {
			if (focusedInteractable == null) {
				return null
			}
			val position = focusedInteractable!!.cursorLocation ?: return null
			return if (position.column < 0 ||
				position.row < 0 ||
				position.column >= focusedInteractable!!.size.columns ||
				position.row >= focusedInteractable!!.size.rows) {
				null
			} else focusedInteractable!!.toBasePane(position)
		}

	protected val basePaneListeners: List<BasePaneListener<T>>
		get() = listeners

	init {
		this.contentHolder = ContentHolder()
		this.listeners = CopyOnWriteArrayList()
		this.interactableLookupMap = InteractableLookupMap(TerminalSize(80, 25))
		this.invalid = false
		this.strictFocusChange = false
		this.enableDirectionBasedMovements = true
		this.theme = null
	}

	override fun invalidate() {
		invalid = true

		//Propagate
		contentHolder.invalidate()
	}

	override fun draw(graphics: TextGUIGraphics) {
		graphics.applyThemeStyle(theme!!.getDefinition(Window::class.java!!).normal)
		graphics.fill(' ')
		contentHolder.draw(graphics)

		if (interactableLookupMap.size != graphics.size) {
			interactableLookupMap = InteractableLookupMap(graphics.size)
		} else {
			interactableLookupMap.reset()
		}
		contentHolder.updateLookupMap(interactableLookupMap)
		//interactableLookupMap.debug();
		invalid = false
	}

	override fun handleInput(key: KeyStroke): Boolean {
		// Fire events first and decide if the event should be sent to the focused component or not
		val deliverEvent = AtomicBoolean(true)
		for (listener in listeners) {
			listener.onInput(self(), key, deliverEvent)
		}
		if (!deliverEvent.get()) {
			return true
		}

		// Now try to deliver the event to the focused component
		var handled = doHandleInput(key)

		// If it wasn't handled, fire the listeners and decide what to report to the TextGUI
		if (!handled) {
			val hasBeenHandled = AtomicBoolean(false)
			for (listener in listeners) {
				listener.onUnhandledInput(self(), key, hasBeenHandled)
			}
			handled = hasBeenHandled.get()
		}
		return handled
	}

	internal abstract fun self(): T

	private fun doHandleInput(key: KeyStroke): Boolean {
		if (key.keyType === KeyType.MouseEvent) {
			val mouseAction = key as MouseAction
			val localCoordinates = fromGlobal(mouseAction.position)
			if (localCoordinates != null) {
				val interactable = interactableLookupMap.getInteractableAt(localCoordinates)
				interactable?.handleInput(key)
			}
		} else if (focusedInteractable != null) {
			var next: Interactable? = null
			var direction: Interactable.FocusChangeDirection = Interactable.FocusChangeDirection.TELEPORT //Default
			var result: Interactable.Result = focusedInteractable!!.handleInput(key)
			if (!enableDirectionBasedMovements) {
				if (result == Interactable.Result.MOVE_FOCUS_DOWN || result == Interactable.Result.MOVE_FOCUS_RIGHT) {
					result = Interactable.Result.MOVE_FOCUS_NEXT
				} else if (result == Interactable.Result.MOVE_FOCUS_UP || result == Interactable.Result.MOVE_FOCUS_LEFT) {
					result = Interactable.Result.MOVE_FOCUS_PREVIOUS
				}
			}
			when (result) {
				Interactable.Result.HANDLED -> return true
				Interactable.Result.UNHANDLED -> {
					//Filter the event recursively through all parent containers until we hit null; give the containers
					//a chance to absorb the event
					var parent: Container? = focusedInteractable!!.parent
					while (parent != null) {
						if (parent.handleInput(key)) {
							return true
						}
						parent = parent.parent
					}
					return false
				}
				Interactable.Result.MOVE_FOCUS_NEXT -> {
					next = contentHolder.nextFocus(focusedInteractable)
					if (next == null) {
						next = contentHolder.nextFocus(null)
					}
					direction = Interactable.FocusChangeDirection.NEXT
				}
				Interactable.Result.MOVE_FOCUS_PREVIOUS -> {
					next = contentHolder.previousFocus(focusedInteractable)
					if (next == null) {
						next = contentHolder.previousFocus(null)
					}
					direction = Interactable.FocusChangeDirection.PREVIOUS
				}
				Interactable.Result.MOVE_FOCUS_DOWN -> {
					next = interactableLookupMap.findNextDown(focusedInteractable)
					direction = Interactable.FocusChangeDirection.DOWN
					if (next == null && !strictFocusChange) {
						next = contentHolder.nextFocus(focusedInteractable)
						direction = Interactable.FocusChangeDirection.NEXT
					}
				}
				Interactable.Result.MOVE_FOCUS_LEFT -> {
					next = interactableLookupMap.findNextLeft(focusedInteractable)
					direction = Interactable.FocusChangeDirection.LEFT
				}
				Interactable.Result.MOVE_FOCUS_RIGHT -> {
					next = interactableLookupMap.findNextRight(focusedInteractable)
					direction = Interactable.FocusChangeDirection.RIGHT
				}
				Interactable.Result.MOVE_FOCUS_UP -> {
					next = interactableLookupMap.findNextUp(focusedInteractable)
					direction = Interactable.FocusChangeDirection.UP
					if (next == null && !strictFocusChange) {
						next = contentHolder.previousFocus(focusedInteractable)
						direction = Interactable.FocusChangeDirection.PREVIOUS
					}
				}
			}
			if (next != null) {
				setFocusedInteractable(next, direction)
			}
			return true
		}
		return false
	}

	override fun getFocusedInteractable() =
		focusedInteractable

	override fun setFocusedInteractable(toFocus: Interactable?) {
		setFocusedInteractable(toFocus,
			if (toFocus != null)
				Interactable.FocusChangeDirection.TELEPORT
			else
				Interactable.FocusChangeDirection.RESET)
	}

	protected fun setFocusedInteractable(toFocus: Interactable?, direction: Interactable.FocusChangeDirection) {
		if (focusedInteractable === toFocus) {
			return
		}
		if (toFocus != null && !toFocus.isEnabled) {
			return
		}
		if (focusedInteractable != null) {
			focusedInteractable!!.onLeaveFocus(direction, focusedInteractable)
		}
		val previous = focusedInteractable
		focusedInteractable = toFocus
		toFocus?.onEnterFocus(direction, previous)
		invalidate()
	}

	override fun setStrictFocusChange(strictFocusChange: Boolean) {
		this.strictFocusChange = strictFocusChange
	}

	override fun setEnableDirectionBasedMovements(enableDirectionBasedMovements: Boolean) {
		this.enableDirectionBasedMovements = enableDirectionBasedMovements
	}

	protected fun addBasePaneListener(basePaneListener: BasePaneListener<T>) {
		listeners.addIfAbsent(basePaneListener)
	}

	protected fun removeBasePaneListener(basePaneListener: BasePaneListener<T>) {
		listeners.remove(basePaneListener)
	}

	protected inner class ContentHolder : AbstractComposite<Container>() {
		override var component: Component?
			get
			set(component) {
				if (getComponent() === component) {
					return
				}
				setFocusedInteractable(null)
				super.setComponent(component)
				if (focusedInteractable == null && component is Interactable) {
					setFocusedInteractable(component as Interactable?)
				} else if (focusedInteractable == null && component is Container) {
					setFocusedInteractable(component.nextFocus(null))
				}
			}

		override val textGUI: TextGUI?
			get() = this@AbstractBasePane.textGUI

		override val basePane: BasePane?
			get() = this@AbstractBasePane

		override fun removeComponent(component: Component): Boolean {
			val removed = super.removeComponent(component)
			if (removed) {
				focusedInteractable = null
			}
			return removed
		}

		override fun createDefaultRenderer(): ComponentRenderer<Container> {
			return object : ComponentRenderer<Container> {
				override fun getPreferredSize(component: Container): TerminalSize {
					val subComponent = getComponent() ?: return TerminalSize.ZERO
					return subComponent.preferredSize
				}

				override fun drawComponent(graphics: TextGUIGraphics, component: Container) {
					val subComponent = getComponent() ?: return
					subComponent.draw(graphics)
				}
			}
		}

		override fun toGlobal(position: TerminalPosition) =
			this@AbstractBasePane.toGlobal(position)

		override fun toBasePane(position: TerminalPosition) =
			position
	}
}
