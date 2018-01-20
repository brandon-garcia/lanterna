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
import com.googlecode.lanterna.bundle.LanternaThemes
import com.googlecode.lanterna.graphics.Theme
import com.googlecode.lanterna.graphics.ThemeDefinition

/**
 * AbstractComponent provides some good default behaviour for a `Component`, all components in Lanterna extends
 * from this class in some way. If you want to write your own component that isn't interactable or theme:able, you
 * probably want to extend from this class.
 *
 *
 * The way you want to declare your new `Component` is to pass in itself as the generic parameter, like this:
 * <pre>
 * `public class MyComponent extends AbstractComponent<MyComponent> {
 * ...
 * }
` *
</pre> *
 * This was, the component renderer will be correctly setup type-wise and you will need to do fewer typecastings when
 * you implement the drawing method your new component.
 *
 * @author Martin
 * @param <T> Should always be itself, this value will be used for the `ComponentRenderer` declaration
</T> */
abstract class AbstractComponent<T : Component> : Component {
	/**
	 * Manually set renderer
	 */
	private var overrideRenderer: ComponentRenderer<T>? = null
	/**
	 * If overrideRenderer is not set, this is used instead if not null, set by the theme
	 */
	private var themeRenderer: ComponentRenderer<T>? = null

	/**
	 * To keep track of the theme that created the themeRenderer, so we can reset it if the theme changes
	 */
	private var themeRenderersTheme: Theme? = null

	/**
	 * If the theme had nothing for this component and no override is set, this is the third fallback
	 */
	private var defaultRenderer: ComponentRenderer<T>? = null

	override var parent: Container? = null
		private set
	override var size: TerminalSize? = null
		private set
	private var explicitPreferredSize: TerminalSize? = null   //This is keeping the value set by the user (if setPreferredSize() is used)
	override var position: TerminalPosition? = null
		private set
	private var themeOverride: Theme? = null
	override var layoutData: LayoutData? = null
		private set
	override var isInvalid: Boolean = false
		private set

	override// First try the override
		// Then try to create and return a renderer from the theme
		// Check if the theme has changed
		// Finally, fallback to the default renderer
	val renderer: ComponentRenderer<T>
		@Synchronized get() {
			if (overrideRenderer != null) {
				return overrideRenderer
			}
			val currentTheme = theme
			if (themeRenderer == null && basePane != null || themeRenderer != null && currentTheme !== themeRenderersTheme) {

				themeRenderer = currentTheme.getDefinition(javaClass).getRenderer(selfClass())
				if (themeRenderer != null) {
					themeRenderersTheme = currentTheme
				}
			}
			if (themeRenderer != null) {
				return themeRenderer
			}
			if (defaultRenderer == null) {
				defaultRenderer = createDefaultRenderer()
				if (defaultRenderer == null) {
					throw IllegalStateException(javaClass + " returned a null default renderer")
				}
			}
			return defaultRenderer
		}

	override val preferredSize: TerminalSize
		get() = if (explicitPreferredSize != null) {
			explicitPreferredSize
		} else {
			calculatePreferredSize()
		}

	override val textGUI: TextGUI?
		get() = if (parent == null) {
			null
		} else parent!!.textGUI

	override val theme: Theme
		@Synchronized get() = if (themeOverride != null) {
			themeOverride
		} else if (parent != null) {
			parent!!.theme
		} else if (basePane != null) {
			basePane!!.theme
		} else {
			LanternaThemes.defaultTheme
		}

	override val themeDefinition: ThemeDefinition
		get() = theme.getDefinition(javaClass)

	override val basePane: BasePane?
		get() = if (parent == null) {
			null
		} else parent!!.basePane

	/**
	 * Default constructor
	 */
	init {
		size = TerminalSize.ZERO
		position = TerminalPosition.TOP_LEFT_CORNER
		explicitPreferredSize = null
		layoutData = null
		isInvalid = true
		parent = null
		overrideRenderer = null
		themeRenderer = null
		themeRenderersTheme = null
		defaultRenderer = null
	}

	/**
	 * When you create a custom component, you need to implement this method and return a Renderer which is responsible
	 * for taking care of sizing the component, rendering it and choosing where to place the cursor (if Interactable).
	 * This value is intended to be overridden by custom themes.
	 * @return Renderer to use when sizing and drawing this component
	 */
	protected abstract fun createDefaultRenderer(): ComponentRenderer<T>

	/**
	 * Takes a `Runnable` and immediately executes it if this is called on the designated GUI thread, otherwise
	 * schedules it for later invocation.
	 * @param runnable `Runnable` to execute on the GUI thread
	 */
	protected fun runOnGUIThreadIfExistsOtherwiseRunDirect(runnable: Runnable) {
		if (textGUI != null && textGUI!!.guiThread != null) {
			textGUI!!.guiThread.invokeLater(runnable)
		} else {
			runnable.run()
		}
	}

	/**
	 * Explicitly sets the `ComponentRenderer` to be used when drawing this component. This will override whatever
	 * the current theme is suggesting or what the default renderer is. If you call this with `null`, the override
	 * is cleared.
	 * @param renderer `ComponentRenderer` to be used when drawing this component
	 * @return Itself
	 */
	fun setRenderer(renderer: ComponentRenderer<T>): T {
		this.overrideRenderer = renderer
		return self()
	}

	override fun invalidate() {
		isInvalid = true
	}

	@Synchronized override fun setSize(size: TerminalSize): T {
		this.size = size
		return self()
	}

	@Synchronized override fun setPreferredSize(explicitPreferredSize: TerminalSize): T {
		this.explicitPreferredSize = explicitPreferredSize
		return self()
	}

	/**
	 * Invokes the component renderer's size calculation logic and returns the result. This value represents the
	 * preferred size and isn't necessarily what it will eventually be assigned later on.
	 * @return Size that the component renderer believes the component should be
	 */
	@Synchronized protected open fun calculatePreferredSize(): TerminalSize {
		return renderer.getPreferredSize(self())
	}

	@Synchronized override fun setPosition(position: TerminalPosition): T {
		this.position = position
		return self()
	}

	@Synchronized override fun draw(graphics: TextGUIGraphics) {
		//Delegate drawing the component to the renderer
		setSize(graphics.size)
		onBeforeDrawing()
		renderer.drawComponent(graphics, self())
		onAfterDrawing(graphics)
		isInvalid = false
	}

	/**
	 * This method is called just before the component's renderer is invoked for the drawing operation. You can use this
	 * hook to do some last-minute adjustments to the component, as an alternative to coding it into the renderer
	 * itself. The component should have the correct size and position at this point, if you call `getSize()` and
	 * `getPosition()`.
	 */
	protected open fun onBeforeDrawing() {
		//No operation by default
	}

	/**
	 * This method is called immediately after the component's renderer has finished the drawing operation. You can use
	 * this hook to do some post-processing if you need, as an alternative to coding it into the renderer. The
	 * `TextGUIGraphics` supplied is the same that was fed into the renderer.
	 * @param graphics Graphics object you can use to manipulate the appearance of the component
	 */
	protected fun onAfterDrawing(graphics: TextGUIGraphics) {
		//No operation by default
	}

	@Synchronized override fun setLayoutData(data: LayoutData): T {
		if (layoutData !== data) {
			layoutData = data
			invalidate()
		}
		return self()
	}

	override fun hasParent(parent: Container): Boolean {
		if (this.parent == null) {
			return false
		}
		var recursiveParent = this.parent
		while (recursiveParent != null) {
			if (recursiveParent === parent) {
				return true
			}
			recursiveParent = recursiveParent.parent
		}
		return false
	}

	@Synchronized override fun setTheme(theme: Theme): Component {
		themeOverride = theme
		invalidate()
		return this
	}

	override fun isInside(container: Container): Boolean {
		var test: Component = this
		while (test.parent != null) {
			if (test.parent === container) {
				return true
			}
			test = test.parent
		}
		return false
	}

	override fun toBasePane(position: TerminalPosition): TerminalPosition? {
		val parent = parent ?: return null
		return parent.toBasePane(position.withRelative(position))
	}

	override fun toGlobal(position: TerminalPosition): TerminalPosition? {
		val parent = parent ?: return null
		return parent.toGlobal(position.withRelative(position))
	}

	@Synchronized override fun withBorder(border: Border): Border {
		border.component = this
		return border
	}

	@Synchronized override fun addTo(panel: Panel): T {
		panel.addComponent(this)
		return self()
	}

	@Synchronized override fun onAdded(container: Container) {
		parent = container
	}

	@Synchronized override fun onRemoved(container: Container) {
		parent = null
		themeRenderer = null
	}

	/**
	 * This is a little hack to avoid doing typecasts all over the place when having to return `T`. Credit to
	 * avl42 for this one!
	 * @return Itself, but as type T
	 */
	protected fun self(): T {
		return this as T
	}

	private fun selfClass(): Class<T> {
		return javaClass as Class<T>
	}
}
