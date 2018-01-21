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

import java.util.*

/**
 * This class is used to keep a 'map' of the usable area and note where all the interact:ables are. It can then be used
 * to find the next interactable in any direction. It is used inside the GUI system to drive arrow key navigation.
 * @author Martin
 */
class InteractableLookupMap internal constructor(size: TerminalSize) {
	private val lookupMap: Array<IntArray>
	private val interactables: MutableList<Interactable>

	internal val size: TerminalSize
		get() = if (lookupMap.size == 0) {
			TerminalSize.ZERO
		} else TerminalSize(lookupMap[0].size, lookupMap.size)

	init {
		lookupMap = Array(size.rows) { IntArray(size.columns) }
		interactables = ArrayList()
		for (aLookupMap in lookupMap) {
			Arrays.fill(aLookupMap, -1)
		}
	}

	internal fun reset() {
		interactables.clear()
		for (aLookupMap in lookupMap) {
			Arrays.fill(aLookupMap, -1)
		}
	}

	/**
	 * Adds an interactable component to the lookup map
	 * @param interactable Interactable to add to the lookup map
	 */
	@Synchronized
	fun add(interactable: Interactable) {
		val topLeft = interactable.toBasePane(TerminalPosition.TOP_LEFT_CORNER)
		val size = interactable.size
		interactables.add(interactable)
		val index = interactables.size - 1
		for (y in topLeft.row until topLeft.row + size.rows) {
			for (x in topLeft.column until topLeft.column + size.columns) {
				//Make sure it's not outside the map
				if (y >= 0 && y < lookupMap.size &&
					x >= 0 && x < lookupMap[y].size) {
					lookupMap[y][x] = index
				}
			}
		}
	}

	/**
	 * Looks up what interactable component is as a particular location in the map
	 * @param position Position to look up
	 * @return The `Interactable` component at the specified location or `null` if there's nothing there
	 */
	@Synchronized
	fun getInteractableAt(position: TerminalPosition) =
		if (position.row < 0 || position.column < 0) {
			null
		} else if (position.row >= lookupMap.size) {
			null
		} else if (position.column >= lookupMap[0].size) {
			null
		} else if (lookupMap[position.row][position.column] == -1) {
			null
		} else {
			interactables[lookupMap[position.row][position.column]]
		}

	/**
	 * Starting from a particular `Interactable` and going up, which is the next interactable?
	 * @param interactable What `Interactable` to start searching from
	 * @return The next `Interactable` above the one specified or `null` if there are no more
	 * `Interactable`:s above it
	 */
	@Synchronized
	fun findNextUp(interactable: Interactable) =
		findNextUpOrDown(interactable, false)

	/**
	 * Starting from a particular `Interactable` and going down, which is the next interactable?
	 * @param interactable What `Interactable` to start searching from
	 * @return The next `Interactable` below the one specified or `null` if there are no more
	 * `Interactable`:s below it
	 */
	@Synchronized
	fun findNextDown(interactable: Interactable) =
		findNextUpOrDown(interactable, true)

	//Avoid code duplication in above two methods
	private fun findNextUpOrDown(interactable: Interactable, isDown: Boolean): Interactable? {
		val directionTerm = if (isDown) 1 else -1
		var startPosition: TerminalPosition? = interactable.cursorLocation
		if (startPosition == null) {
			// If the currently active interactable component is not showing the cursor, use the top-left position
			// instead if we're going up, or the bottom-left position if we're going down
			if (isDown) {
				startPosition = TerminalPosition(0, interactable.size.rows - 1)
			} else {
				startPosition = TerminalPosition.TOP_LEFT_CORNER
			}
		} else {
			//Adjust position so that it's at the bottom of the component if we're going down or at the top of the
			//component if we're going right. Otherwise the lookup might product odd results in certain cases.
			if (isDown) {
				startPosition = startPosition.withRow(interactable.size.rows - 1)
			} else {
				startPosition = startPosition.withRow(0)
			}
		}
		startPosition = interactable.toBasePane(startPosition)
		if (startPosition == null) {
			// The structure has changed, our interactable is no longer inside the base pane!
			return null
		}
		val disqualified = getDisqualifiedInteractables(startPosition, true)
		val size = size
		var maxShiftLeft = interactable.toBasePane(TerminalPosition.TOP_LEFT_CORNER).column
		maxShiftLeft = Math.max(maxShiftLeft, 0)
		var maxShiftRight = interactable.toBasePane(TerminalPosition(interactable.size.columns - 1, 0)).column
		maxShiftRight = Math.min(maxShiftRight, size.columns - 1)
		val maxShift = Math.max(startPosition.column - maxShiftLeft, maxShiftRight - startPosition.row)
		var searchRow = startPosition.row + directionTerm
		while (searchRow >= 0 && searchRow < size.rows) {

			for (xShift in 0..maxShift) {
				for (modifier in intArrayOf(1, -1)) {
					if (xShift == 0 && modifier == -1) {
						break
					}
					val searchColumn = startPosition.column + xShift * modifier
					if (searchColumn < maxShiftLeft || searchColumn > maxShiftRight) {
						continue
					}

					val index = lookupMap[searchRow][searchColumn]
					if (index != -1 && !disqualified.contains(interactables[index])) {
						return interactables[index]
					}
				}
			}
			searchRow += directionTerm
		}
		return null
	}

	/**
	 * Starting from a particular `Interactable` and going left, which is the next interactable?
	 * @param interactable What `Interactable` to start searching from
	 * @return The next `Interactable` left of the one specified or `null` if there are no more
	 * `Interactable`:s left of it
	 */
	@Synchronized
	fun findNextLeft(interactable: Interactable) =
		findNextLeftOrRight(interactable, false)

	/**
	 * Starting from a particular `Interactable` and going right, which is the next interactable?
	 * @param interactable What `Interactable` to start searching from
	 * @return The next `Interactable` right of the one specified or `null` if there are no more
	 * `Interactable`:s right of it
	 */
	@Synchronized
	fun findNextRight(interactable: Interactable) =
		findNextLeftOrRight(interactable, true)

	//Avoid code duplication in above two methods
	private fun findNextLeftOrRight(interactable: Interactable, isRight: Boolean): Interactable? {
		val directionTerm = if (isRight) 1 else -1
		var startPosition: TerminalPosition? = interactable.cursorLocation
		if (startPosition == null) {
			// If the currently active interactable component is not showing the cursor, use the top-left position
			// instead if we're going left, or the top-right position if we're going right
			if (isRight) {
				startPosition = TerminalPosition(interactable.size.columns - 1, 0)
			} else {
				startPosition = TerminalPosition.TOP_LEFT_CORNER
			}
		} else {
			//Adjust position so that it's on the left-most side if we're going left or right-most side if we're going
			//right. Otherwise the lookup might product odd results in certain cases
			if (isRight) {
				startPosition = startPosition.withColumn(interactable.size.columns - 1)
			} else {
				startPosition = startPosition.withColumn(0)
			}
		}
		startPosition = interactable.toBasePane(startPosition)
		if (startPosition == null) {
			// The structure has changed, our interactable is no longer inside the base pane!
			return null
		}
		val disqualified = getDisqualifiedInteractables(startPosition, false)
		val size = size
		var maxShiftUp = interactable.toBasePane(TerminalPosition.TOP_LEFT_CORNER).row
		maxShiftUp = Math.max(maxShiftUp, 0)
		var maxShiftDown = interactable.toBasePane(TerminalPosition(0, interactable.size.rows - 1)).row
		maxShiftDown = Math.min(maxShiftDown, size.rows - 1)
		val maxShift = Math.max(startPosition.row - maxShiftUp, maxShiftDown - startPosition.row)
		var searchColumn = startPosition.column + directionTerm
		while (searchColumn >= 0 && searchColumn < size.columns) {

			for (yShift in 0..maxShift) {
				for (modifier in intArrayOf(1, -1)) {
					if (yShift == 0 && modifier == -1) {
						break
					}
					val searchRow = startPosition.row + yShift * modifier
					if (searchRow < maxShiftUp || searchRow > maxShiftDown) {
						continue
					}
					val index = lookupMap[searchRow][searchColumn]
					if (index != -1 && !disqualified.contains(interactables[index])) {
						return interactables[index]
					}
				}
			}
			searchColumn += directionTerm
		}
		return null
	}

	private fun getDisqualifiedInteractables(startPosition: TerminalPosition, scanHorizontally: Boolean): Set<Interactable> {
		var startPosition = startPosition
		val disqualified = HashSet<Interactable>()
		if (lookupMap.size == 0) {
			return disqualified
		} // safeguard

		val size = size

		//Adjust start position if necessary
		if (startPosition.row < 0) {
			startPosition = startPosition.withRow(0)
		} else if (startPosition.row >= lookupMap.size) {
			startPosition = startPosition.withRow(lookupMap.size - 1)
		}
		if (startPosition.column < 0) {
			startPosition = startPosition.withColumn(0)
		} else if (startPosition.column >= lookupMap[startPosition.row].size) {
			startPosition = startPosition.withColumn(lookupMap[startPosition.row].size - 1)
		}

		if (scanHorizontally) {
			for (column in 0 until size.columns) {
				val index = lookupMap[startPosition.row][column]
				if (index != -1) {
					disqualified.add(interactables[index])
				}
			}
		} else {
			for (row in 0 until size.rows) {
				val index = lookupMap[row][startPosition.column]
				if (index != -1) {
					disqualified.add(interactables[index])
				}
			}
		}
		return disqualified
	}

	internal fun debug() {
		for (row in lookupMap) {
			for (value in row) {
				if (value >= 0) {
					print(" ")
				}
				print(value)
			}
			println()
		}
		println()
	}
}
