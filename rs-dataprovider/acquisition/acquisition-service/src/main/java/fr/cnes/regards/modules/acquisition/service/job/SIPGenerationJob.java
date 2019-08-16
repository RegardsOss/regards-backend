/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.modules.acquisition.service.session.SessionNotifier;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

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
import fr.cnes.regards.modules.ingest.domain.SIP;

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
        try {
            products = productService.retrieve(productNames);
        } catch (ModuleException e) {
            handleInvalidParameter(PRODUCT_NAMES, e);
        }
    }

    @Override
    public void run() {
        logger.debug("[{}] : starting SIP generation job of {} product(s)", processingChain.getLabel(),
                     products.size());
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

        Set<String> sessions = new HashSet<>();

        // Launch generation plugin
        for (Product product : products) {
            if (Thread.currentThread().isInterrupted()) {
                debugInterruption = "before thread interruption";
                break;
            }
            logger.trace("Generating SIP for product {}", product.getProductName());
            sessions.add(product.getSession());
            try {
                SIP sip = generateSipPlugin.generate(product);
                sessionNotifier.notifySipGenerationSucceed(product);
                // Update product
                product.setSip(sip);
                product.setSipState(ProductSIPState.SUBMITTED);
                productService.saveAndSubmitSIP(product);
                generatedCount++;
            } catch (ModuleException e) {
                String message = String.format("Error while generating product \"%s\"", product.getProductName());
                // Message already logged!
                logger.debug(message, e);
                product.setSipState(ProductSIPState.GENERATION_ERROR);
                product.setError(e.getMessage());
                sessionNotifier.notifySipGenerationError(product);
                productService.save(product);
            }
        }

        for (String session : sessions) {
            if (productService.countSIPGenerationJobInfoByProcessingChainAndSipStateIn(
                    processingChain, ProductSIPState.SCHEDULED) == 0) {
                sessionNotifier.notifyEndingChain(processingChain.getLabel(), session);
                break;
            }
        }

        logger.debug("[{}] : {} SIP(s) generated in {} milliseconds {}", processingChain.getLabel(), generatedCount,
                     System.currentTimeMillis() - startTime, debugInterruption);

    }
}
