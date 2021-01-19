/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobRuntimeException;
import fr.cnes.regards.modules.ingest.dao.IOAISDeletionCreatorRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.deletion.OAISDeletionCreatorPayload;
import fr.cnes.regards.modules.ingest.domain.request.deletion.OAISDeletionCreatorRequest;
import fr.cnes.regards.modules.ingest.domain.request.deletion.OAISDeletionRequest;
import fr.cnes.regards.modules.ingest.domain.request.update.AbstractAIPUpdateTask;
import fr.cnes.regards.modules.ingest.service.aip.AIPDeletionService;
import fr.cnes.regards.modules.ingest.service.aip.IAIPService;
import fr.cnes.regards.modules.ingest.service.request.OAISDeletionService;
import fr.cnes.regards.modules.ingest.service.request.RequestService;

/**
 * This job creates {@link AbstractAIPUpdateTask} task to update. It scans AIP and create for each modification a task
 *
 * <br>This job cannot be interrupted as it is a creator for other jobs. It basically does nothing.
 *
 * @author Léo Mieulet
 */
public class OAISDeletionsCreatorJob extends AbstractJob<Void> {

    public static final String REQUEST_ID = "REQUEST_ID";

    private int totalPages = 0;

    @Autowired
    private IAIPService aipRepository;

    @Autowired
    private RequestService requestService;

    @Autowired
    private OAISDeletionService oaisDeletionRequestService;

    @Autowired
    private AIPDeletionService aipDeletionService;

    @Autowired
    private IOAISDeletionCreatorRepository oaisDeletionCreatorRepo;

    /**
     * Limit number of AIPs to retrieve in one page.
     */
    @Value("${regards.ingest.aips.scan.iteration-limit:1000}")
    private Integer aipIterationLimit;

    private OAISDeletionCreatorRequest deletionCreator;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        // Retrieve deletion request
        Long databaseId = getValue(parameters, REQUEST_ID);
        Optional<OAISDeletionCreatorRequest> oDeletionRequest = oaisDeletionRequestService.searchCreator(databaseId);

        if (!oDeletionRequest.isPresent()) {
            throw new JobRuntimeException(String.format("Unknown deletion request with id %d", databaseId));
        }

        deletionCreator = oDeletionRequest.get();
    }

    @Override
    public void run() {
        logger.debug("[OAIS DELETION CREATOR JOB] Running job ...");
        long start = System.currentTimeMillis();
        Pageable pageRequest = PageRequest.of(0, aipIterationLimit, Sort.Direction.ASC, "id");
        Page<AIPEntity> aipsPage;
        int nbRequestScheduled = 0;
        // Set the request as running
        deletionCreator.setState(InternalRequestState.RUNNING);
        oaisDeletionCreatorRepo.save(deletionCreator);
        do {
            OAISDeletionCreatorPayload deletionPayload = deletionCreator.getConfig();
            aipsPage = aipRepository.findByFilters(deletionPayload, pageRequest);
            logger.debug("[OAIS DELETION CREATOR JOB] Scheduling deletion of {} aips", aipsPage.getNumberOfElements());
            // Save number of pages to publish job advancement
            if (totalPages < aipsPage.getTotalPages()) {
                totalPages = aipsPage.getTotalPages();
            }
            // If deletion request is already registered for the given aip do not create a new one.
            List<AbstractRequest> requests = aipsPage.stream()
                    .filter(aip -> !aipDeletionService.deletionAlreadyPending(aip))
                    .map(aip -> OAISDeletionRequest.build(aip, deletionPayload.getDeletionMode(),
                                                          deletionPayload.getDeletePhysicalFiles()))
                    .collect(Collectors.toList());
            nbRequestScheduled += requestService.scheduleRequests(requests);
            if (totalPages > 0) {
                advanceCompletion();
            }
            pageRequest = pageRequest.next();
        } while (aipsPage.hasNext());
        // Delete the request
        requestService.deleteRequest(deletionCreator);

        logger.info("[OAIS DELETION CREATOR JOB] {} OAISDeletionRequest(s) scheduled in {}ms", nbRequestScheduled,
                    System.currentTimeMillis() - start);
    }

    @Override
    public int getCompletionCount() {
        return totalPages;
    }

}