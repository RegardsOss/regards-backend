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

package fr.cnes.regards.modules.acquisition.service.job;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobRuntimeException;
import fr.cnes.regards.framework.modules.plugins.service.PluginService;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductSIPState;
import fr.cnes.regards.modules.acquisition.domain.job.SIPEventJobParameter;
import fr.cnes.regards.modules.acquisition.plugins.IPostProcessSipPlugin;
import fr.cnes.regards.modules.acquisition.service.IAcquisitionProcessingChainService;
import fr.cnes.regards.modules.acquisition.service.IExecAcquisitionProcessingChainService;
import fr.cnes.regards.modules.acquisition.service.IProductService;
import fr.cnes.regards.modules.acquisition.service.step.IPostAcquisitionStep;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;
import fr.cnes.regards.modules.ingest.domain.event.SIPEvent;

/**
 * This job runs a set of step :<br>
 * <li>a step {@link IPostAcquisitionStep}
 *
 * This job runs for one {@link Product}
 *
 * @author Christophe Mertz
 *
 */
public class PostAcquisitionJob extends AbstractJob<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostAcquisitionJob.class);

    @Autowired
    private PluginService pluginService;

    @Autowired
    private IProductService productService;

    @Autowired
    private IExecAcquisitionProcessingChainService execProcessingChainService;

    @Autowired
    private IAcquisitionProcessingChainService acqProcessChainService;

    private SIPEvent sipEvent;

    @Override
    public void run() {
        LOGGER.info("Start POST acquisition SIP job for the product <{}>", sipEvent.getIpId());

        try {
            // Load product
            Product product = productService.retrieve(sipEvent.getIpId());
            // Retrieve acquisition chain
            AcquisitionProcessingChain acqProcessingChain = acqProcessChainService
                    .findByMetaProduct(product.getMetaProduct());
            // Launch post processing plugin if present
            if (acqProcessingChain.getPostProcessSipPluginConf().isPresent()) {
                LOGGER.info("[{}-{}] : starting post acquisition job for job {}", acqProcessingChain.getLabel(),
                            acqProcessingChain.getSession(), product.getProductName());
                // Get an instance of the plugin
                IPostProcessSipPlugin postProcessPlugin = pluginService
                        .getPlugin(acqProcessingChain.getPostProcessSipPluginConf().get().getId());
                postProcessPlugin.runPlugin(product, acqProcessingChain);
            } else {
                LOGGER.info("[{}-{}] : no post processing", acqProcessingChain.getLabel(),
                            acqProcessingChain.getSession());
            }

            // TODO manage event
            // Update product
            product.setSipState(ProductSIPState.INGESTED);
            productService.save(product);

            int nbSipStored = 0;
            int nbSipError = 0;
            if (sipEvent.getState().equals(SIPState.STORED)) {
                nbSipStored = 1;
            } else if (sipEvent.getState().equals(SIPState.STORE_ERROR)) {
                nbSipError = 1;
            }

            execProcessingChainService.updateExecProcessingChain(acqProcessingChain.getSession(), 0, nbSipStored,
                                                                 nbSipError);

            LOGGER.info("[{}] Stop  POST acqusition SIP step for the product <{}>", acqProcessingChain.getSession(),
                        product.getProductName());
        } catch (ModuleException pse) {
            LOGGER.error("Business error", pse);
            throw new JobRuntimeException(pse);
        }
    }

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        if (parameters.isEmpty()) {
            throw new JobParameterMissingException("No parameter provided");
        }
        if (parameters.size() != 1) {
            throw new JobParameterInvalidException("One parameter is expected");
        }

        for (JobParameter jp : parameters.values()) {
            if (SIPEventJobParameter.isCompatible(jp)) {
                sipEvent = jp.getValue();
            } else {
                throw new JobParameterInvalidException("Please use SIPEventJobParameter in place of JobParameter");
            }
        }
    }

}
