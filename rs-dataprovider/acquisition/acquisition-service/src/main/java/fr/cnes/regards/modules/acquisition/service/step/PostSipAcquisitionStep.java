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

package fr.cnes.regards.modules.acquisition.service.step;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.acquisition.domain.ChainGeneration;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.plugins.IPostProcessSipPlugin;
import fr.cnes.regards.modules.acquisition.service.IProductService;
import fr.cnes.regards.modules.acquisition.service.exception.AcquisitionException;
import fr.cnes.regards.modules.acquisition.service.exception.AcquisitionRuntimeException;

/**
 * @author Christophe Mertz
 *
 */
@MultitenantTransactional
@Service
public class PostSipAcquisitionStep extends AbstractStep implements IPostAcquisitionStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostSipAcquisitionStep.class);

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IProductService productService;

    private ChainGeneration chainGeneration;

    private Product product;

    @Override
    public void proceedStep() throws AcquisitionRuntimeException {

        if (chainGeneration == null || product == null) {
            String msg = "The chain generation and the product are mandatory";
            LOGGER.error(msg);
            throw new AcquisitionRuntimeException(msg);
        }

        LOGGER.info("[{}] Start POST acqusition SIP step for the product <{}>", chainGeneration.getSession(),
                    product.getProductName());

        // A plugin for the generate SIP configuration is required
        if (this.chainGeneration.getPostProcessSipPluginConf() == null) {
            String msg = "[" + this.chainGeneration.getLabel() + "] The required IPostProcessSipPlugin is missing";
            LOGGER.error(msg);
            throw new AcquisitionRuntimeException(msg);
        }

        // Launch the generate plugin
        try {
            // get an instance of the plugin
            IPostProcessSipPlugin postProcessPlugin = pluginService
                    .getPlugin(this.chainGeneration.getPostProcessSipPluginConf().getId());
            postProcessPlugin.runPlugin(product, chainGeneration);
            productService.save(this.product);
        } catch (ModuleException e) {
            LOGGER.error(e.getMessage(), e);
            throw new AcquisitionRuntimeException(e.getMessage());
        }

        LOGGER.info("[{}] Stop  POST acqusition SIP step for the product <{}>", chainGeneration.getSession(),
                    product.getProductName());

    }

    @Override
    public void getResources() throws AcquisitionException {
        this.product = process.getProduct();
        this.chainGeneration = process.getChainGeneration();
    }

    @Override
    public void freeResources() throws AcquisitionException {
    }

    @Override
    public void stop() {
    }

    @Override
    public String getName() {
        return this.getClass().getCanonicalName();
    }

}
