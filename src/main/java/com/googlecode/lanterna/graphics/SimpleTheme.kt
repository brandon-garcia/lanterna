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
package com.googlecode.lanterna.graphics

import com.googlecode.lanterna.SGR
import com.googlecode.lanterna.TextColor
import com.googlecode.lanterna.gui2.*
import com.googlecode.lanterna.gui2.table.Table

import java.util.*

/**
 * Very basic implementation of [Theme] that allows you to quickly define a theme in code. It is a very simple
 * implementation that doesn't implement any intelligent fallback based on class hierarchy or package names. If a
 * particular class has not been defined with an explicit override, it will get the default theme style definition.
 *
 * @author Martin
 */
class SimpleTheme
/**
 * Creates a new [SimpleTheme] object that uses the supplied constructor arguments as the default style
 * @param foreground Color to use as the foreground unless overridden
 * @param background Color to use as the background unless overridden
 * @param styles Extra SGR styles to apply unless overridden
 */
(foreground: TextColor, background: TextColor, vararg styles: SGR) : Theme {

	@get:Synchronized override val defaultDefinition: Definition
	private val overrideDefinitions: MutableMap<Class<*>, Definition>
	@get:Synchronized override var windowPostRenderer: WindowPostRenderer? = null
		private set
	@get:Synchronized override var windowDecorationRenderer: WindowDecorationRenderer? = null
		private set

	init {
		this.defaultDefinition = Definition(Style(foreground, background, *styles))
		this.overrideDefinitions = HashMap()
		this.windowPostRenderer = null
		this.windowDecorationRenderer = null
	}

	@Synchronized override fun getDefinition(clazz: Class<*>): Definition =
		overrideDefinitions[clazz] ?: defaultDefinition

	/**
	 * Adds an override for a particular class, or overwrites a previously defined override.
	 * @param clazz Class to override the theme for
	 * @param foreground Color to use as the foreground color for this override style
	 * @param background Color to use as the background color for this override style
	 * @param styles SGR styles to apply for this override
	 * @return The newly created [Definition] that corresponds to this override.
	 */
	@Synchronized
	fun addOverride(clazz: Class<*>, foreground: TextColor, background: TextColor, vararg styles: SGR): Definition {
		val definition = Definition(Style(foreground, background, *styles))
		overrideDefinitions.put(clazz, definition)
		return definition
	}

	/**
	 * Changes the [WindowPostRenderer] this theme will return. If called with `null`, the theme returns no
	 * post renderer and the GUI system will use whatever is the default.
	 * @param windowPostRenderer Post-renderer to use along with this theme, or `null` to remove
	 * @return Itself
	 */
	@Synchronized
	fun setWindowPostRenderer(windowPostRenderer: WindowPostRenderer): SimpleTheme {
		this.windowPostRenderer = windowPostRenderer
		return this
	}

	/**
	 * Changes the [WindowDecorationRenderer] this theme will return. If called with `null`, the theme
	 * returns no decoration renderer and the GUI system will use whatever is the default.
	 * @param windowDecorationRenderer Decoration renderer to use along with this theme, or `null` to remove
	 * @return Itself
	 */
	@Synchronized
	fun setWindowDecorationRenderer(windowDecorationRenderer: WindowDecorationRenderer): SimpleTheme {
		this.windowDecorationRenderer = windowDecorationRenderer
		return this
	}

	interface RendererProvider<T : Component> {
		fun getRenderer(type: Class<T>): ComponentRenderer<T>
	}

	/**
	 * Internal class inside [SimpleTheme] used to allow basic editing of the default style and the optional
	 * overrides.
	 */
	class Definition private constructor(@get:Synchronized override val normal: ThemeStyle) : ThemeDefinition {
		private var preLight: ThemeStyle? = null
		private var selected: ThemeStyle? = null
		private var active: ThemeStyle? = null
		private var insensitive: ThemeStyle? = null
		private val customStyles: MutableMap<String, ThemeStyle>
		private val properties: Properties
		private val characterMap: MutableMap<String, Char>
		private val componentRendererMap: MutableMap<Class<*>, RendererProvider<*>>
		@get:Synchronized override var isCursorVisible: Boolean = false
			private set

		init {
			this.preLight = null
			this.selected = null
			this.active = null
			this.insensitive = null
			this.customStyles = HashMap()
			this.properties = Properties()
			this.characterMap = HashMap()
			this.componentRendererMap = HashMap()
			this.isCursorVisible = true
		}

		@Synchronized override fun getPreLight(): ThemeStyle =
			if (preLight == null) {
				normal
			} else preLight

		/**
		 * Sets the theme definition style "prelight"
		 * @param foreground Foreground color for this style
		 * @param background Background color for this style
		 * @param styles SGR styles to use
		 * @return Itself
		 */
		@Synchronized
		fun setPreLight(foreground: TextColor, background: TextColor, vararg styles: SGR): Definition {
			this.preLight = Style(foreground, background, *styles)
			return this
		}

		@Synchronized override fun getSelected(): ThemeStyle =
			if (selected == null) {
				normal
			} else selected

		/**
		 * Sets the theme definition style "selected"
		 * @param foreground Foreground color for this style
		 * @param background Background color for this style
		 * @param styles SGR styles to use
		 * @return Itself
		 */
		@Synchronized
		fun setSelected(foreground: TextColor, background: TextColor, vararg styles: SGR): Definition {
			this.selected = Style(foreground, background, *styles)
			return this
		}

		@Synchronized override fun getActive(): ThemeStyle =
			if (active == null) {
				normal
			} else active

		/**
		 * Sets the theme definition style "active"
		 * @param foreground Foreground color for this style
		 * @param background Background color for this style
		 * @param styles SGR styles to use
		 * @return Itself
		 */
		@Synchronized
		fun setActive(foreground: TextColor, background: TextColor, vararg styles: SGR): Definition {
			this.active = Style(foreground, background, *styles)
			return this
		}

		@Synchronized override fun getInsensitive(): ThemeStyle =
			if (insensitive == null) {
				normal
			} else insensitive

		/**
		 * Sets the theme definition style "insensitive"
		 * @param foreground Foreground color for this style
		 * @param background Background color for this style
		 * @param styles SGR styles to use
		 * @return Itself
		 */
		@Synchronized
		fun setInsensitive(foreground: TextColor, background: TextColor, vararg styles: SGR): Definition {
			this.insensitive = Style(foreground, background, *styles)
			return this
		}

		@Synchronized override fun getCustom(name: String) =
			customStyles[name]

		@Synchronized override fun getCustom(name: String, defaultValue: ThemeStyle) =
			customStyles[name] ?: defaultValue

		/**
		 * Adds a custom definition style to the theme using the supplied name. This will be returned using the matching
		 * call to [Definition.getCustom].
		 * @param name Name of the custom style
		 * @param foreground Foreground color for this style
		 * @param background Background color for this style
		 * @param styles SGR styles to use
		 * @return Itself
		 */
		@Synchronized
		fun setCustom(name: String, foreground: TextColor, background: TextColor, vararg styles: SGR): Definition {
			customStyles.put(name, Style(foreground, background, *styles))
			return this
		}

		@Synchronized override fun getBooleanProperty(name: String, defaultValue: Boolean) =
			java.lang.Boolean.parseBoolean(properties.getProperty(name, java.lang.Boolean.toString(defaultValue)))

		/**
		 * Attaches a boolean value property to this [SimpleTheme] that will be returned if calling
		 * [Definition.getBooleanProperty] with the same name.
		 * @param name Name of the property
		 * @param value Value to attach to the property name
		 * @return Itself
		 */
		@Synchronized
		fun setBooleanProperty(name: String, value: Boolean): Definition {
			properties.setProperty(name, java.lang.Boolean.toString(value))
			return this
		}

		/**
		 * Sets the value that suggests if the cursor should be visible or not (it's still up to the component renderer
		 * if it's going to honour this or not).
		 * @param cursorVisible If `true` then this theme definition would like the text cursor to be displayed,
		 * `false` if not.
		 * @return Itself
		 */
		@Synchronized
		fun setCursorVisible(cursorVisible: Boolean): Definition {
			this.isCursorVisible = cursorVisible
			return this
		}

		@Synchronized override fun getCharacter(name: String, fallback: Char) =
			characterMap[name] ?: fallback

		/**
		 * Stores a character value in this definition under a specific name. This is used to customize the appearance
		 * of certain components. It is returned with call to [Definition.getCharacter] with the
		 * same name.
		 * @param name Symbolic name for the character
		 * @param character Character to attach to the symbolic name
		 * @return Itself
		 */
		@Synchronized
		fun setCharacter(name: String, character: Char): Definition {
			characterMap.put(name, character)
			return this
		}

		@Synchronized override fun <T : Component> getRenderer(type: Class<T>): ComponentRenderer<T>? {
			val rendererProvider = componentRendererMap[type] as RendererProvider<T> ?: return null
			return rendererProvider.getRenderer(type)
		}

		/**
		 * Registered a callback to get a custom [ComponentRenderer] for a particular class. Use this to make a
		 * certain component (built-in or external) to use a custom renderer.
		 * @param type Class for which to invoke the callback and return the [ComponentRenderer]
		 * @param rendererProvider Callback to invoke when getting a [ComponentRenderer]
		 * @param <T> Type of class
		 * @return Itself
		</T> */
		@Synchronized
		fun <T : Component> setRenderer(type: Class<T>, rendererProvider: RendererProvider<T>?): Definition {
			if (rendererProvider == null) {
				componentRendererMap.remove(type)
			} else {
				componentRendererMap.put(type, rendererProvider)
			}
			return this
		}
	}

	private class Style private constructor(@get:Synchronized override val foreground: TextColor?, @get:Synchronized override val background: TextColor?, vararg sgrs: SGR) : ThemeStyle {
		private val sgrs: EnumSet<SGR>

		override val sgRs: EnumSet<SGR>
			@Synchronized get() = EnumSet.copyOf(sgrs)

		init {
			if (foreground == null) {
				throw IllegalArgumentException("Cannot set SimpleTheme's style foreground to null")
			}
			if (background == null) {
				throw IllegalArgumentException("Cannot set SimpleTheme's style background to null")
			}
			this.sgrs = EnumSet.noneOf(SGR::class.java)
			this.sgrs.addAll(Arrays.asList(*sgrs))
		}
	}

	companion object {

		/**
		 * Helper method that will quickly setup a new theme with some sensible component overrides.
		 * @param activeIsBold Should focused components also use bold SGR style?
		 * @param baseForeground The base foreground color of the theme
		 * @param baseBackground The base background color of the theme
		 * @param editableForeground Foreground color for editable components, or editable areas of components
		 * @param editableBackground Background color for editable components, or editable areas of components
		 * @param selectedForeground Foreground color for the selection marker when a component has multiple selection states
		 * @param selectedBackground Background color for the selection marker when a component has multiple selection states
		 * @param guiBackground Background color of the GUI, if this theme is assigned to the [TextGUI]
		 * @return Assembled [SimpleTheme] using the parameters from above
		 */
		fun makeTheme(
			activeIsBold: Boolean,
			baseForeground: TextColor,
			baseBackground: TextColor,
			editableForeground: TextColor,
			editableBackground: TextColor,
			selectedForeground: TextColor,
			selectedBackground: TextColor,
			guiBackground: TextColor): SimpleTheme {

			val activeStyle = if (activeIsBold) arrayOf(SGR.BOLD) else arrayOfNulls<SGR>(0)

			val theme = SimpleTheme(baseForeground, baseBackground)
			theme.defaultDefinition.setSelected(baseBackground, baseForeground, *activeStyle)
			theme.defaultDefinition.setActive(selectedForeground, selectedBackground, *activeStyle)

			theme.addOverride(AbstractBorder::class.java, baseForeground, baseBackground)
				.setSelected(baseForeground, baseBackground, *activeStyle)
			theme.addOverride(AbstractListBox<*, *>::class.java, baseForeground, baseBackground)
				.setSelected(selectedForeground, selectedBackground, *activeStyle)
			theme.addOverride(Button::class.java, baseForeground, baseBackground)
				.setActive(selectedForeground, selectedBackground, *activeStyle)
				.setSelected(selectedForeground, selectedBackground, *activeStyle)
			theme.addOverride(CheckBox::class.java, baseForeground, baseBackground)
				.setActive(selectedForeground, selectedBackground, *activeStyle)
				.setPreLight(selectedForeground, selectedBackground, *activeStyle)
				.setSelected(selectedForeground, selectedBackground, *activeStyle)
			theme.addOverride(CheckBoxList<*>::class.java, baseForeground, baseBackground)
				.setActive(selectedForeground, selectedBackground, *activeStyle)
			theme.addOverride(ComboBox<*>::class.java, baseForeground, baseBackground)
				.setActive(editableForeground, editableBackground, *activeStyle)
				.setPreLight(editableForeground, editableBackground)
			theme.addOverride(DefaultWindowDecorationRenderer::class.java, baseForeground, baseBackground)
				.setActive(baseForeground, baseBackground, *activeStyle)
			theme.addOverride(GUIBackdrop::class.java, baseForeground, guiBackground)
			theme.addOverride(RadioBoxList<*>::class.java, baseForeground, baseBackground)
				.setActive(selectedForeground, selectedBackground, *activeStyle)
			theme.addOverride(Table<*>::class.java, baseForeground, baseBackground)
				.setActive(editableForeground, editableBackground, *activeStyle)
				.setSelected(baseForeground, baseBackground)
			theme.addOverride(TextBox::class.java, editableForeground, editableBackground)
				.setActive(editableForeground, editableBackground, *activeStyle)
				.setSelected(editableForeground, editableBackground, *activeStyle)

			theme.setWindowPostRenderer(WindowShadowRenderer())

			return theme
		}
	}
}
