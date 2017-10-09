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
package fr.cnes.regards.modules.acquisition.service.plugins;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.modules.acquisition.domain.metadata.dto.MetaProductDto;
import fr.cnes.regards.modules.acquisition.domain.metadata.dto.SetOfMetaFileDto;
import fr.cnes.regards.modules.acquisition.plugins.ICheckFilePlugin;

/**
 * 
 *
 * @author Christophe Mertz
 * @since 1.0-SNAPSHOT
 */
@Plugin(id = "BasicCheckFilePlugin", version = "1.0.0-SNAPSHOT", description = "BasicCheckFilePlugin",
        author = "REGARDS Team", contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI",
        url = "https://github.com/RegardsOss")
public class BasicCheckFilePlugin implements ICheckFilePlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(BasicCheckFilePlugin.class);

    public static final String META_PRODUCT_PARAM = "meta-produt";

    public static final String META_FILE_PARAM = "meta-file";

    public static final String CHAIN_GENERATION_PARAM = "chain";

    @PluginParameter(name = CHAIN_GENERATION_PARAM, optional = true)
    String chainLabel;

    @PluginParameter(name = META_PRODUCT_PARAM, optional = true)
    MetaProductDto metaProductDto;

    // TODO CMZ à voir si fonctionne avec Set<MetaFileDto>
    @PluginParameter(name = META_FILE_PARAM, optional = true)
    SetOfMetaFileDto metaFiles;

    protected String productName;

    protected String nodeIdentifier;

    @Override
    public boolean runPlugin(File fileToCheck, String dataSetId) throws ModuleException {

        LOGGER.info("Start checking for the chain <{}> ", chainLabel);

        productName = fileToCheck.getName();
        nodeIdentifier = dataSetId + " - " + productName;
        boolean result = false;

        // Check file exists
        if (fileToCheck.exists() && fileToCheck.canRead()) {
            nodeIdentifier = fileToCheck.getName();
            String name = fileToCheck.getName();
            int indexExtension = name.lastIndexOf('.');
            if (indexExtension > 0) {
                name = name.substring(0, indexExtension);
            }
            productName = name;
            result = true;
        } else {
            throw new ModuleException("Can't read file " + fileToCheck.getAbsolutePath());
        }

        LOGGER.info("End checking for the chain <{}> ", chainLabel);

        return result;
    }

    @Override
    public int getFileVersion() {
        return 1;
    }

    @Override
    public String getLogFile() {
        return null;
    }

    @Override
    public String getProductName() {
        return productName;
    }

    @Override
    public int getProductVersion() {
        return 0;
    }

    @Override
    public String getNodeIdentifier() {
        return nodeIdentifier;
    }

}
