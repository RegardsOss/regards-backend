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

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductStatus;
import fr.cnes.regards.modules.acquisition.plugins.IPostProcessSipPlugin;
import fr.cnes.regards.modules.acquisition.service.IExecAcquisitionProcessingChainService;
import fr.cnes.regards.modules.acquisition.service.IProductService;
import fr.cnes.regards.modules.acquisition.service.exception.AcquisitionException;
import fr.cnes.regards.modules.acquisition.service.exception.AcquisitionRuntimeException;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;
import fr.cnes.regards.modules.ingest.domain.event.SIPEvent;

/**
 * @author Christophe Mertz
 *
 */
public class PostSipAcquisitionStep extends AbstractStep implements IPostAcquisitionStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostSipAcquisitionStep.class);

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IProductService productService;

    @Autowired
    private IExecAcquisitionProcessingChainService execProcessingChainService;

    private AcquisitionProcessingChain acqProcessingChain;

    private Product product;

    private final SIPEvent sipEvent;

    public PostSipAcquisitionStep(SIPEvent sipEvent) {
        super();
        this.sipEvent = sipEvent;
    }

    @Override
    public void proceedStep() throws AcquisitionRuntimeException, AcquisitionException {

        if (acqProcessingChain == null) {
            String msg = "The chain generation is mandatory";
            LOGGER.error(msg);
            throw new AcquisitionRuntimeException(msg);
        }

        if (product == null) {
            throw new AcquisitionException("The product is mandatory");
        }

        LOGGER.info("[{}] Start POST acqusition SIP step for the product <{}>", acqProcessingChain.getSession(),
                    product.getProductName());

        // A plugin for the generate SIP configuration is required
        if (this.acqProcessingChain.getPostProcessSipPluginConf() == null) {
            throw new AcquisitionException(
                    "[" + this.acqProcessingChain.getLabel() + "] The required IPostProcessSipPlugin is missing");
        }

        // Launch the generate plugin
        try {
            // get an instance of the plugin
            IPostProcessSipPlugin postProcessPlugin = pluginService
                    .getPlugin(this.acqProcessingChain.getPostProcessSipPluginConf().getId());
            postProcessPlugin.runPlugin(product, acqProcessingChain);

            // Update ProductStatus to SAVED
            product.setStatus(ProductStatus.SAVED);
            productService.save(this.product);

            int nbSipStored = 0;
            int nbSipError = 0;
            if (sipEvent.getState().equals(SIPState.STORED)) {
                nbSipStored = 1;
            } else if (sipEvent.getState().equals(SIPState.STORE_ERROR)) {
                nbSipError = 1;
            }

            execProcessingChainService.updateExecProcessingChain(acqProcessingChain.getSession(), 0, nbSipStored, nbSipError);

        } catch (ModuleException e) {
            LOGGER.error(e.getMessage(), e);
            throw new AcquisitionException(e.getMessage());
        }

        LOGGER.info("[{}] Stop  POST acqusition SIP step for the product <{}>", acqProcessingChain.getSession(),
                    product.getProductName());

    }

    @Override
    public void getResources() throws AcquisitionException {
        this.product = process.getProduct();
        this.acqProcessingChain = process.getChainGeneration();
    }

    @Override
    public void freeResources() throws AcquisitionException { // NOSONAR
    }

    @Override
    public void stop() { // NOSONAR
    }

}
