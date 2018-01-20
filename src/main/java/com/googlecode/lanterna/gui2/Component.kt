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
import com.googlecode.lanterna.graphics.ThemeDefinition

/**
 * This is the main interface defining a component in Lanterna, although you will probably not implement this directly
 * but rather extend the `AbstractComponent` or another one of the sub-classes instead to avoid implementing most
 * of the methods in this interface.
 * @author Martin
 */
interface Component : TextGUIElement {
	/**
	 * Returns the top-left corner of this component, measured from its parent.
	 * @return Position of this component
	 */
	val position: TerminalPosition

	/**
	 * Returns how large this component is. If the layout manager has not yet laid this component out, it will return
	 * an empty size (0x0)
	 * @return How large this component is
	 */
	val size: TerminalSize

	/**
	 * Returns the ideal size this component would like to have, in order to draw itself properly. There are no
	 * guarantees the GUI system will decide to give it this size though.
	 * @return Size we would like to be
	 */
	val preferredSize: TerminalSize

	/**
	 * Returns the layout data associated with this component. This data will optionally be used by the layout manager,
	 * see the documentation for each layout manager for more details on valid values and their meaning.
	 * @return This component's layout data
	 */
	val layoutData: LayoutData

	/**
	 * Returns the container which is holding this container, or `null` if it's not assigned to anything.
	 * @return Parent container or null
	 */
	val parent: Container

	/**
	 * Returns the TextGUI that this component is currently part of. If the component hasn't been added to any container
	 * or in any other way placed into a GUI system, this method will return null.
	 * @return The TextGUI that this component belongs to, or null if none
	 */
	val textGUI: TextGUI

	/**
	 * Returns the [Theme] this component should be rendered using. The default implementation through
	 * [AbstractComponent] will retrieve this from the [Window] the component belongs to, or return the
	 * default theme if the component has not been added to a window yet. You can override the theme this component is
	 * assigned to by calling [.setTheme].
	 * @return The currently active [Theme] for this component
	 */
	val theme: Theme

	/**
	 * Returns the [ThemeDefinition] defined in the current [Theme] for this component class. The is the
	 * same as calling:
	 * <pre>
	 * component.getTheme().getThemeDefinition(ComponentClassType.class);
	 * // i.e button.getTheme().getThemeDefinition(Button.class);
	</pre> *
	 * @return [ThemeDefinition] defined in the current [Theme] for this component class
	 */
	val themeDefinition: ThemeDefinition

	/**
	 * Returns the renderer used to draw this component and measure its preferred size. You probably won't need to call
	 * this method unless you know exactly which ComponentRenderer implementation is used and you need to customize it.
	 * @return Renderer this component is using
	 */
	val renderer: ComponentRenderer<out Component>

	/**
	 * Returns the BasePane that this container belongs to. In a window-based GUI system, this will be a Window.
	 * @return The base pane this component is placed on, or `null` if none
	 */
	val basePane: BasePane

	/**
	 * This method will be called by the layout manager when it has decided where the component is to be located. If you
	 * call this method yourself, prepare for unexpected results.
	 * @param position Top-left position of the component, relative to its parent
	 * @return Itself
	 */
	fun setPosition(position: TerminalPosition): Component

	/**
	 * This method will be called by the layout manager when it has decided how large the component will be. If you call
	 * this method yourself, prepare for unexpected results.
	 * @param size Current size of the component
	 * @return Itself
	 */
	fun setSize(size: TerminalSize): Component


	/**
	 * Overrides the components preferred size calculation and makes the `getPreferredSize()` always return the
	 * value passed in here. If you call this will `null`, it will re-enable the preferred size calculation again.
	 * Please note that using this method on components that are not designed to work with arbitrary sizes make have
	 * unexpected behaviour.
	 * @param explicitPreferredSize Preferred size we want to use for this component
	 * @return Itself
	 */
	fun setPreferredSize(explicitPreferredSize: TerminalSize): Component

	/**
	 * Sets optional layout data associated with this component. This meaning of this data is up to the layout manager
	 * to figure out, see each layout manager for examples of how to use it.
	 * @param data Layout data associated with this component
	 * @return Itself
	 */
	fun setLayoutData(data: LayoutData): Component

	/**
	 * Returns `true` if the supplied Container is either the direct or indirect Parent of this component.
	 * @param parent Container to test if it's the parent or grand-parent of this component
	 * @return `true` if the container is either the direct or indirect parent of this component, otherwise `false`
	 */
	fun hasParent(parent: Container): Boolean

	/**
	 * Overrides the [Theme] this component will use so rather than deriving the theme from either the window or
	 * the GUI system, it will always return this theme. If you call this with `null`, it remove the override and
	 * the next call to [.getTheme] will again try to derive the theme by looking at the window or the GUI
	 * system.
	 * @param theme [Theme] to assign to this component, or `null` to use whatever the window uses
	 * @return Itself
	 */
	fun setTheme(theme: Theme): Component

	/**
	 * Returns true if this component is inside of the specified Container. It might be a direct child or not, this
	 * method makes no difference. If `getParent()` is not the same instance as `container`, but if this
	 * method returns true, you can be sure that this component is not a direct child.
	 * @param container Container to test if this component is inside
	 * @return True if this component is contained in some way within the `container`
	 */
	fun isInside(container: Container): Boolean

	/**
	 * Marks the component as invalid and requiring to be re-drawn at next opportunity. Container components should take
	 * this as a hint to layout the child components again.
	 */
	fun invalidate()

	/**
	 * Takes a border object and moves this component inside it and then returns it again. This makes it easy to quickly
	 * wrap a component on creation, like this:
	 * <pre>
	 * container.addComponent(new Button("Test").withBorder(Borders.singleLine()));
	</pre> *
	 * @param border Border to wrap the component with
	 * @return The border with this component wrapped
	 */
	fun withBorder(border: Border): Border

	/**
	 * Translates a position local to the container to the base pane's coordinate space. For a window-based GUI, this
	 * be a coordinate in the window's coordinate space. If the component belongs to no base pane, it will return
	 * `null`.
	 * @param position Position to translate (relative to the container's top-left corner)
	 * @return Position in base pane space, or `null` if the component is an orphan
	 */
	fun toBasePane(position: TerminalPosition): TerminalPosition

	/**
	 * Translates a position local to the container to global coordinate space. This should be the absolute coordinate
	 * in the terminal screen, taking no windows or containers into account. If the component belongs to no base pane,
	 * it will return `null`.
	 * @param position Position to translate (relative to the container's top-left corner)
	 * @return Position in global (or absolute) coordinates, or `null` if the component is an orphan
	 */
	fun toGlobal(position: TerminalPosition): TerminalPosition

	/**
	 * Same as calling `panel.addComponent(thisComponent)`
	 * @param panel Panel to add this component to
	 * @return Itself
	 */
	fun addTo(panel: Panel): Component

	/**
	 * Called by the GUI system when you add a component to a container; DO NOT CALL THIS YOURSELF!
	 * @param container Container that this component was just added to
	 */
	fun onAdded(container: Container)

	/**
	 * Called by the GUI system when you remove a component from a container; DO NOT CALL THIS YOURSELF!
	 * @param container Container that this component was just removed from
	 */
	fun onRemoved(container: Container)
}
