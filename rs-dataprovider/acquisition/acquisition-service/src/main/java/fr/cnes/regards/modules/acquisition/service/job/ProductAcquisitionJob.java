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

import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobRuntimeException;
import fr.cnes.regards.modules.acquisition.domain.ProductState;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.service.IAcquisitionProcessingService;

/**
 * This class manages data driven product creation using following steps :
 * <ul>
 * <li>Scanning and file registering</li>
 * <li>File validation</li>
 * <li>Product creation</li>
 * </ul>
 *
 * And at the end, for all {@link ProductState#COMPLETED} or {@link ProductState#FINISHED} products of the current
 * processing chain, {@link SIPGenerationJob} are scheduled.
 *
 * @author Christophe Mertz
 * @author Marc Sordi
 *
 */
public class ProductAcquisitionJob extends AbstractJob<Void> {

    public static final String CHAIN_PARAMETER_ID = "chain";

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
            handleInvalidParameter(CHAIN_PARAMETER_ID, e);
        }
    }

    @Override
    public void run() {

        try {
            // First step : scan and register files
            processingService.scanAndRegisterFiles(processingChain);
            // Second step : validate in progress files
            processingService.manageRegisteredFiles(processingChain);
            // Third step : restart interrupted jobs
            processingService.restartInterruptedJobs(processingChain);
        } catch (ModuleException e) {
            logger.error("Business error", e);
            throw new JobRuntimeException(e);
        } finally {
            // Job is terminated ... release processing chain
            processingService.unlockChain(processingChain.getId());
        }
    }

    public AcquisitionProcessingChain getAcqProcessingChain() {
        return processingChain;
    }
}
