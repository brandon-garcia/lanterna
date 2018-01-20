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
package com.googlecode.lanterna.bundle

import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL
import java.net.URLConnection
import java.text.MessageFormat
import java.util.Locale
import java.util.PropertyResourceBundle
import java.util.ResourceBundle

/**
 * This class permits to deal easily with bundles.
 * @author silveryocha
 */
abstract class BundleLocator
/**
 * Hidden constructor.
 * @param bundleName the name of the bundle.
 */
protected constructor(private val bundleName: String) {

	/**
	 * Method that centralizes the way to get the value associated to a bundle key.
	 * @param locale the locale.
	 * @param key the key searched for.
	 * @param parameters the parameters to apply to the value associated to the key.
	 * @return the formatted value associated to the given key. Empty string if no value exists for
	 * the given key.
	 */
	protected fun getBundleKeyValue(locale: Locale, key: String, vararg parameters: Any): String? {
		var value: String? = null
		try {
			value = getBundle(locale).getString(key)
		} catch (ignore: Exception) {
		}

		return if (value != null) MessageFormat.format(value, *parameters) else null
	}

	/**
	 * Gets the right bundle.<br></br>
	 * A cache is handled as well as the concurrent accesses.
	 * @param locale the locale.
	 * @return the instance of the bundle.
	 */
	private fun getBundle(locale: Locale): ResourceBundle {
		return ResourceBundle.getBundle(bundleName, locale, loader, UTF8Control())
	}

	// Taken from:
	// http://stackoverflow.com/questions/4659929/how-to-use-utf-8-in-resource-properties-with-resourcebundle
	// I politely refuse to use ISO-8859-1 in these *multi-lingual* property files
	// All credits to poster BalusC (http://stackoverflow.com/users/157882/balusc)
	private class UTF8Control : ResourceBundle.Control() {
		@Throws(IllegalAccessException::class, InstantiationException::class, IOException::class)
		override fun newBundle(baseName: String, locale: Locale, format: String, loader: ClassLoader, reload: Boolean): ResourceBundle? {
			// The below is a copy of the default implementation.
			val bundleName = toBundleName(baseName, locale)
			val resourceName = toResourceName(bundleName, "properties")
			var bundle: ResourceBundle? = null
			var stream: InputStream? = null
			if (reload) {
				val url = loader.getResource(resourceName)
				if (url != null) {
					val connection = url.openConnection()
					if (connection != null) {
						connection.useCaches = false
						stream = connection.getInputStream()
					}
				}
			} else {
				stream = loader.getResourceAsStream(resourceName)
			}
			if (stream != null) {
				try {
					// Only this line is changed to make it to read properties files as UTF-8.
					bundle = PropertyResourceBundle(InputStreamReader(stream, "UTF-8"))
				} finally {
					stream.close()
				}
			}
			return bundle
		}
	}

	companion object {
		private val loader = BundleLocator::class.java!!.getClassLoader()
	}
}
