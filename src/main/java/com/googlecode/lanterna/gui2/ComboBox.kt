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

import java.util.ArrayList
import java.util.Arrays
import java.util.concurrent.CopyOnWriteArrayList

import com.googlecode.lanterna.Symbols
import com.googlecode.lanterna.TerminalPosition
import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.TerminalTextUtils
import com.googlecode.lanterna.graphics.Theme
import com.googlecode.lanterna.graphics.ThemeDefinition
import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.input.KeyType

/**
 * This is a simple combo box implementation that allows the user to select one out of multiple items through a
 * drop-down menu. If the combo box is not in read-only mode, the user can also enter free text in the combo box, much
 * like a `TextBox`.
 * @param <V> Type to use for the items in the combo box
 * @author Martin
</V> */
class ComboBox<V>
/**
 * Creates a new `ComboBox` initialized with N number of items supplied through the items parameter. The
 * initially selected item is specified through the `selectedIndex` parameter. By default 10 items will be
 * displayed at once, more than that and there will be a scroll bar.
 * @param items Items to populate the new combo box with
 * @param selectedIndex Index of the item which should be initially selected
 */
@JvmOverloads constructor(items: Collection<V>, private var selectedIndex: Int = if (items.isEmpty()) -1 else 0) : AbstractInteractableComponent<ComboBox<V>>() {

	private val items: MutableList<V>
	private val listeners: MutableList<Listener>

	private var popupWindow: PopupWindow? = null
	/**
	 * Returns the text currently displayed in the combo box, this will likely be the label of the selected item but for
	 * writable combo boxes it's also what the user has typed in
	 * @return String currently displayed in the combo box
	 */
	var text: String? = null
		private set

	private var readOnly: Boolean = false
	private var dropDownFocused: Boolean = false
	/**
	 * For writable combo boxes, this method returns the position where the text input cursor is right now. Meaning, if
	 * the user types some character, where are those are going to be inserted in the string that is currently
	 * displayed. If the text input position equals the size of the currently displayed text, new characters will be
	 * appended at the end. The user can usually move the text input position by using left and right arrow keys on the
	 * keyboard.
	 * @return Current text input position
	 */
	var textInputPosition: Int = 0
		private set
	/**
	 * Returns the number of items to display in drop down at one time, if there are more items in the model there will
	 * be a scrollbar to help the user navigate. If this returns 0, the combo box will always grow to show all items in
	 * the list, which might cause undesired effects if you put really a lot of items into the combo box.
	 *
	 * @return Number of items (rows) that will be displayed in the combo box, or 0 if the combo box will always grow to
	 * accommodate
	 */
	/**
	 * Sets the number of items to display in drop down at one time, if there are more items in the model there will
	 * be a scrollbar to help the user navigate. Use this method if your combo boxes have large models that fills up
	 * the whole screen. Set it to 0 if you don't want to limit the number.
	 * @param dropDownNumberOfRows Max number of items (rows) to display at one time in the combo box
	 */
	var dropDownNumberOfRows: Int = 0

	/**
	 * Counts and returns the number of items in this combo box
	 * @return Number of items in this combo box
	 */
	val itemCount: Int
		@Synchronized get() = items.size

	/**
	 * Returns `true` if the users input focus is currently on the drop-down button of the combo box, so that
	 * pressing enter would trigger the popup window. This is generally used by renderers only and is always true for
	 * read-only combo boxes as the component won't allow you to focus on the text in that mode.
	 * @return `true` if the input focus is on the drop-down "button" of the combo box
	 */
	val isDropDownFocused: Boolean
		get() = dropDownFocused || isReadOnly()

	/**
	 * Returns the item at the selected index, this is the same as calling:
	 * <pre>
	 * getSelectedIndex() > -1 ? getItem(getSelectedIndex()) : null
	</pre> *
	 * @return The item at the selected index
	 */
	/**
	 * Programmatically selects one item in the combo box by passing in the value the should be selected. If the value
	 * isn't in the combo box model, nothing happens for read-only combo boxes and for editable ones the text content
	 * is changed to match the result from calling the `toString()` method of `item`.
	 *
	 *
	 * If called with `null`, the selection is cleared.
	 * @param item Item in the combo box to select, or null if the selection should be cleared
	 */
	var selectedItem: V?
		@Synchronized get() = if (getSelectedIndex() > -1) getItem(getSelectedIndex()) else null
		@Synchronized set(item) {
			if (item == null) {
				setSelectedIndex(-1)
			} else {
				val indexOf = items.indexOf(item)
				if (indexOf != -1) {
					setSelectedIndex(indexOf)
				} else if (!readOnly) {
					updateText(item.toString())
				}
			}
		}

	/**
	 * Listener interface that can be used to catch user events on the combo box
	 */
	interface Listener {
		/**
		 * This method is called whenever the user changes selection from one item to another in the combo box
		 * @param selectedIndex Index of the item which is now selected
		 * @param previousSelection Index of the item which was previously selected
		 */
		fun onSelectionChanged(selectedIndex: Int, previousSelection: Int)
	}

	/**
	 * Creates a new `ComboBox` initialized with N number of items supplied through the varargs parameter. If at
	 * least one item is given, the first one in the array will be initially selected. By default 10 items will be
	 * displayed at once, more than that and there will be a scroll bar.
	 * @param items Items to populate the new combo box with
	 */
	constructor(vararg items: V) : this(Arrays.asList<V>(*items)) {}

	/**
	 * Creates a new `ComboBox` initialized with N number of items supplied through the items parameter. The
	 * initial text in the combo box is set to a specific value passed in through the `initialText` parameter, it
	 * can be a text which is not contained within the items and the selection state of the combo box will be
	 * "no selection" (so `getSelectedIndex()` will return -1) until the user interacts with the combo box and
	 * manually changes it. By default 10 items will be displayed at once, more than that and there will be a scroll bar.
	 *
	 * @param initialText Text to put in the combo box initially
	 * @param items Items to populate the new combo box with
	 */
	constructor(initialText: String, items: Collection<V>) : this(items, -1) {
		this.text = initialText
	}

	init {
		for (item in items) {
			if (item == null) {
				throw IllegalArgumentException("Cannot add null elements to a ComboBox")
			}
		}
		this.items = ArrayList(items)
		this.listeners = CopyOnWriteArrayList()
		this.popupWindow = null
		this.readOnly = true
		this.dropDownFocused = true
		this.textInputPosition = 0
		this.dropDownNumberOfRows = 10
		if (selectedIndex != -1) {
			this.text = this.items[selectedIndex].toString()
		} else {
			this.text = ""
		}
	}

	/**
	 * Adds a new item to the combo box, at the end
	 * @param item Item to add to the combo box
	 * @return Itself
	 */
	@Synchronized
	fun addItem(item: V?): ComboBox<V> {
		if (item == null) {
			throw IllegalArgumentException("Cannot add null elements to a ComboBox")
		}
		items.add(item)
		if (selectedIndex == -1 && items.size == 1) {
			setSelectedIndex(0)
		}
		invalidate()
		return this
	}

	/**
	 * Adds a new item to the combo box, at a specific index
	 * @param index Index to add the item at
	 * @param item Item to add
	 * @return Itself
	 */
	@Synchronized
	fun addItem(index: Int, item: V?): ComboBox<V> {
		if (item == null) {
			throw IllegalArgumentException("Cannot add null elements to a ComboBox")
		}
		items.add(index, item)
		if (index <= selectedIndex) {
			setSelectedIndex(selectedIndex + 1)
		}
		invalidate()
		return this
	}

	/**
	 * Removes all items from the combo box
	 * @return Itself
	 */
	@Synchronized
	fun clearItems(): ComboBox<V> {
		items.clear()
		setSelectedIndex(-1)
		invalidate()
		return this
	}

	/**
	 * Removes a particular item from the combo box, if it is present, otherwise does nothing
	 * @param item Item to remove from the combo box
	 * @return Itself
	 */
	@Synchronized
	fun removeItem(item: V): ComboBox<V> {
		val index = items.indexOf(item)
		return if (index == -1) {
			this
		} else remoteItem(index)
	}

	/**
	 * Removes an item from the combo box at a particular index
	 * @param index Index of the item to remove
	 * @return Itself
	 * @throws IndexOutOfBoundsException if the index is out of range
	 */
	@Synchronized
	fun remoteItem(index: Int): ComboBox<V> {
		items.removeAt(index)
		if (index < selectedIndex) {
			setSelectedIndex(selectedIndex - 1)
		} else if (index == selectedIndex) {
			setSelectedIndex(-1)
		}
		invalidate()
		return this
	}

	/**
	 * Updates the combo box so the item at the specified index is swapped out with the supplied value in the
	 * `item` parameter
	 * @param index Index of the item to swap out
	 * @param item Item to replace with
	 * @return Itself
	 */
	@Synchronized
	fun setItem(index: Int, item: V?): ComboBox<V> {
		if (item == null) {
			throw IllegalArgumentException("Cannot add null elements to a ComboBox")
		}
		items[index] = item
		invalidate()
		return this
	}

	/**
	 * Returns the item at the specific index
	 * @param index Index of the item to return
	 * @return Item at the specific index
	 * @throws IndexOutOfBoundsException if the index is out of range
	 */
	@Synchronized
	fun getItem(index: Int): V =
		items[index]

	/**
	 * Sets the combo box to either read-only or writable. In read-only mode, the user cannot type in any text in the
	 * combo box but is forced to pick one of the items, displayed by the drop-down. In writable mode, the user can
	 * enter any string in the combo box
	 * @param readOnly If the combo box should be in read-only mode, pass in `true`, otherwise `false` for
	 * writable mode
	 * @return Itself
	 */
	@Synchronized
	fun setReadOnly(readOnly: Boolean): ComboBox<V> {
		this.readOnly = readOnly
		if (readOnly) {
			dropDownFocused = true
		}
		return this
	}

	/**
	 * Returns `true` if this combo box is in read-only mode
	 * @return `true` if this combo box is in read-only mode, `false` otherwise
	 */
	fun isReadOnly(): Boolean =
		readOnly

	/**
	 * Programmatically selects one item in the combo box, which causes the displayed text to change to match the label
	 * of the selected index.
	 * @param selectedIndex Index of the item to select, or -1 if the selection should be cleared
	 * @throws IndexOutOfBoundsException if the index is out of range
	 */
	@Synchronized
	fun setSelectedIndex(selectedIndex: Int) {
		if (items.size <= selectedIndex || selectedIndex < -1) {
			throw IndexOutOfBoundsException("Illegal argument to ComboBox.setSelectedIndex: " + selectedIndex)
		}
		val oldSelection = this.selectedIndex
		this.selectedIndex = selectedIndex
		if (selectedIndex == -1) {
			updateText("")
		} else {
			updateText(items[selectedIndex].toString())
		}
		runOnGUIThreadIfExistsOtherwiseRunDirect {
			for (listener in listeners) {
				listener.onSelectionChanged(selectedIndex, oldSelection)
			}
		}
		invalidate()
	}

	private fun updateText(newText: String) {
		text = newText
		if (textInputPosition > text!!.length) {
			textInputPosition = text!!.length
		}
	}

	/**
	 * Returns the index of the currently selected item or -1 for no selection
	 * @return Index of the currently selected item
	 */
	fun getSelectedIndex() =
		selectedIndex

	/**
	 * Adds a new listener to the `ComboBox` that will be called on certain user actions
	 * @param listener Listener to attach to this `ComboBox`
	 * @return Itself
	 */
	fun addListener(listener: Listener?): ComboBox<V> {
		if (listener != null && !listeners.contains(listener)) {
			listeners.add(listener)
		}
		return this
	}

	/**
	 * Removes a listener from this `ComboBox` so that if it had been added earlier, it will no longer be
	 * called on user actions
	 * @param listener Listener to remove from this `ComboBox`
	 * @return Itself
	 */
	fun removeListener(listener: Listener): ComboBox<V> {
		listeners.remove(listener)
		return this
	}

	override fun afterEnterFocus(direction: Interactable.FocusChangeDirection, previouslyInFocus: Interactable) {
		if (direction == Interactable.FocusChangeDirection.RIGHT && !isReadOnly()) {
			dropDownFocused = false
			selectedIndex = 0
		}
	}

	@Synchronized override fun afterLeaveFocus(direction: Interactable.FocusChangeDirection, nextInFocus: Interactable) {
		if (popupWindow != null) {
			popupWindow!!.close()
			popupWindow = null
		}
	}

	override fun createDefaultRenderer(): InteractableRenderer<ComboBox<V>> =
		DefaultComboBoxRenderer()

	@Synchronized public override fun handleKeyStroke(keyStroke: KeyStroke): Interactable.Result =
		if (isReadOnly()) {
			handleReadOnlyCBKeyStroke(keyStroke)
		} else {
			handleEditableCBKeyStroke(keyStroke)
		}

	private fun handleReadOnlyCBKeyStroke(keyStroke: KeyStroke): Interactable.Result {
		when (keyStroke.keyType) {
			KeyType.ArrowDown -> {
				if (popupWindow != null) {
					popupWindow!!.listBox.handleKeyStroke(keyStroke)
					return Interactable.Result.HANDLED
				}
				return Interactable.Result.MOVE_FOCUS_DOWN
			}

			KeyType.ArrowUp -> {
				if (popupWindow != null) {
					popupWindow!!.listBox.handleKeyStroke(keyStroke)
					return Interactable.Result.HANDLED
				}
				return Interactable.Result.MOVE_FOCUS_UP
			}

			KeyType.PageUp, KeyType.PageDown, KeyType.Home, KeyType.End -> if (popupWindow != null) {
				popupWindow!!.listBox.handleKeyStroke(keyStroke)
				return Interactable.Result.HANDLED
			}

			KeyType.Enter -> if (popupWindow != null) {
				popupWindow!!.listBox.handleKeyStroke(keyStroke)
				popupWindow!!.close()
				popupWindow = null
			} else {
				popupWindow = PopupWindow()
				popupWindow!!.position = toGlobal(TerminalPosition(0, 1))
				(textGUI as WindowBasedTextGUI).addWindow(popupWindow)
			}

			KeyType.Escape -> if (popupWindow != null) {
				popupWindow!!.close()
				popupWindow = null
				return Interactable.Result.HANDLED
			}
		}
		return super.handleKeyStroke(keyStroke)
	}

	private fun handleEditableCBKeyStroke(keyStroke: KeyStroke): Interactable.Result {
		//First check if we are in drop-down focused mode, treat keystrokes a bit differently then
		if (isDropDownFocused) {
			when (keyStroke.keyType) {
				KeyType.ReverseTab, KeyType.ArrowLeft -> {
					dropDownFocused = false
					textInputPosition = text!!.length
					return Interactable.Result.HANDLED
				}

			//The rest we can process in the same way as with read-only combo boxes when we are in drop-down focused mode
				else -> return handleReadOnlyCBKeyStroke(keyStroke)
			}
		}

		when (keyStroke.keyType) {
			KeyType.Character -> {
				text = text!!.substring(0, textInputPosition) + keyStroke.character + text!!.substring(textInputPosition)
				textInputPosition++
				return Interactable.Result.HANDLED
			}

			KeyType.Tab -> {
				dropDownFocused = true
				return Interactable.Result.HANDLED
			}

			KeyType.Backspace -> {
				if (textInputPosition > 0) {
					text = text!!.substring(0, textInputPosition - 1) + text!!.substring(textInputPosition)
					textInputPosition--
				}
				return Interactable.Result.HANDLED
			}

			KeyType.Delete -> {
				if (textInputPosition < text!!.length) {
					text = text!!.substring(0, textInputPosition) + text!!.substring(textInputPosition + 1)
				}
				return Interactable.Result.HANDLED
			}

			KeyType.ArrowLeft -> {
				if (textInputPosition > 0) {
					textInputPosition--
				} else {
					return Interactable.Result.MOVE_FOCUS_LEFT
				}
				return Interactable.Result.HANDLED
			}

			KeyType.ArrowRight -> {
				if (textInputPosition < text!!.length) {
					textInputPosition++
				} else {
					dropDownFocused = true
					return Interactable.Result.HANDLED
				}
				return Interactable.Result.HANDLED
			}

			KeyType.ArrowDown -> {
				if (selectedIndex < items.size - 1) {
					setSelectedIndex(selectedIndex + 1)
				}
				return Interactable.Result.HANDLED
			}

			KeyType.ArrowUp -> {
				if (selectedIndex > 0) {
					setSelectedIndex(selectedIndex - 1)
				}
				return Interactable.Result.HANDLED
			}
		}
		return super.handleKeyStroke(keyStroke)
	}

	private inner class PopupWindow : BasicWindow() {
		private val listBox: ActionListBox

		override var theme: Theme?
			@Synchronized get() = this@ComboBox.theme
			set

		init {
			setHints(Arrays.asList<Hint>(
				Window.Hint.NO_FOCUS,
				Window.Hint.FIXED_POSITION))
			listBox = ActionListBox(this@ComboBox.size!!.withRows(itemCount))
			for (i in 0 until itemCount) {
				val item = items[i]
				listBox.addItem(item.toString()) {
					setSelectedIndex(i)
					close()
				}
			}
			listBox.selectedIndex = getSelectedIndex()
			val dropDownListPreferedSize = listBox.preferredSize
			if (dropDownNumberOfRows > 0) {
				listBox.preferredSize = dropDownListPreferedSize.withRows(
					Math.min(dropDownNumberOfRows, dropDownListPreferedSize.rows))
			}
			component = listBox
		}
	}

	/**
	 * Helper interface that doesn't add any new methods but makes coding new combo box renderers a little bit more clear
	 */
	abstract class ComboBoxRenderer<V> : InteractableRenderer<ComboBox<V>>

	/**
	 * This class is the default renderer implementation which will be used unless overridden. The combo box is rendered
	 * like a text box with an arrow point down to the right of it, which can receive focus and triggers the popup.
	 * @param <V> Type of items in the combo box
	</V> */
	class DefaultComboBoxRenderer<V> : ComboBoxRenderer<V>() {

		private var textVisibleLeftPosition: Int = 0

		/**
		 * Default constructor
		 */
		init {
			this.textVisibleLeftPosition = 0
		}

		override fun getCursorLocation(comboBox: ComboBox<V>): TerminalPosition? {
			if (comboBox.isDropDownFocused) {
				return if (comboBox.themeDefinition.isCursorVisible) {
					TerminalPosition(comboBox.size!!.columns - 1, 0)
				} else {
					null
				}
			} else {
				val textInputPosition = comboBox.textInputPosition
				val textInputColumn = TerminalTextUtils.getColumnWidth(comboBox.text!!.substring(0, textInputPosition))
				return TerminalPosition(textInputColumn - textVisibleLeftPosition, 0)
			}
		}

		override fun getPreferredSize(comboBox: ComboBox<V>): TerminalSize {
			var size = TerminalSize.ONE.withColumns(
				(if (comboBox.itemCount == 0) TerminalTextUtils.getColumnWidth(comboBox.text!!) else 0) + 2)

			synchronized(comboBox) {
				for (i in 0 until comboBox.itemCount) {
					val item = comboBox.getItem(i)
					size = size.max(TerminalSize(TerminalTextUtils.getColumnWidth(item.toString()) + 2 + 1, 1))   // +1 to add a single column of space
				}
			}
			return size
		}

		override fun drawComponent(graphics: TextGUIGraphics, comboBox: ComboBox<V>) {
			val themeDefinition = comboBox.themeDefinition
			if (comboBox.isReadOnly()) {
				graphics.applyThemeStyle(themeDefinition.normal)
			} else {
				if (comboBox.isFocused) {
					graphics.applyThemeStyle(themeDefinition.active)
				} else {
					graphics.applyThemeStyle(themeDefinition.preLight)
				}
			}
			graphics.fill(' ')
			val editableArea = graphics.size.columns - 2 //This is exclusing the 'drop-down arrow'
			val textInputPosition = comboBox.textInputPosition
			val columnsToInputPosition = TerminalTextUtils.getColumnWidth(comboBox.text!!.substring(0, textInputPosition))
			if (columnsToInputPosition < textVisibleLeftPosition) {
				textVisibleLeftPosition = columnsToInputPosition
			}
			if (columnsToInputPosition - textVisibleLeftPosition >= editableArea) {
				textVisibleLeftPosition = columnsToInputPosition - editableArea + 1
			}
			if (columnsToInputPosition - textVisibleLeftPosition + 1 == editableArea &&
				comboBox.text!!.length > textInputPosition &&
				TerminalTextUtils.isCharCJK(comboBox.text!![textInputPosition])) {
				textVisibleLeftPosition++
			}

			val textToDraw = TerminalTextUtils.fitString(comboBox.text!!, textVisibleLeftPosition, editableArea)
			graphics.putString(0, 0, textToDraw)
			graphics.applyThemeStyle(themeDefinition.insensitive)
			graphics.setCharacter(editableArea, 0, themeDefinition.getCharacter("POPUP_SEPARATOR", Symbols.SINGLE_LINE_VERTICAL))
			if (comboBox.isFocused && comboBox.isDropDownFocused) {
				graphics.applyThemeStyle(themeDefinition.selected)
			}
			graphics.setCharacter(editableArea + 1, 0, themeDefinition.getCharacter("POPUP", Symbols.TRIANGLE_DOWN_POINTING_BLACK))
		}
	}
}
/**
 * Creates a new `ComboBox` initialized with N number of items supplied through the items parameter. If at
 * least one item is given, the first one in the collection will be initially selected. By default 10 items will be
 * displayed at once, more than that and there will be a scroll bar.
 * @param items Items to populate the new combo box with
 */
