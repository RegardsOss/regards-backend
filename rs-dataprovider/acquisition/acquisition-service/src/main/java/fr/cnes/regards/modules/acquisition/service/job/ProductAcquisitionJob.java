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
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobRuntimeException;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductState;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.service.IAcquisitionProcessingService;
import fr.cnes.regards.modules.acquisition.service.IProductService;
import fr.cnes.regards.modules.acquisition.service.job.step.AcquisitionCheckStep;
import fr.cnes.regards.modules.acquisition.service.job.step.AcquisitionScanStep;

/**
 * FIXME : revoir la doc en fonction de l'implémentation
 *
 * This class runs a set of step :<br>
 * <li>a step {@link AcquisitionScanStep} to scan and identify the {@link AcquisitionFile} to acquired
 * <li>a step {@link AcquisitionCheckStep} to check the {@link AcquisitionFile} and to determines the {@link Product}
 * associated<br>
 * And for each scanned {@link Product} not already send to Ingest microservice, and with its status equals to
 * {@link ProductState#COMPLETED} or {@link ProductState#FINISHED},
 * a new {@link JobInfo} of class {@link SIPGenerationJob} is create and queued.
 *
 * @author Christophe Mertz
 * @author Marc Sordi
 *
 */
public class ProductAcquisitionJob extends AbstractJob<Void> {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductAcquisitionJob.class);

    public static final String CHAIN_PARAMETER_ID = "chain";

    @Autowired
    private IProductService productService;

    @Autowired
    private IAcquisitionProcessingService processingService;

    /**
     * The current chain to work with!
     */
    private AcquisitionProcessingChain processingChain;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        Long acqProcessingChainId = getValue(parameters, CHAIN_PARAMETER_ID);
        try {
            processingChain = processingService.getChain(acqProcessingChainId);
        } catch (ModuleException e) {
            handleInvalidParameter(CHAIN_PARAMETER_ID, e.getMessage());
        }
    }

    @Override
    public void run() {

        try {
            // First step : scan and register files
            processingService.scanAndRegisterFiles(processingChain);
            // Second step : validate in progress files
            processingService.validateFiles(processingChain);
            // Third step : build products with valid files
            processingService.buildProducts(processingChain);

            // For each complete product, creates and schedules a job to generate SIP
            Set<Product> products = productService.findChainProductsToSchedule(processingChain);
            for (Product p : products) {
                productService.scheduleProductSIPGeneration(p, processingChain);
            }

            // Job is terminated ... release processing chain
            processingChain.setRunning(false);
            processingService.updateChain(processingChain);

        } catch (ModuleException e) {
            LOGGER.error("Business error", e);
            throw new JobRuntimeException(e);
        }
    }

    public AcquisitionProcessingChain getAcqProcessingChain() {
        return processingChain;
    }
}
