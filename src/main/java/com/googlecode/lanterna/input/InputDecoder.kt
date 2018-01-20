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
package com.googlecode.lanterna.input

import com.googlecode.lanterna.input.CharacterPattern.Matching

import java.io.BufferedReader
import java.io.IOException
import java.io.Reader
import java.util.*

/**
 * Used to read the input stream character by character and generate `Key` objects to be put in the input queue.
 *
 * @author Martin, Andreas
 */
class InputDecoder
/**
 * Creates a new input decoder using a specified Reader as the source to read characters from
 * @param source Reader to read characters from, will be wrapped by a BufferedReader
 */
(source: Reader) {
	private val source: Reader
	private val bytePatterns: MutableList<CharacterPattern>
	private val currentMatching: MutableList<Char>
	private var seenEOF: Boolean = false
	private var timeoutUnits: Int = 0

	/**
	 * Returns a collection of all patterns registered in this InputDecoder.
	 * @return Collection of patterns in the InputDecoder
	 */
	val patterns: Collection<CharacterPattern>
		@Synchronized get() {
			synchronized(bytePatterns) {
				return ArrayList(bytePatterns)
			}
		}

	init {
		this.source = BufferedReader(source)
		this.bytePatterns = ArrayList()
		this.currentMatching = ArrayList()
		this.seenEOF = false
		this.timeoutUnits = 0 // default is no wait at all
	}

	/**
	 * Adds another key decoding profile to this InputDecoder, which means all patterns from the profile will be used
	 * when decoding input.
	 * @param profile Profile to add
	 */
	fun addProfile(profile: KeyDecodingProfile) {
		for (pattern in profile.patterns) {
			synchronized(bytePatterns) {
				//If an equivalent pattern already exists, remove it first
				bytePatterns.remove(pattern)
				bytePatterns.add(pattern)
			}
		}
	}

	/**
	 * Removes one pattern from the list of patterns in this InputDecoder
	 * @param pattern Pattern to remove
	 * @return `true` if the supplied pattern was found and was removed, otherwise `false`
	 */
	fun removePattern(pattern: CharacterPattern): Boolean {
		synchronized(bytePatterns) {
			return bytePatterns.remove(pattern)
		}
	}

	/**
	 * Sets the number of 1/4-second units for how long to try to get further input
	 * to complete an escape-sequence for a special Key.
	 *
	 * Negative numbers are mapped to 0 (no wait at all), and unreasonably high
	 * values are mapped to a maximum of 240 (1 minute).
	 * @param units New timeout to use, in 250ms units
	 */
	fun setTimeoutUnits(units: Int) {
		timeoutUnits = if (units < 0)
			0
		else if (units > 240)
			240
		else
			units
	}

	/**
	 * queries the current timeoutUnits value. One unit is 1/4 second.
	 * @return The timeout this InputDecoder will use when waiting for additional input, in units of 1/4 seconds
	 */
	fun getTimeoutUnits(): Int {
		return timeoutUnits
	}

	/**
	 * Reads and decodes the next key stroke from the input stream
	 * @param blockingIO If set to `true`, the call will not return until it has read at least one [KeyStroke]
	 * @return Key stroke read from the input stream, or `null` if none
	 * @throws IOException If there was an I/O error when reading from the input stream
	 */
	@Synchronized
	@Throws(IOException::class)
	fun getNextCharacter(blockingIO: Boolean): KeyStroke? {

		var bestMatch: KeyStroke? = null
		var bestLen = 0
		var curLen = 0

		while (true) {

			if (curLen < currentMatching.size) {
				// (re-)consume characters previously read:
				curLen++
			} else {
				// If we already have a bestMatch but a chance for a longer match
				//   then we poll for the configured number of timeout units:
				// It would be much better, if we could just read with a timeout,
				//   but lacking that, we wait 1/4s units and check for readiness.
				if (bestMatch != null) {
					var timeout = getTimeoutUnits()
					while (timeout > 0 && !source.ready()) {
						try {
							timeout--
							Thread.sleep(250)
						} catch (e: InterruptedException) {
							timeout = 0
						}

					}
				}
				// if input is available, we can just read a char without waiting,
				// otherwise, for readInput() with no bestMatch found yet,
				//  we have to wait blocking for more input:
				if (source.ready() || blockingIO && bestMatch == null) {
					val readChar = source.read()
					if (readChar == -1) {
						seenEOF = true
						if (currentMatching.isEmpty()) {
							return KeyStroke(KeyType.EOF)
						}
						break
					}
					currentMatching.add(readChar.toChar())
					curLen++
				} else { // no more available input at this time.
					// already found something:
					if (bestMatch != null) {
						break // it's something...
					}
					// otherwise: no KeyStroke yet
					return null
				}
			}

			val curSub = currentMatching.subList(0, curLen)
			val matching = getBestMatch(curSub)

			// fullMatch found...
			if (matching.fullMatch != null) {
				bestMatch = matching.fullMatch
				bestLen = curLen

				if (!matching.partialMatch) {
					// that match and no more
					break
				} else {
					// that match, but maybe more


					continue
				}
			} else if (matching.partialMatch) {

				continue
			} else {
				if (bestMatch != null) {
					// there was already a previous full-match, use it:
					break
				} else { // invalid input!
					// remove the whole fail and re-try finding a KeyStroke...
					curSub.clear() // or just 1 char?  currentMatching.remove(0);
					curLen = 0

					continue
				}
			}// no longer match possible at this point:
			// No match found yet, but there's still potential...
		}

		//Did we find anything? Otherwise return null
		if (bestMatch == null) {
			if (seenEOF) {
				currentMatching.clear()
				return KeyStroke(KeyType.EOF)
			}
			return null
		}

		val bestSub = currentMatching.subList(0, bestLen)
		bestSub.clear() // remove matched characters from input
		return bestMatch
	}

	private fun getBestMatch(characterSequence: List<Char>): Matching {
		var partialMatch = false
		var bestMatch: KeyStroke? = null
		synchronized(bytePatterns) {
			for (pattern in bytePatterns) {
				val res = pattern.match(characterSequence)
				if (res != null) {
					if (res.partialMatch) {
						partialMatch = true
					}
					if (res.fullMatch != null) {
						bestMatch = res.fullMatch
					}
				}
			}
		}
		return Matching(partialMatch, bestMatch)
	}
}
