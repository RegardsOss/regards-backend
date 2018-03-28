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
package fr.cnes.regards.modules.acquisition.plugins.ssalto.productmetadata;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.acquisition.domain.model.Attribute;
import fr.cnes.regards.modules.acquisition.domain.model.AttributeFactory;
import fr.cnes.regards.modules.acquisition.domain.model.AttributeTypeEnum;
import fr.cnes.regards.modules.acquisition.exception.DomainModelException;
import fr.cnes.regards.modules.acquisition.exception.PluginAcquisitionException;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.calc.LoadTranslationProperties;
import fr.cnes.regards.modules.acquisition.tools.NetCdfFileHelper;

/**
 * Metadata caculation's plugin for Jason2 Ogdr products.
 *
 * @author Christophe Mertz
 *
 */
@Plugin(author = "CS-SI", description = "Metadata caculation's plugin for Jason2 Ogdr products", owner = "CNES",
        contact = "CS-SI", id = "Jason2OgdrProductMetadataPlugin", licence = "Apache ", url = "http://regards.org",
        version = "1.0")
public class Jason2OgdrProductMetadataPlugin extends Jason2ProductMetadataPlugin {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Jason2OgdrProductMetadataPlugin.class);

    /**
     * The {@link Pattern} "JA2_OP([a-zA-Z0-9]{1})_.*"
     */
    private static Pattern fileNamePattern = Pattern.compile("JA2_OP([a-zA-Z0-9]{1})_.*");

    /**
     * Attribute PRODUCT_OPTION
     */
    private static final String PRODUCT_OPTION = "PRODUCT_OPTION";

    /**
     * The OGDR translation properties file : OGDRTranslationFile.properties
     */
    private static final String TRANSACTION_FILE = "/OGDRTranslationFile.properties";

    @Override
    protected void doCreateDependantSpecificAttributes(Map<File, ?> pFileMap,
            Map<String, List<? extends Object>> attributeValueMap, Map<Integer, Attribute> pAttributeMap)
            throws ModuleException {
        registerOptionAttribute(pFileMap, attributeValueMap, pAttributeMap);
    }

    /**
     * Build an {@link Attribute} PRODUCT_OPTION and add it to the {@link Attribute}'s {@link Map}.<br>
     * The attribute's value is calculated with the translation file OGDRTranslationFile.properties see
     * {@link Jason2OgdrProductMetadataPlugin#TRANSACTION_FILE}.
     * @param fileMap a {@link Map} of the {@link File} to acquire
     * @param attributeValueMap {@link Map} of the {@link Attribute}
     * @throws PluginAcquisitionException an error occurs when the new {@link Attribute} is created
     */
    private void registerOptionAttribute(Map<File, ?> fileMap, Map<String, List<? extends Object>> attributeValueMap,
            Map<Integer, Attribute> attributeMap) throws PluginAcquisitionException {
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
        registerAttribute(attributeMap, PRODUCT_OPTION, optionAttribute);
        LOGGER.info("END building attribute " + PRODUCT_OPTION);
    }

    /**
     * Get the product's type for a set of {@link File}.<br>
     * The attribute's value is calculated with the translation file OGDRTranslationFile.properties see
     * {@link Jason2OgdrProductMetadataPlugin#TRANSACTION_FILE}.
     * @param acquisitionFileList the {@link File}'s {@link Map} for which to calculate the {@link Attribute}
     *            PRODUCT_OPTION.
     * @return the values's {@link List} calculated
     * @throws PluginAcquisitionException a {@link File} name is not respecting the expected pattern
     */
    protected List<String> getOptionValue(Collection<File> acquisitionFileList) throws PluginAcquisitionException {
        String value = null;
        for (final File file : acquisitionFileList) {
            final Matcher fileNameMatcher = fileNamePattern.matcher(file.getName());

            String productType;
            if (fileNameMatcher.matches()) {
                productType = fileNameMatcher.group(1);
            } else {
                throw new PluginAcquisitionException("unable to get productType from fileName " + file.getName());
            }

            // Load the translation for the productType
            final Properties translationProperties = LoadTranslationProperties.getInstance().load(TRANSACTION_FILE);

            final NetCdfFileHelper helper = new NetCdfFileHelper(file);
            String producer = helper.getGlobalAttributeStringValue("institution", null);
            productType = translationProperties.getProperty(productType);

            value = producer + " - " + productType;
        }
        final List<String> valueList = new ArrayList<>();
        valueList.add(value);
        return valueList;
    }

}
