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
package fr.cnes.regards.modules.ingest.service.job;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.reflect.TypeToken;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.jobs.domain.step.IProcessingStep;
import fr.cnes.regards.framework.modules.jobs.domain.step.ProcessingStepException;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.ingest.dao.IIngestProcessingChainRepository;
import fr.cnes.regards.modules.ingest.domain.SIP;
import fr.cnes.regards.modules.ingest.domain.entity.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;
import fr.cnes.regards.modules.ingest.service.chain.IIngestProcessingService;
import fr.cnes.regards.modules.ingest.service.chain.step.GenerationStep;
import fr.cnes.regards.modules.ingest.service.chain.step.PostprocessingStep;
import fr.cnes.regards.modules.ingest.service.chain.step.PreprocessingStep;
import fr.cnes.regards.modules.ingest.service.chain.step.StoreStep;
import fr.cnes.regards.modules.ingest.service.chain.step.TaggingStep;
import fr.cnes.regards.modules.ingest.service.chain.step.ValidationStep;
import fr.cnes.regards.modules.storage.domain.AIP;

/**
 * This job manages processing chain for AIP generation from a SIP
 * @author Marc Sordi
 * @author Sébastien Binda
 */
public class IngestProcessingJob extends AbstractJob<Void> {

    public static final String CHAIN_NAME_PARAMETER = "chain";

    public static final String IDS_PARAMETER = "ids";

    @Autowired
    private IIngestProcessingChainRepository processingChainRepository;

    @Autowired
    private IPluginService pluginService;

    private IngestProcessingChain processingChain;

    @Autowired
    private IIngestProcessingService ingestProcessingService;

    @Autowired
    private IPublisher publisher;

    private Set<SIPEntity> entities;

    /**
     * Entity at work
     */
    private SIPEntity currentEntity;

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

        // Load entities
        Type type = new TypeToken<Set<Long>>() {
        }.getType();
        Set<Long> ids = getValue(parameters, IDS_PARAMETER, type);
        entities = ingestProcessingService.getAllSipEntities(ids);
    }

    @Override
    public void run() {

        //        super.logger.debug("Launching processing chain \"{}\" for SIP \"{}\"", processingChain.getName(),
        //                           entity.getSipId());

        // Initializing steps
        // Step 1 : optional preprocessing
        IProcessingStep<SIP, SIP> preStep = new PreprocessingStep(this);
        // Step 2 : required validation
        IProcessingStep<SIP, Void> validationStep = new ValidationStep(this);
        // Step 3 : required AIP generation
        IProcessingStep<SIP, List<AIP>> generationStep = new GenerationStep(this);
        // Step 5 : optional AIP tagging
        IProcessingStep<List<AIP>, Void> taggingStep = new TaggingStep(this);
        // Step 6
        IProcessingStep<List<AIP>, Void> storeStep = new StoreStep(this);
        // Step 7 : optional postprocessing
        IProcessingStep<SIP, Void> postprocessingStep = new PostprocessingStep(this);

        for (SIPEntity entity : entities) {
            currentEntity = entity;
            try {
                // Step 1 : optional preprocessing
                SIP sip = preStep.execute(entity.getSip());
                // Step 2 : required validation
                validationStep.execute(sip);
                // Step 3 : required AIP generation
                List<AIP> aips = generationStep.execute(sip);
                // Step 4 : Save the session inside the AIP - no plugin involved
                aips = setSessionOnAips(entity, aips);
                // Step 5 : optional AIP tagging
                taggingStep.execute(aips);
                // Step 6
                storeStep.execute(aips);
                // Step 7 : optional postprocessing
                postprocessingStep.execute(sip);
            } catch (ProcessingStepException e) {
                super.logger.error("Error while ingesting SIP \"{}\" with provider id \"{}\"", currentEntity.getSipId(),
                                   currentEntity.getProviderId());
                super.logger.error("Ingestion step error", e);
                // Continuing with following SIPs
            }
        }
    }

    /**
     * Save the current session inside AIPs provenance info
     * @param aips list of aips
     * @return the updated list
     */
    private List<AIP> setSessionOnAips(SIPEntity entity, List<AIP> aips) {
        for (AIP aip : aips) {
            aip.getProperties().getPdi().getProvenanceInformation().setSession(entity.getSession().getId());
        }
        return aips;
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

    public IIngestProcessingService getIngestProcessingService() {
        return ingestProcessingService;
    }

    public IPublisher getPublisher() {
        return publisher;
    }

    public SIPEntity getCurrentEntity() {
        return currentEntity;
    }
}
