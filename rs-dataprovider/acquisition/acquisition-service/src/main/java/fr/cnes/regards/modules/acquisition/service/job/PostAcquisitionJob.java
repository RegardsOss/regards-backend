/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobRuntimeException;
import fr.cnes.regards.framework.modules.plugins.service.PluginService;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.plugins.ISipPostProcessingPlugin;
import fr.cnes.regards.modules.acquisition.service.IProductService;
import fr.cnes.regards.modules.ingest.domain.event.SIPEvent;

/**
 * This job runs a set of step :<br>
 * <li>a step {@link IPostAcquisitionStep}
 *
 * This job runs for one {@link Product}
 *
 * @author Christophe Mertz
 * @author Marc Sordi
 *
 */
public class PostAcquisitionJob extends AbstractJob<Void> {

    public static final String EVENT_PARAMETER = "event";

    @Autowired
    private PluginService pluginService;

    @Autowired
    private IProductService productService;

    private SIPEvent sipEvent;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        sipEvent = getValue(parameters, EVENT_PARAMETER);
    }

    @Override
    public void run() {
        logger.info("Start POST acquisition SIP job for the product <{}>", sipEvent.getIpId());

        try {
            // Load product
            Optional<Product> oProduct = productService.searchProduct(sipEvent.getIpId());

            if (oProduct.isPresent()) {
                Product product = oProduct.get();

                // Update product (store ingest state)
                product.setSipState(sipEvent.getState());
                productService.save(product);

                // Retrieve acquisition chain
                AcquisitionProcessingChain acqProcessingChain = product.getProcessingChain();
                // Launch post processing plugin if present
                if (acqProcessingChain.getPostProcessSipPluginConf().isPresent()) {
                    // Get an instance of the plugin
                    ISipPostProcessingPlugin postProcessPlugin = pluginService
                            .getPlugin(acqProcessingChain.getPostProcessSipPluginConf().get().getId());
                    postProcessPlugin.postProcess(product);
                }
            } else {
                logger.debug("No product associated to SIP id\"{}\"", sipEvent.getIpId());
            }
        } catch (ModuleException pse) {
            logger.error("Business error", pse);
            throw new JobRuntimeException(pse);
        }
    }

}
