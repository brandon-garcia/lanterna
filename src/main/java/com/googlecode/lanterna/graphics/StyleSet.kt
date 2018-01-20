package com.googlecode.lanterna.graphics

import java.util.Arrays
import java.util.EnumSet

import com.googlecode.lanterna.SGR
import com.googlecode.lanterna.TextColor

interface StyleSet<T : StyleSet<T>> {

	/**
	 * Returns the current background color
	 * @return Current background color
	 */
	val backgroundColor: TextColor

	/**
	 * Returns the current foreground color
	 * @return Current foreground color
	 */
	val foregroundColor: TextColor

	/**
	 * Returns all the SGR codes that are currently active
	 * @return Currently active SGR modifiers
	 */
	val activeModifiers: EnumSet<SGR>

	/**
	 * Updates the current background color
	 * @param backgroundColor New background color
	 * @return Itself
	 */
	fun setBackgroundColor(backgroundColor: TextColor): T

	/**
	 * Updates the current foreground color
	 * @param foregroundColor New foreground color
	 * @return Itself
	 */
	fun setForegroundColor(foregroundColor: TextColor): T

	/**
	 * Adds zero or more modifiers to the set of currently active modifiers
	 * @param modifiers Modifiers to add to the set of currently active modifiers
	 * @return Itself
	 */
	fun enableModifiers(vararg modifiers: SGR): T

	/**
	 * Removes zero or more modifiers from the set of currently active modifiers
	 * @param modifiers Modifiers to remove from the set of currently active modifiers
	 * @return Itself
	 */
	fun disableModifiers(vararg modifiers: SGR): T

	/**
	 * Sets the active modifiers to exactly the set passed in to this method. Any previous state of which modifiers are
	 * enabled doesn't matter.
	 * @param modifiers Modifiers to set as active
	 * @return Itself
	 */
	fun setModifiers(modifiers: EnumSet<SGR>): T

	/**
	 * Removes all active modifiers
	 * @return Itself
	 */
	fun clearModifiers(): T

	/**
	 * copy colors and set of SGR codes
	 * @param source Modifiers to set as active
	 * @return Itself
	 */
	fun setStyleFrom(source: StyleSet<*>): T


	open class Set : StyleSet<Set> {
		override var foregroundColor: TextColor
			internal set
		override var backgroundColor: TextColor
			internal set
		internal var style = EnumSet.noneOf<SGR>(SGR::class.java)
		override val activeModifiers: EnumSet<SGR>
			get() = EnumSet.copyOf(style)

		constructor() {}
		constructor(source: StyleSet<*>) {
			setStyleFrom(source)
		}

		override fun setBackgroundColor(backgroundColor: TextColor): Set {
			this.backgroundColor = backgroundColor
			return this
		}

		override fun setForegroundColor(foregroundColor: TextColor): Set {
			this.foregroundColor = foregroundColor
			return this
		}

		override fun enableModifiers(vararg modifiers: SGR): Set {
			style.addAll(Arrays.asList(*modifiers))
			return this
		}

		override fun disableModifiers(vararg modifiers: SGR): Set {
			style.removeAll(Arrays.asList(*modifiers))
			return this
		}

		override fun setModifiers(modifiers: EnumSet<SGR>): Set {
			style.clear()
			style.addAll(modifiers)
			return this
		}

		override fun clearModifiers(): Set {
			style.clear()
			return this
		}

		override fun setStyleFrom(source: StyleSet<*>): Set {
			setBackgroundColor(source.backgroundColor)
			setForegroundColor(source.foregroundColor)
			setModifiers(source.activeModifiers)
			return this
		}

	}
}
