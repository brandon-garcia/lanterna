package com.googlecode.lanterna.gui2

import com.googlecode.lanterna.*
import com.googlecode.lanterna.graphics.*
import com.googlecode.lanterna.screen.TabBehaviour
import java.util.EnumSet

/**
 * Created by Martin on 2017-08-11.
 */
class DefaultTextGUIGraphics internal constructor(override val textGUI: TextGUI, private val backend: TextGraphics) : TextGUIGraphics {

	override val size: TerminalSize
		get() = backend.size

	override val backgroundColor: TextColor
		get() = backend.backgroundColor

	override val foregroundColor: TextColor
		get() = backend.foregroundColor

	override val activeModifiers: EnumSet<SGR>
		get() = backend.activeModifiers

	override val tabBehaviour: TabBehaviour
		get() = backend.tabBehaviour

	@Throws(IllegalArgumentException::class)
	override fun newTextGraphics(topLeftCorner: TerminalPosition, size: TerminalSize) =
		DefaultTextGUIGraphics(textGUI, backend.newTextGraphics(topLeftCorner, size))

	override fun applyThemeStyle(themeStyle: ThemeStyle): DefaultTextGUIGraphics {
		setForegroundColor(themeStyle.foreground)
		setBackgroundColor(themeStyle.background)
		setModifiers(themeStyle.getSGRs())
		return this
	}

	override fun setBackgroundColor(backgroundColor: TextColor): DefaultTextGUIGraphics {
		backend.setBackgroundColor(backgroundColor)
		return this
	}

	override fun setForegroundColor(foregroundColor: TextColor): DefaultTextGUIGraphics {
		backend.setForegroundColor(foregroundColor)
		return this
	}

	override fun enableModifiers(vararg modifiers: SGR): DefaultTextGUIGraphics {
		backend.enableModifiers(*modifiers)
		return this
	}

	override fun disableModifiers(vararg modifiers: SGR): DefaultTextGUIGraphics {
		backend.disableModifiers(*modifiers)
		return this
	}

	override fun setModifiers(modifiers: EnumSet<SGR>): DefaultTextGUIGraphics {
		backend.setModifiers(modifiers)
		return this
	}

	override fun clearModifiers(): DefaultTextGUIGraphics {
		backend.clearModifiers()
		return this
	}

	override fun setTabBehaviour(tabBehaviour: TabBehaviour): DefaultTextGUIGraphics {
		backend.setTabBehaviour(tabBehaviour)
		return this
	}

	override fun fill(c: Char): DefaultTextGUIGraphics {
		backend.fill(c)
		return this
	}

	override fun fillRectangle(topLeft: TerminalPosition, size: TerminalSize, character: Char): DefaultTextGUIGraphics {
		backend.fillRectangle(topLeft, size, character)
		return this
	}

	override fun fillRectangle(topLeft: TerminalPosition, size: TerminalSize, character: TextCharacter): DefaultTextGUIGraphics {
		backend.fillRectangle(topLeft, size, character)
		return this
	}

	override fun drawRectangle(topLeft: TerminalPosition, size: TerminalSize, character: Char): DefaultTextGUIGraphics {
		backend.drawRectangle(topLeft, size, character)
		return this
	}

	override fun drawRectangle(topLeft: TerminalPosition, size: TerminalSize, character: TextCharacter): DefaultTextGUIGraphics {
		backend.drawRectangle(topLeft, size, character)
		return this
	}

	override fun fillTriangle(p1: TerminalPosition, p2: TerminalPosition, p3: TerminalPosition, character: Char): DefaultTextGUIGraphics {
		backend.fillTriangle(p1, p2, p3, character)
		return this
	}

	override fun fillTriangle(p1: TerminalPosition, p2: TerminalPosition, p3: TerminalPosition, character: TextCharacter): DefaultTextGUIGraphics {
		backend.fillTriangle(p1, p2, p3, character)
		return this
	}

	override fun drawTriangle(p1: TerminalPosition, p2: TerminalPosition, p3: TerminalPosition, character: Char): DefaultTextGUIGraphics {
		backend.drawTriangle(p1, p2, p3, character)
		return this
	}

	override fun drawTriangle(p1: TerminalPosition, p2: TerminalPosition, p3: TerminalPosition, character: TextCharacter): DefaultTextGUIGraphics {
		backend.drawTriangle(p1, p2, p3, character)
		return this
	}

	override fun drawLine(fromPoint: TerminalPosition, toPoint: TerminalPosition, character: Char): DefaultTextGUIGraphics {
		backend.drawLine(fromPoint, toPoint, character)
		return this
	}

	override fun drawLine(fromPoint: TerminalPosition, toPoint: TerminalPosition, character: TextCharacter): DefaultTextGUIGraphics {
		backend.drawLine(fromPoint, toPoint, character)
		return this
	}

	override fun drawLine(fromX: Int, fromY: Int, toX: Int, toY: Int, character: Char): DefaultTextGUIGraphics {
		backend.drawLine(fromX, fromY, toX, toY, character)
		return this
	}

	override fun drawLine(fromX: Int, fromY: Int, toX: Int, toY: Int, character: TextCharacter): DefaultTextGUIGraphics {
		backend.drawLine(fromX, fromY, toX, toY, character)
		return this
	}

	override fun drawImage(topLeft: TerminalPosition, image: TextImage): DefaultTextGUIGraphics {
		backend.drawImage(topLeft, image)
		return this
	}

	override fun drawImage(topLeft: TerminalPosition, image: TextImage, sourceImageTopLeft: TerminalPosition, sourceImageSize: TerminalSize): DefaultTextGUIGraphics {
		backend.drawImage(topLeft, image, sourceImageTopLeft, sourceImageSize)
		return this
	}

	override fun setCharacter(position: TerminalPosition, character: Char): DefaultTextGUIGraphics {
		backend.setCharacter(position, character)
		return this
	}

	override fun setCharacter(position: TerminalPosition, character: TextCharacter): DefaultTextGUIGraphics {
		backend.setCharacter(position, character)
		return this
	}

	override fun setCharacter(column: Int, row: Int, character: Char): DefaultTextGUIGraphics {
		backend.setCharacter(column, row, character)
		return this
	}

	override fun setCharacter(column: Int, row: Int, character: TextCharacter): DefaultTextGUIGraphics {
		backend.setCharacter(column, row, character)
		return this
	}

	override fun putString(column: Int, row: Int, string: String): DefaultTextGUIGraphics {
		backend.putString(column, row, string)
		return this
	}

	override fun putString(position: TerminalPosition, string: String): DefaultTextGUIGraphics {
		backend.putString(position, string)
		return this
	}

	override fun putString(column: Int, row: Int, string: String, extraModifier: SGR, vararg optionalExtraModifiers: SGR): DefaultTextGUIGraphics {
		backend.putString(column, row, string, extraModifier, *optionalExtraModifiers)
		return this
	}

	override fun putString(position: TerminalPosition, string: String, extraModifier: SGR, vararg optionalExtraModifiers: SGR): DefaultTextGUIGraphics {
		backend.putString(position, string, extraModifier, *optionalExtraModifiers)
		return this
	}

	override fun putString(column: Int, row: Int, string: String, extraModifiers: Collection<SGR>): DefaultTextGUIGraphics {
		backend.putString(column, row, string, extraModifiers)
		return this
	}

	override fun putCSIStyledString(column: Int, row: Int, string: String): DefaultTextGUIGraphics {
		backend.putCSIStyledString(column, row, string)
		return this
	}

	override fun putCSIStyledString(position: TerminalPosition, string: String): DefaultTextGUIGraphics {
		backend.putCSIStyledString(position, string)
		return this
	}

	override fun getCharacter(column: Int, row: Int) =
		backend.getCharacter(column, row)

	override fun getCharacter(position: TerminalPosition) =
		backend.getCharacter(position)

	override fun setStyleFrom(source: StyleSet<*>): DefaultTextGUIGraphics {
		setBackgroundColor(source.backgroundColor)
		setForegroundColor(source.foregroundColor)
		setModifiers(source.activeModifiers)
		return this
	}

}
