/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.plugins.ISipPostProcessingPlugin;
import fr.cnes.regards.modules.acquisition.service.IProductService;
import fr.cnes.regards.modules.ingest.client.RequestInfo;

/**
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

    private RequestInfo info;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        info = getValue(parameters, EVENT_PARAMETER);
    }

    @Override
    public void run() {
        logger.info("Start POST acquisition SIP job for the product <{}>", info.getProviderId());

        try {
            // Load product
            Optional<Product> oProduct = productService.searchProduct(info.getProviderId());

            if (oProduct.isPresent()) {
                Product product = oProduct.get();

                // Retrieve acquisition chain
                AcquisitionProcessingChain acqProcessingChain = product.getProcessingChain();
                // Launch post processing plugin if present
                if (acqProcessingChain.getPostProcessSipPluginConf().isPresent()) {
                    // Get an instance of the plugin
                    ISipPostProcessingPlugin postProcessPlugin;
                    try {
                        postProcessPlugin = pluginService
                                .getPlugin(acqProcessingChain.getPostProcessSipPluginConf().get().getId());
                        postProcessPlugin.postProcess(product);
                    } catch (NotAvailablePluginConfigurationException e) {
                        LOGGER.warn("Unable to run postprocess plugin as it is disabled");
                        LOGGER.warn(e.getMessage(), e);
                    }

                }
            } else {
                logger.debug("No product associated to SIP id\"{}\"", info.getSipId());
            }
        } catch (ModuleException pse) {
            logger.error("Business error", pse);
            throw new JobRuntimeException(pse);
        }
    }

}
