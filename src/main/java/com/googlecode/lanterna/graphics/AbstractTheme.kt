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
import com.googlecode.lanterna.graphics.AbstractTheme.DefinitionImpl
import com.googlecode.lanterna.gui2.Component
import com.googlecode.lanterna.gui2.ComponentRenderer
import com.googlecode.lanterna.gui2.WindowDecorationRenderer
import com.googlecode.lanterna.gui2.WindowPostRenderer

import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Abstract [Theme] implementation that manages a hierarchical tree of theme nodes ties to Class objects.
 * Sub-classes will inherit their theme properties from super-class definitions, the java.lang.Object class is
 * considered the root of the tree and as such is the fallback for all other classes.
 *
 *
 * You normally use this class through [PropertyTheme], which is the default implementation bundled with Lanterna.
 * @author Martin
 */
abstract class AbstractTheme protected constructor(override val windowPostRenderer: WindowPostRenderer,
												   override val windowDecorationRenderer: WindowDecorationRenderer) : Theme {

	private val rootNode: ThemeTreeNode

	override val defaultDefinition: ThemeDefinition
		get() = DefinitionImpl(rootNode)

	init {

		this.rootNode = ThemeTreeNode(Any::class.java, null)

		rootNode.foregroundMap.put(STYLE_NORMAL, TextColor.ANSI.WHITE)
		rootNode.backgroundMap.put(STYLE_NORMAL, TextColor.ANSI.BLACK)
	}

	protected fun addStyle(definition: String, style: String, value: String): Boolean {
		val node = getNode(definition) ?: return false
		node.apply(style, value)
		return true
	}

	private fun getNode(definition: String?): ThemeTreeNode? {
		try {
			return if (definition == null || definition.trim { it <= ' ' }.isEmpty()) {
				getNode(Any::class.java)
			} else {
				getNode(Class.forName(definition))
			}
		} catch (e: ClassNotFoundException) {
			return null
		}

	}

	private fun getNode(definition: Class<*>): ThemeTreeNode {
		if (definition == Any::class.java) {
			return rootNode
		}
		val parent = getNode(definition.superclass)
		if (parent.childMap.containsKey(definition)) {
			return parent.childMap[definition]
		}

		val node = ThemeTreeNode(definition, parent)
		parent.childMap.put(definition, node)
		return node
	}

	override fun getDefinition(clazz: Class<*>?): ThemeDefinition {
		var clazz = clazz
		val hierarchy = LinkedList<Class<*>>()
		while (clazz != null && clazz != Any::class.java) {
			hierarchy.addFirst(clazz)
			clazz = clazz.superclass
		}

		var node = rootNode
		for (aClass in hierarchy) {
			if (node.childMap.containsKey(aClass)) {
				node = node.childMap[aClass]
			} else {
				break
			}
		}
		return DefinitionImpl(node)
	}

	/**
	 * Returns a list of redundant theme entries in this theme. A redundant entry means that it doesn't need to be
	 * specified because there is a parent node in the hierarchy which has the same property so if the redundant entry
	 * wasn't there, the parent node would be picked up and the end result would be the same.
	 * @return List of redundant theme entries
	 */
	fun findRedundantDeclarations(): List<String> {
		val result = ArrayList<String>()
		for (node in rootNode.childMap.values) {
			findRedundantDeclarations(result, node)
		}
		Collections.sort(result)
		return result
	}

	private fun findRedundantDeclarations(result: MutableList<String>, node: ThemeTreeNode) {
		for (style in node.foregroundMap.keys) {
			var formattedStyle = "[$style]"
			if (formattedStyle.length == 2) {
				formattedStyle = ""
			}
			val color = node.foregroundMap[style]
			val colorFromParent = StyleImpl(node.parent, style).foreground
			if (color == colorFromParent) {
				result.add(node.clazz.name + ".foreground" + formattedStyle)
			}
		}
		for (style in node.backgroundMap.keys) {
			var formattedStyle = "[$style]"
			if (formattedStyle.length == 2) {
				formattedStyle = ""
			}
			val color = node.backgroundMap[style]
			val colorFromParent = StyleImpl(node.parent, style).background
			if (color == colorFromParent) {
				result.add(node.clazz.name + ".background" + formattedStyle)
			}
		}
		for (style in node.sgrMap.keys) {
			var formattedStyle = "[$style]"
			if (formattedStyle.length == 2) {
				formattedStyle = ""
			}
			val sgrs = node.sgrMap[style]
			val sgrsFromParent = StyleImpl(node.parent, style).sgRs
			if (sgrs == sgrsFromParent) {
				result.add(node.clazz.name + ".sgr" + formattedStyle)
			}
		}

		for (childNode in node.childMap.values) {
			findRedundantDeclarations(result, childNode)
		}
	}

	private inner class DefinitionImpl(internal val node: ThemeTreeNode) : ThemeDefinition {

		override val normal: ThemeStyle
			get() = StyleImpl(node, STYLE_NORMAL)

		override val preLight: ThemeStyle
			get() = StyleImpl(node, STYLE_PRELIGHT)

		override val selected: ThemeStyle
			get() = StyleImpl(node, STYLE_SELECTED)

		override val active: ThemeStyle
			get() = StyleImpl(node, STYLE_ACTIVE)

		override val insensitive: ThemeStyle
			get() = StyleImpl(node, STYLE_INSENSITIVE)

		override val isCursorVisible: Boolean
			get() {
				return node.cursorVisible ?: return if (node === rootNode) {
					true
				} else {
					DefinitionImpl(node.parent).isCursorVisible
				}
			}

		override fun getCustom(name: String): ThemeStyle =
			StyleImpl(node, name)

		override fun getCustom(name: String, defaultValue: ThemeStyle): ThemeStyle {
			var customStyle: ThemeStyle? = getCustom(name)
			if (customStyle == null) {
				customStyle = defaultValue
			}
			return customStyle
		}

		override fun getCharacter(name: String, fallback: Char): Char {
			return node.characterMap[name] ?: return if (node === rootNode) {
				fallback
			} else {
				DefinitionImpl(node.parent).getCharacter(name, fallback)
			}
		}

		override fun getBooleanProperty(name: String, defaultValue: Boolean): Boolean {
			val propertyValue = node.propertyMap[name] ?: return if (node === rootNode) {
				defaultValue
			} else {
				DefinitionImpl(node.parent).getBooleanProperty(name, defaultValue)
			}
			return java.lang.Boolean.parseBoolean(propertyValue)
		}

		override fun <T : Component> getRenderer(type: Class<T>): ComponentRenderer<T>? {
			val rendererClass = node.renderer ?: return if (node === rootNode) {
				null
			} else {
				DefinitionImpl(node.parent).getRenderer(type)
			}
			return instanceByClassName(rendererClass) as ComponentRenderer<T>?
		}
	}

	private inner class StyleImpl private constructor(private val styleNode: ThemeTreeNode, private val name: String) : ThemeStyle {

		override val foreground: TextColor
			get() {
				var node: ThemeTreeNode? = styleNode
				while (node != null) {
					if (node.foregroundMap.containsKey(name)) {
						return node.foregroundMap[name]
					}
					node = node.parent
				}
				var fallback: TextColor? = rootNode.foregroundMap[STYLE_NORMAL]
				if (fallback == null) {
					fallback = TextColor.ANSI.WHITE
				}
				return fallback
			}

		override val background: TextColor
			get() {
				var node: ThemeTreeNode? = styleNode
				while (node != null) {
					if (node.backgroundMap.containsKey(name)) {
						return node.backgroundMap[name]
					}
					node = node.parent
				}
				var fallback: TextColor? = rootNode.backgroundMap[STYLE_NORMAL]
				if (fallback == null) {
					fallback = TextColor.ANSI.BLACK
				}
				return fallback
			}

		override val sgRs: EnumSet<SGR>
			get() {
				var node: ThemeTreeNode? = styleNode
				while (node != null) {
					if (node.sgrMap.containsKey(name)) {
						return EnumSet.copyOf(node.sgrMap[name])
					}
					node = node.parent
				}
				var fallback: EnumSet<SGR>? = rootNode.sgrMap[STYLE_NORMAL]
				if (fallback == null) {
					fallback = EnumSet.noneOf(SGR::class.java)
				}
				return EnumSet.copyOf(fallback!!)
			}
	}

	private class ThemeTreeNode private constructor(private val clazz: Class<*>, private val parent: ThemeTreeNode) {
		private val childMap: Map<Class<*>, ThemeTreeNode>
		private val foregroundMap: MutableMap<String, TextColor>
		private val backgroundMap: MutableMap<String, TextColor>
		private val sgrMap: MutableMap<String, EnumSet<SGR>>
		private val characterMap: MutableMap<String, Char>
		private val propertyMap: MutableMap<String, String>
		private var cursorVisible: Boolean? = null
		private var renderer: String? = null

		init {
			this.childMap = HashMap()
			this.foregroundMap = HashMap()
			this.backgroundMap = HashMap()
			this.sgrMap = HashMap()
			this.characterMap = HashMap()
			this.propertyMap = HashMap()
			this.cursorVisible = true
			this.renderer = null
		}

		private fun apply(style: String, value: String) {
			var value = value
			value = value.trim { it <= ' ' }
			val matcher = STYLE_FORMAT.matcher(style)
			if (!matcher.matches()) {
				throw IllegalArgumentException("Unknown style declaration: " + style)
			}
			val styleComponent = matcher.group(1)
			val group = if (matcher.groupCount() > 2) matcher.group(3) else null
			if (styleComponent.toLowerCase().trim({ it <= ' ' }) == "foreground") {
				foregroundMap.put(getCategory(group), parseValue(value))
			} else if (styleComponent.toLowerCase().trim({ it <= ' ' }) == "background") {
				backgroundMap.put(getCategory(group), parseValue(value))
			} else if (styleComponent.toLowerCase().trim({ it <= ' ' }) == "sgr") {
				sgrMap.put(getCategory(group), parseSGR(value))
			} else if (styleComponent.toLowerCase().trim({ it <= ' ' }) == "char") {
				characterMap.put(getCategory(group), if (value.isEmpty()) ' ' else value[0])
			} else if (styleComponent.toLowerCase().trim({ it <= ' ' }) == "cursor") {
				cursorVisible = java.lang.Boolean.parseBoolean(value)
			} else if (styleComponent.toLowerCase().trim({ it <= ' ' }) == "property") {
				propertyMap.put(getCategory(group), if (value.isEmpty()) null else value.trim { it <= ' ' })
			} else if (styleComponent.toLowerCase().trim({ it <= ' ' }) == "renderer") {
				renderer = if (value.trim { it <= ' ' }.isEmpty()) null else value.trim { it <= ' ' }
			} else if (styleComponent.toLowerCase().trim({ it <= ' ' }) == "postrenderer" || styleComponent.toLowerCase().trim({ it <= ' ' }) == "windowdecoration") {
				// Don't do anything with this now, we might use it later
			} else {
				throw IllegalArgumentException("Unknown style component \"$styleComponent\" in style \"$style\"")
			}
		}

		private fun parseValue(value: String) =
			TextColor.Factory.fromString(value)

		private fun parseSGR(value: String): EnumSet<SGR> {
			var value = value
			value = value.trim { it <= ' ' }
			val sgrEntries = value.split(",".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
			val sgrSet = EnumSet.noneOf<SGR>(SGR::class.java)
			for (entry in sgrEntries) {
				entry = entry.trim({ it <= ' ' }).toUpperCase()
				if (!entry.isEmpty()) {
					try {
						sgrSet.add(SGR.valueOf(entry))
					} catch (e: IllegalArgumentException) {
						throw IllegalArgumentException("Unknown SGR code \"" + entry + "\"", e)
					}

				}
			}
			return sgrSet
		}

		private fun getCategory(group: String?): String {
			if (group == null) {
				return STYLE_NORMAL
			}
			for (style in Arrays.asList(STYLE_ACTIVE, STYLE_INSENSITIVE, STYLE_PRELIGHT, STYLE_NORMAL, STYLE_SELECTED)) {
				if (group.toUpperCase() == style) {
					return style
				}
			}
			return group
		}
	}

	companion object {
		private val STYLE_NORMAL = ""
		private val STYLE_PRELIGHT = "PRELIGHT"
		private val STYLE_SELECTED = "SELECTED"
		private val STYLE_ACTIVE = "ACTIVE"
		private val STYLE_INSENSITIVE = "INSENSITIVE"
		private val STYLE_FORMAT = Pattern.compile("([a-zA-Z]+)(\\[([a-zA-Z0-9-_]+)])?")

		protected fun instanceByClassName(className: String?): Any? {
			if (className == null || className.trim { it <= ' ' }.isEmpty()) {
				return null
			}
			try {
				return Class.forName(className).newInstance()
			} catch (e: InstantiationException) {
				throw RuntimeException(e)
			} catch (e: IllegalAccessException) {
				throw RuntimeException(e)
			} catch (e: ClassNotFoundException) {
				throw RuntimeException(e)
			}

		}
	}
}
