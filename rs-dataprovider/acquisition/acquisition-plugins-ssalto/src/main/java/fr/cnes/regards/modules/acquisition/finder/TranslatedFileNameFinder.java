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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import fr.cnes.regards.modules.acquisition.exception.PluginAcquisitionException;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.calc.LoadTranslationProperties;

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

    /**
     * {@link Properties} load in the translation file
     */
    private Properties translationProperties = new Properties();

    /**
     * Load the translation file
     * @param translationPropertiesFilePath the translation file to load
     * @throws PluginAcquisitionException an error occurs when reading the translation file
     */
    public void setTranslationProperties(String translationPropertiesFilePath) throws PluginAcquisitionException {
        translationProperties = LoadTranslationProperties.getInstance().load(translationPropertiesFilePath);
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
     * Get the value in the translation file for a key
     * @param key get the value of this key
     * @return the value in the translation file for a key
     */
    protected Object getTranslatedValue(Object key) {
        return translationProperties.get(key);
    }

    @Override
    public String toString() {
        StringBuilder buff = new StringBuilder(super.toString());
        buff.append(" | translationProperties").append(translationProperties);
        return buff.toString();
    }

}
