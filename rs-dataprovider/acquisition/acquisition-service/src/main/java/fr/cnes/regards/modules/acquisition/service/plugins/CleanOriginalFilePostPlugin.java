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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.ChainGeneration;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.plugins.IPostProcessSipPlugin;

/**
 * A default {@link Plugin} of type {@link IPostProcessSipPlugin}.
 *
 * @author Christophe Mertz
 */
@Plugin(id = "CleanOriginalFilePostPlugin", version = "1.0.0-SNAPSHOT", description = "CleanOriginalFilePostPlugin",
        author = "REGARDS Team", contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI",
        url = "https://github.com/RegardsOss")
public class CleanOriginalFilePostPlugin implements IPostProcessSipPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(CleanOriginalFilePostPlugin.class);

    @Override
    public void runPlugin(Product product, ChainGeneration chain) throws ModuleException {
        LOGGER.info("[{}] Start post processing for the product : {}", chain.getSession(), product.getProductName());

        if (product.getMetaProduct().getCleanOriginalFile()) {
            for (AcquisitionFile acqFile : product.getAcquisitionFile()) {
                acqFile.getFile().delete();
                LOGGER.debug("[{}] The file {} has been deleted", chain.getSession(), acqFile.getFileName());
            }
        }

        LOGGER.info("[{}] End  post processing for the product : {}", chain.getSession(), product.getProductName());
    }

}
