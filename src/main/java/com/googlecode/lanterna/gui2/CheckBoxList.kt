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
import com.googlecode.lanterna.graphics.ThemeDefinition
import com.googlecode.lanterna.graphics.ThemeStyle
import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.input.KeyType

import java.util.ArrayList
import java.util.concurrent.CopyOnWriteArrayList

/**
 * This is a list box implementation where each item has its own checked state that can be toggled on and off
 * @author Martin
 */
open class CheckBoxList<V>
/**
 * Creates a new `CheckBoxList` that is initially empty and has a pre-defined size that it will request. If
 * there are more items that can fit in this size, the list box will use scrollbars.
 * @param preferredSize Size the list box should request, no matter how many items it contains
 */
@JvmOverloads constructor(preferredSize: TerminalSize? = null) : AbstractListBox<V, CheckBoxList<V>>(preferredSize) {

	private val listeners: MutableList<Listener>
	private val itemStatus: MutableList<Boolean>

	/**
	 * Returns all the items in the list box that have checked state, as a list
	 * @return List of all items in the list box that has checked state on
	 */
	val checkedItems: List<V>
		@Synchronized get() {
			val result = ArrayList<V>()
			for (i in itemStatus.indices) {
				if (itemStatus[i]) {
					result.add(getItemAt(i))
				}
			}
			return result
		}

	/**
	 * Listener interface that can be attached to the `CheckBoxList` in order to be notified on user actions
	 */
	interface Listener {
		/**
		 * Called by the `CheckBoxList` when the user changes the toggle state of one item
		 * @param itemIndex Index of the item that was toggled
		 * @param checked If the state of the item is now checked, this will be `true`, otherwise `false`
		 */
		fun onStatusChanged(itemIndex: Int, checked: Boolean)
	}

	init {
		this.listeners = CopyOnWriteArrayList()
		this.itemStatus = ArrayList()
	}

	override fun createDefaultListItemRenderer(): AbstractListBox.ListItemRenderer<V, CheckBoxList<V>> {
		return CheckBoxListItemRenderer()
	}

	@Synchronized override fun clearItems(): CheckBoxList<V> {
		itemStatus.clear()
		return super.clearItems()
	}

	override fun addItem(`object`: V?): CheckBoxList<V> {
		return addItem(`object`, false)
	}

	@Synchronized override fun removeItem(index: Int): V {
		val item = super.removeItem(index)
		itemStatus.removeAt(index)
		return item
	}

	/**
	 * Adds an item to the checkbox list with an explicit checked status
	 * @param object Object to add to the list
	 * @param checkedState If `true`, the new item will be initially checked
	 * @return Itself
	 */
	@Synchronized
	fun addItem(`object`: V?, checkedState: Boolean): CheckBoxList<V> {
		itemStatus.add(checkedState)
		return super.addItem(`object`)
	}

	/**
	 * Checks if a particular item is part of the check box list and returns a boolean value depending on the toggle
	 * state of the item.
	 * @param object Object to check the status of
	 * @return If the item wasn't found in the list box, `null` is returned, otherwise `true` or
	 * `false` depending on checked state of the item
	 */
	@Synchronized
	fun isChecked(`object`: V): Boolean? {
		return if (indexOf(`object`) == -1) null else itemStatus[indexOf(`object`)]

	}

	/**
	 * Checks if a particular item is part of the check box list and returns a boolean value depending on the toggle
	 * state of the item.
	 * @param index Index of the item to check the status of
	 * @return If the index was not valid in the list box, `null` is returned, otherwise `true` or
	 * `false` depending on checked state of the item at that index
	 */
	@Synchronized
	fun isChecked(index: Int): Boolean? {
		return if (index < 0 || index >= itemStatus.size) null else itemStatus[index]

	}

	/**
	 * Programmatically sets the checked state of an item in the list box
	 * @param object Object to set the checked state of
	 * @param checked If `true`, then the item is set to checked, otherwise not
	 * @return Itself
	 */
	@Synchronized
	fun setChecked(`object`: V, checked: Boolean): CheckBoxList<V> {
		val index = indexOf(`object`)
		if (index != -1) {
			setChecked(index, checked)
		}
		return self()
	}

	private fun setChecked(index: Int, checked: Boolean) {
		itemStatus[index] = checked
		runOnGUIThreadIfExistsOtherwiseRunDirect {
			for (listener in listeners) {
				listener.onStatusChanged(index, checked)
			}
		}
	}

	/**
	 * Adds a new listener to the `CheckBoxList` that will be called on certain user actions
	 * @param listener Listener to attach to this `CheckBoxList`
	 * @return Itself
	 */
	@Synchronized
	fun addListener(listener: Listener?): CheckBoxList<V> {
		if (listener != null && !listeners.contains(listener)) {
			listeners.add(listener)
		}
		return this
	}

	/**
	 * Removes a listener from this `CheckBoxList` so that if it had been added earlier, it will no longer be
	 * called on user actions
	 * @param listener Listener to remove from this `CheckBoxList`
	 * @return Itself
	 */
	fun removeListener(listener: Listener): CheckBoxList<V> {
		listeners.remove(listener)
		return this
	}

	@Synchronized override fun handleKeyStroke(keyStroke: KeyStroke): Interactable.Result {
		if (keyStroke.keyType === KeyType.Enter || keyStroke.keyType === KeyType.Character && keyStroke.character == ' ') {
			if (itemStatus[selectedIndex])
				setChecked(selectedIndex, java.lang.Boolean.FALSE)
			else
				setChecked(selectedIndex, java.lang.Boolean.TRUE)
			return Interactable.Result.HANDLED
		}
		return super.handleKeyStroke(keyStroke)
	}

	/**
	 * Default renderer for this component which is used unless overridden. The checked state is drawn on the left side
	 * of the item label using a "[ ]" block filled with an X if the item has checked state on
	 * @param <V> Type of items in the [CheckBoxList]
	</V> */
	class CheckBoxListItemRenderer<V> : AbstractListBox.ListItemRenderer<V, CheckBoxList<V>>() {
		override fun getHotSpotPositionOnLine(selectedIndex: Int): Int {
			return 1
		}

		override fun getLabel(listBox: CheckBoxList<V>, index: Int, item: V?): String {
			var check = " "
			val itemStatus = listBox.itemStatus
			if (itemStatus[index])
				check = "x"

			val text = item!!.toString()
			return "[$check] $text"
		}

		override fun drawItem(graphics: TextGUIGraphics, listBox: CheckBoxList<V>, index: Int, item: V?, selected: Boolean, focused: Boolean) {
			val themeDefinition = listBox.theme.getDefinition(CheckBoxList<*>::class.java!!)
			val itemStyle: ThemeStyle
			if (selected && !focused) {
				itemStyle = themeDefinition.selected
			} else if (selected) {
				itemStyle = themeDefinition.active
			} else if (focused) {
				itemStyle = themeDefinition.insensitive
			} else {
				itemStyle = themeDefinition.normal
			}

			if (themeDefinition.getBooleanProperty("CLEAR_WITH_NORMAL", false)) {
				graphics.applyThemeStyle(themeDefinition.normal)
				graphics.fill(' ')
				graphics.applyThemeStyle(itemStyle)
			} else {
				graphics.applyThemeStyle(itemStyle)
				graphics.fill(' ')
			}

			val brackets = themeDefinition.getCharacter("LEFT_BRACKET", '[') +
				" " +
				themeDefinition.getCharacter("RIGHT_BRACKET", ']')
			if (themeDefinition.getBooleanProperty("FIXED_BRACKET_COLOR", false)) {
				graphics.applyThemeStyle(themeDefinition.preLight)
				graphics.putString(0, 0, brackets)
				graphics.applyThemeStyle(itemStyle)
			} else {
				graphics.putString(0, 0, brackets)
			}

			val text = (item ?: "<null>").toString()
			graphics.putString(4, 0, text)

			val itemChecked = listBox.isChecked(index)!!
			val marker = themeDefinition.getCharacter("MARKER", 'x')
			if (themeDefinition.getBooleanProperty("MARKER_WITH_NORMAL", false)) {
				graphics.applyThemeStyle(themeDefinition.normal)
			}
			if (selected && focused && themeDefinition.getBooleanProperty("HOTSPOT_PRELIGHT", false)) {
				graphics.applyThemeStyle(themeDefinition.preLight)
			}
			graphics.setCharacter(1, 0, if (itemChecked) marker else ' ')
		}
	}
}
/**
 * Creates a new `CheckBoxList` that is initially empty and has no hardcoded preferred size, so it will
 * attempt to be as big as necessary to draw all items.
 */
