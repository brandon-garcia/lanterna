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

/**
 * Abstract implementation of `Border` interface that has some of the methods filled out. If you want to create
 * your own `Border` implementation, should should probably extend from this.
 * @author Martin
 */
abstract class AbstractBorder : AbstractComposite<Border>(), Border {
	override var component: Component?
		get
		set(component) {
			super.setComponent(component)
			if (component != null) {
				component.position = TerminalPosition.TOP_LEFT_CORNER
			}
		}

	override val renderer: Border.BorderRenderer
		get() = super.renderer as Border.BorderRenderer

	override val layoutData: LayoutData?
		get() = getComponent()!!.layoutData

	private val wrappedComponentTopLeftOffset: TerminalPosition
		get() = renderer.wrappedComponentTopLeftOffset

	override fun setSize(size: TerminalSize): Border {
		super.setSize(size)
		getComponent()!!.size = getWrappedComponentSize(size)
		return self()
	}

	override fun setLayoutData(ld: LayoutData): Border {
		getComponent()!!.layoutData = ld
		return this
	}

	override fun toBasePane(position: TerminalPosition) =
		super.toBasePane(position)!!.withRelative(wrappedComponentTopLeftOffset)

	override fun toGlobal(position: TerminalPosition) =
		super.toGlobal(position)!!.withRelative(wrappedComponentTopLeftOffset)

	private fun getWrappedComponentSize(borderSize: TerminalSize) =
		renderer.getWrappedComponentSize(borderSize)
}
