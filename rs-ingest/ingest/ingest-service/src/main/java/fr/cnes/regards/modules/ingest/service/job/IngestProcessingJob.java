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
package fr.cnes.regards.modules.ingest.service.job;

import com.google.common.collect.Sets;
import com.google.gson.reflect.TypeToken;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.jobs.domain.step.IProcessingStep;
import fr.cnes.regards.framework.modules.jobs.domain.step.ProcessingStepException;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.ingest.dao.IIngestProcessingChainRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.chain.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequest;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.dto.aip.AIP;
import fr.cnes.regards.modules.ingest.dto.aip.StorageMetadata;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import fr.cnes.regards.modules.ingest.service.chain.step.*;
import fr.cnes.regards.modules.ingest.service.notification.IAIPNotificationService;
import fr.cnes.regards.modules.ingest.service.request.IIngestRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

import java.lang.reflect.Type;
import java.util.*;

/**
 * This job manages processing chain for AIP generation from a SIP
 *
 * @author Marc Sordi
 * @author Sébastien Binda
 */
public class IngestProcessingJob extends AbstractJob<Void> {

    public static final String CHAIN_NAME_PARAMETER = "chain";

    public static final String IDS_PARAMETER = "ids";

    private static final String INFO_TAB = "     >>>>>     ";

    @Autowired
    private AutowireCapableBeanFactory beanFactory;

    @Autowired
    private IIngestProcessingChainRepository processingChainRepository;

    @Autowired
    private IIngestRequestService ingestRequestService;

    @Autowired
    private INotificationClient notificationClient;

    @Autowired
    private IAIPNotificationService aipNotificationService;

    private IngestProcessingChain ingestChain;

    private List<IngestRequest> requests;

    /**
     * The request we are currently processing
     */
    private IngestRequest request;

    /**
     * The SIP entity we are currently working on
     */
    private SIPEntity currentEntity;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
        throws JobParameterMissingException, JobParameterInvalidException {

        // Load ingest requests
        Type type = new TypeToken<Set<Long>>() {

        }.getType();
        Set<Long> ids = getValue(parameters, IDS_PARAMETER, type);
        requests = ingestRequestService.loadByIds(ids);

        // Retrieve processing chain from parameters
        String processingChainName = getValue(parameters, CHAIN_NAME_PARAMETER);

        // Load processing chain
        Optional<IngestProcessingChain> chain = processingChainRepository.findOneByName(processingChainName);
        if (!chain.isPresent()) {
            String message = String.format("No related chain has been found for value \"%s\"", processingChainName);
            // Save error inside requests
            for (IngestRequest requestInError : requests) {
                requestInError.setErrors(Sets.newHashSet(message));
                requestInError.setState(InternalRequestState.ERROR);
            }
            // Monitoring
            ingestRequestService.handleUnknownChain(requests);
            handleInvalidParameter(CHAIN_NAME_PARAMETER, message);
        } else {
            ingestChain = chain.get();
        }
    }

    @Override
    public void run() {
        // Lets prepare a fex things in case there is errors
        StringJoiner notifMsg = new StringJoiner("\n");
        notifMsg.add("Errors occurred during SIPs processing using " + ingestChain.getName() + ":");

        // Internal initial step
        IProcessingStep<IngestRequest, SIPEntity> initStep = new InternalInitialStep(this, ingestChain);
        beanFactory.autowireBean(initStep);

        // Initializing steps
        // Step 1 : optional preprocessing
        IProcessingStep<SIP, SIP> preStep = new PreprocessingStep(this, ingestChain);
        beanFactory.autowireBean(preStep);
        // Step 2 : required validation
        IProcessingStep<SIP, Void> validationStep = new ValidationStep(this, ingestChain);
        beanFactory.autowireBean(validationStep);
        // Step 3 : required AIP generation
        IProcessingStep<SIPEntity, List<AIP>> generationStep = new GenerationStep(this, ingestChain);
        beanFactory.autowireBean(generationStep);
        // Step 4 : optional AIP update storage metadata
        IProcessingStep<List<StorageMetadata>, Void> aipStorageMetadataUpdateStep = new AipStorageMetadataUpdateStep(
            this,
            ingestChain);
        beanFactory.autowireBean(aipStorageMetadataUpdateStep);
        // Step 5 : optional AIP tagging
        IProcessingStep<List<AIP>, Void> taggingStep = new TaggingStep(this, ingestChain);
        beanFactory.autowireBean(taggingStep);
        /** Step 6 : optional postprocessing has to be run after storage ends. See {@link IngestPostProcessingJob}.  */

        // Internal final step
        IProcessingStep<List<AIP>, List<AIPEntity>> finalStep = new InternalFinalStep(this, ingestChain);
        beanFactory.autowireBean(finalStep);

        long start = System.currentTimeMillis();
        int sipIngested = 0;
        int sipInError = 0;

        // To notify again ingest request with NOTIFICATION_ERROR step
        Set<AbstractRequest> notificationRequests = Sets.newHashSet();

        for (IngestRequest request : requests) {
            //FIXME add logic to handle interruption
            this.request = request;
            try {
                long start2 = System.currentTimeMillis();
                List<AIP> aips;
                // retry the process only from the step needed
                switch (request.getStep()) {
                    case LOCAL_SCHEDULED, LOCAL_INIT, LOCAL_PRE_PROCESSING, LOCAL_VALIDATION, LOCAL_GENERATION, LOCAL_AIP_STORAGE_METADATA_UPDATE, LOCAL_TAGGING, LOCAL_POST_PROCESSING -> {
                        ingestRequestService.handleIngestJobStart(request);
                        // Internal preparation step (no plugin involved)
                        currentEntity = initStep.execute(request);
                        // Step 1 : optional preprocessing
                        SIP sip = preStep.execute(request.getSip());
                        // Propagate to entity
                        currentEntity.setSip(sip);
                        // Step 2 : required validation
                        validationStep.execute(sip);
                        // Step 3 : required AIP generation
                        aips = generationStep.execute(currentEntity);
                        // Step 4 : optional AIP update storage metadata
                        aipStorageMetadataUpdateStep.execute(request.getMetadata().getStorages());
                        // Step 5 : optional AIP tagging
                        taggingStep.execute(aips);
                        // Internal finalization step (no plugin involved)
                        // Do all persistence actions in this step
                        finalStep.execute(aips);
                    }
                    case LOCAL_FINAL, REMOTE_STORAGE_REQUESTED, REMOTE_STORAGE_ERROR, REMOTE_STORAGE_DENIED ->
                        // According to storage dev, it is better to simply request a new storage,
                        // if request already exists anyway it will be retried
                        ingestRequestService.requestRemoteStorage(request);
                    case REMOTE_NOTIFICATION_ERROR ->
                        // add request to list of requests to be notified again
                        notificationRequests.add(request);
                    default -> logger.debug(
                        "{}SIP \"{}\" ingestion has been retried and nothing had to be done in local",
                        INFO_TAB,
                        request.getSip().getId());
                }
                sipIngested++;
                logger.debug("{}SIP \"{}\" ingested in {} ms",
                             INFO_TAB,
                             request.getSip().getId(),
                             System.currentTimeMillis() - start2);

            } catch (ProcessingStepException e) {
                if (request.getState() != InternalRequestState.WAITING_VERSIONING_MODE
                    && request.getState() != InternalRequestState.IGNORED) {
                    logger.error("SIP \"{}\" ingestion error", request.getSip().getId());
                    sipInError++;
                    String msg = String.format("Error while ingesting SIP \"%s\" in request \"%s\"",
                                               request.getSip().getId(),
                                               request.getRequestId());
                    notifMsg.add(msg);
                    logger.error(msg);
                    logger.error("Ingestion step error", e);
                    // Continue with following SIPs
                } else {
                    logger.debug(e.getMessage(), e);
                }
            }
        }

        // notify if errors occurred
        if (sipInError > 0) {
            notificationClient.notify(notifMsg.toString(),
                                      "Error occurred during SIPs Ingestion.",
                                      NotificationLevel.INFO,
                                      DefaultRole.ADMIN);
            logger.error("{}{} SIP(s) INGESTED and {} in ERROR in {} ms",
                         INFO_TAB,
                         sipIngested,
                         sipInError,
                         System.currentTimeMillis() - start);
        } else {
            logger.info("{}{} SIP(s) INGESTED in {} ms", INFO_TAB, sipIngested, System.currentTimeMillis() - start);
        }

        // if there are requests to be notified again, send them to notifier
        if (!notificationRequests.isEmpty()) {
            aipNotificationService.sendRequestsToNotifier(notificationRequests);
        }
    }

    @Override
    public int getCompletionCount() {
        return 8;
    }

    public SIPEntity getCurrentEntity() {
        return currentEntity;
    }

    public IngestRequest getCurrentRequest() {
        return request;
    }
}
