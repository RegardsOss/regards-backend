/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobRuntimeException;
import fr.cnes.regards.modules.acquisition.domain.ProductState;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.service.IAcquisitionProcessingService;
import fr.cnes.regards.modules.acquisition.service.IProductService;
import fr.cnes.regards.modules.acquisition.service.session.SessionNotifier;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.Optional;

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

    public static final String CHAIN_PARAMETER_SESSION = "session";

    public static final String CHAIN_PARAMETER_ONLY_ERRORS = "onlyErr";

    @Autowired
    private IAcquisitionProcessingService processingService;

    @Autowired
    private IProductService productService;

    @Autowired
    private SessionNotifier sessionNotifier;

    /**
     * The current chain to work with!
     */
    private AcquisitionProcessingChain processingChain;

    /**
     * The current session
     */
    private String session;

    /**
     * Only retry generation errors ?
     */
    private Boolean onlyErrors = Boolean.FALSE;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        Long acqProcessingChainId = getValue(parameters, CHAIN_PARAMETER_ID);
        session = getValue(parameters, CHAIN_PARAMETER_SESSION);
        onlyErrors = getValue(parameters, CHAIN_PARAMETER_ONLY_ERRORS);
        try {
            processingChain = processingService.getChain(acqProcessingChainId);
        } catch (ModuleException e) {
            handleInvalidParameter(CHAIN_PARAMETER_ID, e);
        }
    }

    @Override
    public void run() {
        if (!processingService.hasExecutionBlockers(processingChain, true)) {
            try {
                sessionNotifier.notifyStartingChain(processingChain.getLabel(), session);
                // Trying to restart products that fail during SIP generation
                if (Boolean.TRUE.equals(onlyErrors)) {
                    processingService.retrySIPGeneration(processingChain, Optional.of(session));
                } else {
                    // Restart interrupted jobs
                    processingService.restartInterruptedJobs(processingChain);
                    // Nominal process
                    // First step : scan and register files
                    processingService.scanAndRegisterFiles(processingChain, session);
                    // Second step : validate in progress files, build and
                    // schedule SIP generation for newly completed or finished products
                    processingService.manageRegisteredFiles(processingChain, session);
                    // Third step : compute new product state for already completed or finished products and schedule SIP generation.
                    // Doing this in a third step and not within the second one allows us to
                    // schedule update products only once for SIP generation
                    productService.manageUpdatedProducts(processingChain);
                }
            } catch (ModuleException e) {
                logger.error("Business error", e);
                throw new JobRuntimeException(e);
            } finally {
                sessionNotifier.notifyEndingChain(processingChain.getLabel(), session);
                processingService.unlockChain(processingChain.getId());
            }
        }
        // Job is terminated ... release processing chain
        processingService.unlockChain(processingChain.getId());
    }

    public AcquisitionProcessingChain getAcqProcessingChain() {
        return processingChain;
    }
}
