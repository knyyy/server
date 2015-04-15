/*******************************************************************************
 * Copyright 2013 Open mHealth
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.ohmage.jee.listener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

/**
 * <p>
 * Attempts to read the configuration file, if it exists, and stores it. Any
 * number of the configuration options may be chosen, and all users of this
 * class should have a default in place.
 * </p>
 *
 * <p>
 * The file will be loaded when the web application starts. The main usage for
 * this class is the {@link #getCustomProperties()} function.
 * </p>
 *
 * @author John Jenkins
 */
public class ConfigurationFileImport
	extends PropertyPlaceholderConfigurer
	implements ServletContextListener {

	/**
	 * The location of the default configuration file.
	 */
	private static final String CONFIG_FILE_DEFAULT =
		"WEB-INF/config/default.properties";
	/**
	 * The default location for the configuration file on Windows.
	 */
	private static final String CONFIG_FILE_DEFAULT_WINDOWS =
		"%PROGRAMDATA%\\OpenmHealth\\config\\ohmage.properties";
	/**
	 * The default location for the configuration file on POSIX-compliant
	 * systems.
	 */
	private static final String CONFIG_FILE_DEFAULT_POSIX =
		"/etc/ohmage.conf";

	/**
	 * The logger for this class.
	 */
	private static final Logger LOGGER =
		Logger.getLogger(ConfigurationFileImport.class.getName());

	/**
	 * The properties merged between the defaults and the custom ones.
	 */
	private static final Properties PROPERTIES = new Properties();

	/**
	 * The array of Properties files that were decoded from all of the possible
	 * configuration files.
	 *
	 * @see #CONFIG_FILE_DEFAULT
	 * @see #CONFIG_FILE_DEFAULT_POSIX
	 * @see #CONFIG_FILE_DEFAULT_WINDOWS
	 */
	private static final Properties[] PROPERTIES_ARRAY = new Properties[2];

	/**
	 * Default constructor. This will set the properties array for the
	 * {@link PropertyPlaceholderConfigurer}, which may be empty if
	 * {@link #contextInitialized(ServletContextEvent)} has not yet been
	 * called at least once.
	 *
	 * @see PropertyPlaceholderConfigurer#setPropertiesArray(Properties[])
	 */
	public ConfigurationFileImport() {
		setPropertiesArray(PROPERTIES_ARRAY);
	}

	/**
	 * Find the log file, if it exists, and add its properties to the system
	 * properties.
	 */
	@Override
	public void contextInitialized(ServletContextEvent event) {
		// An empty Properties object that will be populated with the default
		// configuration.
		Properties defaultProperties = new Properties();
		File defaultConfiguration;
		if(System.getProperty("os.name").toLowerCase().startsWith("mac")){
			defaultConfiguration = new File(
				event.getServletContext().getRealPath("/") + "/" +
					CONFIG_FILE_DEFAULT);
		}
		else{
			defaultConfiguration = new File(
				event.getServletContext().getRealPath("/") +
					CONFIG_FILE_DEFAULT);
		}
		try {
			defaultProperties.load(new FileReader(defaultConfiguration));

		}
		// The default properties file didn't exist, which is alarming.
		catch(FileNotFoundException e) {
			LOGGER
				.log(
					Level.WARNING,
					"The default properties file is missing: " +
						defaultConfiguration.getAbsolutePath(),
					e);

		}
		// There was an error reading the default properties file.
		catch(IOException e) {
			LOGGER
				.log(
					Level.WARNING,
					"There was an error reading the default properties " +
						"file: " +
						defaultConfiguration.getAbsolutePath(),
					e);
		}

		// Get a handler for the properties file based on the operating system.
		File propertiesFile;
		if(System.getProperty("os.name").contains("Windows")) {
			propertiesFile = new File(CONFIG_FILE_DEFAULT_WINDOWS);
		}
		else {
			propertiesFile = new File(CONFIG_FILE_DEFAULT_POSIX);
		}

		// Attempts to retrieve the custom configuration file and store it.
		Properties customProperties = new Properties();
		try {
			customProperties.load(new FileReader(propertiesFile));
			LOGGER
				.log(
					Level.INFO,
					"The properties file was imported: " +
						propertiesFile.getAbsolutePath());
		}
		// The properties file didn't exist, which is fine.
		catch(FileNotFoundException e) {
			LOGGER
				.log(
					Level.INFO,
					"The properties file does not exist: " +
						propertiesFile.getAbsolutePath());
		}
		// There was a problem reading the properties.
		catch(IOException e) {
			LOGGER
				.log(
					Level.WARNING,
					"There was an error reading the properties file: " +
						propertiesFile.getAbsolutePath(),
					e);
		}

		// Set the properties files.
		PROPERTIES_ARRAY[0] = defaultProperties;
		PROPERTIES_ARRAY[1] = customProperties;

		// Create a merged properties and save it.
		for(Object key : defaultProperties.keySet()) {
			PROPERTIES.put(key, defaultProperties.get(key));
		}
		for(Object key : customProperties.keySet()) {
			PROPERTIES.put(key, customProperties.get(key));
		}
	}

	/*
	 * (non-Javadoc)
	 * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.ServletContextEvent)
	 */
	@Override
	public void contextDestroyed(ServletContextEvent event) {
		// Do nothing.
	}

	/**
	 * Returns the custom properties defined by the external configuration
	 * file.
	 *
	 * @return A valid {@link Properties} object, which may or may not contain
	 * 		   the desired property.
	 */
	public static Properties getCustomProperties() {
		return PROPERTIES;
	}
}