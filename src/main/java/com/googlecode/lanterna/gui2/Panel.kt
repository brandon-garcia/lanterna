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

import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.input.KeyStroke
import java.util.ArrayList
import java.util.Collections

/**
 * This class is the basic building block for creating user interfaces, being the standard implementation of
 * `Container` that supports multiple children. A `Panel` is a component that can contain one or more
 * other components, including nested panels. The panel itself doesn't have any particular appearance and isn't
 * interactable by itself, although you can set a border for the panel and interactable components inside the panel will
 * receive input focus as expected.
 *
 * @author Martin
 */
open class Panel @JvmOverloads constructor(layoutManager: LayoutManager? = LinearLayout()) : AbstractComponent<Panel>(), Container {
	private val components: MutableList<Component>
	private var layoutManager: LayoutManager? = null
	private var cachedPreferredSize: TerminalSize? = null

	override val childCount: Int
		get() {
			synchronized(components) {
				return components.size
			}
		}

	override val children: Collection<Component>
		get() {
			synchronized(components) {
				return ArrayList(components)
			}
		}

	override val isInvalid: Boolean
		get() {
			synchronized(components) {
				for (component in components) {
					if (component.isInvalid) {
						return true
					}
				}
			}
			return super.isInvalid || layoutManager!!.hasChanged()
		}

	init {
		var layoutManager = layoutManager
		if (layoutManager == null) {
			layoutManager = AbsoluteLayout()
		}
		this.components = ArrayList()
		this.layoutManager = layoutManager
		this.cachedPreferredSize = null
	}

	/**
	 * Adds a new child component to the panel. Where within the panel the child will be displayed is up to the layout
	 * manager assigned to this panel. If the component has already been added to another panel, it will first be
	 * removed from that panel before added to this one.
	 * @param component Child component to add to this panel
	 * @return Itself
	 */
	fun addComponent(component: Component?): Panel {
		if (component == null) {
			throw IllegalArgumentException("Cannot add null component")
		}
		synchronized(components) {
			if (components.contains(component)) {
				return this
			}
			if (component.parent != null) {
				component.parent.removeComponent(component)
			}
			components.add(component)
		}
		component.onAdded(this)
		invalidate()
		return this
	}

	/**
	 * This method is a shortcut for calling:
	 * <pre>
	 * `component.setLayoutData(layoutData);
	 * panel.addComponent(component);
	` *
	</pre> *
	 * @param component Component to add to the panel
	 * @param layoutData Layout data to assign to the component
	 * @return Itself
	 */
	fun addComponent(component: Component?, layoutData: LayoutData): Panel {
		if (component != null) {
			component.layoutData = layoutData
			addComponent(component)
		}
		return this
	}

	override fun containsComponent(component: Component?): Boolean {
		return component != null && component.hasParent(this)
	}

	override fun removeComponent(component: Component?): Boolean {
		if (component == null) {
			throw IllegalArgumentException("Cannot remove null component")
		}
		synchronized(components) {
			val index = components.indexOf(component)
			if (index == -1) {
				return false
			}
			if (basePane != null && basePane!!.focusedInteractable === component) {
				basePane!!.focusedInteractable = null
			}
			components.removeAt(index)
		}
		component.onRemoved(this)
		invalidate()
		return true
	}

	/**
	 * Removes all child components from this panel
	 * @return Itself
	 */
	fun removeAllComponents(): Panel {
		synchronized(components) {
			for (component in ArrayList(components)) {
				removeComponent(component)
			}
		}
		return this
	}

	/**
	 * Assigns a new layout manager to this panel, replacing the previous layout manager assigned. Please note that if
	 * the panel is not empty at the time you assign a new layout manager, the existing components might not show up
	 * where you expect them and their layout data property might need to be re-assigned.
	 * @param layoutManager New layout manager this panel should be using
	 * @return Itself
	 */
	@Synchronized
	fun setLayoutManager(layoutManager: LayoutManager?): Panel {
		var layoutManager = layoutManager
		if (layoutManager == null) {
			layoutManager = AbsoluteLayout()
		}
		this.layoutManager = layoutManager
		invalidate()
		return this
	}

	/**
	 * Returns the layout manager assigned to this panel
	 * @return Layout manager assigned to this panel
	 */
	fun getLayoutManager(): LayoutManager? {
		return layoutManager
	}

	override fun createDefaultRenderer(): ComponentRenderer<Panel> {
		return object : ComponentRenderer<Panel> {

			override fun getPreferredSize(component: Panel): TerminalSize? {
				synchronized(components) {
					cachedPreferredSize = layoutManager!!.getPreferredSize(components)
				}
				return cachedPreferredSize
			}

			override fun drawComponent(graphics: TextGUIGraphics, component: Panel) {
				if (isInvalid) {
					layout(graphics.size)
				}

				// Reset the area
				graphics.applyThemeStyle(themeDefinition.normal)
				graphics.fill(' ')

				synchronized(components) {
					for (child in components) {
						val componentGraphics = graphics.newTextGraphics(child.position, child.size)
						child.draw(componentGraphics)
					}
				}
			}
		}
	}

	public override fun calculatePreferredSize(): TerminalSize {
		return if (cachedPreferredSize != null && !isInvalid) {
			cachedPreferredSize
		} else super.calculatePreferredSize()
	}

	override fun nextFocus(fromThis: Interactable?): Interactable? {
		var chooseNextAvailable = fromThis == null

		synchronized(components) {
			for (component in components) {
				if (chooseNextAvailable) {
					if (component is Interactable && component.isEnabled && component.isFocusable) {
						return component
					} else if (component is Container) {
						val firstInteractable = component.nextFocus(null)
						if (firstInteractable != null) {
							return firstInteractable
						}
					}
					continue
				}

				if (component === fromThis) {
					chooseNextAvailable = true
					continue
				}

				if (component is Container) {
					if (fromThis!!.isInside(component)) {
						val next = component.nextFocus(fromThis)
						if (next == null) {
							chooseNextAvailable = true
						} else {
							return next
						}
					}
				}
			}
			return null
		}
	}

	override fun previousFocus(fromThis: Interactable?): Interactable? {
		var chooseNextAvailable = fromThis == null

		val revComponents = ArrayList<Component>()
		synchronized(components) {
			revComponents.addAll(components)
		}
		Collections.reverse(revComponents)

		for (component in revComponents) {
			if (chooseNextAvailable) {
				if (component is Interactable && component.isEnabled && component.isFocusable) {
					return component
				}
				if (component is Container) {
					val lastInteractable = component.previousFocus(null)
					if (lastInteractable != null) {
						return lastInteractable
					}
				}
				continue
			}

			if (component === fromThis) {
				chooseNextAvailable = true
				continue
			}

			if (component is Container) {
				if (fromThis!!.isInside(component)) {
					val next = component.previousFocus(fromThis)
					if (next == null) {
						chooseNextAvailable = true
					} else {
						return next
					}
				}
			}
		}
		return null
	}

	override fun handleInput(key: KeyStroke): Boolean {
		return false
	}

	override fun updateLookupMap(interactableLookupMap: InteractableLookupMap) {
		synchronized(components) {
			for (component in components) {
				if (component is Container) {
					component.updateLookupMap(interactableLookupMap)
				} else if (component is Interactable && component.isEnabled && component.isFocusable) {
					interactableLookupMap.add(component)
				}
			}
		}
	}

	override fun invalidate() {
		super.invalidate()

		synchronized(components) {
			//Propagate
			for (component in components) {
				component.invalidate()
			}
		}
	}

	private fun layout(size: TerminalSize) {
		synchronized(components) {
			layoutManager!!.doLayout(size, components)
		}
	}
}
/**
 * Default constructor, creates a new panel with no child components and by default set to a vertical
 * `LinearLayout` layout manager.
 */
