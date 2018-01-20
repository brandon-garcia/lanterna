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

import com.googlecode.lanterna.terminal.ansi.TelnetProtocol.*
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.net.SocketAddress
import java.nio.charset.Charset
import java.util.Arrays
import java.util.ArrayList

/**
 * This class is used by the `TelnetTerminalServer` class when a client has connected in; this class will be the
 * interaction point for that client. All operations are sent to the client over the network socket and some of the
 * meta-operations (like echo mode) are communicated using Telnet negotiation language. You can't create objects of this
 * class directly; they are created for you when you are listening for incoming connections using a
 * `TelnetTerminalServer` and a client connects.
 *
 *
 * A good resource on telnet communication is http://www.tcpipguide.com/free/t_TelnetProtocol.htm<br></br>
 * Also here: http://support.microsoft.com/kb/231866
 * @see TelnetTerminalServer
 *
 * @author martin
 */
class TelnetTerminal//This weird construction is just so that we can access the input filter without changing the visibility in StreamBasedTerminal
@Throws(IOException::class)
private constructor(private val socket: Socket, inputStream: TelnetClientIACFilterer, outputStream: OutputStream, terminalCharset: Charset) : ANSITerminal(inputStream, outputStream, terminalCharset) {
	/**
	 * Retrieves the current negotiation state with the client, containing details on what options have been enabled
	 * and what the client has said it supports.
	 * @return The current negotiation state for this client
	 */
	val negotiationState: NegotiationState

	/**
	 * Returns the socket address for the remote endpoint of the telnet connection
	 * @return SocketAddress representing the remote client
	 */
	val remoteSocketAddress: SocketAddress
		get() = socket.remoteSocketAddress

	@Throws(IOException::class)
	internal constructor(socket: Socket, terminalCharset: Charset) : this(socket, TelnetClientIACFilterer(socket.getInputStream()), socket.getOutputStream(), terminalCharset) {
	}

	init {
		this.negotiationState = inputStream.negotiationState
		inputStream.setEventListener(object : TelnetClientEventListener {
			override fun onResize(columns: Int, rows: Int) {
				this@TelnetTerminal.onResized(columns, rows)
			}

			@Throws(IOException::class)
			override fun requestReply(will: Boolean, option: Byte) {
				writeToTerminal(COMMAND_IAC, if (will) COMMAND_WILL else COMMAND_WONT, option)
			}
		})
		setLineMode0()
		setEchoOff()
		setResizeNotificationOn()
	}

	@Throws(IOException::class)
	private fun setEchoOff() {
		writeToTerminal(COMMAND_IAC, COMMAND_WILL, OPTION_ECHO)
		flush()
	}

	@Throws(IOException::class)
	private fun setLineMode0() {
		writeToTerminal(
			COMMAND_IAC, COMMAND_DO, OPTION_LINEMODE,
			COMMAND_IAC, COMMAND_SUBNEGOTIATION, OPTION_LINEMODE, 1.toByte(), 0.toByte(), COMMAND_IAC, COMMAND_SUBNEGOTIATION_END)
		flush()
	}

	@Throws(IOException::class)
	private fun setResizeNotificationOn() {
		writeToTerminal(
			COMMAND_IAC, COMMAND_DO, OPTION_NAWS)
		flush()
	}

	/**
	 * Closes the socket to the client, effectively ending the telnet session and the terminal.
	 * @throws IOException If there was an underlying I/O error
	 */
	@Throws(IOException::class)
	override fun close() {
		super.close()
		socket.close()
	}

	/**
	 * This class contains some of the various states that the Telnet negotiation protocol defines. Lanterna doesn't
	 * support all of them but the more common ones are represented.
	 */
	class NegotiationState internal constructor() {
		/**
		 * Is the telnet client echo mode turned on (client is echoing characters locally)
		 * @return `true` if client echo is enabled
		 */
		val isClientEcho: Boolean
		/**
		 * Is the telnet client line mode 0 turned on (client sends character by character instead of line by line)
		 * @return `true` if client line mode 0 is enabled
		 */
		val isClientLineMode0: Boolean
		/**
		 * Is the telnet client resize notification turned on (client notifies server when the terminal window has
		 * changed size)
		 * @return `true` if client resize notification is enabled
		 */
		val isClientResizeNotification: Boolean
		/**
		 * Is the telnet client suppress go-ahead turned on
		 * @return `true` if client suppress go-ahead is enabled
		 */
		val isSuppressGoAhead: Boolean
		/**
		 * Is the telnet client extended ascii turned on
		 * @return `true` if client extended ascii is enabled
		 */
		val isExtendedAscii: Boolean

		init {
			this.isClientEcho = true
			this.isClientLineMode0 = false
			this.isClientResizeNotification = false
			this.isSuppressGoAhead = true
			this.isExtendedAscii = true
		}

		private fun onUnsupportedStateCommand(enabling: Boolean, value: Byte) {
			System.err.println("Unsupported operation: Client says it " + (if (enabling) "will" else "won't") + " do " + TelnetProtocol.CODE_TO_NAME[value])
		}

		private fun onUnsupportedRequestCommand(askedToDo: Boolean, value: Byte) {
			System.err.println("Unsupported request: Client asks us, " + (if (askedToDo) "do" else "don't") + " " + TelnetProtocol.CODE_TO_NAME[value])
		}

		private fun onUnsupportedSubnegotiation(option: Byte, additionalData: ByteArray) {
			System.err.println("Unsupported subnegotiation: Client send " + TelnetProtocol.CODE_TO_NAME[option] + " with extra data " +
				toList(additionalData))
		}

		private fun toList(array: ByteArray): List<String> {
			val list = ArrayList<String>(array.size)
			for (b in array) {
				list.add(String.format("%02X ", b))
			}
			return list
		}
	}

	private interface TelnetClientEventListener {
		fun onResize(columns: Int, rows: Int)
		@Throws(IOException::class)
		fun requestReply(will: Boolean, option: Byte)
	}

	private class TelnetClientIACFilterer internal constructor(private val inputStream: InputStream) : InputStream() {
		private val negotiationState: NegotiationState
		private val buffer: ByteArray
		private val workingBuffer: ByteArray
		private var bytesInBuffer: Int = 0
		private var eventListener: TelnetClientEventListener? = null

		init {
			this.negotiationState = NegotiationState()
			this.buffer = ByteArray(64 * 1024)
			this.workingBuffer = ByteArray(1024)
			this.bytesInBuffer = 0
			this.eventListener = null
		}

		private fun setEventListener(eventListener: TelnetClientEventListener) {
			this.eventListener = eventListener
		}

		@Throws(IOException::class)
		override fun read(): Int {
			throw UnsupportedOperationException("TelnetClientIACFilterer doesn't support .read()")
		}

		@Throws(IOException::class)
		override fun close() {
			inputStream.close()
		}

		@Throws(IOException::class)
		override fun available(): Int {
			val underlyingStreamAvailable = inputStream.available()
			if (underlyingStreamAvailable == 0 && bytesInBuffer == 0) {
				return 0
			} else if (underlyingStreamAvailable == 0) {
				return bytesInBuffer
			} else if (bytesInBuffer == buffer.size) {
				return bytesInBuffer
			}
			fillBuffer()
			return bytesInBuffer
		}

		@Throws(IOException::class)
		override//I can't find the correct way to fix this!
		fun read(b: ByteArray, off: Int, len: Int): Int {
			if (available() == 0) {
				// There was nothing in the buffer and the underlying
				// stream has nothing available, so do a blocking read
				// from the stream.
				fillBuffer()
			}
			if (bytesInBuffer == 0) {
				return -1
			}
			val bytesToCopy = Math.min(len, bytesInBuffer)
			System.arraycopy(buffer, 0, b, off, bytesToCopy)
			System.arraycopy(buffer, bytesToCopy, buffer, 0, buffer.size - bytesToCopy)
			bytesInBuffer -= bytesToCopy
			return bytesToCopy
		}

		@Throws(IOException::class)
		private fun fillBuffer() {
			val readBytes = inputStream.read(workingBuffer, 0, Math.min(workingBuffer.size, buffer.size - bytesInBuffer))
			if (readBytes == -1) {
				return
			}
			var i = 0
			while (i < readBytes) {
				if (workingBuffer[i] == COMMAND_IAC) {
					i++
					if (Arrays.asList(COMMAND_DO, COMMAND_DONT, COMMAND_WILL, COMMAND_WONT).contains(workingBuffer[i])) {
						parseCommand(workingBuffer, i, readBytes)
						++i
						i++
						continue
					} else if (workingBuffer[i] == COMMAND_SUBNEGOTIATION) {   //0xFA = SB = Subnegotiation
						i += parseSubNegotiation(workingBuffer, ++i, readBytes)
						continue
					} else if (workingBuffer[i] != COMMAND_IAC) {   //Double IAC = 255
						System.err.println("Unknown Telnet command: " + workingBuffer[i])
					}
				}
				buffer[bytesInBuffer++] = workingBuffer[i]
				i++
			}
		}

		@Throws(IOException::class)
		private fun parseCommand(buffer: ByteArray, position: Int, max: Int) {
			if (position + 1 >= max) {
				throw IllegalStateException("State error, we got a command signal from the remote telnet client but " + "not enough characters available in the stream")
			}
			val command = buffer[position]
			val value = buffer[position + 1]
			when (command) {
				COMMAND_DO, COMMAND_DONT -> if (value == OPTION_SUPPRESS_GO_AHEAD) {
					negotiationState.isSuppressGoAhead = command == COMMAND_DO
					eventListener!!.requestReply(command == COMMAND_DO, value)
				} else if (value == OPTION_EXTEND_ASCII) {
					negotiationState.isExtendedAscii = command == COMMAND_DO
					eventListener!!.requestReply(command == COMMAND_DO, value)
				} else {
					negotiationState.onUnsupportedRequestCommand(command == COMMAND_DO, value)
				}
				COMMAND_WILL, COMMAND_WONT -> if (value == OPTION_ECHO) {
					negotiationState.isClientEcho = command == COMMAND_WILL
				} else if (value == OPTION_LINEMODE) {
					negotiationState.isClientLineMode0 = command == COMMAND_WILL
				} else if (value == OPTION_NAWS) {
					negotiationState.isClientResizeNotification = command == COMMAND_WILL
				} else {
					negotiationState.onUnsupportedStateCommand(command == COMMAND_WILL, value)
				}
				else -> throw UnsupportedOperationException("No command handler implemented for " + TelnetProtocol.CODE_TO_NAME[command])
			}
		}

		private fun parseSubNegotiation(buffer: ByteArray, position: Int, max: Int): Int {
			var position = position
			val originalPosition = position

			//Read operation
			val operation = buffer[position++]

			//Read until [IAC SE]
			val outputBuffer = ByteArrayOutputStream()
			while (position < max) {
				val read = buffer[position]
				if (read != COMMAND_IAC) {
					outputBuffer.write(read.toInt())
				} else {
					if (position + 1 == max) {
						throw IllegalStateException("State error, unexpected end of buffer when reading subnegotiation")
					}
					position++
					if (buffer[position] == COMMAND_IAC) {
						outputBuffer.write(COMMAND_IAC.toInt())    //Escaped IAC
					} else if (buffer[position] == COMMAND_SUBNEGOTIATION_END) {
						parseSubNegotiation(operation, outputBuffer.toByteArray())
						return ++position - originalPosition
					}
				}
				position++
			}
			throw IllegalStateException("State error, unexpected end of buffer when reading subnegotiation, no IAC SE")
		}

		private fun parseSubNegotiation(option: Byte, additionalData: ByteArray) {
			when (option) {
				OPTION_NAWS -> eventListener!!.onResize(
					convertTwoBytesToInt2(additionalData[1], additionalData[0]),
					convertTwoBytesToInt2(additionalData[3], additionalData[2]))
				OPTION_LINEMODE -> {
				}
				else -> negotiationState.onUnsupportedSubnegotiation(option, additionalData)
			}//We don't parse this, as this is a very complicated command :(
			//Let's leave it for now, fingers crossed
		}
	}

	companion object {

		private fun convertTwoBytesToInt2(b1: Byte, b2: Byte): Int {
			return b2 and 0xFF shl 8 or (b1 and 0xFF)
		}
	}
}
