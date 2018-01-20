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
package com.googlecode.lanterna.terminal.ansi

import java.lang.reflect.Field
import java.util.Collections
import java.util.HashMap

/**
 * Contains the telnet protocol commands, although not a complete set.
 * @author Martin
 */
internal object TelnetProtocol {
	val COMMAND_SUBNEGOTIATION_END = 0xf0.toByte()  //SE
	val COMMAND_NO_OPERATION = 0xf1.toByte()    //NOP
	val COMMAND_DATA_MARK = 0xf2.toByte()   //DM
	val COMMAND_BREAK = 0xf3.toByte()       //BRK
	val COMMAND_INTERRUPT_PROCESS = 0xf4.toByte()   //IP
	val COMMAND_ABORT_OUTPUT = 0xf5.toByte()    //AO
	val COMMAND_ARE_YOU_THERE = 0xf6.toByte()   //AYT
	val COMMAND_ERASE_CHARACTER = 0xf7.toByte() //EC
	val COMMAND_ERASE_LINE = 0xf8.toByte()  //WL
	val COMMAND_GO_AHEAD = 0xf9.toByte()    //GA
	val COMMAND_SUBNEGOTIATION = 0xfa.toByte()  //SB
	val COMMAND_WILL = 0xfb.toByte()
	val COMMAND_WONT = 0xfc.toByte()
	val COMMAND_DO = 0xfd.toByte()
	val COMMAND_DONT = 0xfe.toByte()
	val COMMAND_IAC = 0xff.toByte()

	val OPTION_TRANSMIT_BINARY = 0x00.toByte()
	val OPTION_ECHO = 0x01.toByte()
	val OPTION_SUPPRESS_GO_AHEAD = 0x03.toByte()
	val OPTION_STATUS = 0x05.toByte()
	val OPTION_TIMING_MARK = 0x06.toByte()
	val OPTION_NAOCRD = 0x0a.toByte()
	val OPTION_NAOHTS = 0x0b.toByte()
	val OPTION_NAOHTD = 0x0c.toByte()
	val OPTION_NAOFFD = 0x0d.toByte()
	val OPTION_NAOVTS = 0x0e.toByte()
	val OPTION_NAOVTD = 0x0f.toByte()
	val OPTION_NAOLFD = 0x10.toByte()
	val OPTION_EXTEND_ASCII = 0x01.toByte()
	val OPTION_TERMINAL_TYPE = 0x18.toByte()
	val OPTION_NAWS = 0x1f.toByte()
	val OPTION_TERMINAL_SPEED = 0x20.toByte()
	val OPTION_TOGGLE_FLOW_CONTROL = 0x21.toByte()
	val OPTION_LINEMODE = 0x22.toByte()
	val OPTION_AUTHENTICATION = 0x25.toByte()

	val NAME_TO_CODE = createName2CodeMap()
	val CODE_TO_NAME = reverseMap(NAME_TO_CODE)

	private fun createName2CodeMap(): Map<String, Byte> {
		val result = HashMap<String, Byte>()
		for (field in TelnetProtocol::class.java!!.getDeclaredFields()) {
			if (field.getType() != Byte::class.javaPrimitiveType || !field.getName().startsWith("COMMAND_") && !field.getName().startsWith("OPTION_")) {
				continue
			}
			try {
				val namePart = field.getName().substring(field.getName().indexOf("_") + 1)
				result.put(namePart, field.get(null) as Byte)
			} catch (ignored: IllegalAccessException) {
			} catch (ignored: IllegalArgumentException) {
			}

		}
		return Collections.unmodifiableMap(result)
	}

	private fun <V, K> reverseMap(n2c: Map<K, V>): Map<V, K> {
		val result = HashMap<V, K>()
		for ((key, value) in n2c) {
			result.put(value, key)
		}
		return Collections.unmodifiableMap(result)
	}
}
/** Cannot instantiate.  */
