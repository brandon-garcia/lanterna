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
package com.googlecode.lanterna.gui2.dialogs

import com.googlecode.lanterna.TerminalTextUtils
import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.gui2.*

import java.io.File
import java.util.Arrays
import java.util.Comparator

/**
 * Dialog that allows the user to iterate the file system and pick file to open/save
 *
 * @author Martin
 */
class FileDialog
/**
 * Default constructor for `FileDialog`
 * @param title Title of the dialog
 * @param description Description of the dialog, is displayed at the top of the content area
 * @param actionLabel Label to use on the "confirm" button, for example "open" or "save"
 * @param dialogSize Rough estimation of how big you want the dialog to be
 * @param showHiddenFilesAndDirs If `true`, hidden files and directories will be visible
 * @param selectedObject Initially selected file node
 */
(
	title: String,
	description: String?,
	actionLabel: String,
	dialogSize: TerminalSize,
	private val showHiddenFilesAndDirs: Boolean,
	selectedObject: File?) : DialogWindow(title) {

	private val fileListBox: ActionListBox
	private val directoryListBox: ActionListBox
	private val fileBox: TextBox
	private val okButton: Button

	private var directory: File? = null
	private var selectedFile: File? = null

	init {
		var selectedObject = selectedObject
		this.selectedFile = null

		if (selectedObject == null || !selectedObject.exists()) {
			selectedObject = File("").absoluteFile
		}
		selectedObject = selectedObject!!.absoluteFile

		val contentPane = Panel()
		contentPane.setLayoutManager(GridLayout(2))

		if (description != null) {
			Label(description)
				.setLayoutData(
					GridLayout.createLayoutData(
						GridLayout.Alignment.BEGINNING,
						GridLayout.Alignment.CENTER,
						false,
						false,
						2,
						1))
				.addTo(contentPane)
		}

		val unitWidth = dialogSize.columns / 3
		val unitHeight = dialogSize.rows

		FileSystemLocationLabel()
			.setLayoutData(GridLayout.createLayoutData(
				GridLayout.Alignment.FILL,
				GridLayout.Alignment.CENTER,
				true,
				false,
				2,
				1))
			.addTo(contentPane)

		fileListBox = ActionListBox(TerminalSize(unitWidth * 2, unitHeight))
		fileListBox.withBorder(Borders.singleLine())
			.setLayoutData(GridLayout.createLayoutData(
				GridLayout.Alignment.BEGINNING,
				GridLayout.Alignment.CENTER,
				false,
				false))
			.addTo(contentPane)
		directoryListBox = ActionListBox(TerminalSize(unitWidth, unitHeight))
		directoryListBox.withBorder(Borders.singleLine())
			.addTo(contentPane)

		fileBox = TextBox()
			.setLayoutData(GridLayout.createLayoutData(
				GridLayout.Alignment.FILL,
				GridLayout.Alignment.CENTER,
				true,
				false,
				2,
				1))
			.addTo(contentPane)

		Separator(Direction.HORIZONTAL)
			.setLayoutData(
				GridLayout.createLayoutData(
					GridLayout.Alignment.FILL,
					GridLayout.Alignment.CENTER,
					true,
					false,
					2,
					1))
			.addTo(contentPane)

		okButton = Button(actionLabel, OkHandler())
		Panels.grid(2,
			okButton,
			Button(LocalizedString.Cancel.toString()!!, CancelHandler()))
			.setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.END, GridLayout.Alignment.CENTER, false, false, 2, 1))
			.addTo(contentPane)

		if (selectedObject!!.isFile) {
			directory = selectedObject.parentFile
			fileBox.setText(selectedObject.name)
		} else if (selectedObject.isDirectory) {
			directory = selectedObject
		}

		reloadViews(directory)
		component = contentPane
	}

	/**
	 * {@inheritDoc}
	 * @param textGUI Text GUI to add the dialog to
	 * @return The file which was selected in the dialog or `null` if the dialog was cancelled
	 */
	override fun showDialog(textGUI: WindowBasedTextGUI): File? {
		selectedFile = null
		super.showDialog(textGUI)
		return selectedFile
	}

	private inner class OkHandler : Runnable {
		override fun run() {
			if (!fileBox.text.isEmpty()) {
				selectedFile = File(directory, fileBox.text)
				close()
			} else {
				MessageDialog.showMessageDialog(getTextGUI(), "Error", "Please select a valid file name", MessageDialogButton.OK)
			}
		}
	}

	private inner class CancelHandler : Runnable {
		override fun run() {
			selectedFile = null
			close()
		}
	}

	private inner class DoNothing : Runnable {
		override fun run() {}
	}

	private fun reloadViews(directory: File) {
		directoryListBox.clearItems()
		fileListBox.clearItems()
		val entries = directory.listFiles() ?: return
		Arrays.sort(entries) { o1, o2 -> o1.name.toLowerCase().compareTo(o2.name.toLowerCase()) }
		if (directory.absoluteFile.parentFile != null) {
			directoryListBox.addItem("..", Runnable {
				this@FileDialog.directory = directory.absoluteFile.parentFile
				reloadViews(directory.absoluteFile.parentFile)
			})
		} else {
			val roots = File.listRoots()
			for (entry in roots) {
				if (entry.canRead()) {
					directoryListBox.addItem('[' + entry.path + ']', Runnable {
						this@FileDialog.directory = entry
						reloadViews(entry)
					})
				}
			}
		}
		for (entry in entries) {
			if (entry.isHidden && !showHiddenFilesAndDirs) {
				continue
			}
			if (entry.isDirectory) {
				directoryListBox.addItem(entry.name, Runnable {
					this@FileDialog.directory = entry
					reloadViews(entry)
				})
			} else {
				fileListBox.addItem(entry.name, Runnable {
					fileBox.setText(entry.name)
					setFocusedInteractable(okButton)
				})
			}
		}
		if (fileListBox.isEmpty) {
			fileListBox.addItem("<empty>", DoNothing())
		}
	}

	private inner class FileSystemLocationLabel : Label("") {
		init {
			setPreferredSize(TerminalSize.ONE)
		}

		public override fun onBeforeDrawing() {
			val area = size
			var absolutePath = directory!!.absolutePath
			val absolutePathLengthInColumns = TerminalTextUtils.getColumnWidth(absolutePath)
			if (area!!.columns < absolutePathLengthInColumns) {
				absolutePath = absolutePath.substring(absolutePathLengthInColumns - area.columns)
				absolutePath = "..." + absolutePath.substring(Math.min(absolutePathLengthInColumns, 3))
			}
			text = absolutePath
		}
	}
}
