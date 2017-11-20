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
import fr.cnes.regards.modules.acquisition.plugins.ICheckFilePlugin;

/**
 * This {@link Plugin} checks that the {@link File} exists and can be read.<br>
 * The product name is the file name less the file extension and the product name length is 128 char maximum.
 * 
 * @author Christophe Mertz
 *
 */
@Plugin(description = "CheckInPlugin", id = "CheckInPlugin", version = "1.0.0", author = "REGARDS Team",
        contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI", url = "https://github.com/RegardsOss")
public class CheckInPlugin implements ICheckFilePlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckInPlugin.class);

    public static final String CHAIN_GENERATION_PARAM = "chain-label";

    protected static final int PRODUCT_NAME_MAX_SIZE = 128;

    @PluginParameter(name = CHAIN_GENERATION_PARAM, optional = true)
    private String chainLabel;

    protected String productName;

    public CheckInPlugin() {
        super();
    }

    @Override
    public boolean runPlugin(File fileToCheck, String dataSetId) throws ModuleException {
        LOGGER.info("Start check file <{}> for the chain <{}>", fileToCheck.getAbsoluteFile(), chainLabel);
        boolean result = false;

        // Check file exists
        if (fileToCheck.exists() && fileToCheck.canRead()) {
            String name = fileToCheck.getName();

            // Delete extension if any
            int indexExtension = name.lastIndexOf('.');
            if (indexExtension > 0) {
                name = name.substring(0, indexExtension);
            }

            if (name.length() > PRODUCT_NAME_MAX_SIZE) {
                productName = name.substring(0, PRODUCT_NAME_MAX_SIZE);
            } else {
                productName = name;
            }

            result = true;
        } else {
            LOGGER.error("Can't read file <{}>", fileToCheck.getAbsolutePath());
        }

        LOGGER.info("End check file <{}> for the chain <{}>", fileToCheck.getAbsoluteFile(), chainLabel);

        return result;
    }

    @Override
    public String getProductName() {
        return productName;
    }
}
