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
package fr.cnes.regards.modules.acquisition.plugins.properties;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.modules.acquisition.exception.PluginAcquisitionException;
import fr.cnes.regards.modules.acquisition.finder.AbstractAttributeFinder;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.calc.LoadTranslationProperties;

/**
 * Class to load the <b>pluginConfiguration.properties</b> file
 * 
 * @author Christophe Mertz
 *
 */
public class PluginConfigurationProperties {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginConfigurationProperties.class);

    /**
     * The {@link Properties} read in the ORL file properties
     */
    protected Properties pluginProperties;

    /**
     * The ORF_FILEPATH_PATTERN key used in the ORL file properties
     */
    protected static final String ORF_FILE_PATH_KEY = "ORF_FILEPATH_PATTERN";

    /**
     * The CYCLE_FILEPATH key used in the ORL file properties
     */
    protected static final String CYCLE_FILE_PATH_KEY = "CYCLE_FILEPATH";

    /**
     * The ARCS_FILEPATH key used in the ORL file properties
     */
    protected static final String ARCS_FILEPATH_KEY = "ARCS_FILEPATH";

    /**
     * The separator's value used in the ORL file properties
     */
    private static final String SEPARATOR = ";";

    /**
     * The ORL file properties name
     */
    private static final String CYCLES_ORL_PROPERTIES = "pluginConfiguration.properties";

    /**
     * Log message
     */
    private static final String LOG_PROPERTY_NOT_FOUND = "Property not found %s in file '%s'";

    /**
     * Log message
     */
    private static final String LOG_PROJECT_NOT_SET = "The required project is not set : JASON, JASON2 ...";

    /**
     * filePattern du nom du fichier
     */
    protected String fileNamePattern;

    /**
     * liste des finder
     */
    private SortedMap<Integer, AbstractAttributeFinder> finderList;

    /**
     * nom du projet utilisant le fichier properties : JASON, JASON2, ...</br>
     * Les proprietes du fichier properties seront prefixees par le nom du projet.
     */
    private String project;

    /**
     * Default constructor
     */
    public PluginConfigurationProperties() {
        super();
        loadProperties();
    }

    public void setProject(String projectName) {
        project = projectName.toUpperCase();
    }

    public String getCycleFileFilepath() {
        return getPropertyValue(CYCLE_FILE_PATH_KEY);
    }

    public String getArcPath() {
        return getPropertyValue(ARCS_FILEPATH_KEY);
    }

    /**
     * Load the {@link Properties} in pluginConfiguration.properties file
     */
    private void loadProperties() {
        String confPath = "";
        try {
            Properties confProperties = LoadTranslationProperties.getInstance().loadPluginsRepository();
            confPath = (String) confProperties.get("regards.acquisition.ssalto.cycle-orf-conf-path");
        } catch (PluginAcquisitionException e) {
            LOGGER.error(e.getMessage(), e);
        }

        pluginProperties = new Properties();
        File cycleOrfFile = new File(confPath, CYCLES_ORL_PROPERTIES);

        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(cycleOrfFile.getPath())) {
            pluginProperties.load(stream);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     * Get the property value for the current project for a key 
     * @param key the key to search in the file properties for the current project 
     * @return the property value for the current project for a key
     */
    private String getPropertyValue(String key) {
        if (project == null) {
            LOGGER.error(LOG_PROJECT_NOT_SET);
        }

        String propertyName = project + "_" + key;
        String propertyValue = pluginProperties.getProperty(propertyName);

        if (propertyValue == null) {
            LOGGER.error(String.format(LOG_PROPERTY_NOT_FOUND, propertyName, CYCLES_ORL_PROPERTIES));
        }

        return propertyValue;
    }

    /**
     * Get the value's properties ORF_FILEPATH_PATTERN for the current project
     * @return
     */
    public String[] getOrfFilepath() {
        String[] orfFilePath = new String[0];

        if (project == null) {
            LOGGER.error(LOG_PROJECT_NOT_SET);
            return orfFilePath;
        }

        String propertyName = project + "_" + ORF_FILE_PATH_KEY;
        String propertyValue = pluginProperties.getProperty(propertyName);

        if (propertyValue == null) {
            LOGGER.error(String.format(LOG_PROPERTY_NOT_FOUND, propertyName, CYCLES_ORL_PROPERTIES));
            return orfFilePath;
        }

        orfFilePath = propertyValue.split(SEPARATOR);

        return orfFilePath;
    }

    public String getFileNamePattern() {
        return fileNamePattern;
    }

    public void setFileNamePattern(String filePattern) {
        fileNamePattern = filePattern;
    }

    /**
     * ajoute un finder standard
     * 
     * @param finder
     */
    public void addFileFinder(AbstractAttributeFinder finder) {
        if (finderList == null) {
            finderList = new TreeMap<>();
        }
        finderList.put(Integer.valueOf(finder.getOrder()), finder);
    }

    public Collection<AbstractAttributeFinder> getFinderList() {
        if (finderList == null) {
            return new ArrayList<>();
        } else {
            return finderList.values();
        }
    }

}
