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
package fr.cnes.regards.modules.ingest.service.chain;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobRuntimeException;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.ingest.dao.IIngestProcessingChainRepository;
import fr.cnes.regards.modules.ingest.dao.ISIPRepository;
import fr.cnes.regards.modules.ingest.domain.SIP;
import fr.cnes.regards.modules.ingest.domain.entity.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.exception.ProcessingStepException;
import fr.cnes.regards.modules.ingest.service.chain.step.GenerationStep;
import fr.cnes.regards.modules.ingest.service.chain.step.IProcessingStep;
import fr.cnes.regards.modules.ingest.service.chain.step.PostprocessingStep;
import fr.cnes.regards.modules.ingest.service.chain.step.PreprocessingStep;
import fr.cnes.regards.modules.ingest.service.chain.step.StoreStep;
import fr.cnes.regards.modules.ingest.service.chain.step.TaggingStep;
import fr.cnes.regards.modules.ingest.service.chain.step.ValidationStep;
import fr.cnes.regards.modules.storage.domain.AIP;

/**
 * This job manages processing chain for AIP generation from a SIP
 *
 * @author Marc Sordi
 * @author Sébastien Binda
 */
public class IngestProcessingJob extends AbstractJob<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(IngestProcessingJob.class);

    public static final String CHAIN_NAME_PARAMETER = "chain";

    public static final String SIP_PARAMETER = "sip";

    @Autowired
    private IIngestProcessingChainRepository processingChainRepository;

    @Autowired
    private ISIPRepository sipRepository;

    @Autowired
    private IPluginService pluginService;

    private IngestProcessingChain processingChain;

    private SIPEntity entity;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {

        // Retrieve processing chain from parameters
        String processingChainName = getValue(parameters, CHAIN_NAME_PARAMETER);

        // Load processing chain
        Optional<IngestProcessingChain> chain = processingChainRepository.findOneByName(processingChainName);
        if (!chain.isPresent()) {
            String message = String.format("No related chain has been found for value \"%s\"", processingChainName);
            handleInvalidParameter(CHAIN_NAME_PARAMETER, message);
        } else {
            processingChain = chain.get();
        }

        // Load SIPEntity
        Long id = getValue(parameters, SIP_PARAMETER);
        entity = sipRepository.findOne(id);
        if (entity == null) {
            String message = String.format("No sip found for id \"%s\"", id);
            handleInvalidParameter(SIP_PARAMETER, message);
        }

        LOGGER.info("Parameters set for SIP \"{}\" (\"{}\") and processing chain \"{}\"", entity.getIpId(),
                    entity.getSipId(), processingChainName);
    }

    @Override
    public void run() {

        LOGGER.debug("Launching processing chain \"{}\" for SIP \"{}\"", processingChain.getName(), entity.getIpId());

        try {
            // Step 1 : optional preprocessing
            IProcessingStep<SIP, SIP> step = new PreprocessingStep(this);
            SIP sip = step.execute(entity.getSip());
            // Step 2 : required validation
            IProcessingStep<SIP, Void> validationStep = new ValidationStep(this);
            validationStep.execute(sip);
            // Step 3 : required AIP generation
            IProcessingStep<SIP, List<AIP>> generationStep = new GenerationStep(this);
            List<AIP> aips = generationStep.execute(sip);
            // Step 4 : optional AIP tagging
            IProcessingStep<List<AIP>, Void> taggingStep = new TaggingStep(this);
            taggingStep.execute(aips);
            // Step 5
            IProcessingStep<List<AIP>, Void> storeStep = new StoreStep(this);
            storeStep.execute(aips);
            // Step 6 : optional postprocessing
            IProcessingStep<SIP, Void> postprocessingStep = new PostprocessingStep(this);
            postprocessingStep.execute(sip);
        } catch (ProcessingStepException e) {
            LOGGER.error("Business error", e);
            throw new JobRuntimeException(e);
        }
    }

    @Override
    public int getCompletionCount() {
        return 6;
    }

    public IPluginService getPluginService() {
        return pluginService;
    }

    public IngestProcessingChain getProcessingChain() {
        return processingChain;
    }

    public SIPEntity getEntity() {
        return entity;
    }

    public ISIPRepository getSIPRepository() {
        return sipRepository;
    }
}
