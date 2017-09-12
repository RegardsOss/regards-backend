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
package fr.cnes.regards.modules.acquisition.plugins.ssalto;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.acquisition.domain.model.Attribute;
import fr.cnes.regards.modules.acquisition.domain.model.AttributeFactory;
import fr.cnes.regards.modules.acquisition.domain.model.AttributeTypeEnum;
import fr.cnes.regards.modules.acquisition.exception.DomainModelException;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.exception.PluginAcquisitionException;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.properties.PluginsRepositoryProperties;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.tools.NetCdfFileHelper;

/**
 * 
 * @author Christophe Mertz
 *
 */
@Plugin(author = "CS-SI", description = "Jason2OgdrProductMetadataPlugin", owner = "CNES", contact = "CS-SI",
        id = "Jason2OgdrProductMetadataPlugin", licence = "Apache ", url = "http://regards.org", version = "1.0")
public class Jason2OgdrProductMetadataPlugin extends Jason2ProductMetadataPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(Jason2OgdrProductMetadataPlugin.class);

    private static Pattern fileNamePattern = Pattern.compile("JA2_OP([a-zA-Z0-9]{1})_.*");

    private static final String PRODUCT_OPTION = "PRODUCT_OPTION";

    private static final String TRANSACTION_FILE = "OGDRTranslationFile.properties";

    @Autowired
    private PluginsRepositoryProperties pluginsRepositoryProperties;

    @Override
    protected PluginsRepositoryProperties getPluginsRepositoryProperties() {
        return pluginsRepositoryProperties;
    }

    /**
     *
     * @param fileMap
     * @param attributeMap
     * @throws PluginAcquisitionException
     */
    private void registerOptionAttribute(Map<File, ?> fileMap, Map<Integer, Attribute> attributeMap)
            throws PluginAcquisitionException {
        LOGGER.info("START building attribute " + PRODUCT_OPTION);
        Attribute optionAttribute;
        try {
            optionAttribute = AttributeFactory.createAttribute(AttributeTypeEnum.TYPE_STRING, PRODUCT_OPTION,
                                                               getOptionValue(fileMap.keySet()));
            attributeValueMap.put(PRODUCT_OPTION, optionAttribute.getValueList());

        } catch (final DomainModelException e) {
            final String msg = "unable to create attribute" + PRODUCT_OPTION;
            LOGGER.error(msg);
            throw new PluginAcquisitionException(msg, e);
        }
        registerAttribute(PRODUCT_OPTION, attributeMap, optionAttribute);
        LOGGER.info("END building attribute " + PRODUCT_OPTION);
    }

    /**
     *
     * @param acquisitionFileList
     * @return
     * @throws PluginAcquisitionException
     */
    protected List<String> getOptionValue(Collection<File> acquisitionFileList) throws PluginAcquisitionException {
        String value = null;
        for (final File file : acquisitionFileList) {
            final Matcher fileNameMatcher = fileNamePattern.matcher(file.getName());
            final NetCdfFileHelper helper = new NetCdfFileHelper(file);
            final String producer = helper.getGlobalAttributeStringValue("institution", null);
            String productType;
            if (fileNameMatcher.matches()) {
                productType = fileNameMatcher.group(1);
            } else {
                throw new PluginAcquisitionException("unable to get productType from fileName " + file.getName());
            }
            // translate the productType using
            final Properties translationProperties = new Properties();
            try {
                // Get file from project configured directory
                final String translationDirectory = pluginsRepositoryProperties.getPluginTranslationFilesDir();
                final File translationFile = new File(translationDirectory, TRANSACTION_FILE);
                if ((translationFile != null) && translationFile.exists() && translationFile.canRead()) {
                    translationProperties.load(new FileReader(translationFile));
                } else {
                    LOGGER.warn("Unable to find translaction file " + translationFile.getPath()
                            + ". Checking in classpath ...");
                    translationProperties.load(new FileReader("/ssalto/domain/plugins/impl/" + TRANSACTION_FILE));
                }
            } catch (IOException e) {
                final String msg = "unable to load the translation properties file";
                LOGGER.error(msg, e);
                throw new PluginAcquisitionException(msg, e);
            }
            productType = translationProperties.getProperty(productType);
            value = producer + " - " + productType;

        }
        final List<String> valueList = new ArrayList<>();
        valueList.add(value);
        return valueList;
    }

}
