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
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductSIPState;
import fr.cnes.regards.modules.acquisition.domain.ProductState;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.plugins.ISipGenerationPlugin;
import fr.cnes.regards.modules.acquisition.service.IAcquisitionJobReportService;
import fr.cnes.regards.modules.acquisition.service.IAcquisitionProcessingService;
import fr.cnes.regards.modules.acquisition.service.IProductService;
import fr.cnes.regards.modules.ingest.domain.SIP;

/**
 * This job manages SIP generation for a given product
 *
 * @author Christophe Mertz
 * @author Marc Sordi
 *
 */
public class SIPGenerationJob extends AbstractJob<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SIPGenerationJob.class);

    public static final String CHAIN_PARAMETER_ID = "chain_id";

    public static final String PRODUCT_ID = "product_id";

    @Autowired
    private IProductService productService;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IAcquisitionProcessingService processingService;

    @Autowired
    private IAcquisitionJobReportService jobReportService;

    /**
     * The current chain to work with!
     */
    private AcquisitionProcessingChain processingChain;

    /**
     * The product used for SIP generation
     */
    private Product product;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {

        Long acqProcessingChainId = getValue(parameters, CHAIN_PARAMETER_ID);
        try {
            processingChain = processingService.getChain(acqProcessingChainId);
        } catch (ModuleException e) {
            handleInvalidParameter(CHAIN_PARAMETER_ID, e.getMessage());
        }

        Long productId = getValue(parameters, PRODUCT_ID);
        try {
            product = productService.loadProduct(productId);
        } catch (ModuleException e) {
            handleInvalidParameter(PRODUCT_ID, e.getMessage());
        }
    }

    @Override
    public void run() {
        LOGGER.info("[{}] : starting SIP generation job for the product <{}>", processingChain.getLabel(),
                    product.getProductName());

        try {

            // Report starting
            jobReportService.reportJobStarted(product.getLastSIPGenerationJobReport());

            // Get an instance of the plugin
            ISipGenerationPlugin generateSipPlugin = pluginService
                    .getPlugin(processingChain.getGenerateSipPluginConf().getId());
            // Launch generation plugin
            SIP sip = generateSipPlugin.generate(product);

            // FIXME add dataset tag if exists

            // Update product
            product.setSip(sip);
            product.setSipState(ProductSIPState.GENERATED);
            productService.save(product);

        } catch (ModuleException e) {
            LOGGER.error(e.getMessage(), e);
            // Update product
            product.setState(ProductState.ERROR);
            product.setSipState(ProductSIPState.GENERATION_ERROR);
            productService.save(product);
            throw new JobRuntimeException(e.getMessage());
        } finally {
            // Report stopping
            jobReportService.reportJobStopped(product.getLastSIPGenerationJobReport());
        }
    }
}
