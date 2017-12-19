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
 * 
 * @author Christophe Mertz
 *
 */
public class PluginConfigurationProperties {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginConfigurationProperties.class);

    protected Properties pluginProperties;

    protected static final String ORF_FILE_PATH_KEY = "ORF_FILEPATH_PATTERN";

    protected static final String CYCLE_FILE_PATH_KEY = "CYCLE_FILEPATH";

    protected static final String ARCS_FILEPATH_KEY = "ARCS_FILEPATH";

    private static final String SEPARATOR = ";";

    private static final String CYCLES_ORL_PROPERTIES = "pluginConfiguration.properties";

    private static final String LOG_PROPERTY_NOT_FOUND = "Property not found %s in file '%s'";

    private static final String LOG_PROJECT_NOT_SET = "The required project is not set : JASON, JASON2 ...";

    private static final String KEY_CONF_DIR = "regards.acquisition.ssalto.plugin-conf-path";

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
        String confPath = "";
        try {
            Properties confProperties = LoadTranslationProperties.getInstance().loadPluginsRepository();
            confPath = (String) confProperties.get(KEY_CONF_DIR);
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

    private String getPropertyValue(String value) {
        if (project == null) {
            LOGGER.error(LOG_PROJECT_NOT_SET);
        }

        String propertyName = project + "_" + value;
        String propertyValue = pluginProperties.getProperty(propertyName);

        if (propertyValue == null) {
            LOGGER.error(String.format(LOG_PROPERTY_NOT_FOUND, propertyName, CYCLES_ORL_PROPERTIES));
        }

        return propertyValue;
    }

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
