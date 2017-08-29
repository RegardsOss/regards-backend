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
package fr.cnes.regards.modules.acquisition.plugins.ssalto.finder;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.modules.acquisition.plugins.ssalto.exception.PluginAcquisitionException;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.properties.PluginsRespositoryProperties;

/**
 * ce finder recupere une valeur dans le nom d'un fichier a partir du filePattern et de la liste de groupe de capture et
 * ensuite utilise le fichier de traduction pour trouver la valeur correspondante.
 * 
 * @author Christophe Mertz
 *
 */
public class TranslatedFileNameFinder extends FileNameFinder {

    private static final Logger LOGGER = LoggerFactory.getLogger(TranslatedAttributeFromArcFile.class);
    

    /**
     * fichier de traduction
     *
     * @since 1.2
     */
    private Properties translationProperties_;
    
    @Autowired
    protected PluginsRespositoryProperties pluginsRespositoryProperties;


    /**
     * @param pTranslationPropertiesFilePath
     * @throws PluginAcquisitionException
     * @since 1.2
     */
    public void setTranslationProperties(String pTranslationPropertiesFilePath) throws PluginAcquisitionException {
        translationProperties_ = new Properties();

        try {
            // Get file from project configured directory
            String translationDirectory = pluginsRespositoryProperties.getPluginTranslationFilesDir();
            File translationFile = new File(translationDirectory, pTranslationPropertiesFilePath);
            if ((translationFile != null) && translationFile.exists() && translationFile.canRead()) {
                InputStream inStream = new FileInputStream(translationFile);
                translationProperties_.load(inStream);
            }
            else {
                LOGGER.warn("Unable to find translaction file " + translationFile.getPath()
                             + ". Checking in classpath ...");
                // TODO CMZ à confirmer
                File ff = new File("/ssalto/domain/plugins/impl" + pTranslationPropertiesFilePath);
                InputStream inStream = new FileInputStream(ff);
                translationProperties_.load(inStream);
            }
        }
        catch (Exception e) {
            String msg = "unable to load the translation properties file";
            LOGGER.error(msg, e);
            throw new PluginAcquisitionException(msg, e);
        }
    }

    @Override
    public List<?> getValueList(Map<File, ?> pFileMap, Map<String, List<? extends Object>> pAttributeValueMap)
            throws PluginAcquisitionException {
        @SuppressWarnings("unchecked")
        List<Object> valueList = (List<Object>) super.getValueList(pFileMap, pAttributeValueMap);
        List<Object> translatedValues = new ArrayList<>();
        for (Object element : valueList) {
            if (getTranslatedValue(element) != null) {
                translatedValues.add(getTranslatedValue(element));
            }
            else {
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
    protected Object getTranslatedValue(Object pValue) {
        return translationProperties_.get(pValue);
    }

    @Override
    public String toString() {
        StringBuffer buff = new StringBuffer();
        buff.append(super.toString());
        buff.append(" | translationProperties_").append(translationProperties_);
        return buff.toString();
    }
}
