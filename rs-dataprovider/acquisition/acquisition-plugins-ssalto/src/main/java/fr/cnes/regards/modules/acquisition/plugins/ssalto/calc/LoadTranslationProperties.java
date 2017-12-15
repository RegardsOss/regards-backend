/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.acquisition.plugins.ssalto.calc;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.modules.acquisition.exception.PluginAcquisitionException;
import fr.cnes.regards.modules.acquisition.finder.TranslatedAttributeFromArcFile;

/**
 * Load a translation properties file from the classpath .<br>
 * The folder path for the translation file is read in the file <b>pluginsRepository.properties</b>, the key <b>regards.acquisition.ssalto.plugin-translation-files-dir</b>.
 * If the translation file can not be load from this path, try to load the translation file in the default path <b>income/plugins/translations</b>
 * 
 * @author Christophe Mertz
 *
 */
public final class LoadTranslationProperties {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(TranslatedAttributeFromArcFile.class);

    /**
     * Default path for the translation files
     */
    private static final String DEFAULT_PATH_PROPERTIES = "income/plugins/translations";

    /**
     * File repository properties
     */
    private static final String FILE_REPO_PROPERTIES = "pluginsRepository.properties";

    /**
     * Key for the property translation file path 
     */
    private static final String KEY_TRANSLATION_DIR = "regards.acquisition.ssalto.plugin-translation-files-dir";

    /**
     * An instance of {@link LoadTranslationProperties}
     */
    private static final LoadTranslationProperties instance = new LoadTranslationProperties();

    /**
     * Default private constructor
     */
    private LoadTranslationProperties() {
    }

    /**
     * Get the {@link LoadTranslationProperties} instance
     * @return the {@link TranslatedAttributeFromArcFile} instance
     */
    public static LoadTranslationProperties getInstance() {
        return instance;
    }

    /**
     * Load a translation properties file
     * @param translationPropertiesFilePath a path to the translation properties file to load
     * @return the {@link Properties} loaded
     * @throws PluginAcquisitionException if an error occurs
     */
    public Properties load(String translationPropertiesFilePath) throws PluginAcquisitionException {
        Properties pluginsRepo = new Properties();

        // Load plugins repository properties
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(FILE_REPO_PROPERTIES)) {
            pluginsRepo.load(stream);
        } catch (IOException e) {
            LOGGER.error("unable to read " + FILE_REPO_PROPERTIES, e);
            throw new PluginAcquisitionException(e.getMessage());
        }

        String translationDirectory = (String) pluginsRepo.get(KEY_TRANSLATION_DIR);
        File translationFile = new File(translationDirectory, translationPropertiesFilePath);

        boolean isReadable = true;
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(translationFile.getPath())) {
            // Try to read the InputStream
            stream.available();
        } catch (IOException e) {
            LOGGER.warn("NOT FOUND " + translationFile.getPath()); // NOSONAR
            isReadable = false;
        }

        if (!isReadable) {
            // the translation file is not find or not readable
            LOGGER.warn("Unable to find translaction file {}. Checking in classpath ...", translationFile.getPath());
            translationFile = new File(DEFAULT_PATH_PROPERTIES + translationPropertiesFilePath);
        }

        Properties translationProperties = new Properties();
        // Load the translation file
        try (InputStream inStream = getClass().getClassLoader().getResourceAsStream(translationFile.getPath())) {
            translationProperties.load(inStream);
        } catch (IOException e) {
            LOGGER.error("unable to read " + translationFile.getPath(), e);
        }

        return translationProperties;
    }
}
