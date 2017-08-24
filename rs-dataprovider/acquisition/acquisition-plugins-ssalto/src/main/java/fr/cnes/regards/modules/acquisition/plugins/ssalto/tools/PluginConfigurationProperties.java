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
package fr.cnes.regards.modules.acquisition.plugins.ssalto.tools;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.modules.acquisition.plugins.ssalto.finder.AttributeFinder;

/**
 * 
 * @author Christophe Mertz
 *
 */
public class PluginConfigurationProperties {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginConfigurationProperties.class);

    /**
     * Default class loader
     */
    private final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    private Properties pluginProperties;

    private static final String ORF_FILE_PATH_KEY = "ORF_FILEPATH_PATTERN";

    private static final String CYCLE_FILE_PATH_KEY = "CYCLE_FILEPATH";

    private static final String ARCS_FILEPATH_KEY = "ARCS_FILEPATH";

    private static final String SEPARATOR = ";";

    private static final String fileNameProperties = "pluginConfiguration.properties";

    private static final String pluginConfigurationLocationPath = "/ssalto/domain/plugins/impl/tools";

    /**
     * filePattern du nom du fichier
     * TODO CMZ : à confirmer utilité
     */
    protected String fileNamePattern;
    
    /**
     * liste des finder
     * TODO CMZ : à confirmer utilité
     */
    private SortedMap<Integer, AttributeFinder> finderList;

    /**
     * nom du projet utilisant le fichier properties : JASON, JASON2, ...</br>
     * Les proprietes du fichier properties seront prefixees par le nom du projet.
     */
    private String project;

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

    private void loadProperties() {
        pluginProperties = new Properties();

        String fileName = pluginConfigurationLocationPath + File.separator + fileNameProperties;

        try (InputStream input = classLoader.getResourceAsStream(fileName)) {
            if (input == null) {
                LOGGER.info("Unable to read plugin configuration properties file \"{}\"", fileName);
            } else {
                pluginProperties.load(input);
            }
        } catch (IOException e) {
            LOGGER.error("Error reading or closing plugin properties\"{}\" \n{}", fileName, e);
        }
    }

    private String getPropertyValue(String pPropertyValue) {
        if (project == null) {
            LOGGER.error("project was not set : JASON, JASON2 ...");
        }
        String propertyName = project + "_" + pPropertyValue;
        String propertyValue = pluginProperties.getProperty(propertyName);
        if (propertyValue == null) {
            LOGGER.error("Property not found " + propertyName + " in file " + pluginConfigurationLocationPath
                    + File.separator + fileNameProperties);
        }
        return propertyValue;
    }

    public String[] getOrfFilepath() {
        // test if project was set
        if (project == null) {
            LOGGER.error("project was not set : JASON, JASON2 ...");
        }
        String propertyName = project + "_" + ORF_FILE_PATH_KEY;
        String propertyValue = pluginProperties.getProperty(propertyName);

        if (propertyValue == null) {
            LOGGER.error("Property not found " + propertyName + " in file " + pluginConfigurationLocationPath
                    + File.separator + fileNameProperties);
        }
        String[] orfFilePath = propertyValue.split(SEPARATOR);
        return orfFilePath;
    }

    // TODO CMZ : à confirmer utilité
    public String getFileNamePattern() {
        return fileNamePattern;
    }

    // TODO CMZ : à confirmer utilité
    public void setFileNamePattern(String pFileNamePattern) {
        fileNamePattern = pFileNamePattern;
    }
    
    /**
     * ajoute un finder standard
     * 
     * @param pFinder
     * TODO CMZ : à confirmer utilité
     */
    public void addFileFinder(AttributeFinder pFinder) {
        if (finderList == null) {
            finderList = new TreeMap<>();
        }
        finderList.put(new Integer(pFinder.getOrder()), pFinder);
    }
    
    // TODO CMZ : à confirmer utilité
    public Collection<AttributeFinder> getFinderList() {
        if (finderList != null) {
            return finderList.values();
        } else {
            return null;
        }
    }

}
