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

import java.lang.ref.WeakReference
import java.util.*

/**
 * This is a special label that contains not just a single text to display but a number of frames that are cycled
 * through. The class will manage a timer on its own and ensure the label is updated and redrawn. There is a static
 * helper method available to create the classic "spinning bar": `createClassicSpinningLine()`
 */
class AnimatedLabel
/**
 * Creates a new animated label, initially set to one frame. You will need to add more frames and call
 * `startAnimation()` for this to start moving.
 *
 * @param firstFrameText The content of the label at the first frame
 */
(firstFrameText: String) : Label(firstFrameText) {

	private val frames: MutableList<Array<String>>
	private var combinedMaximumPreferredSize: TerminalSize? = null
	private var currentFrame: Int = 0

	init {
		frames = ArrayList()
		currentFrame = 0
		combinedMaximumPreferredSize = TerminalSize.ZERO

		val lines = splitIntoMultipleLines(firstFrameText)
		frames.add(lines)
		ensurePreferredSize(lines)
	}

	@Synchronized override fun calculatePreferredSize(): TerminalSize {
		return super.calculatePreferredSize().max(combinedMaximumPreferredSize!!)
	}

	/**
	 * Adds one more frame at the end of the list of frames
	 * @param text Text to use for the label at this frame
	 * @return Itself
	 */
	@Synchronized
	fun addFrame(text: String): AnimatedLabel {
		val lines = splitIntoMultipleLines(text)
		frames.add(lines)
		ensurePreferredSize(lines)
		return this
	}

	private fun ensurePreferredSize(lines: Array<String>) {
		combinedMaximumPreferredSize = combinedMaximumPreferredSize!!.max(getBounds(lines, combinedMaximumPreferredSize))
	}

	/**
	 * Advances the animated label to the next frame. You normally don't need to call this manually as it will be done
	 * by the animation thread.
	 */
	@Synchronized
	fun nextFrame() {
		currentFrame++
		if (currentFrame >= frames.size) {
			currentFrame = 0
		}
		super.setLines(frames[currentFrame])
		invalidate()
	}

	override fun onRemoved(container: Container) {
		stopAnimation()
	}

	/**
	 * Starts the animation thread which will periodically call `nextFrame()` at the interval specified by the
	 * `millisecondsPerFrame` parameter. After all frames have been cycled through, it will start over from the
	 * first frame again.
	 * @param millisecondsPerFrame The interval in between every frame
	 * @return Itself
	 */
	@Synchronized
	fun startAnimation(millisecondsPerFrame: Long): AnimatedLabel {
		if (TIMER == null) {
			TIMER = Timer("AnimatedLabel")
		}
		val animationTimerTask = AnimationTimerTask(this)
		SCHEDULED_TASKS.put(this, animationTimerTask)
		TIMER!!.scheduleAtFixedRate(animationTimerTask, millisecondsPerFrame, millisecondsPerFrame)
		return this
	}

	/**
	 * Halts the animation thread and the label will stop at whatever was the current frame at the time when this was
	 * called
	 * @return Itself
	 */
	@Synchronized
	fun stopAnimation(): AnimatedLabel {
		removeTaskFromTimer(this)
		return this
	}

	private class AnimationTimerTask private constructor(label: AnimatedLabel) : TimerTask() {
		private val labelRef: WeakReference<AnimatedLabel>

		init {
			this.labelRef = WeakReference(label)
		}

		override fun run() {
			val animatedLabel = labelRef.get()
			if (animatedLabel == null) {
				cancel()
				canCloseTimer()
			} else {
				if (animatedLabel.basePane == null) {
					animatedLabel.stopAnimation()
				} else {
					animatedLabel.nextFrame()
				}
			}
		}
	}

	companion object {
		private var TIMER: Timer? = null
		private val SCHEDULED_TASKS = WeakHashMap<AnimatedLabel, TimerTask>()

		/**
		 * Creates a classic spinning bar which can be used to signal to the user that an operation in is process.
		 * @param speed Delay in between each frame
		 * @return `AnimatedLabel` instance which is setup to show a spinning bar
		 */
		@JvmOverloads
		fun createClassicSpinningLine(speed: Int = 150): AnimatedLabel {
			val animatedLabel = AnimatedLabel("-")
			animatedLabel.addFrame("\\")
			animatedLabel.addFrame("|")
			animatedLabel.addFrame("/")
			animatedLabel.startAnimation(speed.toLong())
			return animatedLabel
		}

		@Synchronized private fun removeTaskFromTimer(animatedLabel: AnimatedLabel) {
			SCHEDULED_TASKS[animatedLabel].cancel()
			SCHEDULED_TASKS.remove(animatedLabel)
			canCloseTimer()
		}

		@Synchronized private fun canCloseTimer() {
			if (SCHEDULED_TASKS.isEmpty()) {
				TIMER!!.cancel()
				TIMER = null
			}
		}
	}
}
/**
 * Creates a classic spinning bar which can be used to signal to the user that an operation in is process.
 * @return `AnimatedLabel` instance which is setup to show a spinning bar
 */
