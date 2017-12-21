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

import java.util.List;
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
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductSIPState;
import fr.cnes.regards.modules.acquisition.domain.ProductState;
import fr.cnes.regards.modules.acquisition.domain.job.AcquisitionProcessingChainJobParameter;
import fr.cnes.regards.modules.acquisition.domain.job.ProductJobParameter;
import fr.cnes.regards.modules.acquisition.plugins.IGenerateSIPPlugin;
import fr.cnes.regards.modules.acquisition.service.IAcquisitionFileService;
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

    @Autowired
    private IProductService productService;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IAcquisitionFileService acquisitionFileService;

    private AcquisitionProcessingChain acqProcessingChain;

    private String productName;

    @Override
    public void run() {
        LOGGER.info("[{}-{}] : starting SIP generation job for the product <{}>", acqProcessingChain.getLabel(),
                    acqProcessingChain.getSession(), productName);

        // Load the product
        Product product;
        try {
            product = productService.retrieve(productName);
        } catch (ModuleException e) {
            LOGGER.error("Cannot load product", e);
            throw new JobRuntimeException(e.getMessage());
        }

        try {
            // Build the plugin parameters
            PluginParametersFactory factory = PluginParametersFactory.build();
            for (Map.Entry<String, String> entry : this.acqProcessingChain.getGenerateSipParameter().entrySet()) {
                factory.addParameter(entry.getKey(), entry.getValue());
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("[{}] Add parameter <{}> with value : {}", acqProcessingChain.getSession(),
                                 entry.getKey(), entry.getValue());
                }
            }

            // Get an instance of the plugin
            IGenerateSIPPlugin generateSipPlugin = pluginService
                    .getPlugin(acqProcessingChain.getGenerateSipPluginConf().getId(), factory.asArray());

            // Retrieve files
            List<AcquisitionFile> acqFiles = acquisitionFileService.findByProduct(productName);

            // Launch generation plugin
            SIP sip = generateSipPlugin.runPlugin(acqFiles, Optional.of(acqProcessingChain.getDataSet()));

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
        }
    }

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        if (parameters.isEmpty()) {
            throw new JobParameterMissingException("No parameter provided");
        }
        if (parameters.size() != 2) {
            throw new JobParameterInvalidException("Two parameters are expected.");
        }

        for (JobParameter jp : parameters.values()) {
            if (AcquisitionProcessingChainJobParameter.isCompatible(jp)) {
                acqProcessingChain = jp.getValue();
            } else {
                if (ProductJobParameter.isCompatible(jp)) {
                    productName = jp.getValue();
                } else {
                    throw new JobParameterInvalidException(
                            "Please use AcquisitionProcessingChainJobParameter or ProductJobParameter in place of JobParameter");
                }
            }
        }
    }

}
