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
import java.util.List;
import java.util.Map;
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
import fr.cnes.regards.modules.acquisition.exception.PluginAcquisitionException;
import fr.cnes.regards.modules.acquisition.plugins.properties.PluginsRepositoryProperties;

/**
 * Metadata caculation's plugin for PK4 products.
 * 
 * @author Christophe Mertz
 *
 */
@Plugin(description = "Metadata caculation's plugin for PK4 products", id = "Pk4OnBoardProductMetaDataPlugin", version = "1.0.0",
        author = "REGARDS Team", contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI",
        url = "https://github.com/RegardsOss")
public class Pk4OnBoardProductMetaDataPlugin extends AbstractProductMetadataPlugin {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Pk4OnBoardProductMetaDataPlugin.class);

    /**
     * Project name
     */
    private static final String PROJECT_NAME = "PK4";

    private static final String DISK_ID_ATTRIBUTE = "DISK_ID";

    private static final String DISK_ID_DEFAULT_VALUE = ".";

    private static final String PATH_PATTERN = ".*/(Disk[0-9]*)/.*";

    @Autowired
    private PluginsRepositoryProperties pluginsRepositoryProperties;

    @Override
    protected PluginsRepositoryProperties getPluginsRepositoryProperties() {
        return pluginsRepositoryProperties;
    }

    @Override
    protected String getProjectName() {
        return PROJECT_NAME;
    }

    @Override
    protected void doCreateIndependantSpecificAttributes(Map<File, ?> pFileMap, Map<Integer, Attribute> pAttributeMap)
            throws PluginAcquisitionException {

        final Pattern pattern = Pattern.compile(PATH_PATTERN);
        String diskParam = DISK_ID_DEFAULT_VALUE;

        // 1. Loop on product file and get unique Disk param value in path
        for (final File pFile : pFileMap.keySet()) {
            final File originalFile = (File) pFileMap.get(pFile);
            final String path = originalFile.getAbsolutePath();
            final Matcher matcher = pattern.matcher(path);
            if (matcher.matches()) {
                diskParam = matcher.group(1);
            } else {
                LOGGER.warn("Attribute DISK_ID NOT FOUND FOR FILE : " + path);
            }
        }

        // 2. Create DISK_ID Attribute
        final List<String> values = new ArrayList<>();
        values.add(diskParam);
        Attribute diskAttribute;
        try {
            diskAttribute = AttributeFactory.createAttribute(AttributeTypeEnum.TYPE_STRING, DISK_ID_ATTRIBUTE, values);
        } catch (final DomainModelException e) {
            final String msg = "unable to create attribute" + DISK_ID_ATTRIBUTE;
            throw new PluginAcquisitionException(msg, e);
        }

        // 3. Register attribute
        registerAttribute(pAttributeMap, DISK_ID_ATTRIBUTE, diskAttribute);
    }
}
