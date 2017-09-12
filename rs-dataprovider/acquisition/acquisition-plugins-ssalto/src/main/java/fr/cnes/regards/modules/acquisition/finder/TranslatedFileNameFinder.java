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
package fr.cnes.regards.modules.acquisition.finder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fr.cnes.regards.modules.acquisition.exception.PluginAcquisitionException;
import fr.cnes.regards.modules.acquisition.plugins.properties.PluginsRepositoryProperties;

/**
 * ce finder recupere une valeur dans le nom d'un fichier a partir du filePattern et de la liste de groupe de capture et
 * ensuite utilise le fichier de traduction pour trouver la valeur correspondante
 * 
 * @author Christophe Mertz
 *
 */
@Component
public class TranslatedFileNameFinder extends FileNameFinder {

    private static final Logger LOGGER = LoggerFactory.getLogger(TranslatedAttributeFromArcFile.class);

    private static final String PATH_PROPERTIES = "income/plugins/translations";

    /**
     * fichier de traduction
     */
    private Properties translationProperties;

    @Autowired
    protected PluginsRepositoryProperties pluginsRepositoryProperties;

    /**
     * @param translationPropertiesFilePath
     * @throws PluginAcquisitionException
     */
    public void setTranslationProperties(String translationPropertiesFilePath) throws PluginAcquisitionException {
        translationProperties = new Properties();

//        try {
            // Get file from project configured directory
            // TODO CMZ à revoir pour le moment pluginsRepositoryProperties est null

            //            String translationDirectory = pluginsRepositoryProperties.getPluginTranslationFilesDir();
            //            File translationFile = new File(translationDirectory, translationPropertiesFilePath);
            //            if ((translationFile != null) && translationFile.exists() && translationFile.canRead()) {
            //                InputStream inStream = new FileInputStream(translationFile);
            //                translationProperties.load(inStream);
            //            } else {
            //                LOGGER.warn("Unable to find translaction file " + translationFile.getPath()
            //                        + ". Checking in classpath ...");
            //                File ff = new File("income/plugins/translations" + translationPropertiesFilePath);
            //                InputStream inStream = new FileInputStream(ff);
            //                translationProperties.load(inStream);

            try (InputStream stream = getClass().getClassLoader()
                    .getResourceAsStream(PATH_PROPERTIES + translationPropertiesFilePath)) {
                translationProperties.load(stream);
            } catch (IOException e) {
                String msg = "unable to load the translation properties file";
                LOGGER.error(msg, e);
                throw new PluginAcquisitionException(msg, e);
            }

            //            }
        //        } catch (Exception e) {
        //            String msg = "unable to load the translation properties file";
        //            LOGGER.error(msg, e);
        //            throw new PluginAcquisitionException(msg, e);
        //        }
    }

    @Override
    public List<?> getValueList(Map<File, ?> fileMap, Map<String, List<? extends Object>> attributeValueMap)
            throws PluginAcquisitionException {
        @SuppressWarnings("unchecked")
        List<Object> valueList = (List<Object>) super.getValueList(fileMap, attributeValueMap);
        List<Object> translatedValues = new ArrayList<>();
        for (Object element : valueList) {
            if (getTranslatedValue(element) != null) {
                translatedValues.add(getTranslatedValue(element));
            } else {
                LOGGER.debug("unable to find translation for value " + element.toString());
            }
        }
        return translatedValues;
    }

    /**
     * recupere la valeur dans le fichier de traduction dont la clef est pValue.
     *
     * @param pvalue
     * @return
     */
    protected Object getTranslatedValue(Object value) {
        return translationProperties.get(value);
    }

    @Override
    public String toString() {
        StringBuilder buff = new StringBuilder(super.toString());
        buff.append(" | translationProperties").append(translationProperties);
        return buff.toString();
    }
}
