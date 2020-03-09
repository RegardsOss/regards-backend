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
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobRuntimeException;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductSIPState;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.plugins.ISipGenerationPlugin;
import fr.cnes.regards.modules.acquisition.service.IAcquisitionProcessingService;
import fr.cnes.regards.modules.acquisition.service.IProductService;
import fr.cnes.regards.modules.acquisition.service.session.SessionNotifier;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;

/**
 * This job manages SIP generation for a given product
 *
 * @author Christophe Mertz
 * @author Marc Sordi
 *
 */
public class SIPGenerationJob extends AbstractJob<Void> {

    public static final String CHAIN_PARAMETER_ID = "chain_id";

    public static final String PRODUCT_NAMES = "product_names";

    @Autowired
    private IProductService productService;

    @Autowired
    private SessionNotifier sessionNotifier;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IAcquisitionProcessingService processingService;

    /**
     * The current chain to work with!
     */
    private AcquisitionProcessingChain processingChain;

    /**
     * The product used for SIP generation
     */
    private Set<Product> products;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {

        Long acqProcessingChainId = getValue(parameters, CHAIN_PARAMETER_ID);
        try {
            processingChain = processingService.getChain(acqProcessingChainId);
        } catch (ModuleException e) {
            handleInvalidParameter(CHAIN_PARAMETER_ID, e);
        }

        Set<String> productNames = getValue(parameters, PRODUCT_NAMES);
        products = productService.retrieve(productNames);
    }

    @Override
    public void run() {
        logger.info("[{}] : starting SIP generation job of {} product(s)", processingChain.getLabel(), products.size());
        long startTime = System.currentTimeMillis();
        int generatedCount = 0; // Effectively count generated products in case of interruption
        String debugInterruption = "";

        ISipGenerationPlugin generateSipPlugin;
        try {
            // Get an instance of the plugin
            generateSipPlugin = pluginService.getPlugin(processingChain.getGenerateSipPluginConf().getBusinessId());
        } catch (ModuleException | NotAvailablePluginConfigurationException e) {
            // Throw a global job error, do not iterate on products
            logger.error(e.getMessage(), e);
            throw new JobRuntimeException(e.getMessage());
        }

        Set<Product> success = Sets.newHashSet();
        Set<Product> errors = Sets.newHashSet();

        for (Product product : products) {
            if (Thread.currentThread().isInterrupted()) {
                debugInterruption = "before thread interruption";
                break;
            }
            logger.trace("Generating SIP for product {}", product.getProductName());
            try {
                // Launch generation plugin
                SIP sip = generateSipPlugin.generate(product);
                // Update product
                sessionNotifier.notifyChangeProductState(product, ProductSIPState.SUBMITTED);
                product.setSip(sip);
                product.setSipState(ProductSIPState.SUBMITTED);
                success.add(product);
                generatedCount++;
            } catch (Exception e) {
                String message = String.format("Error while generating product \"%s\"", product.getProductName());
                logger.error(message, e);
                sessionNotifier.notifyChangeProductState(product, ProductSIPState.GENERATION_ERROR);
                product.setSipState(ProductSIPState.GENERATION_ERROR);
                product.setError(e.getMessage());
                errors.add(product);
            }
        }

        productService.handleGeneratedProducts(processingChain, success, errors);
        success.clear();
        errors.clear();

        logger.info("[{}] : {} SIP(s) generated in {} milliseconds {}", processingChain.getLabel(), generatedCount,
                    System.currentTimeMillis() - startTime, debugInterruption);
        products.clear();
    }
}
